import SwiftUI

struct ShoppingView: View {
    @EnvironmentObject var vm: MainViewModel
    @State private var showAddItem = false
    @State private var searchText = ""
    @State private var selectedCategory = "Tümü"
    @State private var showCheckedOnly = false
    @State private var editingItem: ShoppingItem?
    
    let categories = ["Tümü", "Market", "Temizlik", "Kişisel Bakım", "Atıştırmalık", "Ev Gereçleri", "Çocuk"]
    
    var filteredItems: [ShoppingItem] {
        var result = vm.shoppingItems
        
        if selectedCategory != "Tümü" {
            result = result.filter { $0.category == selectedCategory }
        }
        
        if showCheckedOnly {
            result = result.filter { $0.checked }
        }
        
        if !searchText.isEmpty {
            result = result.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
        }
        
        return result.sorted { !$0.checked && $1.checked }
    }
    
    var uncheckedCount: Int {
        vm.shoppingItems.filter { !$0.checked }.count
    }
    
    var checkedCount: Int {
        vm.shoppingItems.filter { $0.checked }.count
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Stats Bar
                HStack(spacing: 16) {
                    ShoppingStatBadge(count: uncheckedCount, label: "Alınacak", color: .orange, icon: "cart")
                    ShoppingStatBadge(count: checkedCount, label: "Alındı", color: .green, icon: "checkmark")
                    ShoppingStatBadge(count: vm.shoppingItems.count, label: "Toplam", color: .blue, icon: "list.bullet")
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                
                // Category Filter
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(categories, id: \.self) { category in
                            CategoryFilterButton(
                                title: category,
                                isSelected: selectedCategory == category,
                                count: category == "Tümü" ? vm.shoppingItems.count : vm.shoppingItems.filter { $0.category == category }.count
                            ) {
                                selectedCategory = category
                            }
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 8)
                }
                
                // Item List
                List {
                    if filteredItems.isEmpty {
                        ContentUnavailableView("Ürün bulunamadı", systemImage: "cart", description: Text("Yeni bir ürün ekleyin"))
                    } else {
                        ForEach(filteredItems) { item in
                            ShoppingItemRow(item: item) {
                                vm.toggleShoppingItem(item)
                            }
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    vm.deleteShoppingItem(item)
                                } label: {
                                    Label("Sil", systemImage: "trash")
                                }
                                
                                Button {
                                    editingItem = item
                                } label: {
                                    Label("Düzenle", systemImage: "pencil")
                                }
                                .tint(.blue)
                            }
                        }
                    }
                }
                .listStyle(.plain)
                .searchable(text: $searchText, prompt: "Ürün ara...")
            }
            .navigationTitle("Alışveriş Listesi")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        showCheckedOnly.toggle()
                    } label: {
                        Image(systemName: showCheckedOnly ? "checkmark.circle.fill" : "checkmark.circle")
                            .foregroundStyle(showCheckedOnly ? .green : .secondary)
                    }
                }
                
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showAddItem = true
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                    }
                }
            }
            .sheet(isPresented: $showAddItem) {
                AddShoppingItemSheet()
            }
            .sheet(item: $editingItem) { item in
                EditShoppingItemSheet(item: item)
            }
        }
    }
}

// MARK: - Shopping Item Row
struct ShoppingItemRow: View {
    let item: ShoppingItem
    let onToggle: () -> Void
    
    var categoryIcon: String {
        switch item.category {
        case "Market": return "cart.fill"
        case "Temizlik": return "sparkles"
        case "Kişisel Bakım": return "person.fill"
        case "Atıştırmalık": return "leaf.fill"
        case "Ev Gereçleri": return "wrench.and.screwdriver"
        case "Çocuk": return "figure.child"
        default: return "bag.fill"
        }
    }
    
    var categoryColor: Color {
        switch item.category {
        case "Market": return .green
        case "Temizlik": return .blue
        case "Kişisel Bakım": return .purple
        case "Atıştırmalık": return .orange
        case "Ev Gereçleri": return .brown
        case "Çocuk": return .pink
        default: return .gray
        }
    }
    
    var body: some View {
        HStack(spacing: 12) {
            Button(action: onToggle) {
                Image(systemName: item.checked ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundStyle(item.checked ? .green : .secondary)
            }
            .buttonStyle(.plain)
            
            Image(systemName: categoryIcon)
                .font(.title3)
                .foregroundStyle(categoryColor)
                .frame(width: 30)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                    .font(.headline)
                    .strikethrough(item.checked)
                
                HStack(spacing: 8) {
                    Text(item.category)
                        .font(.caption)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(categoryColor.opacity(0.15))
                        .foregroundStyle(categoryColor)
                        .cornerRadius(4)
                    
                    if !item.addedBy.isEmpty {
                        HStack(spacing: 2) {
                            Image(systemName: "person.circle.fill")
                                .font(.caption2)
                            Text(item.addedBy)
                        }
                        .font(.caption)
                        .foregroundStyle(.blue)
                    }
                }
            }
            
            Spacer()
            
            if item.quantity > 1 {
                Text("×\(item.quantity)")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundStyle(.orange)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.orange.opacity(0.1))
                    .cornerRadius(6)
            }
        }
        .padding(.vertical, 4)
        .opacity(item.checked ? 0.6 : 1.0)
    }
}

// MARK: - Shopping Stat Badge
struct ShoppingStatBadge: View {
    let count: Int
    let label: String
    let color: Color
    let icon: String
    
    var body: some View {
        VStack(spacing: 2) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.caption)
                Text("\(count)")
                    .font(.title3)
                    .fontWeight(.bold)
            }
            .foregroundStyle(color)
            
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(color.opacity(0.1))
        .cornerRadius(10)
    }
}

// MARK: - Category Filter Button
struct CategoryFilterButton: View {
    let title: String
    let isSelected: Bool
    let count: Int
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Text(title)
                    .font(.caption)
                if count > 0 {
                    Text("\(count)")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 1)
                        .background(isSelected ? .white.opacity(0.3) : .gray.opacity(0.3))
                        .cornerRadius(4)
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(isSelected ? Color.blue : Color(.systemGray6))
            .foregroundStyle(isSelected ? .white : .primary)
            .cornerRadius(8)
        }
    }
}

// MARK: - Add Shopping Item Sheet
struct AddShoppingItemSheet: View {
    @EnvironmentObject var vm: MainViewModel
    @Environment(\.dismiss) var dismiss
    @State private var name = ""
    @State private var quantity = 1
    @State private var category = "Market"
    @State private var addedBy = ""
    @State private var batchMode = false
    @State private var batchItems = ""
    
    let categories = ["Market", "Temizlik", "Kişisel Bakım", "Atıştırmalık", "Ev Gereçleri", "Çocuk"]
    let familyMembers = ["Mehmet", "Ayşe", "Zeynep", "Emir"]
    
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle("Toplu Ekleme", isOn: $batchMode)
                }
                
                if batchMode {
                    Section("Toplu Ekleme (her satır bir ürün)") {
                        TextEditor(text: $batchItems)
                            .frame(minHeight: 150)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(Color.gray.opacity(0.3))
                            )
                    }
                } else {
                    Section("Ürün Detayları") {
                        TextField("Ürün adı", text: $name)
                        Stepper("Adet: \(quantity)", value: $quantity, in: 1...99)
                        Picker("Kategori", selection: $category) {
                            ForEach(categories, id: \.self) { Text($0) }
                        }
                        Picker("Ekleyen", selection: $addedBy) {
                            Text("Yok").tag("")
                            ForEach(familyMembers, id: \.self) { Text($0) }
                        }
                    }
                }
            }
            .navigationTitle(batchMode ? "Toplu Ekle" : "Yeni Ürün")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("İptal") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Ekle") {
                        if batchMode {
                            let items = batchItems.components(separatedBy: "\n").filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
                            items.forEach { item in
                                vm.addShoppingItem(name: item.trimmingCharacters(in: .whitespaces), category: category, addedBy: addedBy)
                            }
                        } else {
                            vm.addShoppingItem(name: name, quantity: quantity, category: category, addedBy: addedBy)
                        }
                        dismiss()
                    }
                    .disabled(batchMode ? batchItems.trimmingCharacters(in: .whitespaces).isEmpty : name.isEmpty)
                }
            }
        }
    }
}

// MARK: - Edit Shopping Item Sheet
struct EditShoppingItemSheet: View {
    @EnvironmentObject var vm: MainViewModel
    @Environment(\.dismiss) var dismiss
    let item: ShoppingItem
    @State private var name: String
    @State private var quantity: Int
    @State private var category: String
    
    let categories = ["Market", "Temizlik", "Kişisel Bakım", "Atıştırmalık", "Ev Gereçleri", "Çocuk"]
    
    init(item: ShoppingItem) {
        self.item = item
        _name = State(initialValue: item.name)
        _quantity = State(initialValue: item.quantity)
        _category = State(initialValue: item.category)
    }
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Ürün Detayları") {
                    TextField("Ürün adı", text: $name)
                    Stepper("Adet: \(quantity)", value: $quantity, in: 1...99)
                    Picker("Kategori", selection: $category) {
                        ForEach(categories, id: \.self) { Text($0) }
                    }
                }
            }
            .navigationTitle("Düzenle")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("İptal") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Kaydet") {
                        item.name = name
                        item.quantity = quantity
                        item.category = category
                        vm.save()
                        dismiss()
                    }
                    .disabled(name.isEmpty)
                }
            }
        }
    }
}

#Preview {
    ShoppingView()
        .environmentObject(MainViewModel())
}
