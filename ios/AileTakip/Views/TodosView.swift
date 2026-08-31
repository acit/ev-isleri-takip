import SwiftUI

struct TodosView: View {
    @EnvironmentObject var vm: MainViewModel
    @State private var showAddTask = false
    @State private var searchText = ""
    @State private var filterStatus = "Tümü"
    @State private var filterPriority = "Tümü"
    @State private var sortBy = "Tarih"
    
    let statuses = ["Tümü", "Bekleyen", "Devam Ediyor", "Tamamlanan"]
    let priorities = ["Tümü", "düşük", "orta", "yüksek"]
    let sortOptions = ["Tarih", "Öncelik", "Sorumlu", "Kategori"]
    
    var filteredTasks: [Task] {
        var result = vm.tasks
        
        // Status filter
        switch filterStatus {
        case "Bekleyen": result = result.filter { $0.status == "bekleyen" }
        case "Devam Ediyor": result = result.filter { $0.status == "devam_ediyor" }
        case "Tamamlanan": result = result.filter { $0.status == "tamamlanan" }
        default: break
        }
        
        // Priority filter
        if filterPriority != "Tümü" {
            result = result.filter { $0.priority == filterPriority }
        }
        
        // Search
        if !searchText.isEmpty {
            result = result.filter { $0.title.localizedCaseInsensitiveContains(searchText) || $0.description.localizedCaseInsensitiveContains(searchText) }
        }
        
        // Sort
        switch sortBy {
        case "Öncelik":
            result = result.sorted { priorityOrder($0.priority) < priorityOrder($1.priority) }
        case "Sorumlu":
            result = result.sorted { $0.assignee < $1.assignee }
        case "Kategori":
            result = result.sorted { $0.category < $1.category }
        default:
            result = result.sorted { $0.createdAt > $1.createdAt }
        }
        
        return result
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Stats Bar
                HStack(spacing: 16) {
                    TaskStatBadge(count: vm.pendingTaskCount, label: "Bekleyen", color: .orange)
                    TaskStatBadge(count: vm.tasks.filter { $0.status == "devam_ediyor" }.count, label: "Devam", color: .blue)
                    TaskStatBadge(count: vm.tasks.filter { $0.status == "tamamlanan" }.count, label: "Tamam", color: .green)
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                
                // Filters
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        FilterChip(text: filterStatus, options: statuses, selected: $filterStatus)
                        FilterChip(text: filterPriority, options: priorities, selected: $filterPriority)
                        FilterChip(text: sortBy, options: sortOptions, selected: $sortBy)
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 8)
                }
                
                // Task List
                List {
                    if filteredTasks.isEmpty {
                        ContentUnavailableView("Görev bulunamadı", systemImage: "checkmark.circle", description: Text("Yeni bir görev ekleyin"))
                    } else {
                        ForEach(filteredTasks) { task in
                            TaskRow(task: task) {
                                vm.toggleTask(task)
                            }
                        }
                        .onDelete { indexSet in
                            let tasksToDelete = indexSet.map { filteredTasks[$0] }
                            tasksToDelete.forEach { vm.deleteTask($0) }
                        }
                    }
                }
                .listStyle(.plain)
                .searchable(text: $searchText, prompt: "Görev ara...")
            }
            .navigationTitle("Görevler")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showAddTask = true
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                    }
                }
            }
            .sheet(isPresented: $showAddTask) {
                AddTaskSheet()
            }
        }
    }
    
    private func priorityOrder(_ priority: String) -> Int {
        switch priority {
        case "yüksek": return 0
        case "orta": return 1
        case "düşük": return 2
        default: return 3
        }
    }
}

// MARK: - Task Row
struct TaskRow: View {
    let task: Task
    let onToggle: () -> Void
    
    var statusIcon: String {
        switch task.status {
        case "tamamlanan": return "checkmark.circle.fill"
        case "devam_ediyor": return "arrow.triangle.2.circlepath"
        default: return "circle"
        }
    }
    
    var statusColor: Color {
        switch task.status {
        case "tamamlanan": return .green
        case "devam_ediyor": return .blue
        default: return .secondary
        }
    }
    
    var body: some View {
        HStack(spacing: 12) {
            Button(action: onToggle) {
                Image(systemName: statusIcon)
                    .font(.title2)
                    .foregroundStyle(statusColor)
            }
            .buttonStyle(.plain)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(task.title)
                    .font(.headline)
                    .strikethrough(task.status == "tamamlanan")
                
                if !task.description.isEmpty {
                    Text(task.description)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                
                HStack(spacing: 8) {
                    CategoryTag(text: task.category, color: .blue)
                    
                    if !task.assignee.isEmpty {
                        HStack(spacing: 2) {
                            Image(systemName: "person.circle.fill")
                                .font(.caption2)
                            Text(task.assignee)
                        }
                        .font(.caption)
                        .foregroundStyle(.purple)
                    }
                    
                    if !task.dueDate.isEmpty {
                        HStack(spacing: 2) {
                            Image(systemName: "calendar")
                                .font(.caption2)
                            Text(task.dueDate)
                        }
                        .font(.caption)
                        .foregroundStyle(.red)
                    }
                }
            }
            
            Spacer()
            
            PriorityBadge(priority: task.priority)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Task Stat Badge
struct TaskStatBadge: View {
    let count: Int
    let label: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 2) {
            Text("\(count)")
                .font(.title2)
                .fontWeight(.bold)
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

// MARK: - Filter Chip
struct FilterChip: View {
    let text: String
    let options: [String]
    @Binding var selected: String
    
    var body: some View {
        Menu {
            ForEach(options, id: \.self) { option in
                Button {
                    selected = option
                } label: {
                    HStack {
                        Text(option)
                        if selected == option {
                            Image(systemName: "checkmark")
                        }
                    }
                }
            }
        } label: {
            HStack(spacing: 4) {
                Text(text)
                    .font(.caption)
                Image(systemName: "chevron.down")
                    .font(.caption2)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color(.systemGray6))
            .cornerRadius(8)
        }
    }
}

// MARK: - Category Tag
struct CategoryTag: View {
    let text: String
    let color: Color
    
    var body: some View {
        Text(text)
            .font(.caption2)
            .fontWeight(.medium)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(color.opacity(0.15))
            .foregroundStyle(color)
            .cornerRadius(4)
    }
}

// MARK: - Add Task Sheet
struct AddTaskSheet: View {
    @EnvironmentObject var vm: MainViewModel
    @Environment(\.dismiss) var dismiss
    @State private var title = ""
    @State private var description = ""
    @State private var category = "Genel"
    @State private var priority = "orta"
    @State private var assignee = ""
    @State private var dueDate = Date()
    @State private var hasDueDate = false
    
    let categories = ["Genel", "Ev", "Eğitim", "Sağlık", "Araç", "Spor", "Alışveriş"]
    let priorities = ["düşük", "orta", "yüksek"]
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Görev Detayları") {
                    TextField("Görev başlığı", text: $title)
                    TextField("Açıklama (isteğe bağlı)", text: $description)
                }
                
                Section("Kategori & Öncelik") {
                    Picker("Kategori", selection: $category) {
                        ForEach(categories, id: \.self) { Text($0) }
                    }
                    Picker("Öncelik", selection: $priority) {
                        ForEach(priorities, id: \.self) { Text($0) }
                    }
                }
                
                Section("Sorumlu & Tarih") {
                    Picker("Sorumlu", selection: $assignee) {
                        Text("Yok").tag("")
                        ForEach(vm.members) { member in
                            Text(member.name).tag(member.name)
                        }
                    }
                    
                    Toggle("Bitiş Tarihi", isOn: $hasDueDate)
                    if hasDueDate {
                        DatePicker("Tarih", selection: $dueDate, displayedComponents: .date)
                    }
                }
            }
            .navigationTitle("Yeni Görev")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("İptal") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Ekle") {
                        let dateStr = hasDueDate ? formatDate(dueDate) : ""
                        vm.addTask(title: title, description: description, category: category, priority: priority, assignee: assignee, dueDate: dateStr)
                        dismiss()
                    }
                    .disabled(title.isEmpty)
                }
            }
        }
    }
    
    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }
}

#Preview {
    TodosView()
        .environmentObject(MainViewModel())
}
