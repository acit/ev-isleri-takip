import SwiftUI

struct DashboardView: View {
    @EnvironmentObject var vm: MainViewModel
    
    let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Header
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Merhaba! 👋")
                                .font(.title2)
                                .fontWeight(.bold)
                            Text("\(vm.members.count) aile üyesi")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Circle()
                            .fill(Color.blue)
                            .frame(width: 50, height: 50)
                            .overlay(Text("YT").font(.headline).foregroundStyle(.white))
                    }
                    .padding(.horizontal)
                    
                    // Quick Stats - iOS 26 Liquid Glass
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            StatCard(title: "💧 Su", value: "\(vm.todayWaterMl)ml", color: .blue)
                            StatCard(title: "🔥 Kalori", value: "\(vm.todayCalories)", color: .orange)
                            StatCard(title: "📋 Görev", value: "\(vm.pendingTaskCount)", color: .green)
                            StatCard(title: "🛒 Alışveriş", value: "\(vm.uncheckedShoppingCount)", color: .purple)
                        }
                        .padding(.horizontal)
                    }
                    
                    // Feature Grid - iOS 26 Liquid Glass
                    LazyVGrid(columns: columns, spacing: 12) {
                        FeatureCard(icon: "checkmark.circle.fill", title: "Görevler", count: vm.pendingTaskCount, color: .green, route: "tasks")
                        FeatureCard(icon: "cart.fill", title: "Alışveriş", count: vm.uncheckedShoppingCount, color: .purple, route: "shopping")
                        FeatureCard(icon: "message.fill", title: "Mesajlar", count: vm.messages.count, color: .blue, route: "messages")
                        FeatureCard(icon: "note.text", title: "Notlar", count: vm.notes.count, color: .orange, route: "notes")
                        FeatureCard(icon: "bell.fill", title: "Hatırlatıcılar", count: vm.reminders.filter { !$0.isCompleted }.count, color: .red, route: "reminders")
                        FeatureCard(icon: "doc.text.fill", title: "Faturalar", count: vm.invoices.filter { $0.status == "pending" }.count, color: .cyan, route: "invoices")
                        FeatureCard(icon: "figure.run", title: "Sağlık", count: nil, color: .mint, route: "health")
                        FeatureCard(icon: "list.bullet", title: "Yemek Planı", count: nil, color: .brown, route: "meal-plan")
                    }
                    .padding(.horizontal)
                }
                .padding(.vertical)
            }
            .navigationTitle("Ana Sayfa")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Stat Card - iOS 26 Liquid Glass
struct StatCard: View {
    let title: String
    let value: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
                .foregroundStyle(color)
        }
        .frame(width: 100)
        .padding()
        .background(color.opacity(0.1))
        .glassEffect(.regular.interactive)
        .cornerRadius(12)
    }
}

// MARK: - Feature Card - iOS 26 Liquid Glass
struct FeatureCard: View {
    let icon: String
    let title: String
    let count: Int?
    let color: Color
    let route: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundStyle(color)
                Spacer()
                if let count = count {
                    Text("\(count)")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundStyle(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(color)
                        .clipShape(Capsule())
                }
            }
            Text(title)
                .font(.headline)
        }
        .padding()
        .background(Color(.systemBackground))
        .glassEffect(.regular)
        .cornerRadius(12)
    }
}

#Preview {
    DashboardView()
        .environmentObject(MainViewModel())
}
