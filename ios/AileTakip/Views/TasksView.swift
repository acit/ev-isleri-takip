import SwiftUI

struct TasksView: View {
    @EnvironmentObject var vm: MainViewModel
    @State private var showAddTask = false
    @State private var newTitle = ""
    @State private var newDescription = ""
    @State private var newPriority = "orta"
    @State private var newAssignee = ""
    
    let priorities = ["düşük", "orta", "yüksek"]
    let assignees = ["Mehmet", "Ayşe", "Zeynep", "Emir"]
    
    var body: some View {
        NavigationStack {
            List {
                ForEach(vm.tasks) { task in
                    HStack {
                        Button {
                            vm.toggleTask(task)
                        } label: {
                            Image(systemName: task.status == "tamamlanan" ? "checkmark.circle.fill" : "circle")
                                .foregroundStyle(task.status == "tamamlanan" ? .green : .secondary)
                        }
                        .buttonStyle(.plain)
                        
                        VStack(alignment: .leading) {
                            Text(task.title)
                                .strikethrough(task.status == "tamamlanan")
                            Text(task.category)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        
                        Spacer()
                        
                        PriorityBadge(priority: task.priority)
                    }
                }
                .onDelete { indexSet in
                    indexSet.forEach { vm.deleteTask(vm.tasks[$0]) }
                }
            }
            .navigationTitle("Görevler")
            .toolbar {
                Button {
                    showAddTask = true
                } label: {
                    Image(systemName: "plus")
                }
            }
            .sheet(isPresented: $showAddTask) {
                NavigationStack {
                    Form {
                        TextField("Görev başlığı", text: $newTitle)
                        TextField("Açıklama", text: $newDescription)
                        Picker("Öncelik", selection: $newPriority) {
                            ForEach(priorities, id: \.self) { Text($0) }
                        }
                        Picker("Sorumlu", selection: $newAssignee) {
                            Text("Yok").tag("")
                            ForEach(assignees, id: \.self) { Text($0) }
                        }
                    }
                    .navigationTitle("Yeni Görev")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("İptal") { showAddTask = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Ekle") {
                                vm.addTask(title: newTitle, description: newDescription, priority: newPriority, assignee: newAssignee)
                                showAddTask = false
                                newTitle = ""
                                newDescription = ""
                            }
                            .disabled(newTitle.isEmpty)
                        }
                    }
                }
                .presentationDetents([.medium])
            }
        }
    }
}

struct PriorityBadge: View {
    let priority: String
    
    var color: Color {
        switch priority {
        case "yüksek": return .red
        case "orta": return .orange
        default: return .green
        }
    }
    
    var body: some View {
        Text(priority.prefix(1).uppercased())
            .font(.caption2)
            .fontWeight(.bold)
            .foregroundStyle(.white)
            .frame(width: 20, height: 20)
            .background(color)
            .clipShape(Circle())
    }
}

#Preview {
    TasksView()
        .environmentObject(MainViewModel())
}
