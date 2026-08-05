//
//  LiteraryFragmentsApp.swift
//  LiteraryFragments
//
//  Created by awashima takehito on 2026/03/28.
//

import SwiftUI
import AVFoundation

/// シミュレータから App Store 用スクショを撮るときだけ使う。
/// 起動例: `-UIScreenshotScreen language` （language|welcome|howto|home|settings|tickets）
enum UIScreenshotMode: String {
    case language, welcome, howto, home, home_en, settings, tickets

    static var active: UIScreenshotMode? {
        let args = ProcessInfo.processInfo.arguments
        guard let i = args.firstIndex(of: "-UIScreenshotScreen"),
              args.indices.contains(i + 1) else { return nil }
        return UIScreenshotMode(rawValue: args[i + 1])
    }

    var onboardingStep: Int? {
        switch self {
        case .language: return 0
        case .welcome: return 1
        case .howto: return 2
        default: return nil
        }
    }
}

@main
struct LiteraryFragmentsApp: App {
    init() {
        if let mode = UIScreenshotMode.active {
            UserDefaults.standard.set("日本語", forKey: "nativeLanguage")
            UserDefaults.standard.set(1, forKey: "themePreference") // light paper
            switch mode {
            case .language, .welcome, .howto:
                UserDefaults.standard.set(false, forKey: "hasCompletedOnboarding")
            case .home, .home_en, .settings, .tickets:
                UserDefaults.standard.set(true, forKey: "hasCompletedOnboarding")
            }
            // スクショ撮影時は音声ウォームアップを飛ばして起動を安定させる
            return
        }

        // アプリ起動時にAudioSessionを初期化してAVSpeechSynthesizerの初回遅延を防ぐ
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let audioSession = AVAudioSession.sharedInstance()
                try audioSession.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
                try audioSession.setActive(true)
                
                // 無音の短い音声をバックグラウンドで一度再生してエンジンを温めておく（Warm-up）
                let synthesizer = AVSpeechSynthesizer()
                let utterance = AVSpeechUtterance(string: " ")
                utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
                utterance.volume = 0.01 // 極小音量
                utterance.rate = AVSpeechUtteranceMaximumSpeechRate
                
                DispatchQueue.main.async {
                    synthesizer.speak(utterance)
                }
            } catch {
                print("AudioSession Warmup Failed: \(error.localizedDescription)")
            }
        }
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
