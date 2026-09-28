import SwiftUI
import AVFoundation
import CoreImage.CIFilterBuiltins

// MARK: - Davet kodu çözücü (Android `FamilyInviteCodec` paritesi)

struct FamilyInvite: Equatable {
    let groupId: String
    let passcode: String
}

enum FamilyInviteCodec {

    /// Tek satırda, QR/okuyucu dostu davet kodu.
    static func encode(groupId: String, passcode: String) -> String {
        "AILETAKIP:g=\(groupId);p=\(passcode)"
    }

    /// QR koddan, davet kodundan veya paylaşılan metinden davet bilgilerini çözer.
    ///
    /// Kabul edilen biçimler:
    ///  - `AILETAKIP:g=<grup>;p=<şifre>`
    ///  - "Grup ID: <grup>" ve "şifresi: <şifre>" içeren serbest metin
    static func decode(_ raw: String) -> FamilyInvite? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        // Kompakt biçim
        let compact = captures(in: trimmed, pattern: #"AILETAKIP:g=([^;\s]+);p=(\S+)"#)
        if compact.count >= 2, !compact[0].isEmpty, !compact[1].isEmpty {
            return FamilyInvite(groupId: compact[0], passcode: compact[1])
        }

        // Serbest metin (paylaşılan davet yazısı)
        let group = captures(in: trimmed, pattern: #"Grup ID[:\s]+(\S+)"#).first
        let pass = captures(in: trimmed, pattern: #"şifresi[:\s]+(\S+)"#).first
        if let group = group, let pass = pass, !group.isEmpty, !pass.isEmpty {
            return FamilyInvite(groupId: group, passcode: pass)
        }
        return nil
    }

    private static func captures(in text: String, pattern: String) -> [String] {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else {
            return []
        }
        let range = NSRange(text.startIndex..., in: text)
        guard let match = regex.firstMatch(in: text, options: [], range: range) else { return [] }
        return (1..<match.numberOfRanges).compactMap { index in
            guard let r = Range(match.range(at: index), in: text) else { return nil }
            return String(text[r])
        }
    }
}

// MARK: - QR kod üretici (yerleşik CIFilter; ek bağımlılık yok)

struct QRCodeImage: View {
    let content: String
    var size: CGFloat = 220

    var body: some View {
        Image(uiImage: Self.generate(content))
            .interpolation(.none)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .background(Color.white)
            .cornerRadius(8)
    }

    static func generate(_ content: String) -> UIImage {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(content.utf8)
        filter.correctionLevel = "M"
        if let output = filter.outputImage {
            let scaled = output.transformed(by: CGAffineTransform(scaleX: 10, y: 10))
            if let cg = CIContext().createCGImage(scaled, from: scaled.extent) {
                return UIImage(cgImage: cg)
            }
        }
        return UIImage()
    }
}

// MARK: - Kamera QR tarayıcı (AVFoundation)

struct QRScannerView: UIViewRepresentable {

    var onScanned: (String) -> Void
    /// Kamera hiç açılamazsa (simülatör/izin yok) çağrılır.
    var onUnavailable: ((CameraUnavailableReason) -> Void)? = nil

    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }

    final class Coordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
        let session = AVCaptureSession()
        private let parent: QRScannerView
        private var started = false
        private var lastValue: String?
        private var lastTime = Date.distantPast
        /// Kamera açılamadığında (simülatör / izin yok / donanım yok) UI'ya haber verir.
        var onUnavailable: ((CameraUnavailableReason) -> Void)?

        init(_ parent: QRScannerView) { self.parent = parent }

        func start() {
            guard !started else { return }
            started = true
            // Simülatörde kamera donanımı yok: talep etmeden kontrol et.
            guard AVCaptureDevice.default(for: .video) != nil else {
                DispatchQueue.main.async { self.onUnavailable?(.noCamera) }
                return
            }
            AVCaptureDevice.requestAccess(for: .video) { granted in
                guard granted else {
                    DispatchQueue.main.async { self.onUnavailable?(.permissionDenied) }
                    return
                }
                DispatchQueue.global(qos: .userInitiated).async {
                    self.configureAndRun()
                }
            }
        }

        private func configureAndRun() {
            guard let device = AVCaptureDevice.default(for: .video),
                  let input = try? AVCaptureDeviceInput(device: device),
                  session.canAddInput(input) else {
                DispatchQueue.main.async { self.onUnavailable?(.noCamera) }
                return
            }
            session.addInput(input)
            let output = AVCaptureMetadataOutput()
            guard session.canAddOutput(output) else {
                DispatchQueue.main.async { self.onUnavailable?(.noCamera) }
                return
            }
            session.addOutput(output)
            output.setMetadataObjectsDelegate(self, queue: .main)
            output.metadataObjectTypes = [.qr]
            if !session.isRunning { session.startRunning() }
        }

        func stop() {
            guard started else { return }
            started = false
            DispatchQueue.global(qos: .userInitiated).async {
                if self.session.isRunning { self.session.stopRunning() }
            }
        }

        func metadataOutput(_ output: AVCaptureMetadataOutput,
                            didOutput metadataObjects: [AVMetadataObject],
                            from connection: AVCaptureConnection) {
            guard let object = metadataObjects.compactMap({ $0 as? AVMetadataMachineReadableCodeObject }).first,
                  object.type == .qr, let value = object.stringValue else { return }
            // Aynı kodun üst üste tetiklemesini engelle (2 sn)
            guard value != lastValue || Date().timeIntervalSince(lastTime) > 2 else { return }
            lastValue = value
            lastTime = Date()
            parent.onScanned(value)
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.previewLayer.session = context.coordinator.session
        view.previewLayer.videoGravity = .resizeAspectFill
        context.coordinator.onUnavailable = onUnavailable
        context.coordinator.start()
        return view
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {
        DispatchQueue.main.async { uiView.previewLayer.frame = uiView.bounds }
    }

    static func dismantleUIView(_ uiView: PreviewView, coordinator: Coordinator) {
        coordinator.stop()
    }
}

// MARK: - Kamera kullanılamama nedenleri (UI mesajları için)

enum CameraUnavailableReason {
    case permissionDenied
    case noCamera

    var title: String {
        switch self {
        case .permissionDenied: return "Kamera erişimi kapalı"
        case .noCamera: return "Kamera kullanılamıyor"
        }
    }

    var message: String {
        switch self {
        case .permissionDenied:
            return "Davet QR kodunu okutmak için Ayarlar → Aile Takip → Kamera iznini açın."
        case .noCamera:
            return "Bu cihazda kamera bulunamadı (örn. simülatör). Davet kodunu elle yapıştırabilirsiniz."
        }
    }

    var showsSettingsButton: Bool { self == .permissionDenied }
}

// MARK: - Tarama sayfası (izin + kamera + talimat)

struct QrScanSheet: View {
    @Environment(\.dismiss) private var dismiss
    var onScanned: (String) -> Void

    @State private var permissionDenied =
        AVCaptureDevice.authorizationStatus(for: .video) == .denied
    /// Kamera yok (simülatör) veya akış başlatılamadı durumunda manuel giriş göster.
    @State private var cameraUnavailable = false
    @State private var unavailableReason: CameraUnavailableReason?
    @State private var manualCode = ""

    var body: some View {
        VStack(spacing: 16) {
            if permissionDenied || cameraUnavailable {
                Spacer()
                Image(systemName: "video.slash.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(.secondary)
                Text(unavailableReason?.title ?? "Kamera erişimi kapalı")
                    .font(.headline)
                Text(unavailableReason?.message
                    ?? "Davet QR kodunu okutmak için Ayarlar → Aile Takip → Kamera iznini açın.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                if unavailableReason?.showsSettingsButton ?? true {
                    Button("Ayarları Aç") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                }
                manualEntrySection
                Spacer()
            } else {
                ZStack {
                    QRScannerView(
                        onScanned: { value in
                            onScanned(value)
                            dismiss()
                        },
                        onUnavailable: { reason in
                            cameraUnavailable = true
                            unavailableReason = reason
                        }
                    )
                    .ignoresSafeArea(edges: .bottom)

                    // Okuma çerçevesi
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.white, lineWidth: 3)
                        .frame(width: 240, height: 240)
                        .shadow(radius: 4)
                }
                Text("Davet QR kodunu çerçeveye sığdırın")
                    .font(.footnote)
                    .foregroundStyle(.white)
                    .padding(8)
                    .background(Color.black.opacity(0.55))
                    .cornerRadius(8)
                    .padding(.bottom, 24)
            }

            Button("Vazgeç") { dismiss() }
                .buttonStyle(.bordered)
                .padding(.bottom, 12)
        }
        .background(Color.black)
        .onAppear {
            refreshPermission()
        }
        // Ayarlar'dan dönüldüğünde izin durumunu yeniden değerlendir
        .onReceive(NotificationCenter.default.publisher(
            for: UIApplication.didBecomeActiveNotification)) { _ in
            refreshPermission()
        }
    }

    /// İzin durumunu tazeler; .denied ise (QRScannerView zaten haber vermiş olabilir)
    /// kapalı-kamera ekranına geçer.
    private func refreshPermission() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .denied, .restricted:
            permissionDenied = true
            unavailableReason = .permissionDenied
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                permissionDenied = !granted
                if !granted { unavailableReason = .permissionDenied }
            }
        case .authorized:
            permissionDenied = false
        @unknown default:
            break
        }
    }

    /// Kamera kullanılamıyorken davet kodunu elle yapıştırma/giriş bölümü.
    @ViewBuilder
    private var manualEntrySection: some View {
        VStack(spacing: 8) {
            Text("Davet kodu elle gir")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.secondary)
            TextField("AILETAKIP:g=...;p=...", text: $manualCode)
                .textFieldStyle(.roundedBorder)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                .padding(.horizontal, 32)
            Button("Kodu Kullan") {
                if FamilyInviteCodec.decode(manualCode) != nil {
                    onScanned(manualCode)
                    dismiss()
                }
            }
            .buttonStyle(.bordered)
            .disabled(FamilyInviteCodec.decode(manualCode) == nil)
        }
    }
}
