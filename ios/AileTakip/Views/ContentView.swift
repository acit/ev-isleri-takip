import SwiftUI

struct ContentView: View {
    @EnvironmentObject var vm: MainViewModel
    @State private var selectedTab = 0
    
    var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView()
                .tabItem {
                    Label("Ana Sayfa", systemImage: "house.fill")
                }
                .tag(0)
            
            TodosView()
                .tabItem {
                    Label("Görevler", systemImage: "checkmark.circle")
                }
                .tag(1)
            
            ShoppingView()
                .tabItem {
                    Label("Alışveriş", systemImage: "cart")
                }
                .tag(2)
            
            BudgetView()
                .tabItem {
                    Label("Bütçe", systemImage: "dollarsign.circle")
                }
                .tag(3)
            
            MessagesView()
                .tabItem {
                    Label("Mesajlar", systemImage: "message")
                }
                .tag(4)
            
            ProfileView()
                .tabItem {
                    Label("Profil", systemImage: "person.circle")
                }
                .tag(5)
        }
        .onAppear {
            vm.loadSampleData()
        }
    }
}

// MARK: - Messages View
struct MessagesView: View {
    @EnvironmentObject var vm: MainViewModel
    @State private var newMessage = ""
    
    var body: some View {
        NavigationStack {
            VStack {
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(vm.messages.reversed()) { message in
                                MessageBubble(message: message)
                                    .id(message.id)
                            }
                        }
                        .padding()
                    }
                    .onChange(of: vm.messages.count) { _, _ in
                        if let last = vm.messages.last {
                            withAnimation {
                                proxy.scrollTo(last.id, anchor: .bottom)
                            }
                        }
                    }
                }
                
                // Message Input
                HStack {
                    TextField("Mesaj yaz...", text: $newMessage)
                        .textFieldStyle(.roundedBorder)
                    
                    Button {
                        if !newMessage.isEmpty {
                            vm.sendMessage(content: newMessage)
                            newMessage = ""
                        }
                    } label: {
                        Image(systemName: "paperplane.fill")
                            .foregroundStyle(newMessage.isEmpty ? .gray : .blue)
                    }
                    .disabled(newMessage.isEmpty)
                }
                .padding()
            }
            .navigationTitle("Mesajlar")
        }
    }
}

struct MessageBubble: View {
    let message: Message
    
    var isMe: Bool { message.senderId == "self" }
    
    var body: some View {
        HStack {
            if isMe { Spacer() }
            
            VStack(alignment: isMe ? .trailing : .leading, spacing: 4) {
                if !isMe {
                    Text(message.senderName)
                        .font(.caption)
                        .foregroundStyle(.blue)
                        .fontWeight(.semibold)
                }
                Text(message.content)
                    .padding(10)
                    .background(isMe ? Color.blue : Color(.systemGray5))
                    .foregroundStyle(isMe ? .white : .primary)
                    .cornerRadius(16)
            }
            .frame(maxWidth: 280, alignment: isMe ? .trailing : .leading)
            
            if !isMe { Spacer() }
        }
    }
}

// MARK: - Profile View
struct ProfileView: View {
    @EnvironmentObject var vm: MainViewModel
    
    var body: some View {
        NavigationStack {
            List {
                Section("Aile Üyeleri") {
                    ForEach(vm.members) { member in
                        HStack {
                            Circle()
                                .fill(Color(hex: member.color))
                                .frame(width: 40, height: 40)
                                .overlay(Text(String(member.name.prefix(1)))
                                    .font(.headline)
                                    .foregroundStyle(.white))
                            
                            VStack(alignment: .leading) {
                                Text(member.name)
                                    .font(.headline)
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
                }
                
                Section("İstatistikler") {
                    HStack {
                        Text("Toplam Görev")
                        Spacer()
                        Text("\(vm.tasks.count)")
                    }
                    HStack {
                        Text("Toplam Not")
                        Spacer()
                        Text("\(vm.notes.count)")
                    }
                    HStack {
                        Text("Toplam Mesaj")
                        Spacer()
                        Text("\(vm.messages.count)")
                    }
                }
                
                Section {
                    HStack {
                        Image(systemName: "moon.fill")
                        Text("Dark Mode")
                        Spacer()
                    }
                }
            }
            .navigationTitle("Profil")
        }
    }
}

// MARK: - Color Extension
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}

#Preview {
    ContentView()
        .environmentObject(MainViewModel())
}
