import SwiftUI

// MARK: - Notes View
struct NotesView: View {
    @EnvironmentObject var vm: MainViewModel
    @State private var showAdd = false
    @State private var title = ""
    @State private var content = ""
    @State private var category = "Genel"
    @State private var color = "#3498DB"

    var body: some View {
        List {
            ForEach(vm.notes) { note in
                HStack(spacing: 12) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Color(hex: note.color))
                        .frame(width: 4, height: 40)
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 4) {
                            if note.isPinned {
                                Image(systemName: "pin.fill")
                                    .font(.caption2)
                                    .foregroundStyle(.orange)
                            }
                            Text(note.title).font(.headline)
                        }
                        Text(note.content)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                    Spacer()
                    Text(note.category)
                        .font(.caption2)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Capsule().fill(Color(hex: note.color).opacity(0.15)))
                }
            }
            .onDelete { indexSet in
                indexSet.forEach { vm.deleteNote(vm.notes[$0]) }
            }
        }
        .navigationTitle("Notlar")
        .toolbar {
            Button { showAdd = true } label: { Image(systemName: "plus") }
        }
        .alert("Yeni Not", isPresented: $showAdd) {
            TextField("Başlık", text: $title)
            TextField("İçerik", text: $content)
            Button("Kaydet") {
                if !title.isEmpty {
                    vm.addNote(title: title, content: content, category: category, color: color)
                    title = ""
                    content = ""
                }
            }
            Button("İptal", role: .cancel) { title = ""; content = "" }
        }
    }
}

// MARK: - Reminders View
struct RemindersView: View {
    @EnvironmentObject var vm: MainViewModel
    @State private var showAdd = false
    @State private var title = ""
    @State private var desc = ""
    @State private var fireDate = Date().addingTimeInterval(3600)

    var body: some View {
        List {
            if vm.reminders.isEmpty {
                Text("Henüz hatırlatıcı yok")
                    .foregroundStyle(.secondary)
            }
            ForEach(vm.reminders) { reminder in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(reminder.title)
                            .strikethrough(reminder.isCompleted)
                        Text(reminder.description)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text(reminder.reminderTime.formatted(date: .omitted, time: .shortened))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Button {
                        vm.completeReminder(reminder)
                    } label: {
                        Image(systemName: reminder.isCompleted ? "checkmark.circle.fill" : "circle")
                            .foregroundStyle(reminder.isCompleted ? .green : .secondary)
                    }
                    .buttonStyle(.plain)
                }
            }
            .onDelete { indexSet in
                indexSet.forEach { vm.deleteReminder(vm.reminders[$0]) }
            }
        }
        .navigationTitle("Hatırlatıcılar")
        .toolbar {
            Button { showAdd = true } label: { Image(systemName: "plus") }
        }
        .alert("Yeni Hatırlatıcı", isPresented: $showAdd) {
            TextField("Başlık", text: $title)
            TextField("Açıklama", text: $desc)
            Button("Kaydet") {
                if !title.isEmpty {
                    vm.addReminder(title: title, description: desc, reminderTime: fireDate)
                }
                title = ""
                desc = ""
                fireDate = Date().addingTimeInterval(3600)
            }
            Button("İptal", role: .cancel) { title = ""; desc = "" }
        } message: {
            Text("1 saat sonrasına ayarlanır")
        }
    }
}

// MARK: - Invoices View
struct InvoicesView: View {
    @EnvironmentObject var vm: MainViewModel
    @State private var showAdd = false
    @State private var title = ""
    @State private var amountText = ""
    @State private var dueDate = ""

    var totalPending: Double {
        vm.invoices.filter { $0.status == "pending" }.reduce(0) { $0 + $1.amount }
    }

    var body: some View {
        List {
            Section {
                ForEach(vm.invoices) { invoice in
                    HStack {
                        Image(systemName: invoice.status == "paid" ? "checkmark.circle.fill" : "exclamationmark.circle")
                            .foregroundStyle(invoice.status == "paid" ? .green : .orange)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(invoice.title)
                                .strikethrough(invoice.status == "paid")
                            Text(invoice.dueDate.isEmpty ? "Son tarih yok" : "Son: \(invoice.dueDate)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text(String(format: "₺%.2f", invoice.amount))
                            .fontWeight(.semibold)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { vm.toggleInvoiceStatus(invoice) }
                }
                .onDelete { indexSet in
                    indexSet.forEach { vm.deleteInvoice(vm.invoices[$0]) }
                }
            } header: {
                Text("Ödenecek toplam: ₺" + String(format: "%.2f", totalPending))
            }
        }
        .navigationTitle("Faturalar")
        .toolbar {
            Button { showAdd = true } label: { Image(systemName: "plus") }
        }
        .alert("Yeni Fatura", isPresented: $showAdd) {
            TextField("Başlık", text: $title)
            TextField("Tutar", text: $amountText)
                .keyboardType(.decimalPad)
            TextField("Son tarih (YYYY-AA-GG)", text: $dueDate)
            Button("Kaydet") {
                let normalized = amountText.replacingOccurrences(of: ",", with: ".")
                if !title.isEmpty, let amount = Double(normalized) {
                    vm.addInvoice(title: title, amount: amount, dueDate: dueDate)
                    title = ""
                    amountText = ""
                    dueDate = ""
                }
            }
            Button("İptal", role: .cancel) { title = ""; amountText = ""; dueDate = "" }
        }
    }
}

// MARK: - Inventory View
struct InventoryView: View {
    @EnvironmentObject var vm: MainViewModel
    @State private var showAdd = false
    @State private var name = ""
    @State private var quantityText = "1"
    @State private var location = ""

    var lowStock: [InventoryItem] {
        vm.inventoryItems.filter { $0.minStock > 0 && $0.quantity <= $0.minStock }
    }

    var body: some View {
        List {
            if !lowStock.isEmpty {
                Section("Kritik Stok") {
                    ForEach(lowStock) { item in
                        HStack {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundStyle(.orange)
                            Text(item.name)
                            Spacer()
                            Text("\(item.quantity) \(item.unit)")
                                .foregroundStyle(.orange)
                        }
                    }
                }
            }
            Section("Tüm Ürünler") {
                ForEach(vm.inventoryItems) { item in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.name)
                            if !item.location.isEmpty {
                                Text(item.location)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Spacer()
                        Text("\(item.quantity) \(item.unit)")
                            .foregroundStyle(.secondary)
                    }
                }
                .onDelete { indexSet in
                    indexSet.forEach { vm.deleteInventoryItem(vm.inventoryItems[$0]) }
                }
            }
        }
        .navigationTitle("Envanter")
        .toolbar {
            Button { showAdd = true } label: { Image(systemName: "plus") }
        }
        .alert("Yeni Ürün", isPresented: $showAdd) {
            TextField("Ürün adı", text: $name)
            TextField("Adet", text: $quantityText)
                .keyboardType(.numberPad)
            TextField("Konum (örn. Mutfak)", text: $location)
            Button("Kaydet") {
                if !name.isEmpty {
                    vm.addInventoryItem(name: name, quantity: Int(quantityText) ?? 1, location: location)
                    name = ""
                    quantityText = "1"
                    location = ""
                }
            }
            Button("İptal", role: .cancel) { name = ""; quantityText = "1"; location = "" }
        }
    }
}

// MARK: - Meal Plan View
struct MealPlanView: View {
    @EnvironmentObject var vm: MainViewModel
    let days = ["Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"]
    @State private var showAdd = false
    @State private var selectedDay = 0
    @State private var mealType = "Kahvaltı"
    @State private var dish = ""

    var body: some View {
        List {
            ForEach(0..<7, id: \.self) { dayIndex in
                Section(days[dayIndex]) {
                    ForEach(["Kahvaltı", "Öğle", "Akşam"], id: \.self) { type in
                        let plan = vm.mealPlans.first { $0.dayOfWeek == dayIndex + 1 && $0.mealType == type }
                        HStack {
                            Text(type)
                                .font(.subheadline)
                                .frame(width: 70, alignment: .leading)
                            Text(plan?.dish ?? "—")
                                .foregroundStyle(plan != nil ? Color.primary : Color.secondary)
                            Spacer()
                            if let plan = plan {
                                Button(role: .destructive) {
                                    vm.deleteMealPlan(plan)
                                } label: {
                                    Image(systemName: "trash")
                                        .font(.caption)
                                }
                                .buttonStyle(.borderless)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Yemek Planı")
        .toolbar {
            Button { showAdd = true } label: { Image(systemName: "plus") }
        }
        .alert("Yemek Ekle", isPresented: $showAdd) {
            TextField("Yemek (örn. Menemen)", text: $dish)
            Button("Ekle") {
                if !dish.isEmpty {
                    vm.addMealPlan(dayOfWeek: selectedDay + 1, mealType: mealType, dish: dish)
                    dish = ""
                }
            }
            Button("İptal", role: .cancel) { dish = "" }
        } message: {
            Text("Varsayılan: Pazartesi - Kahvaltı")
        }
    }
}

// MARK: - Health Dashboard View
struct HealthDashboardView: View {
    @EnvironmentObject var vm: MainViewModel

    var body: some View {
        List {
            Section("Bugün") {
                HStack {
                    Image(systemName: "flame.fill").foregroundStyle(.orange)
                    Text("Kalori")
                    Spacer()
                    Text("\(vm.todayCalories) kcal")
                }
                HStack {
                    Image(systemName: "drop.fill").foregroundStyle(.blue)
                    Text("Su")
                    Spacer()
                    Text("\(vm.todayWaterMl) ml")
                }
            }
            Section("Egzersiz") {
                if vm.workoutLogs.isEmpty {
                    Text("Henüz egzersiz kaydı yok").foregroundStyle(.secondary)
                }
                ForEach(vm.workoutLogs.prefix(5)) { workout in
                    HStack {
                        Image(systemName: "dumbbell.fill")
                        Text(workout.workoutType)
                        Spacer()
                        Text("\(workout.duration) dk - \(workout.caloriesBurned) kcal")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            Section("Uyku") {
                if vm.sleepLogs.isEmpty {
                    Text("Henüz uyku kaydı yok").foregroundStyle(.secondary)
                }
                ForEach(vm.sleepLogs.prefix(3)) { sleep in
                    HStack {
                        Image(systemName: "bed.double.fill")
                        VStack(alignment: .leading) {
                            Text(sleep.quality)
                            Text(sleep.date)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text("\(sleep.durationMinutes) dk")
                    }
                }
            }
            Section("Spor Kulüpleri") {
                ForEach(vm.sportsClubs) { club in
                    HStack {
                        Image(systemName: "figure.run")
                        VStack(alignment: .leading) {
                            Text(club.name)
                            Text(club.type)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text(String(format: "₺%.0f/ay", club.monthlyFee))
                            .font(.caption)
                    }
                }
            }
        }
        .navigationTitle("Sağlık")
    }
}

// MARK: - Family Members View
struct FamilyMembersView: View {
    @EnvironmentObject var vm: MainViewModel
    @State private var showAdd = false
    @State private var name = ""
    @State private var role = "Üye"
    @State private var color = "#3498DB"

    var body: some View {
        List {
            ForEach(vm.members) { member in
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color(hex: member.color))
                            .frame(width: 40, height: 40)
                        Text(String(member.name.prefix(1)).uppercased())
                            .font(.headline)
                            .foregroundStyle(.white)
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text(member.name).font(.headline)
                        Text(member.role)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text("\(member.points) puan")
                        .font(.caption)
                        .foregroundStyle(.orange)
                }
            }
            .onDelete { indexSet in
                indexSet.forEach { vm.deleteMember(vm.members[$0]) }
            }
        }
        .navigationTitle("Aile Üyeleri")
        .toolbar {
            Button { showAdd = true } label: { Image(systemName: "plus") }
        }
        .alert("Yeni Üye", isPresented: $showAdd) {
            TextField("İsim", text: $name)
            Button("Kaydet") {
                if !name.isEmpty {
                    vm.addMember(name: name, role: role, color: color)
                    name = ""
                }
            }
            Button("İptal", role: .cancel) { name = "" }
        }
    }
}

// MARK: - Sync Settings View
struct SyncSettingsView: View {
    @EnvironmentObject var vm: MainViewModel
    @ObservedObject var sync: FirebaseSyncService
    @State private var email = ""
    @State private var password = ""
    @State private var joinGroupId = ""
    @State private var joinPasscode = ""
    @State private var newPasscode = ""
    @State private var memberName = ""
    @State private var copied = false
    @State private var busy = false
    @State private var statusMessage = ""
    @State private var firebaseApiKey = FirebaseSyncClient.apiKey
    @State private var firebaseDbUrl = FirebaseSyncClient.databaseURL
    @State private var showScanner = false
    @State private var showInviteQr = false

    var body: some View {
        Form {
            if !sync.isConfigured {
                Section("Firebase Ayarları") {
                    Text("Firebase Console → Proje Ayarları → Genel → Web API Anahtarı ve Realtime Database URL girin. Bir kez girilir, cihazda saklanır.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    TextField("Web API Key", text: $firebaseApiKey)
                    TextField("Database URL", text: $firebaseDbUrl)
                        .keyboardType(.URL)
                        .autocorrectionDisabled()
                    Button("Kaydet") {
                        FirebaseSyncClient.saveConfig(apiKey: firebaseApiKey, databaseURL: firebaseDbUrl)
                    }
                }
            }

            switch sync.membership {
            case .signedOut:
                authSection
            case .notMember:
                joinOrCreateSection
            case .pending(let uid):
                Section("Katılım Bekleniyor") {
                    Text("Katılım isteğiniz gönderildi (\(uid.prefix(8))…). Bir aile bireyi onayladığında senkron otomatik başlar.")
                        .font(.footnote)
                }
            case .rejected:
                Section("Reddedildi") {
                    Text("Katılım isteğiniz reddedildi. Aile bireyine danışın.")
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            case .member:
                memberSection
            case .error(let message):
                Section("Hata") {
                    Text(message).foregroundStyle(.red).font(.footnote)
                }
                joinOrCreateSection
            }

            Section("Otomatik Senkron") {
                Toggle("Her değişiklikte gönder", isOn: $vm.autoSyncEnabled)
            }

            Section("Nasıl Çalışır") {
                Text("Senkronizasyon Firebase ile çalışır. Bir aile bireyi grup oluşturur; diğerleri grup kimliği + aile şifresiyle katılır ve onay sonrası veriler aile arasında paylaştırılır. Çakışmada son değiştiren kazanır.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle("Senkronizasyon")
        .sheet(isPresented: $showScanner) {
            QrScanSheet { raw in handleScan(raw) }
        }
        .sheet(isPresented: $showInviteQr) {
            inviteQrSheet
        }
        .onAppear {
            memberName = vm.currentGroupId.isEmpty ? "Ben" : (vm.members.first?.name ?? "Ben")
            joinGroupId = vm.currentGroupId
        }
    }

    /// QR tarama sonucunu işler (Android tarama akışı paritesi):
    /// davet kodunu çöz → alanları doldur → uygunsa isteği anında gönder.
    private func handleScan(_ raw: String) {
        if let invite = FamilyInviteCodec.decode(raw) {
            joinGroupId = invite.groupId
            joinPasscode = invite.passcode
            // "Tek tarama": giriş yapılmışsa ve üye/bekleyen değilsek isteği hemen gönder
            var canAutoJoin = sync.isSignedIn
            if case .member = sync.membership { canAutoJoin = false }
            if case .pending = sync.membership { canAutoJoin = false }
            if canAutoJoin {
                sync.setGroup(invite.groupId)
                Swift.Task {
                    _ = await sync.requestJoin(passcode: invite.passcode, memberName: memberName)
                }
            }
        } else {
            // Davet kodu değilse düz grup kimliği olarak kabul et
            joinGroupId = raw
        }
    }

    /// Kendi davet QR kodunu gösteren sayfa (Android "QR Kodu Büyüt" paritesi).
    private var inviteQrSheet: some View {
        VStack(spacing: 16) {
            Text("Aile Davet QR Kodu")
                .font(.headline)
            QRCodeImage(content: inviteCode(), size: 220)
            Text(inviteCode())
                .font(.caption.monospaced())
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            Text("Aile bireyi: Senkronizasyon → Katılım → QR ile Tara")
                .font(.caption2)
                .foregroundStyle(.secondary)
            Button("Kapat") { showInviteQr = false }
                .buttonStyle(.bordered)
        }
        .padding(.vertical, 32)
    }

    // MARK: Alt bölümler

    private var authSection: some View {
        Section("Aile Hesabı") {
            Text("Senkronizasyon gerçek üye kimliğiyle (Firebase UID) çalışır. Aile bireyleri kendi e-posta/şifresiyle giriş yapar.")
                .font(.footnote)
                .foregroundStyle(.secondary)
            TextField("E-posta", text: $email)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            SecureField("Şifre (en az 6 karakter)", text: $password)
            HStack {
                Button("Giriş Yap") { run { _ in await sync.signIn(email: email, password: password) } }
                    .disabled(busy || email.isEmpty || password.isEmpty)
                Spacer()
                Button("Hesap Oluştur") { run { _ in await sync.signUp(email: email, password: password) } }
                    .disabled(busy || email.isEmpty || password.isEmpty)
            }
        }
    }

    private var joinOrCreateSection: some View {
        Section("Aile Grubu") {
            TextField("Üye adınız", text: $memberName)
            HStack(spacing: 8) {
                TextField("Grup ID (katılım için)", text: $joinGroupId)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Button {
                    showScanner = true
                } label: {
                    Image(systemName: "qrcode.viewfinder")
                        .font(.title3)
                }
                .accessibilityLabel("QR ile Tara")
            }
            SecureField("Aile şifresi", text: $joinPasscode)
            Button("Katılım İsteği Gönder") {
                run { _ in
                    sync.setGroup(joinGroupId)
                    _ = await sync.requestJoin(passcode: joinPasscode, memberName: memberName)
                }
            }
            .disabled(busy || joinGroupId.isEmpty || joinPasscode.count < 4)

            SecureField("Yeni grup şifresi", text: $newPasscode)
            Button("Yeni Aile Grubu Oluştur") {
                run { _ in
                    let newId = Self.newGroupId()
                    sync.setGroup(newId)
                    let result = await sync.createFamily(newGroupId: newId, passcode: newPasscode, memberName: memberName)
                    if result.isMember {
                        vm.setFamilyGroup(groupId: newId, passcode: newPasscode)
                    }
                }
            }
            .disabled(busy || newPasscode.count < 4 || memberName.isEmpty)
        }
    }

    private var memberSection: some View {
        Group {
            Section("Durum") {
                HStack {
                    Text("Bağlantı")
                    Spacer()
                    Text(stateText).foregroundStyle(.secondary)
                }
                if let last = sync.lastSyncTime {
                    HStack {
                        Text("Son senkron")
                        Spacer()
                        Text(last.formatted(date: .abbreviated, time: .shortened))
                            .foregroundStyle(.secondary)
                    }
                }
                if sync.conflictsSolved > 0 {
                    HStack {
                        Text("Çakışma çözüldü")
                        Spacer()
                        Text("\(sync.conflictsSolved)").foregroundStyle(.orange)
                    }
                }
                Button("Şimdi Senkronize Et") {
                    run { _ in await sync.pushAll() }
                }
            }

            Section("Davet Paylaş") {
                HStack {
                    Text("Grup ID")
                    Spacer()
                    Text(vm.currentGroupId.isEmpty ? "—" : vm.currentGroupId)
                        .font(.caption.monospaced())
                        .foregroundStyle(.secondary)
                }
                Button {
                    showInviteQr = true
                } label: {
                    Label("QR Kodu Göster", systemImage: "qrcode")
                }
                Button {
                    WhatsAppInvite.share(groupId: vm.currentGroupId, passcode: vm.currentPasscode)
                } label: {
                    Label("WhatsApp ile Davet", systemImage: "message.fill")
                }
                Button {
                    UIPasteboard.general.string = inviteCode()
                    copied = true
                } label: {
                    Label(copied ? "Kopyalandı" : "Davet Kodunu Kopyala",
                          systemImage: copied ? "checkmark" : "doc.on.doc")
                }
            }

            if !sync.pendingJoins.isEmpty {
                Section("Katılım İstekleri") {
                    ForEach(sync.pendingJoins) { request in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(request.name.isEmpty ? String(request.uid.prefix(10)) + "…" : request.name)
                                        .font(.headline)
                                    Text(Date(timeIntervalSince1970: Double(request.requestedAt) / 1000)
                                        .formatted(date: .abbreviated, time: .shortened))
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Button("Onayla") {
                                    run { _ in _ = await sync.approveJoin(request.uid, name: request.name) }
                                }
                                .buttonStyle(.borderedProminent)
                                Button("Ret") {
                                    run { _ in _ = await sync.rejectJoin(request.uid) }
                                }
                                .buttonStyle(.bordered)
                            }
                        }
                    }
                }
            }

            Section {
                Button("Oturumu Kapat", role: .destructive) {
                    sync.signOut()
                }
            }
        }
    }

    private var stateText: String {
        switch sync.syncState {
        case .connected: return "Bağlı"
        case .connecting: return "Bağlanıyor…"
        case .syncing: return "Senkronize ediliyor…"
        case .disconnected: return "Bağlantı yok"
        case .error(let msg): return "Hata: \(msg)"
        }
    }

    private func inviteCode() -> String {
        let group = vm.currentGroupId.isEmpty ? "GRUP-KODU" : vm.currentGroupId
        let pass = vm.currentPasscode.isEmpty ? "SIFRE" : vm.currentPasscode
        return "AILETAKIP:g=\(group);p=\(pass)"
    }

    /// Ortak asenkron yürütücü: buton kilidi + hata mesajı.
    private func run(_ operation: @escaping (FirebaseSyncService) async -> Void) {
        busy = true
        statusMessage = ""
        Swift.Task {
            await operation(sync)
            await MainActor.run { busy = false }
        }
    }

    /// Android `SyncPreferences.newGroupId` paritesi: tahmin edilemez grup kimliği.
    static func newGroupId() -> String {
        let alphabet = Array("ABCDEFGHJKLMNPQRSTUVWXYZ23456789")
        var generator = SystemRandomNumberGenerator()
        let body = (0..<16).map { _ in String(alphabet[Int.random(in: 0..<alphabet.count, using: &generator)]) }.joined()
        return "aile_" + body
    }
}
