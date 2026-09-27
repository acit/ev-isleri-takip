import Foundation
import SwiftData

/// Tek bir senkron satırı: kimlik + gönderilebilir içerik (Android `SyncRow` paritesi).
struct SyncRow {
    let id: String
    let content: [String: Any]
}

/// Tüm senkronize modellerin paylaştığı arayüz: kimlik alanı.
/// `#Predicate` genel tip parametresiyle kullanılamadığından kimlik eşleştirmesi
/// bu protokol üzerinden yapılır.
protocol IdentifiableIDModel: PersistentModel {
    var id: String { get }
}

/// SwiftData ↔ Firebase JSON dönüşüm katmanı.
///
/// Wire format Android (`FirebaseSyncService` toMap/parse fonksiyonları) ile
/// birebir aynıdır: alan adları, tipler ve varsayılanlar eşleşir. Zaman alanları
/// epoch milisaniye (Int64), iOS modellerindeki `Date` ile çevrilerek yazılır.
struct SyncDataCoordinator {

    let modelContext: ModelContext

    // MARK: - Anlık görüntü (yerel → gönderilebilir)

    func snapshot(table: String) -> [SyncRow] {
        switch table {
        case "members":          return fetch(FamilyMember.self).map { SyncRow(id: $0.id, content: memberToMap($0)) }
        case "tasks":            return fetch(Task.self).map { SyncRow(id: $0.id, content: taskToMap($0)) }
        case "shopping":         return fetch(ShoppingItem.self).map { SyncRow(id: $0.id, content: shoppingToMap($0)) }
        case "messages":         return fetch(Message.self).map { SyncRow(id: $0.id, content: messageToMap($0)) }
        case "invoices":         return fetch(Invoice.self).map { SyncRow(id: $0.id, content: invoiceToMap($0)) }
        case "budgets":          return fetch(Budget.self).map { SyncRow(id: $0.id, content: budgetToMap($0)) }
        case "expenses":         return fetch(Expense.self).map { SyncRow(id: $0.id, content: expenseToMap($0)) }
        case "inventory":        return fetch(InventoryItem.self).map { SyncRow(id: $0.id, content: inventoryToMap($0)) }
        case "meal_plans":       return fetch(MealPlan.self).map { SyncRow(id: $0.id, content: mealPlanToMap($0)) }
        case "sports_clubs":     return fetch(SportsClub.self).map { SyncRow(id: $0.id, content: sportsClubToMap($0)) }
        case "workout_logs":     return fetch(WorkoutLog.self).map { SyncRow(id: $0.id, content: workoutToMap($0)) }
        case "calorie_logs":     return fetch(CalorieLog.self).map { SyncRow(id: $0.id, content: calorieToMap($0)) }
        case "menstrual_cycles": return fetch(MenstrualCycle.self).map { SyncRow(id: $0.id, content: menstrualCycleToMap($0)) }
        case "notes":            return fetch(Note.self).map { SyncRow(id: $0.id, content: noteToMap($0)) }
        case "reminders":        return fetch(Reminder.self).map { SyncRow(id: $0.id, content: reminderToMap($0)) }
        case "water_logs":       return fetch(WaterLog.self).map { SyncRow(id: $0.id, content: waterLogToMap($0)) }
        case "sleep_logs":       return fetch(SleepLog.self).map { SyncRow(id: $0.id, content: sleepLogToMap($0)) }
        default:                 return []
        }
    }

    private func fetch<T: PersistentModel>(_ type: T.Type) -> [T] {
        (try? modelContext.fetch(FetchDescriptor<T>())) ?? []
    }

    // MARK: - Uzak veriyi uygula (uzak → yerel)

    /// Uzak kaydı yerel veritabanına yazar (varsa günceller, yoksa ekler).
    @discardableResult
    func applyRemote(table: String, map: [String: Any]) -> Bool {
        switch table {
        case "members":          upsertMember(map)
        case "tasks":            upsertTask(map)
        case "shopping":         upsertShopping(map)
        case "messages":         upsertMessage(map)
        case "invoices":         upsertInvoice(map)
        case "budgets":          upsertBudget(map)
        case "expenses":         upsertExpense(map)
        case "inventory":        upsertInventory(map)
        case "meal_plans":       upsertMealPlan(map)
        case "sports_clubs":     upsertSportsClub(map)
        case "workout_logs":     upsertWorkout(map)
        case "calorie_logs":     upsertCalorie(map)
        case "menstrual_cycles": upsertMenstrualCycle(map)
        case "notes":            upsertNote(map)
        case "reminders":        upsertReminder(map)
        case "water_logs":       upsertWaterLog(map)
        case "sleep_logs":       upsertSleepLog(map)
        default:                 return false
        }
        try? modelContext.save()
        return true
    }

    /// Yerel kaydı siler (mezar taşı uygulanırken kullanılır).
    func deleteLocal(table: String, id: String) {
        func deleteBy<T: PersistentModel & IdentifiableIDModel>(_ type: T.Type, _ id: String) {
            let found = fetch(type).filter { $0.id == id }
            guard !found.isEmpty else { return }
            found.forEach { modelContext.delete($0) }
            try? modelContext.save()
        }
        switch table {
        case "members":          deleteBy(FamilyMember.self, id)
        case "tasks":            deleteBy(Task.self, id)
        case "shopping":         deleteBy(ShoppingItem.self, id)
        case "messages":         deleteBy(Message.self, id)
        case "invoices":         deleteBy(Invoice.self, id)
        case "budgets":          deleteBy(Budget.self, id)
        case "expenses":         deleteBy(Expense.self, id)
        case "inventory":        deleteBy(InventoryItem.self, id)
        case "meal_plans":       deleteBy(MealPlan.self, id)
        case "sports_clubs":     deleteBy(SportsClub.self, id)
        case "workout_logs":     deleteBy(WorkoutLog.self, id)
        case "calorie_logs":     deleteBy(CalorieLog.self, id)
        case "menstrual_cycles": deleteBy(MenstrualCycle.self, id)
        case "notes":            deleteBy(Note.self, id)
        case "reminders":        deleteBy(Reminder.self, id)
        case "water_logs":       deleteBy(WaterLog.self, id)
        case "sleep_logs":       deleteBy(SleepLog.self, id)
        default:                 break
        }
    }

    // MARK: - Ortak dönüşüm yardımcıları

    private static func millis(_ date: Date?) -> Int64 {
        guard let date = date else { return 0 }
        return Int64(date.timeIntervalSince1970 * 1000)
    }

    private static func date(_ value: Any?) -> Date {
        if let n = value as? NSNumber, n.doubleValue > 0 {
            return Date(timeIntervalSince1970: n.doubleValue / 1000)
        }
        if let d = value as? Double, d > 0 {
            return Date(timeIntervalSince1970: d / 1000)
        }
        return Date(timeIntervalSince1970: 0)
    }

    private static func str(_ value: Any?) -> String { value as? String ?? "" }
    private static func int(_ value: Any?) -> Int { (value as? NSNumber)?.intValue ?? 0 }
    private static func double(_ value: Any?) -> Double { (value as? NSNumber)?.doubleValue ?? 0.0 }
    private static func bool(_ value: Any?) -> Bool { (value as? NSNumber)?.boolValue ?? false }

    /// Kaydı kimliğine göre bulur; yoksa verilen kurucuyu çağırıp ekler.
    /// Not: #Predicate makrosu genel tip parametresiyle derlenemediği için
    /// filtreleme bellekte yapılır (aile ölçeğindeki veri için yeterli).
    private func upsert<T: PersistentModel>(_ type: T.Type, id: String, make: @escaping () -> T, update: (T) -> Void) where T: IdentifiableIDModel {
        if let existing = fetch(type).first(where: { $0.id == id }) {
            update(existing)
        } else {
            modelContext.insert(make())
        }
    }

    // MARK: - members

    private func memberToMap(_ m: FamilyMember) -> [String: Any] {
        ["id": m.id, "name": m.name, "role": m.role, "color": m.color,
         "points": m.points, "createdAt": Self.millis(m.createdAt), "syncVersion": Self.millis(m.syncVersion)]
    }

    private func upsertMember(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(FamilyMember.self, id: id,
               make: { FamilyMember(id: id, name: Self.str(d["name"]), role: Self.str(d["role"]), color: Self.str(d["color"]), points: Self.int(d["points"])) },
               update: { m in
                   m.name = Self.str(d["name"]); m.role = Self.str(d["role"])
                   m.color = Self.str(d["color"]); m.points = Self.int(d["points"])
                   m.createdAt = Self.date(d["createdAt"]); m.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - tasks

    private func taskToMap(_ t: Task) -> [String: Any] {
        ["id": t.id, "title": t.title, "description": t.description, "category": t.category,
         "priority": t.priority, "assignee": t.assignee, "status": t.status, "dueDate": t.dueDate,
         "createdAt": Self.millis(t.createdAt), "completedAt": Self.millis(t.completedAt),
         "syncVersion": Self.millis(t.syncVersion)]
    }

    private func upsertTask(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        let completed = Self.millis(Optional(Self.date(d["completedAt"])))
        let completedDate: Date? = completed > 0 ? Date(timeIntervalSince1970: Double(completed) / 1000) : nil
        upsert(Task.self, id: id,
               make: { Task(id: id, title: Self.str(d["title"]), description: Self.str(d["description"]),
                            category: Self.str(d["category"]), priority: Self.str(d["priority"]),
                            assignee: Self.str(d["assignee"]), status: Self.str(d["status"]), dueDate: Self.str(d["dueDate"])) },
               update: { t in
                   t.title = Self.str(d["title"]); t.description = Self.str(d["description"])
                   t.category = Self.str(d["category"]); t.priority = Self.str(d["priority"])
                   t.assignee = Self.str(d["assignee"]); t.status = Self.str(d["status"])
                   t.dueDate = Self.str(d["dueDate"]); t.createdAt = Self.date(d["createdAt"])
                   t.completedAt = completedDate; t.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - shopping

    private func shoppingToMap(_ s: ShoppingItem) -> [String: Any] {
        // iOS modelinde bulunmayan Android alanları varsayılanlarla gönderilir (format uyumu)
        ["id": s.id, "name": s.name, "quantity": s.quantity, "category": s.category,
         "checked": s.checked, "addedBy": s.addedBy, "barcode": "", "brand": "",
         "description": "", "imageBase64": "", "unitPrice": 0.0, "totalPrice": 0.0,
         "store": "", "notes": "", "lastPurchaseDate": "",
         "createdAt": Self.millis(s.createdAt), "syncVersion": Self.millis(s.syncVersion)]
    }

    private func upsertShopping(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(ShoppingItem.self, id: id,
               make: { ShoppingItem(id: id, name: Self.str(d["name"]), quantity: Self.int(d["quantity"]),
                                    category: Self.str(d["category"]), checked: Self.bool(d["checked"]),
                                    addedBy: Self.str(d["addedBy"])) },
               update: { s in
                   s.name = Self.str(d["name"]); s.quantity = Self.int(d["quantity"])
                   s.category = Self.str(d["category"]); s.checked = Self.bool(d["checked"])
                   s.addedBy = Self.str(d["addedBy"])
                   s.createdAt = Self.date(d["createdAt"]); s.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - messages

    private func messageToMap(_ m: Message) -> [String: Any] {
        ["id": m.id, "senderName": m.senderName, "senderId": m.senderId, "content": m.content,
         "channel": m.channel, "read": m.read, "attachments": m.attachments,
         "createdAt": Self.millis(m.createdAt), "syncVersion": Self.millis(m.syncVersion)]
    }

    private func upsertMessage(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(Message.self, id: id,
               make: { Message(id: id, senderName: Self.str(d["senderName"]), senderId: Self.str(d["senderId"]),
                               content: Self.str(d["content"]), channel: Self.str(d["channel"]),
                               attachments: Self.str(d["attachments"])) },
               update: { m in
                   m.senderName = Self.str(d["senderName"]); m.senderId = Self.str(d["senderId"])
                   m.content = Self.str(d["content"]); m.channel = Self.str(d["channel"])
                   m.read = Self.bool(d["read"]); m.attachments = Self.str(d["attachments"])
                   m.createdAt = Self.date(d["createdAt"]); m.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - invoices

    private func invoiceToMap(_ i: Invoice) -> [String: Any] {
        var map: [String: Any] = ["id": i.id, "title": i.title, "amount": i.amount, "category": i.category,
                                  "dueDate": i.dueDate, "notes": i.notes, "status": i.status,
                                  "createdBy": i.createdBy, "createdAt": Self.millis(i.createdAt),
                                  "syncVersion": Self.millis(i.syncVersion)]
        // null bırakılırsa anahtar hiç yazılmaz (Android paritesi)
        if let image = i.imageBase64 { map["imageBase64"] = image }
        return map
    }

    private func upsertInvoice(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(Invoice.self, id: id,
               make: { Invoice(id: id, title: Self.str(d["title"]), amount: Self.double(d["amount"]),
                               category: Self.str(d["category"]), dueDate: Self.str(d["dueDate"]),
                               notes: Self.str(d["notes"]), status: Self.str(d["status"]),
                               createdBy: Self.str(d["createdBy"])) },
               update: { i in
                   i.title = Self.str(d["title"]); i.amount = Self.double(d["amount"])
                   i.category = Self.str(d["category"]); i.dueDate = Self.str(d["dueDate"])
                   i.notes = Self.str(d["notes"]); i.imageBase64 = d["imageBase64"] as? String
                   i.status = Self.str(d["status"]); i.createdBy = Self.str(d["createdBy"])
                   i.createdAt = Self.date(d["createdAt"]); i.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - budgets

    private func budgetToMap(_ b: Budget) -> [String: Any] {
        ["id": b.id, "category": b.category, "monthlyLimit": b.monthlyLimit,
         "spentAmount": b.spentAmount, "monthYear": b.monthYear,
         "createdAt": Self.millis(b.createdAt), "syncVersion": Self.millis(b.syncVersion)]
    }

    private func upsertBudget(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(Budget.self, id: id,
               make: { Budget(id: id, category: Self.str(d["category"]), monthlyLimit: Self.double(d["monthlyLimit"]),
                              spentAmount: Self.double(d["spentAmount"]), monthYear: Self.str(d["monthYear"])) },
               update: { b in
                   b.category = Self.str(d["category"]); b.monthlyLimit = Self.double(d["monthlyLimit"])
                   b.spentAmount = Self.double(d["spentAmount"]); b.monthYear = Self.str(d["monthYear"])
                   b.createdAt = Self.date(d["createdAt"]); b.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - expenses

    private func expenseToMap(_ e: Expense) -> [String: Any] {
        ["id": e.id, "budgetId": e.budgetId, "category": e.category, "amount": e.amount,
         "description": e.description, "expenseDate": e.expenseDate,
         "createdAt": Self.millis(e.createdAt), "syncVersion": Self.millis(e.syncVersion)]
    }

    private func upsertExpense(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(Expense.self, id: id,
               make: { Expense(id: id, budgetId: Self.str(d["budgetId"]), category: Self.str(d["category"]),
                               amount: Self.double(d["amount"]), description: Self.str(d["description"]),
                               expenseDate: Self.str(d["expenseDate"])) },
               update: { e in
                   e.budgetId = Self.str(d["budgetId"]); e.category = Self.str(d["category"])
                   e.amount = Self.double(d["amount"]); e.description = Self.str(d["description"])
                   e.expenseDate = Self.str(d["expenseDate"])
                   e.createdAt = Self.date(d["createdAt"]); e.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - inventory

    private func inventoryToMap(_ i: InventoryItem) -> [String: Any] {
        ["id": i.id, "name": i.name, "category": i.category, "quantity": i.quantity,
         "unit": i.unit, "minStock": i.minStock, "location": i.location, "notes": i.notes,
         "imageBase64": "", "lastPrice": 0.0, "lastStore": "", "lastPurchaseDate": "",
         "averagePrice": 0.0, "priceHistory": "",
         "createdAt": Self.millis(i.createdAt), "syncVersion": Self.millis(i.syncVersion)]
    }

    private func upsertInventory(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(InventoryItem.self, id: id,
               make: { InventoryItem(id: id, name: Self.str(d["name"]), category: Self.str(d["category"]),
                                     quantity: Self.int(d["quantity"]), unit: Self.str(d["unit"]),
                                     minStock: Self.int(d["minStock"]), location: Self.str(d["location"]),
                                     notes: Self.str(d["notes"])) },
               update: { i in
                   i.name = Self.str(d["name"]); i.category = Self.str(d["category"])
                   i.quantity = Self.int(d["quantity"]); i.unit = Self.str(d["unit"])
                   i.minStock = Self.int(d["minStock"]); i.location = Self.str(d["location"])
                   i.notes = Self.str(d["notes"])
                   i.createdAt = Self.date(d["createdAt"]); i.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - meal_plans

    private func mealPlanToMap(_ mp: MealPlan) -> [String: Any] {
        ["id": mp.id, "dayOfWeek": mp.dayOfWeek, "mealType": mp.mealType, "dish": mp.dish,
         "notes": mp.notes, "createdAt": Self.millis(mp.createdAt), "syncVersion": Self.millis(mp.syncVersion)]
    }

    private func upsertMealPlan(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(MealPlan.self, id: id,
               make: { MealPlan(id: id, dayOfWeek: Self.int(d["dayOfWeek"]), mealType: Self.str(d["mealType"]),
                                dish: Self.str(d["dish"]), notes: Self.str(d["notes"])) },
               update: { mp in
                   mp.dayOfWeek = Self.int(d["dayOfWeek"]); mp.mealType = Self.str(d["mealType"])
                   mp.dish = Self.str(d["dish"]); mp.notes = Self.str(d["notes"])
                   mp.createdAt = Self.date(d["createdAt"]); mp.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - sports_clubs

    private func sportsClubToMap(_ c: SportsClub) -> [String: Any] {
        ["id": c.id, "name": c.name, "type": c.type, "address": c.address, "phone": c.phone,
         "membershipStart": c.membershipStart, "membershipEnd": c.membershipEnd,
         "monthlyFee": c.monthlyFee, "isActive": c.isActive, "memberId": c.memberId, "notes": c.notes,
         "createdAt": Self.millis(c.createdAt), "syncVersion": Self.millis(c.syncVersion)]
    }

    private func upsertSportsClub(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(SportsClub.self, id: id,
               make: { SportsClub(id: id, name: Self.str(d["name"]), type: Self.str(d["type"]),
                                  address: Self.str(d["address"]), phone: Self.str(d["phone"]),
                                  membershipStart: Self.str(d["membershipStart"]),
                                  membershipEnd: Self.str(d["membershipEnd"]),
                                  monthlyFee: Self.double(d["monthlyFee"]),
                                  isActive: Self.bool(d["isActive"]),
                                  memberId: Self.str(d["memberId"]), notes: Self.str(d["notes"])) },
               update: { c in
                   c.name = Self.str(d["name"]); c.type = Self.str(d["type"])
                   c.address = Self.str(d["address"]); c.phone = Self.str(d["phone"])
                   c.membershipStart = Self.str(d["membershipStart"])
                   c.membershipEnd = Self.str(d["membershipEnd"])
                   c.monthlyFee = Self.double(d["monthlyFee"]); c.isActive = Self.bool(d["isActive"])
                   c.memberId = Self.str(d["memberId"]); c.notes = Self.str(d["notes"])
                   c.createdAt = Self.date(d["createdAt"]); c.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - workout_logs

    private func workoutToMap(_ w: WorkoutLog) -> [String: Any] {
        ["id": w.id, "clubId": w.clubId, "memberId": w.memberId, "workoutType": w.workoutType,
         "duration": w.duration, "caloriesBurned": w.caloriesBurned, "date": w.date, "notes": w.notes,
         "createdAt": Self.millis(w.createdAt), "syncVersion": Self.millis(w.syncVersion)]
    }

    private func upsertWorkout(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(WorkoutLog.self, id: id,
               make: { WorkoutLog(id: id, clubId: Self.str(d["clubId"]), memberId: Self.str(d["memberId"]),
                                  workoutType: Self.str(d["workoutType"]), duration: Self.int(d["duration"]),
                                  caloriesBurned: Self.int(d["caloriesBurned"]), date: Self.str(d["date"]),
                                  notes: Self.str(d["notes"])) },
               update: { w in
                   w.clubId = Self.str(d["clubId"]); w.memberId = Self.str(d["memberId"])
                   w.workoutType = Self.str(d["workoutType"]); w.duration = Self.int(d["duration"])
                   w.caloriesBurned = Self.int(d["caloriesBurned"]); w.date = Self.str(d["date"])
                   w.notes = Self.str(d["notes"])
                   w.createdAt = Self.date(d["createdAt"]); w.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - calorie_logs

    private func calorieToMap(_ c: CalorieLog) -> [String: Any] {
        ["id": c.id, "memberId": c.memberId, "mealType": c.mealType, "foodName": c.foodName,
         "calories": c.calories, "protein": c.protein, "carbs": c.carbs, "fat": c.fat,
         "servingSize": c.servingSize, "date": c.date,
         "createdAt": Self.millis(c.createdAt), "syncVersion": Self.millis(c.syncVersion)]
    }

    private func upsertCalorie(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(CalorieLog.self, id: id,
               make: { CalorieLog(id: id, memberId: Self.str(d["memberId"]), mealType: Self.str(d["mealType"]),
                                  foodName: Self.str(d["foodName"]), calories: Self.int(d["calories"]),
                                  protein: Self.double(d["protein"]), carbs: Self.double(d["carbs"]),
                                  fat: Self.double(d["fat"]), servingSize: Self.str(d["servingSize"]),
                                  date: Self.str(d["date"])) },
               update: { c in
                   c.memberId = Self.str(d["memberId"]); c.mealType = Self.str(d["mealType"])
                   c.foodName = Self.str(d["foodName"]); c.calories = Self.int(d["calories"])
                   c.protein = Self.double(d["protein"]); c.carbs = Self.double(d["carbs"])
                   c.fat = Self.double(d["fat"]); c.servingSize = Self.str(d["servingSize"])
                   c.date = Self.str(d["date"])
                   c.createdAt = Self.date(d["createdAt"]); c.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - menstrual_cycles

    private func menstrualCycleToMap(_ c: MenstrualCycle) -> [String: Any] {
        ["id": c.id, "memberId": c.memberId, "startDate": c.startDate, "endDate": c.endDate,
         "cycleLength": c.cycleLength, "periodLength": c.periodLength, "symptoms": c.symptoms,
         "mood": c.mood, "flow": c.flow, "notes": c.notes, "isPersonal": c.isPersonal,
         "createdAt": Self.millis(c.createdAt), "syncVersion": Self.millis(c.syncVersion)]
    }

    private func upsertMenstrualCycle(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(MenstrualCycle.self, id: id,
               make: { MenstrualCycle(id: id, memberId: Self.str(d["memberId"]), startDate: Self.str(d["startDate"]),
                                      endDate: Self.str(d["endDate"]), cycleLength: Self.int(d["cycleLength"]),
                                      periodLength: Self.int(d["periodLength"]), symptoms: Self.str(d["symptoms"]),
                                      mood: Self.str(d["mood"]), flow: Self.str(d["flow"]),
                                      notes: Self.str(d["notes"]), isPersonal: Self.bool(d["isPersonal"])) },
               update: { c in
                   c.memberId = Self.str(d["memberId"]); c.startDate = Self.str(d["startDate"])
                   c.endDate = Self.str(d["endDate"]); c.cycleLength = Self.int(d["cycleLength"])
                   c.periodLength = Self.int(d["periodLength"]); c.symptoms = Self.str(d["symptoms"])
                   c.mood = Self.str(d["mood"]); c.flow = Self.str(d["flow"])
                   c.notes = Self.str(d["notes"]); c.isPersonal = Self.bool(d["isPersonal"])
                   c.createdAt = Self.date(d["createdAt"]); c.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - notes

    private func noteToMap(_ n: Note) -> [String: Any] {
        ["id": n.id, "title": n.title, "content": n.content, "category": n.category,
         "color": n.color, "isPinned": n.isPinned, "isArchived": n.isArchived,
         "attachments": n.attachments, "createdBy": n.createdBy,
         "createdAt": Self.millis(n.createdAt), "syncVersion": Self.millis(n.syncVersion)]
    }

    private func upsertNote(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(Note.self, id: id,
               make: { Note(id: id, title: Self.str(d["title"]), content: Self.str(d["content"]),
                            category: Self.str(d["category"]), color: Self.str(d["color"]),
                            isPinned: Self.bool(d["isPinned"]), attachments: Self.str(d["attachments"]),
                            createdBy: Self.str(d["createdBy"])) },
               update: { n in
                   n.title = Self.str(d["title"]); n.content = Self.str(d["content"])
                   n.category = Self.str(d["category"]); n.color = Self.str(d["color"])
                   n.isPinned = Self.bool(d["isPinned"]); n.isArchived = Self.bool(d["isArchived"])
                   n.attachments = Self.str(d["attachments"]); n.createdBy = Self.str(d["createdBy"])
                   n.createdAt = Self.date(d["createdAt"]); n.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - reminders

    private func reminderToMap(_ r: Reminder) -> [String: Any] {
        ["id": r.id, "title": r.title, "description": r.description,
         "reminderTime": Self.millis(r.reminderTime), "repeatType": r.repeatType,
         "repeatDays": r.repeatDays, "repeatInterval": r.repeatInterval,
         "repeatEndDate": Self.millis(r.repeatEndDate), "category": r.category,
         "priority": r.priority, "alarmSound": r.alarmSound, "vibrate": r.vibrate,
         "snoozeMinutes": r.snoozeMinutes, "isCompleted": r.isCompleted, "isSnoozed": r.isSnoozed,
         "snoozeUntil": Self.millis(r.snoozeUntil), "lastFiredAt": Self.millis(r.lastFiredAt),
         "nextFireAt": Self.millis(r.nextFireAt), "linkedId": r.linkedId, "linkedType": r.linkedType,
         "createdBy": r.createdBy, "createdAt": Self.millis(r.createdAt), "syncVersion": Self.millis(r.syncVersion)]
    }

    private func upsertReminder(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        let reminderMillis = Self.millis(Optional(Self.date(d["reminderTime"])))
        let reminderDate = Date(timeIntervalSince1970: Double(max(reminderMillis, 0)) / 1000)
        upsert(Reminder.self, id: id,
               make: { Reminder(id: id, title: Self.str(d["title"]), description: Self.str(d["description"]),
                                reminderTime: reminderDate, repeatType: Self.str(d["repeatType"]),
                                category: Self.str(d["category"]), priority: Self.str(d["priority"]),
                                alarmSound: Self.str(d["alarmSound"]), createdBy: Self.str(d["createdBy"])) },
               update: { r in
                   r.title = Self.str(d["title"]); r.description = Self.str(d["description"])
                   r.reminderTime = reminderDate; r.repeatType = Self.str(d["repeatType"])
                   r.repeatDays = Self.str(d["repeatDays"]); r.repeatInterval = Self.int(d["repeatInterval"])
                   r.category = Self.str(d["category"]); r.priority = Self.str(d["priority"])
                   r.alarmSound = Self.str(d["alarmSound"]); r.vibrate = Self.bool(d["vibrate"])
                   r.snoozeMinutes = Self.int(d["snoozeMinutes"]); r.isCompleted = Self.bool(d["isCompleted"])
                   r.isSnoozed = Self.bool(d["isSnoozed"])
                   r.snoozeUntil = Self.optionalDate(d["snoozeUntil"])
                   r.lastFiredAt = Self.optionalDate(d["lastFiredAt"])
                   r.nextFireAt = Self.optionalDate(d["nextFireAt"])
                   r.linkedId = Self.str(d["linkedId"]); r.linkedType = Self.str(d["linkedType"])
                   r.createdBy = Self.str(d["createdBy"])
                   r.createdAt = Self.date(d["createdAt"]); r.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - water_logs

    private func waterLogToMap(_ w: WaterLog) -> [String: Any] {
        ["id": w.id, "memberId": w.memberId, "amountMl": w.amountMl, "drinkType": w.drinkType,
         "date": w.date, "createdAt": Self.millis(w.createdAt), "syncVersion": Self.millis(w.syncVersion)]
    }

    private func upsertWaterLog(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(WaterLog.self, id: id,
               make: { WaterLog(id: id, memberId: Self.str(d["memberId"]), amountMl: Self.int(d["amountMl"]),
                                drinkType: Self.str(d["drinkType"]), date: Self.str(d["date"])) },
               update: { w in
                   w.memberId = Self.str(d["memberId"]); w.amountMl = Self.int(d["amountMl"])
                   w.drinkType = Self.str(d["drinkType"]); w.date = Self.str(d["date"])
                   w.createdAt = Self.date(d["createdAt"]); w.syncVersion = Self.date(d["syncVersion"])
               })
    }

    // MARK: - sleep_logs

    private func sleepLogToMap(_ s: SleepLog) -> [String: Any] {
        ["id": s.id, "memberId": s.memberId, "bedtime": Self.millis(s.bedtime),
         "wakeTime": Self.millis(s.wakeTime), "durationMinutes": s.durationMinutes,
         "quality": s.quality, "interruptions": s.interruptions, "notes": s.notes, "date": s.date,
         "createdAt": Self.millis(s.createdAt), "syncVersion": Self.millis(s.syncVersion)]
    }

    private func upsertSleepLog(_ d: [String: Any]) {
        guard let id = d["id"] as? String else { return }
        upsert(SleepLog.self, id: id,
               make: { SleepLog(id: id, memberId: Self.str(d["memberId"]),
                                bedtime: Self.date(d["bedtime"]), wakeTime: Self.date(d["wakeTime"]),
                                quality: Self.str(d["quality"]), interruptions: Self.int(d["interruptions"]),
                                notes: Self.str(d["notes"]), date: Self.str(d["date"])) },
               update: { s in
                   s.memberId = Self.str(d["memberId"]); s.bedtime = Self.date(d["bedtime"])
                   s.wakeTime = Self.date(d["wakeTime"]); s.quality = Self.str(d["quality"])
                   s.interruptions = Self.int(d["interruptions"]); s.notes = Self.str(d["notes"])
                   s.date = Self.str(d["date"])
                   s.createdAt = Self.date(d["createdAt"]); s.syncVersion = Self.date(d["syncVersion"])
               })
    }
}

private extension SyncDataCoordinator {
    /// 0/eksik değerleri nil'e çevirir (iOS modelindeki opsiyonel Date alanları için).
    static func optionalDate(_ value: Any?) -> Date? {
        let millisValue: Int64
        if let n = value as? NSNumber { millisValue = n.int64Value }
        else if let d = value as? Double { millisValue = Int64(d) }
        else { return nil }
        return millisValue > 0 ? Date(timeIntervalSince1970: Double(millisValue) / 1000) : nil
    }
}
