//
//  LiteraryFragmentsApp.swift
//  LiteraryFragments
//
//  Created by awashima takehito on 2026/03/28.
//

import SwiftUI
import AVFoundation

@main
struct LiteraryFragmentsApp: App {
    init() {
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
