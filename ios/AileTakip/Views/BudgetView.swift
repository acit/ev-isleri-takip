import SwiftUI

struct BudgetView: View {
    @EnvironmentObject var vm: MainViewModel
    @State private var showAddBudget = false
    @State private var showAddExpense = false
    @State private var selectedPeriod = "Bu Ay"
    
    let periods = ["Bu Ay", "Geçen Ay", "Özel"]
    
    var totalBudget: Double {
        vm.budgets.reduce(0) { $0 + $1.monthlyLimit }
    }
    
    var totalSpent: Double {
        vm.expenses.reduce(0) { $0 + $1.amount }
    }
    
    var spentPercentage: Double {
        totalBudget > 0 ? min(totalSpent / totalBudget, 1.0) : 0
    }
    
    var remainingBudget: Double {
        max(totalBudget - totalSpent, 0)
    }
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Overview Card
                    BudgetOverviewCard(
                        totalBudget: totalBudget,
                        totalSpent: totalSpent,
                        remaining: remainingBudget,
                        percentage: spentPercentage
                    )
                    
                    // Quick Actions
                    HStack(spacing: 12) {
                        BudgetActionButton(
                            icon: "plus.circle.fill",
                            title: "Bütçe Ekle",
                            color: .blue
                        ) {
                            showAddBudget = true
                        }
                        
                        BudgetActionButton(
                            icon: "cart.fill.badge.plus",
                            title: "Harcama Ekle",
                            color: .orange
                        ) {
                            showAddExpense = true
                        }
                    }
                    .padding(.horizontal)
                    
                    // Budget Categories
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Bütçe Kategorileri")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        if vm.budgets.isEmpty {
                            EmptyBudgetCard()
                        } else {
                            ForEach(vm.budgets) { budget in
                                BudgetCategoryCard(budget: budget, expenses: vm.expenses)
                            }
                        }
                    }
                    
                    // Recent Expenses
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Son Harcamalar")
                                .font(.headline)
                            Spacer()
                            Text("\(vm.expenses.count) harcama")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .padding(.horizontal)
                        
                        if vm.expenses.isEmpty {
                            EmptyExpenseCard()
                        } else {
                            ForEach(vm.expenses.prefix(10)) { expense in
                                ExpenseRow(expense: expense)
                            }
                        }
                    }
                }
                .padding(.vertical)
            }
            .navigationTitle("Bütçe")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        ForEach(periods, id: \.self) { period in
                            Button {
                                selectedPeriod = period
                            } label: {
                                HStack {
                                    Text(period)
                                    if selectedPeriod == period {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        Image(systemName: "calendar.circle")
                    }
                }
            }
            .sheet(isPresented: $showAddBudget) {
                AddBudgetSheet()
            }
            .sheet(isPresented: $showAddExpense) {
                AddExpenseSheet()
            }
        }
    }
}

// MARK: - Budget Overview Card
struct BudgetOverviewCard: View {
    let totalBudget: Double
    let totalSpent: Double
    let remaining: Double
    let percentage: Double
    
    var progressColor: Color {
        if percentage > 0.8 { return .red }
        if percentage > 0.6 { return .orange }
        return .green
    }
    
    var body: some View {
        VStack(spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Aylık Bütçe")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text("₺\(Int(totalBudget))")
                        .font(.title)
                        .fontWeight(.bold)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 4) {
                    Text("Kalan")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text("₺\(Int(remaining))")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundStyle(remaining > 0 ? .green : .red)
                }
            }
            
            // Progress Bar
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Color(.systemGray5))
                        .frame(height: 12)
                    
                    RoundedRectangle(cornerRadius: 6)
                        .fill(progressColor)
                        .frame(width: geometry.size.width * percentage, height: 12)
                        .animation(.easeInOut(duration: 0.5), value: percentage)
                }
            }
            .frame(height: 12)
            
            HStack {
                Label("₺\(Int(totalSpent)) harcandı", systemImage: "arrow.up.circle")
                    .font(.caption)
                    .foregroundStyle(.red)
                Spacer()
                Text("%\(Int(percentage * 100))")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundStyle(progressColor)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 5, y: 2)
        .padding(.horizontal)
    }
}

// MARK: - Budget Category Card
struct BudgetCategoryCard: View {
    let budget: Budget
    let expenses: [Expense]
    
    var categorySpent: Double {
        expenses.filter { $0.category == budget.category }.reduce(0) { $0 + $1.amount }
    }
    
    var percentage: Double {
        budget.monthlyLimit > 0 ? min(categorySpent / budget.monthlyLimit, 1.0) : 0
    }
    
    var categoryColor: Color {
        switch budget.category {
        case "Market": return .green
        case "Faturalar": return .blue
        case "Eğlence": return .purple
        case "Sağlık": return .red
        case "Eğitim": return .orange
        case "Ulaşım": return .cyan
        case "Giyim": return .pink
        case "Spor": return .mint
        default: return .gray
        }
    }
    
    var categoryIcon: String {
        switch budget.category {
        case "Market": return "cart.fill"
        case "Faturalar": return "doc.text.fill"
        case "Eğlence": return "gamecontroller.fill"
        case "Sağlık": return "heart.fill"
        case "Eğitim": return "book.fill"
        case "Ulaşım": return "car.fill"
        case "Giyim": return "tshirt.fill"
        case "Spor": return "figure.run"
        default: return "folder.fill"
        }
    }
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: categoryIcon)
                .font(.title2)
                .foregroundStyle(categoryColor)
                .frame(width: 40)
            
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(budget.category)
                        .font(.headline)
                    Spacer()
                    Text("₺\(Int(categorySpent)) / ₺\(Int(budget.monthlyLimit))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 3)
                            .fill(Color(.systemGray5))
                            .frame(height: 6)
                        
                        RoundedRectangle(cornerRadius: 3)
                            .fill(categoryColor)
                            .frame(width: geometry.size.width * percentage, height: 6)
                    }
                }
                .frame(height: 6)
                
                HStack {
                    Text("%\(Int(percentage * 100)) kullanıldı")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Spacer()
                    let remaining = budget.monthlyLimit - categorySpent
                    Text("₺\(Int(max(remaining, 0))) kaldı")
                        .font(.caption2)
                        .foregroundStyle(remaining > 0 ? .green : .red)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.03), radius: 3, y: 1)
        .padding(.horizontal)
    }
}

// MARK: - Budget Action Button
struct BudgetActionButton: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.title2)
                Text(title)
                    .font(.caption)
                    .fontWeight(.medium)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(color.opacity(0.1))
            .foregroundStyle(color)
            .cornerRadius(12)
        }
    }
}

// MARK: - Expense Row
struct ExpenseRow: View {
    let expense: Expense
    
    var categoryColor: Color {
        switch expense.category {
        case "Market": return .green
        case "Faturalar": return .blue
        case "Eğlence": return .purple
        case "Sağlık": return .red
        case "Eğitim": return .orange
        case "Ulaşım": return .cyan
        case "Giyim": return .pink
        case "Spor": return .mint
        default: return .gray
        }
    }
    
    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(categoryColor.opacity(0.2))
                .frame(width: 40, height: 40)
                .overlay(
                    Text(String(expense.category.prefix(1)))
                        .font(.headline)
                        .foregroundStyle(categoryColor)
                )
            
            VStack(alignment: .leading, spacing: 2) {
                Text(expense.description.isEmpty ? expense.category : expense.description)
                    .font(.headline)
                HStack {
                    Text(expense.category)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("•")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(expense.expenseDate)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            
            Spacer()
            
            Text("₺\(Int(expense.amount))")
                .font(.headline)
                .foregroundStyle(.red)
        }
        .padding(.horizontal)
        .padding(.vertical, 4)
    }
}

// MARK: - Empty States
struct EmptyBudgetCard: View {
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "tray")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text("Henüz bütçe eklenmemiş")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text("Aylık bütçe kategorileri oluşturarak harcamalarınızı takip edin")
                .font(.caption)
                .foregroundStyle(.tertiary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(32)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .padding(.horizontal)
    }
}

struct EmptyExpenseCard: View {
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "receipt")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text("Henüz harcama kaydı yok")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .padding(.horizontal)
    }
}

// MARK: - Add Budget Sheet
struct AddBudgetSheet: View {
    @EnvironmentObject var vm: MainViewModel
    @Environment(\.dismiss) var dismiss
    @State private var category = ""
    @State private var limit = ""
    
    let suggestions = ["Market", "Faturalar", "Eğlence", "Sağlık", "Eğitim", "Ulaşım", "Giyim", "Spor"]
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Kategori Seçin") {
                    ForEach(suggestions, id: \.self) { suggestion in
                        Button {
                            category = suggestion
                        } label: {
                            HStack {
                                Text(suggestion)
                                Spacer()
                                if category == suggestion {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(.blue)
                                }
                            }
                        }
                        .foregroundStyle(.primary)
                    }
                    
                    TextField("Özel kategori", text: $category)
                }
                
                Section("Aylık Limit") {
                    TextField("₺0", text: $limit)
                        .keyboardType(.numberPad)
                }
            }
            .navigationTitle("Bütçe Ekle")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("İptal") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Ekle") {
                        if let limitValue = Double(limit), !category.isEmpty {
                            let formatter = DateFormatter()
                            formatter.dateFormat = "yyyy-MM"
                            vm.addBudget(category: category, limit: limitValue, monthYear: formatter.string(from: Date()))
                            dismiss()
                        }
                    }
                    .disabled(category.isEmpty || Double(limit) == nil)
                }
            }
        }
    }
}

// MARK: - Add Expense Sheet
struct AddExpenseSheet: View {
    @EnvironmentObject var vm: MainViewModel
    @Environment(\.dismiss) var dismiss
    @State private var category = ""
    @State private var amount = ""
    @State private var description = ""
    @State private var date = Date()
    
    let suggestions = ["Market", "Faturalar", "Eğlence", "Sağlık", "Eğitim", "Ulaşım", "Giyim", "Spor"]
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Kategori Seçin") {
                    ForEach(suggestions, id: \.self) { suggestion in
                        Button {
                            category = suggestion
                        } label: {
                            HStack {
                                Text(suggestion)
                                Spacer()
                                if category == suggestion {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(.blue)
                                }
                            }
                        }
                        .foregroundStyle(.primary)
                    }
                    
                    TextField("Özel kategori", text: $category)
                }
                
                Section("Detaylar") {
                    TextField("₺0", text: $amount)
                        .keyboardType(.decimalPad)
                    TextField("Açıklama (isteğe bağlı)", text: $description)
                    DatePicker("Tarih", selection: $date, displayedComponents: .date)
                }
            }
            .navigationTitle("Harcama Ekle")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("İptal") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Ekle") {
                        if let amountValue = Double(amount), !category.isEmpty {
                            let formatter = DateFormatter()
                            formatter.dateFormat = "yyyy-MM-dd"
                            vm.addExpense(category: category, amount: amountValue, description: description, date: formatter.string(from: date))
                            dismiss()
                        }
                    }
                    .disabled(category.isEmpty || Double(amount) == nil)
                }
            }
        }
    }
}

#Preview {
    BudgetView()
        .environmentObject(MainViewModel())
}
