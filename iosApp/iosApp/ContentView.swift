import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .background(themeBackground.ignoresSafeArea())
    }

    /// AppColors.Bg 와 같은 색 — 다크: #101114, 라이트: #FFFFFF.
    private var themeBackground: Color {
        Color(UIColor { traits in
            traits.userInterfaceStyle == .dark
                ? UIColor(red: 0x10 / 255.0, green: 0x11 / 255.0, blue: 0x14 / 255.0, alpha: 1.0)
                : UIColor.white
        })
    }
}
