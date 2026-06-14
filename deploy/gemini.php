<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

$secretsPath = __DIR__ . '/gemini_secrets.php';
if (is_file($secretsPath)) {
    require_once $secretsPath;
}

$apiKey = '';
if (defined('FRAGMENTS_GEMINI_API_KEY')) {
    $apiKey = trim((string) FRAGMENTS_GEMINI_API_KEY);
}
if ($apiKey === '') {
    $env = getenv('GEMINI_API_KEY');
    if (is_string($env) && trim($env) !== '') {
        $apiKey = trim($env);
    }
}
if ($apiKey === '') {
    echo json_encode(['error' => 'Gemini APIキーが未設定です。gemini_secrets.php を配置してください。']);
    exit;
}

$json = file_get_contents('php://input');

if (empty($json)) {
    echo json_encode(['error' => 'データがありません']);
    exit;
}

$url = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent';

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $json);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'x-goog-api-key: ' . $apiKey,
]);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);

if (curl_errno($ch)) {
    echo json_encode(['error' => curl_error($ch)]);
} else {
    echo $response;
}
curl_close($ch);
