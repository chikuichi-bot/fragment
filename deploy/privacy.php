<?php
/**
 * Literary Fragments — プライバシーポリシー + サポート（お問い合わせフォーム）
 * 公開先例: https://lagado.jp/fragments/privacy.php
 * Android 版: https://lagado.jp/fragments/privacy-android.php
 * 正本: deploy/privacy.php（ロリポップ public_html/fragments/ へアップロード）
 *
 * 同型: アボモン / そばメモ（受信は lagadolab@gmail.com · From は info@lagado.jp で SPF）
 */
$to = "lagadolab@gmail.com";
// From は lagado.jp（SPF）。Gmail アドレスを From にするとロリポップ経由で弾かれる。
$from = "info@lagado.jp";
$status_message = "";
$status_ok = false;

/**
 * メールが迷惑フォルダでも問い合わせを落とさないよう、サーバに控えを残す。
 * Web 非公開: fragments/_inbox/（.htaccess で拒否）
 */
function fragments_save_inquiry(string $subject, string $body): bool
{
    $dir = __DIR__ . "/_inbox";
    if (!is_dir($dir) && !@mkdir($dir, 0700, true)) {
        return false;
    }
    $htaccess = $dir . "/.htaccess";
    if (!is_file($htaccess)) {
        @file_put_contents(
            $htaccess,
            "Require all denied\nDeny from all\nOptions -Indexes\n"
        );
    }
    $entry = "==== " . date("Y-m-d H:i:s") . " JST ====\n";
    $entry .= "Subject: {$subject}\n\n";
    $entry .= $body;
    $entry .= "\n\n";
    return @file_put_contents($dir . "/inquiries.log", $entry, FILE_APPEND | LOCK_EX) !== false;
}

if ($_SERVER["REQUEST_METHOD"] === "POST") {
    // ブラウザが website を自動入力すると、送信せず成功表示になるため別名にする
    $honeypot = trim((string) ($_POST["hp_field"] ?? ""));
    $name = trim((string) ($_POST["name"] ?? ""));
    $email = trim((string) ($_POST["email"] ?? ""));
    $topic = trim((string) ($_POST["topic"] ?? ""));
    $device = trim((string) ($_POST["device"] ?? ""));
    $message = trim((string) ($_POST["message"] ?? ""));

    if ($honeypot !== "") {
        $status_ok = true;
        $status_message = "お問い合わせを送信しました。ご連絡ありがとうございます。 / Thank you. Your message was sent.";
    } elseif ($name === "" || $email === "" || $message === "") {
        $status_message = "入力されていない項目があります。 / Please fill in all required fields.";
    } elseif (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        $status_message = "メールアドレスの形式が正しくありません。 / Please enter a valid email address.";
    } else {
        $name_h = htmlspecialchars($name, ENT_QUOTES, "UTF-8");
        $email_h = htmlspecialchars($email, ENT_QUOTES, "UTF-8");
        $topic_h = htmlspecialchars($topic, ENT_QUOTES, "UTF-8");
        $device_h = htmlspecialchars($device, ENT_QUOTES, "UTF-8");
        $message_h = htmlspecialchars($message, ENT_QUOTES, "UTF-8");

        $subject = "【Literary Fragments】お問い合わせ";
        if ($topic !== "") {
            $subject .= "（{$topic}）";
        }
        $body = "Literary Fragments のサポートページからお問い合わせがありました。\n\n";
        $body .= "【お名前】\n{$name_h}\n\n";
        $body .= "【返信用メール】\n{$email_h}\n\n";
        if ($topic !== "") {
            $body .= "【種別】\n{$topic_h}\n\n";
        }
        if ($device !== "") {
            $body .= "【端末・OS・アプリ版】\n{$device_h}\n\n";
        }
        $body .= "【内容】\n{$message_h}\n";

        // 迷惑メール行きでも控えは残す
        $logged = fragments_save_inquiry($subject, $body);

        mb_language("Japanese");
        mb_internal_encoding("UTF-8");

        // Content-Type は付けない（mb_send_mail が日本語用に変換・付与する）
        $headers = "From: Literary Fragments <{$from}>\r\n";
        $headers .= "Reply-To: {$email}\r\n";

        $mailed = @mb_send_mail($to, $subject, $body, $headers, "-f{$from}");

        if ($mailed || $logged) {
            $status_ok = true;
            $status_message = "お問い合わせを送信しました。ご連絡ありがとうございます。 / Thank you. Your message was sent.";
        } else {
            $status_message = "送信に失敗しました。しばらくしてから再度お試しいただくか、直接メール（lagadolab@gmail.com）へご連絡ください。 / Sending failed. Please try again or email lagadolab@gmail.com.";
        }
    }
}
?>
<!DOCTYPE html>
<html lang="ja">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
        <title>プライバシーポリシー・サポート / Privacy &amp; Support - Literary Fragments</title>
        <style>
            :root {
                --bg-color: #f5f1ea;
                --card-bg: #ffffff;
                --border-color: #e5e0d6;
                --text-main: #1d1d1d;
                --text-sub: #6b6b6b;
                --accent: #0a84ff;
            }
            body {
                background-color: var(--bg-color);
                color: var(--text-main);
                font-family: "Hiragino Sans", "Hiragino Kaku Gothic ProN", "Helvetica Neue", sans-serif;
                display: flex;
                justify-content: center;
                margin: 0;
                padding: 0;
                -webkit-text-size-adjust: 100%;
            }
            .container {
                width: 100%;
                max-width: 800px;
                padding: max(24px, env(safe-area-inset-top)) 20px max(24px, env(safe-area-inset-bottom));
                box-sizing: border-box;
            }
            .header { text-align: center; margin-bottom: 16px; }
            .header h1 {
                font-weight: 700;
                margin: 0;
                font-size: 20px;
                letter-spacing: 0.04em;
                line-height: 1.45;
            }
            .header p {
                margin: 6px 0 0;
                font-size: 14px;
                color: var(--text-sub);
            }
            .nav-jump {
                display: flex;
                flex-wrap: wrap;
                gap: 8px;
                justify-content: center;
                margin-bottom: 20px;
            }
            .nav-jump a {
                display: inline-block;
                background: #fff;
                border: 1.5px solid var(--accent);
                color: var(--accent);
                text-decoration: none;
                font-size: 13px;
                font-weight: 700;
                border-radius: 999px;
                padding: 8px 14px;
            }
            .card {
                background-color: var(--card-bg);
                border: 1px solid var(--border-color);
                border-radius: 16px;
                width: 100%;
                padding: 30px 24px;
                box-sizing: border-box;
                line-height: 1.9;
                font-size: 15px;
                letter-spacing: 0.02em;
                margin-bottom: 28px;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
            }
            .card h2 {
                font-size: 16px;
                border-bottom: 1px dashed var(--border-color);
                padding-bottom: 8px;
                margin-top: 28px;
                margin-bottom: 14px;
                font-weight: 700;
            }
            .card h2:first-of-type { margin-top: 0; }
            .card p { margin-bottom: 14px; }
            .card ul { margin: 0 0 14px; padding-left: 1.35em; }
            .card li { margin-bottom: 6px; }
            .lang-label {
                display: inline-block;
                background: var(--accent);
                color: #fff;
                font-size: 12px;
                font-weight: 700;
                border-radius: 999px;
                padding: 4px 14px;
                margin-bottom: 14px;
                letter-spacing: 0.06em;
            }
            .footer-note {
                margin-top: 36px;
                font-size: 12px;
                color: var(--text-sub);
                text-align: right;
            }
            a { color: var(--accent); }
            .contact-form {
                margin-top: 12px;
                background: rgba(245, 241, 234, 0.55);
                border: 1px dashed var(--border-color);
                border-radius: 12px;
                padding: 22px 18px;
            }
            .form-group { margin-bottom: 16px; }
            .form-group label {
                display: block;
                margin-bottom: 6px;
                font-size: 13px;
                color: var(--text-sub);
                font-weight: 700;
            }
            .form-group .hint {
                display: block;
                font-size: 12px;
                font-weight: 500;
                color: var(--text-sub);
                margin-top: 2px;
                margin-bottom: 6px;
            }
            .form-group input,
            .form-group select,
            .form-group textarea {
                width: 100%;
                padding: 12px 14px;
                border: 1px solid var(--border-color);
                border-radius: 10px;
                font-family: inherit;
                font-size: 15px;
                background: #fff;
                color: var(--text-main);
                box-sizing: border-box;
                outline: none;
            }
            .form-group input:focus,
            .form-group select:focus,
            .form-group textarea:focus { border-color: var(--accent); }
            .hp {
                position: absolute;
                left: -9999px;
                width: 1px;
                height: 1px;
                overflow: hidden;
            }
            .btn-submit {
                width: 100%;
                padding: 14px;
                background: var(--accent);
                border: none;
                border-radius: 999px;
                color: #fff;
                font-family: inherit;
                font-size: 15px;
                font-weight: 800;
                cursor: pointer;
            }
            .btn-submit:active { opacity: 0.88; transform: translateY(1px); }
            .msg-success {
                background: #e6f4ea;
                color: #166534;
                border: 1px solid #bbf7d0;
                padding: 14px;
                border-radius: 10px;
                text-align: center;
                margin-bottom: 18px;
                font-weight: 700;
                font-size: 14px;
            }
            .msg-error {
                background: #fef2f2;
                color: #b91c1c;
                border: 1px solid #fecaca;
                padding: 14px;
                border-radius: 10px;
                text-align: center;
                margin-bottom: 18px;
                font-weight: 700;
                font-size: 14px;
            }
            .mail { font-weight: 800; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1>プライバシーポリシー・サポート<br>Privacy &amp; Support</h1>
                <p>Literary Fragments（Fragments） / Lagado Research Institute</p>
            </div>

            <nav class="nav-jump" aria-label="ページ内リンク">
                <a href="#support">サポート / Support</a>
                <a href="#privacy-ja">プライバシー（日本語）</a>
                <a href="#privacy-en">Privacy (English)</a>
                <a href="privacy-android.php">Android 版ポリシー</a>
            </nav>

            <div class="card" id="support">
                <span class="lang-label">サポート / Support</span>
                <p>不具合のご報告・ご質問・ご要望は、下のフォームからお送りください。直接メールでも受け付けています。</p>
                <p>
                    制作: ラガード研究所 / Lagado Research Institute<br>
                    メール: <a class="mail" href="mailto:lagadolab@gmail.com">lagadolab@gmail.com</a>
                </p>

                <?php if ($status_message !== ""): ?>
                    <div class="<?php echo $status_ok ? "msg-success" : "msg-error"; ?>">
                        <?php echo $status_message; ?>
                    </div>
                <?php endif; ?>

                <?php if (!$status_ok): ?>
                <form action="#support" method="POST" class="contact-form">
                    <div class="hp" aria-hidden="true">
                        <label for="hp_field">Leave blank</label>
                        <input type="text" id="hp_field" name="hp_field" value="" tabindex="-1" autocomplete="off">
                    </div>
                    <div class="form-group">
                        <label for="name">お名前（ペンネーム可） / Name</label>
                        <input type="text" id="name" name="name" required maxlength="120" placeholder="Fragments 太郎">
                    </div>
                    <div class="form-group">
                        <label for="email">返信用メール / Email</label>
                        <input type="email" id="email" name="email" required maxlength="200" placeholder="mail@example.com">
                    </div>
                    <div class="form-group">
                        <label for="topic">種別 / Topic</label>
                        <select id="topic" name="topic">
                            <option value="">（選択してください / Optional）</option>
                            <option value="不具合">不具合 / Bug</option>
                            <option value="購入・チケット">購入・チケット / Purchase</option>
                            <option value="気配・位置情報">気配・位置情報 / Atmosphere</option>
                            <option value="ご要望">ご要望 / Feedback</option>
                            <option value="その他">その他 / Other</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="device">端末・OS・アプリ版（任意） / Device info (optional)</label>
                        <span class="hint">例: iPhone 15 / iOS 18 / Fragments 1.0.3</span>
                        <input type="text" id="device" name="device" maxlength="200" placeholder="iPhone 15 / iOS 18 / 1.0.3">
                    </div>
                    <div class="form-group">
                        <label for="message">お問い合わせ内容 / Message</label>
                        <textarea id="message" name="message" rows="7" required maxlength="5000" placeholder="どんな操作のあと、何が起きたかなど、できるだけ具体的に…"></textarea>
                    </div>
                    <button type="submit" class="btn-submit">送信する / Send</button>
                </form>
                <?php endif; ?>

                <h2>購入・チケットについて</h2>
                <p>AI 解説用チケットのアプリ内課金は App Store（iOS）または Google Play（Android）経由です。領収書・請求は各ストアのヘルプもご利用ください。</p>
            </div>

            <div class="card" id="privacy-ja">
                <span class="lang-label">日本語 · プライバシー</span>
                <p>「Literary Fragments」（表示名 Fragments、以下「本アプリ」）は、ユーザーの皆様のプライバシーを尊重し、個人情報の保護に努めています。本アプリのご利用にあたり、情報の取り扱いについて以下のように定めます。Android（Google Play）向けの記載は <a href="privacy-android.php">こちら</a> です。</p>

                <h2>1. 個人情報の収集について</h2>
                <p>本アプリは、ユーザーの名前、連絡先、端末の固有IDなど、個人を特定できる情報を収集・保存・追跡する目的では利用しません。広告や解析（トラッキング）ツールも使用していません。</p>

                <h2>2. 端末内データの保存について</h2>
                <p>お気に入り（ストック）、表示履歴、チケット残数、テーマなどの設定、購入台帳（端末内）は、ユーザーの端末内にのみ保存されます。これらのデータを開発者がアカウントと紐づけて収集することはありません。</p>

                <h2>3. 位置情報（「今の気配」）について</h2>
                <p>「今の気配を読み取る」機能では、端末の位置情報（When in Use / 使用中のみ）の許可を求めることがあります。許可された場合、天気取得および文学的キーワード生成のため、座標や国・都市レベルの情報を一時的に利用します。位置情報を第三者に販売・提供したり、追跡目的でサーバーに長期保存したりすることはありません。拒否した場合もアプリは利用でき、京都などへのフォールバックで動作します。</p>

                <h2>4. 外部通信について</h2>
                <p>本アプリは機能提供のため、次の外部通信を行います。</p>
                <ul>
                    <li><strong>lagado.jp（本アプリ用 API）:</strong> 文学断片の取得・検索・気配キーワードに基づく抽選</li>
                    <li><strong>Google Gemini API（サーバー経由）:</strong> 選択した断片テキスト等を送り、AI 解説・翻訳を生成（個人を特定する情報と紐づけません）</li>
                    <li><strong>Open-Meteo:</strong> 気配機能のための天気情報（座標を一時送信）</li>
                    <li><strong>App Store / Google Play:</strong> チケット購入に必要な決済通信</li>
                    <li><strong>書籍検索（任意）:</strong> ユーザー操作により Google 等の検索を開く場合があります</li>
                </ul>

                <h2>5. 課金および決済情報の取り扱いについて</h2>
                <p>本アプリは AI 解説用の消耗型チケット（アプリ内課金）を提供しています。決済処理は Apple Inc.（App Store）または Google LLC（Google Play）を通じて行われ、開発元（Lagado Research Institute / ラガード研究所）がクレジットカード番号等の決済情報に直接アクセスしたり保持したりすることはありません。有料チケット残数および購入トークン台帳は端末内に保存されます。</p>

                <h2>6. 本ウェブサイトのお問い合わせフォームについて</h2>
                <p>本ページのサポートフォームからお問い合わせいただく場合、ご記入いただいたお名前・メールアドレス・お問い合わせ内容を、返信のためにメールで受け取ります。これらの情報は問い合わせ対応以外の目的では使用せず、第三者に提供しません。</p>

                <h2>7. 免責</h2>
                <p>AI による解説・翻訳の正確性について、開発元は保証しません。本アプリの利用により生じた損害について、法令上許容される範囲で責任を負いません。</p>

                <h2>8. プライバシーポリシーの変更について</h2>
                <p>本アプリは、必要に応じて本プライバシーポリシーを変更することがあります。重要な変更がある場合は、アプリ内または各ストアのページでお知らせいたします。</p>

                <h2>9. お問い合わせ</h2>
                <p>本アプリに関するお問い合わせは、<a href="#support">ページ上部のサポートフォーム</a>、または <a href="mailto:lagadolab@gmail.com">lagadolab@gmail.com</a> までご連絡ください。<br>
                制作: ラガード研究所 / Lagado Research Institute</p>

                <p class="footer-note">制定日: 2026年4月7日<br>改定日: 2026年7月29日</p>
            </div>

            <div class="card" id="privacy-en">
                <span class="lang-label">English · Privacy</span>
                <p>"Literary Fragments" (display name Fragments; the "App") respects your privacy. This policy explains how information is handled. The Android (Google Play) policy is <a href="privacy-android.php">here</a>.</p>

                <h2>1. Collection of Personal Information</h2>
                <p>The App does not collect, store, or track personally identifiable information such as your name, contact details, or device identifiers for profiling. The App contains no advertising and no analytics/tracking SDKs.</p>

                <h2>2. Data Stored on Your Device</h2>
                <p>Favorites, reading history, ticket balances, theme settings, and the on-device purchase ledger are stored only on your device. The developer does not collect this data linked to a user account.</p>

                <h2>3. Location ("Sense the Moment")</h2>
                <p>The atmosphere feature may request location permission while the App is in use. If granted, coordinates or country/city-level cues are used temporarily to fetch weather and literary keywords. We do not sell location data, use it for tracking, or keep it long-term on our servers. You can decline and still use the App (with a fallback such as Kyoto).</p>

                <h2>4. Network Communication</h2>
                <p>To provide features, the App may communicate with:</p>
                <ul>
                    <li><strong>lagado.jp (App API):</strong> fetching, searching, and atmosphere quote selection</li>
                    <li><strong>Google Gemini API (via our server):</strong> selected quote text for AI explanation/translation (not tied to your identity)</li>
                    <li><strong>Open-Meteo:</strong> weather for the atmosphere feature (temporary coordinates)</li>
                    <li><strong>App Store / Google Play:</strong> in-app ticket purchases</li>
                    <li><strong>Book search (optional):</strong> opening Google or similar when you choose to search</li>
                </ul>

                <h2>5. In-App Purchases</h2>
                <p>The App offers consumable tickets for AI explanations. Payments are processed by Apple Inc. (App Store) or Google LLC (Google Play). Lagado Research Institute never accesses or stores your payment card details. Paid ticket balances and purchase tokens are kept on your device.</p>

                <h2>6. Contact Form on This Website</h2>
                <p>If you use the support form, we receive your name, email, and message by email solely to reply. We do not use this for other purposes or share it with third parties.</p>

                <h2>7. Disclaimer</h2>
                <p>AI explanations and translations are provided without warranty of accuracy. To the extent permitted by law, the developer is not liable for damages arising from use of the App.</p>

                <h2>8. Changes to This Policy</h2>
                <p>This policy may be updated from time to time. Significant changes will be announced in the App or on the store listing.</p>

                <h2>9. Contact</h2>
                <p>Use the <a href="#support">support form above</a> or email <a href="mailto:lagadolab@gmail.com">lagadolab@gmail.com</a>.<br>
                Presented by: Lagado Research Institute</p>

                <p class="footer-note">Effective: April 7, 2026<br>Updated: July 29, 2026</p>
            </div>
        </div>
    </body>
</html>
