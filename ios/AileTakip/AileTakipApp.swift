import SwiftUI
import SwiftData

@main
struct AileTakipApp: App {
    @StateObject private var vm = MainViewModel()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(vm)
        }
        .modelContainer(for: [
            FamilyMember.self,
            Task.self,
            ShoppingItem.self,
            Message.self,
            Note.self,
            Reminder.self,
            Invoice.self,
            Budget.self,
            Expense.self,
            InventoryItem.self,
            MealPlan.self,
            SportsClub.self,
            WorkoutLog.self,
            CalorieLog.self,
            MenstrualCycle.self,
            WaterLog.self,
            SleepLog.self,
            UserAuth.self,
            SyncEvent.self,
            Attachment.self
        ])
    }
}
