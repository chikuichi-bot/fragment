<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

$action = isset($_GET['action']) ? $_GET['action'] : 'random';
$mode = isset($_GET['mode']) ? $_GET['mode'] : 'short'; // short, long, both

// 🌟 フォルダのパス設定
$short_dir = __DIR__; // 現在のフォルダ (fragments/)
$long_dir = __DIR__ . '/literary_fragments_long.db'; // 長文用のフォルダ

// 🌟 モードに合わせて検索対象のデータベースファイルを取得
$db_files = [];
if ($mode === 'short' || $mode === 'both') {
    $short_files = glob($short_dir . "/fragments_part_*.db");
    if ($short_files) {
        $db_files = array_merge($db_files, $short_files);
    }
}
if ($mode === 'long' || $mode === 'both') {
    $long_files = glob($long_dir . "/fragments_part_*.db");
    if ($long_files) {
        $db_files = array_merge($db_files, $long_files);
    }
}

if (empty($db_files)) {
    echo json_encode(["error" => "Database parts not found."]);
    exit;
}

// ---------------------------------------------------------
// アクション：ランダム取得 (おみくじ・気配)
// ---------------------------------------------------------
if ($action === 'random') {
    $random_db = $db_files[array_rand($db_files)];
    $pdo = new PDO("sqlite:" . $random_db);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->exec("PRAGMA synchronous = OFF");
    $pdo->exec("PRAGMA journal_mode = MEMORY");
    
    $stmt = $pdo->query("SELECT MAX(id) as max_id FROM quotes");
    $max_id = (int)$stmt->fetch(PDO::FETCH_ASSOC)['max_id'];
    $random_id = mt_rand(1, $max_id);
    
    // すでにファイルで長さが分かれているので、文字数制限なしで最速取得
    $stmt = $pdo->prepare("SELECT quote, author, title FROM quotes WHERE id >= :id LIMIT 1");
    $stmt->execute([':id' => $random_id]);
    $quote = $stmt->fetch(PDO::FETCH_ASSOC);
    
    echo json_encode($quote ? $quote : ["error" => "No quotes found."]);
}

// ---------------------------------------------------------
// アクション：検索 (短文・長文すべてを横断検索)
// ---------------------------------------------------------
elseif ($action === 'search') {
    $keyword = isset($_GET['keyword']) ? $_GET['keyword'] : '';
    $scope = isset($_GET['scope']) ? $_GET['scope'] : 'all';
    if (empty($keyword)) { echo json_encode([]); exit; }
    
    $like_keyword = '%' . $keyword . '%';
    $results = [];
    shuffle($db_files); // 短文・長文のDBリストをシャッフル
    
    foreach ($db_files as $db) {
        $pdo = new PDO("sqlite:" . $db);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        
        $sql = "SELECT quote, author, title FROM quotes WHERE ";
        if ($scope === 'quote') { $sql .= "quote LIKE :kw"; }
        elseif ($scope === 'author') { $sql .= "author LIKE :kw"; }
        elseif ($scope === 'title') { $sql .= "title LIKE :kw"; }
        else { $sql .= "(quote LIKE :kw OR author LIKE :kw OR title LIKE :kw)"; }
        
        $sql .= " LIMIT " . (100 - count($results));
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute([':kw' => $like_keyword]);
        $results = array_merge($results, $stmt->fetchAll(PDO::FETCH_ASSOC));
        
        // 100件見つかったらそこで検索終了
        if (count($results) >= 100) break;
    }
    shuffle($results);
    echo json_encode($results);
}

// ---------------------------------------------------------
// アクション：気配 (モードに合わせたDBから検索)
// ---------------------------------------------------------
elseif ($action === 'atmosphere') {
    // 全世界対応: keywords 先頭群は国・土地。本文に加え title/author も検索し、場所ヒットを優先。
    $keywords_str = isset($_GET['keywords']) ? $_GET['keywords'] : '';
    if (empty($keywords_str)) { echo json_encode([]); exit; }

    $keywords = explode(',', $keywords_str);
    $conditions = [];
    $params = [];
    $keyword_list = [];
    foreach ($keywords as $index => $kw) {
        $kw = trim($kw);
        if ($kw === '') { continue; }
        $param_name = ":kw" . $index;
        // 書名・著者にも土地が載ることが多い（国の書物・土地の情景）
        $conditions[] = "(quote LIKE $param_name OR author LIKE $param_name OR title LIKE $param_name)";
        $params[$param_name] = '%' . $kw . '%';
        $keyword_list[] = $kw;
    }
    if (empty($conditions)) { echo json_encode([]); exit; }

    shuffle($db_files);
    $results = [];

    // 対象のDBからランダムに3つ選んで検索（上限を少し広げて場所優先ソートに余地を）
    foreach (array_slice($db_files, 0, 3) as $db) {
        $pdo = new PDO("sqlite:" . $db);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $sql = "SELECT quote, author, title FROM quotes WHERE (" . implode(" OR ", $conditions) . ") LIMIT " . (150 - count($results));
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $results = array_merge($results, $stmt->fetchAll(PDO::FETCH_ASSOC));
        if (count($results) >= 150) break;
    }

    // 先頭キーワード（場所）ほど高スコア。クライアント側でも再ソートする。
    foreach ($results as &$row) {
        $hay = strtolower(($row['quote'] ?? '') . ' ' . ($row['author'] ?? '') . ' ' . ($row['title'] ?? ''));
        $score = 0;
        foreach ($keyword_list as $i => $kw) {
            if ($kw === '') { continue; }
            if (strpos($hay, strtolower($kw)) !== false) {
                $weight = ($i < 8) ? (14 - min($i, 7)) : 1;
                $score += $weight;
            }
        }
        $row['_score'] = $score;
    }
    unset($row);

    usort($results, function ($a, $b) {
        return ($b['_score'] ?? 0) <=> ($a['_score'] ?? 0);
    });

    $strong = [];
    $weak = [];
    foreach ($results as $row) {
        $s = $row['_score'] ?? 0;
        unset($row['_score']);
        if ($s > 0) { $strong[] = $row; } else { $weak[] = $row; }
    }
    // 場所ヒット群を先に（群内は軽くシャッフル）、気候だけの弱ヒットは後ろ
    shuffle($strong);
    shuffle($weak);
    $results = array_merge($strong, $weak);
    if (count($results) > 100) {
        $results = array_slice($results, 0, 100);
    }
    echo json_encode($results);
}
?>