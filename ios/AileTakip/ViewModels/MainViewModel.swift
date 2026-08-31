import Foundation
import SwiftData
import SwiftUI

@MainActor
class MainViewModel: ObservableObject {
    private let modelContainer: ModelContainer
    private let modelContext: ModelContext
    
    // MARK: - Auth State
    @Published var isAuthenticated = true
    @Published var currentUser: FamilyMember?
    
    // MARK: - Data
    @Published var members: [FamilyMember] = []
    @Published var tasks: [Task] = []
    @Published var shoppingItems: [ShoppingItem] = []
    @Published var messages: [Message] = []
    @Published var notes: [Note] = []
    @Published var reminders: [Reminder] = []
    @Published var invoices: [Invoice] = []
    @Published var budgets: [Budget] = []
    @Published var expenses: [Expense] = []
    @Published var inventoryItems: [InventoryItem] = []
    @Published var mealPlans: [MealPlan] = []
    @Published var sportsClubs: [SportsClub] = []
    @Published var workoutLogs: [WorkoutLog] = []
    @Published var calorieLogs: [CalorieLog] = []
    @Published var menstrualCycles: [MenstrualCycle] = []
    @Published var waterLogs: [WaterLog] = []
    @Published var sleepLogs: [SleepLog] = []
    @Published var userAuth: UserAuth?
    @Published var syncEvents: [SyncEvent] = []
    
    // MARK: - Computed
    var pendingTaskCount: Int {
        tasks.filter { $0.status == "bekleyen" }.count
    }
    
    var uncheckedShoppingCount: Int {
        shoppingItems.filter { !$0.checked }.count
    }
    
    var todayCalories: Int {
        let today = formatDate(Date())
        return calorieLogs.filter { $0.date == today }.reduce(0) { $0 + $1.calories }
    }
    
    var todayWaterMl: Int {
        let today = formatDate(Date())
        return waterLogs.filter { $0.date == today }.reduce(0) { $0 + $1.amountMl }
    }
    
    var activeSportsClubCount: Int {
        sportsClubs.filter { $0.isActive }.count
    }
    
    var unpaidInvoiceCount: Int {
        invoices.filter { $0.status == "pending" }.count
    }
    
    // MARK: - Init
    init() {
        do {
            let config = ModelConfiguration(isStoredInMemoryOnly: false)
            let container = try ModelContainer(
                for: FamilyMember.self, Task.self, ShoppingItem.self, Message.self,
                     Note.self, Reminder.self, Invoice.self, Budget.self, Expense.self,
                     InventoryItem.self, MealPlan.self, SportsClub.self,
                     WorkoutLog.self, CalorieLog.self, MenstrualCycle.self,
                     WaterLog.self, SleepLog.self, UserAuth.self, SyncEvent.self,
                     Attachment.self,
                configurations: config
            )
            self.modelContainer = container
            self.modelContext = container.mainContext
            loadData()
        } catch {
            fatalError("Could not initialize ModelContainer: \(error)")
        }
    }
    
    // MARK: - Load Data
    func loadData() {
        fetchMembers()
        fetchTasks()
        fetchShopping()
        fetchMessages()
        fetchNotes()
        fetchReminders()
        fetchInvoices()
        fetchBudgets()
        fetchExpenses()
        fetchInventory()
        fetchMealPlans()
        fetchSportsClubs()
        fetchWorkouts()
        fetchCalories()
        fetchMenstrualCycles()
        fetchWater()
        fetchSleep()
        fetchUserAuth()
        fetchSyncEvents()
    }
    
    // MARK: - Fetch Methods
    private func fetchMembers() {
        let descriptor = FetchDescriptor<FamilyMember>(sortBy: [SortDescriptor(\.createdAt)])
        members = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchTasks() {
        let descriptor = FetchDescriptor<Task>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        tasks = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchShopping() {
        let descriptor = FetchDescriptor<ShoppingItem>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        shoppingItems = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchMessages() {
        let descriptor = FetchDescriptor<Message>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        messages = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchNotes() {
        let descriptor = FetchDescriptor<Note>(sortBy: [SortDescriptor(\.isPinned, order: .reverse), SortDescriptor(\.createdAt, order: .reverse)])
        notes = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchReminders() {
        let descriptor = FetchDescriptor<Reminder>(sortBy: [SortDescriptor(\.reminderTime)])
        reminders = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchInvoices() {
        let descriptor = FetchDescriptor<Invoice>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        invoices = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchBudgets() {
        let descriptor = FetchDescriptor<Budget>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        budgets = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchExpenses() {
        let descriptor = FetchDescriptor<Expense>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        expenses = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchInventory() {
        let descriptor = FetchDescriptor<InventoryItem>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        inventoryItems = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchMealPlans() {
        let descriptor = FetchDescriptor<MealPlan>(sortBy: [SortDescriptor(\.dayOfWeek)])
        mealPlans = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchSportsClubs() {
        let descriptor = FetchDescriptor<SportsClub>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        sportsClubs = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchWorkouts() {
        let descriptor = FetchDescriptor<WorkoutLog>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        workoutLogs = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchCalories() {
        let descriptor = FetchDescriptor<CalorieLog>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        calorieLogs = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchMenstrualCycles() {
        let descriptor = FetchDescriptor<MenstrualCycle>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        menstrualCycles = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchWater() {
        let descriptor = FetchDescriptor<WaterLog>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        waterLogs = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchSleep() {
        let descriptor = FetchDescriptor<SleepLog>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        sleepLogs = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    private func fetchUserAuth() {
        let descriptor = FetchDescriptor<UserAuth>()
        userAuth = (try? modelContext.fetch(descriptor))?.first
    }
    
    private func fetchSyncEvents() {
        let descriptor = FetchDescriptor<SyncEvent>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        syncEvents = (try? modelContext.fetch(descriptor)) ?? []
    }
    
    // MARK: - Task Operations
    func addTask(title: String, description: String = "", category: String = "Genel", priority: String = "orta", assignee: String = "", dueDate: String = "") {
        let task = Task(title: title, description: description, category: category, priority: priority, assignee: assignee, dueDate: dueDate)
        modelContext.insert(task)
        save()
        fetchTasks()
    }
    
    func toggleTask(_ task: Task) {
        task.status = task.status == "tamamlanan" ? "bekleyen" : "tamamlanan"
        task.completedAt = task.status == "tamamlanan" ? Date() : nil
        save()
        fetchTasks()
    }
    
    func deleteTask(_ task: Task) {
        modelContext.delete(task)
        save()
        fetchTasks()
    }
    
    // MARK: - Shopping Operations
    func addShoppingItem(name: String, quantity: Int = 1, category: String = "Market", addedBy: String = "") {
        let item = ShoppingItem(name: name, quantity: quantity, category: category, addedBy: addedBy)
        modelContext.insert(item)
        save()
        fetchShopping()
    }
    
    func toggleShoppingItem(_ item: ShoppingItem) {
        item.checked.toggle()
        save()
        fetchShopping()
    }
    
    func deleteShoppingItem(_ item: ShoppingItem) {
        modelContext.delete(item)
        save()
        fetchShopping()
    }
    
    // MARK: - Message Operations
    func sendMessage(content: String, senderName: String = "Ben", attachments: String = "") {
        let message = Message(senderName: senderName, senderId: "self", content: content, attachments: attachments)
        modelContext.insert(message)
        save()
        fetchMessages()
    }
    
    func deleteMessage(_ message: Message) {
        modelContext.delete(message)
        save()
        fetchMessages()
    }
    
    // MARK: - Note Operations
    func addNote(title: String, content: String = "", category: String = "Genel", color: String = "#3498DB", attachments: String = "") {
        let note = Note(title: title, content: content, category: category, color: color, attachments: attachments)
        modelContext.insert(note)
        save()
        fetchNotes()
    }
    
    func togglePinNote(_ note: Note) {
        note.isPinned.toggle()
        save()
        fetchNotes()
    }
    
    func deleteNote(_ note: Note) {
        modelContext.delete(note)
        save()
        fetchNotes()
    }
    
    // MARK: - Reminder Operations
    func addReminder(title: String, description: String = "", reminderTime: Date, repeatType: String = "once", category: String = "Genel", priority: String = "orta", alarmSound: String = "default") {
        let reminder = Reminder(title: title, description: description, reminderTime: reminderTime, repeatType: repeatType, category: category, priority: priority, alarmSound: alarmSound)
        modelContext.insert(reminder)
        save()
        fetchReminders()
    }
    
    func completeReminder(_ reminder: Reminder) {
        reminder.isCompleted = true
        save()
        fetchReminders()
    }
    
    func deleteReminder(_ reminder: Reminder) {
        modelContext.delete(reminder)
        save()
        fetchReminders()
    }
    
    // MARK: - Invoice Operations
    func addInvoice(title: String, amount: Double, category: String = "Genel", dueDate: String = "", notes: String = "", imageBase64: String? = nil) {
        let invoice = Invoice(title: title, amount: amount, category: category, dueDate: dueDate, notes: notes, imageBase64: imageBase64)
        modelContext.insert(invoice)
        save()
        fetchInvoices()
    }
    
    func toggleInvoiceStatus(_ invoice: Invoice) {
        invoice.status = invoice.status == "paid" ? "pending" : "paid"
        save()
        fetchInvoices()
    }
    
    func deleteInvoice(_ invoice: Invoice) {
        modelContext.delete(invoice)
        save()
        fetchInvoices()
    }
    
    // MARK: - Member Operations
    func addMember(name: String, role: String = "Üye", color: String = "#3498DB") {
        let member = FamilyMember(name: name, role: role, color: color)
        modelContext.insert(member)
        save()
        fetchMembers()
    }
    
    func deleteMember(_ member: FamilyMember) {
        modelContext.delete(member)
        save()
        fetchMembers()
    }
    
    // MARK: - Water & Sleep
    func addWater(amountMl: Int = 250, drinkType: String = "Su") {
        let water = WaterLog(memberId: "self", amountMl: amountMl, drinkType: drinkType, date: formatDate(Date()))
        modelContext.insert(water)
        save()
        fetchWater()
    }
    
    func addSleep(bedtime: Date, wakeTime: Date, quality: String = "orta", interruptions: Int = 0) {
        let sleep = SleepLog(memberId: "self", bedtime: bedtime, wakeTime: wakeTime, quality: quality, interruptions: interruptions, date: formatDate(Date()))
        modelContext.insert(sleep)
        save()
        fetchSleep()
    }
    
    // MARK: - Budget & Expense
    func addBudget(category: String, limit: Double, monthYear: String) {
        let budget = Budget(category: category, monthlyLimit: limit, monthYear: monthYear)
        modelContext.insert(budget)
        save()
        fetchBudgets()
    }
    
    func addExpense(category: String, amount: Double, description: String = "", date: String, budgetId: String = "") {
        let expense = Expense(budgetId: budgetId, category: category, amount: amount, description: description, expenseDate: date)
        modelContext.insert(expense)
        save()
        fetchExpenses()
    }
    
    // MARK: - Workout & Calorie
    func addWorkout(clubId: String = "", workoutType: String, duration: Int, calories: Int, notes: String = "") {
        let workout = WorkoutLog(clubId: clubId, memberId: "self", workoutType: workoutType, duration: duration, caloriesBurned: calories, date: formatDate(Date()), notes: notes)
        modelContext.insert(workout)
        save()
        fetchWorkouts()
    }
    
    func addCalorie(mealType: String, foodName: String, calories: Int, protein: Double = 0, carbs: Double = 0, fat: Double = 0, servingSize: String = "") {
        let calorie = CalorieLog(memberId: "self", mealType: mealType, foodName: foodName, calories: calories, protein: protein, carbs: carbs, fat: fat, servingSize: servingSize, date: formatDate(Date()))
        modelContext.insert(calorie)
        save()
        fetchCalories()
    }
    
    // MARK: - Inventory Operations
    func addInventoryItem(name: String, category: String = "Genel", quantity: Int = 1, unit: String = "adet", location: String = "") {
        let item = InventoryItem(name: name, category: category, quantity: quantity, unit: unit, location: location)
        modelContext.insert(item)
        save()
        fetchInventory()
    }
    
    func deleteInventoryItem(_ item: InventoryItem) {
        modelContext.delete(item)
        save()
        fetchInventory()
    }
    
    // MARK: - Meal Plan Operations
    func addMealPlan(dayOfWeek: Int, mealType: String, dish: String, notes: String = "") {
        let plan = MealPlan(dayOfWeek: dayOfWeek, mealType: mealType, dish: dish, notes: notes)
        modelContext.insert(plan)
        save()
        fetchMealPlans()
    }
    
    func deleteMealPlan(_ plan: MealPlan) {
        modelContext.delete(plan)
        save()
        fetchMealPlans()
    }
    
    // MARK: - Sports Club Operations
    func addSportsClub(name: String, type: String = "Spor Salonu", monthlyFee: Double = 0, memberId: String = "") {
        let club = SportsClub(name: name, type: type, monthlyFee: monthlyFee, memberId: memberId)
        modelContext.insert(club)
        save()
        fetchSportsClubs()
    }
    
    func deleteSportsClub(_ club: SportsClub) {
        modelContext.delete(club)
        save()
        fetchSportsClubs()
    }
    
    // MARK: - Menstrual Cycle Operations
    func addMenstrualCycle(startDate: String, cycleLength: Int = 28, periodLength: Int = 5, symptoms: String = "", mood: String = "") {
        let cycle = MenstrualCycle(memberId: "self", startDate: startDate, cycleLength: cycleLength, periodLength: periodLength, symptoms: symptoms, mood: mood)
        modelContext.insert(cycle)
        save()
        fetchMenstrualCycles()
    }
    
    func deleteMenstrualCycle(_ cycle: MenstrualCycle) {
        modelContext.delete(cycle)
        save()
        fetchMenstrualCycles()
    }
    
    // MARK: - Helpers
    private func save() {
        try? modelContext.save()
    }
    
    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }
    
    // MARK: - Sample Data
    func loadSampleData() {
        guard members.isEmpty else { return }
        
        // Family members
        let familyMembers = [
            FamilyMember(name: "Mehmet", role: "baba", color: "#2196F3", points: 150),
            FamilyMember(name: "Ayşe", role: "anne", color: "#E91E63", points: 220),
            FamilyMember(name: "Zeynep", role: "kız", color: "#9C27B0", points: 180),
            FamilyMember(name: "Emir", role: "oğul", color: "#FF9800", points: 120)
        ]
        familyMembers.forEach { modelContext.insert($0) }
        
        // Tasks
        let tasksList = [
            Task(title: "Mutfak temizliği", description: "Tezgah ve dolapları sil", category: "Ev", priority: "yüksek", assignee: "Mehmet", status: "bekleyen"),
            Task(title: "Çamaşıraları yıka", description: "Beyaz ve renklileri ayır", category: "Ev", priority: "orta", assignee: "Ayşe", status: "devam_ediyor"),
            Task(title: "Ödev kontrolü - Matematik", description: "Zeynep'in matematik ödevini kontrol et", category: "Eğitim", priority: "yüksek", assignee: "Mehmet"),
            Task(title: "Emir'in spor çantasını hazırla", description: "Yarın futbol antrenmanı var", category: "Spor", priority: "orta", assignee: "Ayşe"),
            Task(title: "Araba bakımı", description: "Yağ değişimi ve lastik kontrolü", category: "Araç", priority: "düşük", assignee: "Mehmet"),
            Task(title: "Doktor randevusu - Zeynep", description: "Diş hekimi kontrolü saat 14:30", category: "Sağlık", priority: "yüksek", assignee: "Ayşe"),
        ]
        tasksList.forEach { modelContext.insert($0) }
        
        // Shopping items
        let shoppingList = [
            ShoppingItem(name: "Süt (1L)", quantity: 3, category: "Market", addedBy: "Ayşe"),
            ShoppingItem(name: "Ekmek", quantity: 2, category: "Market", addedBy: "Ayşe"),
            ShoppingItem(name: "Yumurta (30'lu)", quantity: 1, category: "Market", addedBy: "Mehmet"),
            ShoppingItem(name: "Peynir (500g)", quantity: 1, category: "Market", addedBy: "Ayşe"),
            ShoppingItem(name: "Domates (1kg)", quantity: 1, category: "Market", addedBy: "Ayşe"),
            ShoppingItem(name: "Bulaşık Deterjanı", quantity: 1, category: "Temizlik", addedBy: "Ayşe"),
            ShoppingItem(name: "Tuvalet Kağıdı (12'li)", quantity: 1, category: "Temizlik", addedBy: "Mehmet"),
        ]
        shoppingList.forEach { modelContext.insert($0) }
        
        // Notes
        let notesList = [
            Note(title: "Ev Kuralları", content: "1. Yemekten sonra bulaşıkları hemen yıka\n2. TV izleme süresi hafta içi 1 saat\n3. Odalar her akşam toplanacak", category: "Genel", color: "#2196F3", isPinned: true, createdBy: "Ayşe"),
            Note(title: "Zeynep Okul Programı", content: "Pazartesi: Matematik, Türkçe, İngilizce\nSalı: Fen, Müzik, Beden\nÇarşamba: Matematik, Resim, Türkçe", category: "Önemli", color: "#E91E63", isPinned: true, createdBy: "Ayşe"),
            Note(title: "Tarif: Mercimek Çorbası", content: "Malzemeler: 2 bardak kırmızı mercimek, 1 soğan, 1 havuç, tuz, karabiber, kimyon", category: "Tarif", color: "#4CAF50", createdBy: "Ayşe"),
        ]
        notesList.forEach { modelContext.insert($0) }
        
        // Messages
        let messagesList = [
            Message(senderName: "Ayşe", senderId: "m2", content: "Akşam ne yiyelim? 😊"),
            Message(senderName: "Mehmet", senderId: "m1", content: "Mangal yapılsa güzel olur 🍖"),
            Message(senderName: "Zeynep", senderId: "m3", content: "Annecim yarın okula erken gitmem lazım"),
            Message(senderName: "Emir", senderId: "m4", content: "Babacım topu nereye koydun? ⚽"),
        ]
        messagesList.forEach { modelContext.insert($0) }
        
        // Invoices
        let invoicesList = [
            Invoice(title: "Elektrik Faturası", amount: 280.0, category: "Faturalar", status: "pending", createdBy: "Mehmet"),
            Invoice(title: "İnternet Faturası", amount: 120.0, category: "Faturalar", status: "paid", createdBy: "Mehmet"),
            Invoice(title: "Su Faturası", amount: 95.0, category: "Faturalar", status: "pending", createdBy: "Mehmet"),
        ]
        invoicesList.forEach { modelContext.insert($0) }
        
        // Sports Clubs
        let clubs = [
            SportsClub(name: "World Fitness Gym", type: "Spor Salonu", monthlyFee: 450, memberId: "m1"),
            SportsClub(name: "Olimpik Yüzme Havuzu", type: "Yüzme", monthlyFee: 300, memberId: "m2"),
            SportsClub(name: "Çocuklar Futbol Kulübü", type: "Futbol", monthlyFee: 200, memberId: "m4"),
        ]
        clubs.forEach { modelContext.insert($0) }
        
        // Workout Logs
        let workouts = [
            WorkoutLog(clubId: clubs[0].id, memberId: "m1", workoutType: "Kardiyo", duration: 45, caloriesBurned: 350, date: formatDate(Date())),
            WorkoutLog(clubId: clubs[0].id, memberId: "m1", workoutType: "Ağırlık", duration: 60, caloriesBurned: 400, date: formatDate(Date(timeIntervalSinceNow: -86400))),
            WorkoutLog(clubId: clubs[1].id, memberId: "m2", workoutType: "Yüzme", duration: 30, caloriesBurned: 250, date: formatDate(Date())),
        ]
        workouts.forEach { modelContext.insert($0) }
        
        // Calorie Logs
        let calories = [
            CalorieLog(memberId: "m1", mealType: "Kahvaltı", foodName: "Yumurta + Peynir", calories: 350, protein: 25, carbs: 10, fat: 22, servingSize: "1 porsiyon", date: formatDate(Date())),
            CalorieLog(memberId: "m1", mealType: "Öğle", foodName: "Tavuk Salata", calories: 420, protein: 35, carbs: 15, fat: 25, servingSize: "1 porsiyon", date: formatDate(Date())),
            CalorieLog(memberId: "m2", mealType: "Kahvaltı", foodName: "Müsli + Süt", calories: 280, protein: 12, carbs: 45, fat: 8, servingSize: "1 kase", date: formatDate(Date())),
        ]
        calories.forEach { modelContext.insert($0) }
        
        // Water Logs
        let waters = [
            WaterLog(memberId: "m1", amountMl: 250, drinkType: "Su", date: formatDate(Date())),
            WaterLog(memberId: "m1", amountMl: 200, drinkType: "Çay", date: formatDate(Date())),
            WaterLog(memberId: "m2", amountMl: 250, drinkType: "Su", date: formatDate(Date())),
            WaterLog(memberId: "m2", amountMl: 300, drinkType: "Meyve Suyu", date: formatDate(Date())),
        ]
        waters.forEach { modelContext.insert($0) }
        
        // Budgets
        let budgetList = [
            Budget(category: "Market", monthlyLimit: 5000, spentAmount: 2800, monthYear: formatDate(Date())),
            Budget(category: "Faturalar", monthlyLimit: 1000, spentAmount: 495, monthYear: formatDate(Date())),
            Budget(category: "Eğlence", monthlyLimit: 2000, spentAmount: 750, monthYear: formatDate(Date())),
        ]
        budgetList.forEach { modelContext.insert($0) }
        
        // Reminders
        let reminderList = [
            Reminder(title: "İlaç Zamanı", description: "Tansiyon ilacı", reminderTime: Date().addingTimeInterval(3600), repeatType: "daily", category: "Sağlık", priority: "yüksek", alarmSound: "alarm"),
            Reminder(title: "Okul Servisi", description: "Zeynep ve Emir'i hazırla", reminderTime: Date().addingTimeInterval(28800), repeatType: "daily", category: "Etkinlik", priority: "yüksek", alarmSound: "bell"),
            Reminder(title: "Fatura Ödeme", description: "Elektrik faturası son gün", reminderTime: Date().addingTimeInterval(86400 * 3), repeatType: "once", category: "Fatura", priority: "orta"),
        ]
        reminderList.forEach { modelContext.insert($0) }
        
        // Inventory Items
        let inventoryList = [
            InventoryItem(name: "Bulaşık Deterjanı", category: "Temizlik", quantity: 2, unit: "adet", location: "Mutfak"),
            InventoryItem(name: "Tuvalet Kağıdı", category: "Temizlik", quantity: 12, unit: "rulo", location: "Banyo"),
            InventoryItem(name: "Pil (AA)", category: "Ev Gereçleri", quantity: 8, unit: "adet", location: "Çekmece"),
            InventoryItem(name: "İlk Yardım Çantası", category: "Sağlık", quantity: 1, unit: "adet", location: "Koridor"),
        ]
        inventoryList.forEach { modelContext.insert($0) }
        
        // Meal Plans
        let mealPlanList = [
            MealPlan(dayOfWeek: 1, mealType: "Kahvaltı", dish: "Menemen", notes: "Domates ağırlıklı"),
            MealPlan(dayOfWeek: 1, mealType: "Öğle", dish: "Mercimek Çorbası + Salata"),
            MealPlan(dayOfWeek: 1, mealType: "Akşam", dish: "Tavuk Izgara + Pirinç Pilavı"),
            MealPlan(dayOfWeek: 2, mealType: "Kahvaltı", dish: "Yumurta + Peynir + Zeytin"),
            MealPlan(dayOfWeek: 2, mealType: "Öğle", dish: "İskender Kebap"),
            MealPlan(dayOfWeek: 2, mealType: "Akşam", dish: "Balık + Çoban Salata"),
            MealPlan(dayOfWeek: 3, mealType: "Kahvaltı", dish: "Müsli + Meyve"),
            MealPlan(dayOfWeek: 3, mealType: "Öğle", dish: "Tavuk Sote + Makarna"),
            MealPlan(dayOfWeek: 3, mealType: "Akşam", dish: "Kuru Fasulye + Pilav"),
        ]
        mealPlanList.forEach { modelContext.insert($0) }
        
        save()
        loadData()
    }
}
