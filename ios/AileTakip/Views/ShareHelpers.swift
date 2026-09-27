import SwiftUI
import UIKit

/// Aile davet metnini WhatsApp ile paylaşır; WhatsApp yüklü değilse
/// genel paylaşım sayfasına (UIActivityViewController) düşer.
enum WhatsAppInvite {

    /// Android tarafındaki FamilyInviteCodec.shareText ile aynı biçim.
    static func shareText(groupId: String, passcode: String) -> String {
        """
        \u{1F3E0} Aile Takip daveti

        Aile grubumuza katılın:
        Grup ID: \(groupId)
        Aile şifresi: \(passcode)

        Uygulama > Profil > Senkronizasyon ekranından bu bilgileri girin.
        (Katılımınız bir aile bireyi onayladıktan sonra senkron başlar.)
        """
    }

    static func share(groupId: String, passcode: String) {
        let text = shareText(groupId: groupId, passcode: passcode)
        // Yalnızca URL'de geçerli karakterler kalsın (Türkçe karakterler için)
        let encoded = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? text
        if let url = URL(string: "https://wa.me/?text=\(encoded)"),
           UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
        } else {
            openShareSheet(text: text)
        }
    }

    static func openShareSheet(text: String) {
        let activityVC = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first
        var top = scene?.keyWindow?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        top?.present(activityVC, animated: true)
    }
}
