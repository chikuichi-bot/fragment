<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

// 🌟 ここが「金庫」です！ここにGeminiのAPIキーを書きます。
// iPhoneアプリからはこのファイルの中身は絶対に見えません。
$apiKey = getenv('GEMINI_API_KEY') ?: ''; // ⚠️本物のキーに書き換えてください

// iPhoneアプリから送られてきたデータ（プロンプト等）を受け取る
$json = file_get_contents('php://input');

if (empty($json)) {
    echo json_encode(['error' => 'データがありません']);
    exit;
}

// Gemini APIにリクエストを送る
$url = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=' . $apiKey;

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $json);
curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

// Geminiからの返事を受け取る
$response = curl_exec($ch);

if(curl_errno($ch)){
    echo json_encode(['error' => curl_error($ch)]);
} else {
    // iPhoneにそのまま返事を送り返す
    echo $response;
}
curl_close($ch);
?>