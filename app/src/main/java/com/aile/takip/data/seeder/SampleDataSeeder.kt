package com.aile.takip.data.seeder

import com.aile.takip.data.db.AppDatabase
import com.aile.takip.data.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * İlk çalıştırmada veritabanını gerçekçi örnek verilerle doldurur.
 * Aile: Yılmaz Ailesi (4 kişi)
 */
object SampleDataSeeder {

    private val today: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun daysAgo(n: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -n)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    private fun daysFromNow(n: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, n)
        return cal.timeInMillis
    }

    private fun hoursFromNow(h: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, h)
        return cal.timeInMillis
    }

    suspend fun seedIfNeeded(db: AppDatabase) {
        val count = db.memberDao().getAllOnce().size
        if (count > 0) return // zaten veri var

        val members = listOf(
            FamilyMember(id = "m1", name = "Mehmet", role = "baba", color = "#2196F3", points = 150),
            FamilyMember(id = "m2", name = "Ayşe", role = "anne", color = "#E91E63", points = 220),
            FamilyMember(id = "m3", name = "Zeynep", role = "kız", color = "#9C27B0", points = 180),
            FamilyMember(id = "m4", name = "Emir", role = "oğul", color = "#FF9800", points = 120)
        )
        db.memberDao().upsertAll(members)

        // ── ALIŞVERİŞ LİSTESİ ──
        val shoppingItems = listOf(
            // Market
            ShoppingItem(name = "Süt (1L)", quantity = 3, category = "Market", addedBy = "Ayşe"),
            ShoppingItem(name = "Ekmek", quantity = 2, category = "Market", addedBy = "Ayşe"),
            ShoppingItem(name = "Yumurta (30'lu)", quantity = 1, category = "Market", addedBy = "Mehmet"),
            ShoppingItem(name = "Peynir (500g)", quantity = 1, category = "Market", addedBy = "Ayşe"),
            ShoppingItem(name = "Domates (1kg)", quantity = 1, category = "Market", addedBy = "Ayşe"),
            ShoppingItem(name = "Biber (500g)", quantity = 1, category = "Market", addedBy = "Mehmet"),
            ShoppingItem(name = "Soğan (1kg)", quantity = 1, category = "Market", addedBy = "Ayşe"),
            ShoppingItem(name = "Patates (2kg)", quantity = 1, category = "Market", addedBy = "Mehmet"),
            ShoppingItem(name = "Zeytinyağı (1L)", quantity = 1, category = "Market", addedBy = "Ayşe"),
            ShoppingItem(name = "Şeker (1kg)", quantity = 1, category = "Market", addedBy = "Ayşe"),
            ShoppingItem(name = "Çay (500g)", quantity = 1, category = "Market", addedBy = "Mehmet"),
            ShoppingItem(name = "Makarna (500g)", quantity = 3, category = "Market", addedBy = "Emir"),
            ShoppingItem(name = "Pirinç (1kg)", quantity = 1, category = "Market", addedBy = "Ayşe"),
            ShoppingItem(name = "Un (1kg)", quantity = 2, category = "Market", addedBy = "Ayşe"),
            ShoppingItem(name = "Tereyağı (200g)", quantity = 1, category = "Market", addedBy = "Ayşe"),
            // Temizlik
            ShoppingItem(name = "Bulaşık Deterjanı", quantity = 1, category = "Temizlik", addedBy = "Ayşe"),
            ShoppingItem(name = "Çamaşır Deterjanı", quantity = 1, category = "Temizlik", addedBy = "Ayşe"),
            ShoppingItem(name = "Tuvalet Kağıdı (12'li)", quantity = 1, category = "Temizlik", addedBy = "Mehmet"),
            ShoppingItem(name = "Kağıt Havlu", quantity = 2, category = "Temizlik", addedBy = "Ayşe"),
            ShoppingItem(name = "Yüzey Temizleyici", quantity = 1, category = "Temizlik", addedBy = "Ayşe"),
            // Kişisel Bakım
            ShoppingItem(name = "Şampuan", quantity = 1, category = "Kişisel Bakım", addedBy = "Zeynep"),
            ShoppingItem(name = "Diş Macunu", quantity = 2, category = "Kişisel Bakım", addedBy = "Mehmet"),
            ShoppingItem(name = "Deodorant", quantity = 2, category = "Kişisel Bakım", addedBy = "Emir"),
            ShoppingItem(name = "Güneş Kremi", quantity = 1, category = "Kişisel Bakım", addedBy = "Zeynep"),
            // Atıştırmalık
            ShoppingItem(name = "Bisküvi", quantity = 2, category = "Atıştırmalık", addedBy = "Emir"),
            ShoppingItem(name = "Meyve Suyu (1L)", quantity = 3, category = "Atıştırmalık", addedBy = "Zeynep"),
            ShoppingItem(name = "Çikolata", quantity = 2, category = "Atıştırmalık", addedBy = "Zeynep"),
            ShoppingItem(name = "Kuru Üzüm", quantity = 1, category = "Atıştırmalık", addedBy = "Ayşe"),
            // Ev Gereçleri
            ShoppingItem(name = "Ampül (LED)", quantity = 2, category = "Ev Gereçleri", addedBy = "Mehmet"),
            ShoppingItem(name = "Pil (AA 4'lü)", quantity = 1, category = "Ev Gereçleri", addedBy = "Mehmet"),
            // Çocuk
            ShoppingItem(name = "Okul Defteri", quantity = 5, category = "Çocuk", addedBy = "Zeynep"),
            ShoppingItem(name = "Kalem Seti", quantity = 1, category = "Çocuk", addedBy = "Zeynep"),
            ShoppingItem(name = "Boya Kalemi (12'li)", quantity = 2, category = "Çocuk", addedBy = "Emir"),
        )
        db.shoppingDao().upsertAll(shoppingItems)

        // ── GÖREVLER ──
        val tasks = listOf(
            Task(title = "Mutfak temizliği", description = "Tezgah ve dolapları sil", category = "Ev", priority = "yüksek", assignee = "Mehmet", status = "bekleyen", dueDate = today),
            Task(title = "Çamaşıraları yıka", description = "Beyaz ve renklileri ayır", category = "Ev", priority = "orta", assignee = "Ayşe", status = "devam_ediyor", dueDate = today),
            Task(title = "Ödev kontrolü - Matematik", description = "Zeynep'in matematik ödevini kontrol et", category = "Eğitim", priority = "yüksek", assignee = "Mehmet", status = "bekleyen", dueDate = daysFromNow(1).toString()),
            Task(title = "Emir'in spor çantasını hazırla", description = "Yarın futbol antrenmanı var", category = "Spor", priority = "orta", assignee = "Ayşe", status = "bekleyen", dueDate = daysFromNow(1).toString()),
            Task(title = "Araba bakımı", description = "Yağ değişimi ve lastik kontrolü", category = "Araç", priority = "düşük", assignee = "Mehmet", status = "bekleyen", dueDate = daysFromNow(7).toString()),
            Task(title = "Doktor randevusu - Zeynep", description = "Diş hekimi kontrolü saat 14:30", category = "Sağlık", priority = "yüksek", assignee = "Ayşe", status = "bekleyen", dueDate = daysFromNow(3).toString()),
            Task(title = "Fatura öde - Elektrik", description = "150 TL - son gün yarın", category = "Fatura", priority = "yüksek", assignee = "Mehmet", status = "devam_ediyor", dueDate = daysFromNow(1).toString()),
            Task(title = "Fatura öde - İnternet", description = "120 TL", category = "Fatura", priority = "orta", assignee = "Mehmet", status = "tamamlandı", dueDate = daysAgo(1)),
            Task(title = "Bahçe sulama", description = "Domates ve biberleri sulamayı unutma", category = "Bahçe", priority = "düşük", assignee = "Emir", status = "bekleyen", dueDate = today),
            Task(title = "Market poşetlerini yerleştir", description = "Alınan malzemeleri dolaplara koy", category = "Ev", priority = "düşük", assignee = "Emir", status = "tamamlandı", dueDate = daysAgo(1)),
            Task(title = "Zeynep için elbise al", description = "Okul gezi için yeni elbise lazım", category = "Alışveriş", priority = "orta", assignee = "Ayşe", status = "bekleyen", dueDate = daysFromNow(5).toString()),
            Task(title = "Su faturası sorgula", description = "Online sistemden borcu kontrol et", category = "Fatura", priority = "orta", assignee = "Mehmet", status = "bekleyen", dueDate = daysFromNow(2).toString()),
        )
        db.taskDao().upsertAll(tasks)

        // ── SPOR KULÜPLERI ──
        val clubs = listOf(
            SportsClub(name = "World Fitness Gym", type = "Spor Salonu", address = "Atatürk Cad. No:45", phone = "0212-555-1234", monthlyFee = 450.0, memberId = "m1", membershipStart = daysAgo(180)),
            SportsClub(name = "Olimpik Yüzme Havuzu", type = "Yüzme", address = "Cumhuriyet Mah. No:12", phone = "0212-555-5678", monthlyFee = 300.0, memberId = "m2", membershipStart = daysAgo(90)),
            SportsClub(name = "Çocuklar Futbol Kulübü", type = "Futbol", address = "Spor Sahası No:3", phone = "0212-555-9012", monthlyFee = 200.0, memberId = "m4", membershipStart = daysAgo(60)),
            SportsClub(name = "Zen Yoga Stüdyo", type = "Yoga", address = "Barbaros Blv. No:78", phone = "0212-555-3456", monthlyFee = 350.0, memberId = "m3", membershipStart = daysAgo(30)),
        )
        clubs.forEach { db.sportsClubDao().upsert(it) }

        // ── SPOR KAYITLARI ──
        val workouts = listOf(
            WorkoutLog(clubId = "m1", memberId = "m1", workoutType = "Kardiyo", duration = 45, caloriesBurned = 380, date = today, notes = "Koşu bandı 30 dk + bisiklet 15 dk"),
            WorkoutLog(clubId = "m1", memberId = "m1", workoutType = "Ağırlık", duration = 60, caloriesBurned = 420, date = daysAgo(1), notes = "Göğüs ve kol antrenmanı"),
            WorkoutLog(clubId = "m1", memberId = "m1", workoutType = "Kardiyo", duration = 30, caloriesBurned = 250, date = daysAgo(2), notes = "HIIT antrenman"),
            WorkoutLog(clubId = "m2", memberId = "m2", workoutType = "Yüzme", duration = 40, caloriesBurned = 350, date = today, notes = "Serbest stil 1500m"),
            WorkoutLog(clubId = "m2", memberId = "m2", workoutType = "Yüzme", duration = 45, caloriesBurned = 400, date = daysAgo(2), notes = "Karışık stil 2000m"),
            WorkoutLog(clubId = "m3", memberId = "m3", workoutType = "Yoga", duration = 60, caloriesBurned = 200, date = daysAgo(1), notes = "Vinyasa akışı"),
            WorkoutLog(clubId = "m4", memberId = "m4", workoutType = "Futbol", duration = 90, caloriesBurned = 600, date = today, notes = "Takım antrenmanı"),
            WorkoutLog(clubId = "m4", memberId = "m4", workoutType = "Futbol", duration = 75, caloriesBurned = 500, date = daysAgo(3), notes = "Maç antrenmanı"),
        )
        workouts.forEach { db.workoutLogDao().upsert(it) }

        // ── KALORİ KAYITLARI ──
        val calorieLogs = listOf(
            // Mehmet - bugün
            CalorieLog(memberId = "m1", mealType = "Kahvaltı", foodName = "Menemen", calories = 320, protein = 15.0, carbs = 22.0, fat = 18.0, date = today),
            CalorieLog(memberId = "m1", mealType = "Öğle", foodName = "Tavuk Döner", calories = 550, protein = 35.0, carbs = 45.0, fat = 22.0, date = today),
            CalorieLog(memberId = "m1", mealType = "Akşam", foodName = "Mercimek Çorbası + Salata", calories = 380, protein = 18.0, carbs = 48.0, fat = 10.0, date = today),
            // Ayşe - bugün
            CalorieLog(memberId = "m2", mealType = "Kahvaltı", foodName = "Yulaf Ezmesi + Meyve", calories = 280, protein = 12.0, carbs = 42.0, fat = 6.0, date = today),
            CalorieLog(memberId = "m2", mealType = "Öğle", foodName = "Akdeniz Salatası", calories = 350, protein = 20.0, carbs = 30.0, fat = 16.0, date = today),
            CalorieLog(memberId = "m2", mealType = "Atıştırmalık", foodName = "Yoğurt + Ceviz", calories = 180, protein = 8.0, carbs = 12.0, fat = 12.0, date = today),
            // Zeynep - bugün
            CalorieLog(memberId = "m3", mealType = "Kahvaltı", foodName = "Kaşarlı Omlet", calories = 350, protein = 22.0, carbs = 8.0, fat = 26.0, date = today),
            CalorieLog(memberId = "m3", mealType = "Öğle", foodName = "Makarna (Penne)", calories = 480, protein = 15.0, carbs = 65.0, fat = 16.0, date = today),
            // Emir - bugün
            CalorieLog(memberId = "m4", mealType = "Kahvaltı", foodName = "Sütlü Müsli", calories = 300, protein = 10.0, carbs = 48.0, fat = 8.0, date = today),
            CalorieLog(memberId = "m4", mealType = "Öğle", foodName = "Köfte Ekmek", calories = 600, protein = 28.0, carbs = 52.0, fat = 30.0, date = today),
            CalorieLog(memberId = "m4", mealType = "Atıştırmalık", foodName = "Muz", calories = 105, protein = 1.3, carbs = 27.0, fat = 0.4, date = today),
        )
        calorieLogs.forEach { db.calorieLogDao().upsert(it) }

        // ── HAFTALIK YEMEK PLANI ──
        val mealPlans = listOf(
            // Pazartesi
            MealPlan(dayOfWeek = Calendar.MONDAY, mealType = "Kahvaltı", dish = "Menemen", notes = "Domates, biber, yumurta"),
            MealPlan(dayOfWeek = Calendar.MONDAY, mealType = "Öğle", dish = "Mercimek Çorbası + Ekmek", notes = "Kırmızı mercimek"),
            MealPlan(dayOfWeek = Calendar.MONDAY, mealType = "Akşam", dish = "Tavuk Sote + Pirinç Pilavı", notes = "Sebzeli tavuk"),
            // Salı
            MealPlan(dayOfWeek = Calendar.TUESDAY, mealType = "Kahvaltı", dish = "Peynirli Börek", notes = "Yufkadan"),
            MealPlan(dayOfWeek = Calendar.TUESDAY, mealType = "Öğle", dish = "İskender Kebap", notes = "Dışarıda yenilebilir"),
            MealPlan(dayOfWeek = Calendar.TUESDAY, mealType = "Akşam", dish = "Zeytinyağlı Fasulye + Pirinç", notes = "Soğuk servis"),
            // Çarşamba
            MealPlan(dayOfWeek = Calendar.WEDNESDAY, mealType = "Kahvaltı", dish = "Kaşarlı Tost + Çay", notes = "Hızlı kahvaltı"),
            MealPlan(dayOfWeek = Calendar.WEDNESDAY, mealType = "Öğle", dish = "Tavuk Salatası", notes = "Mevsim yeşillikleri"),
            MealPlan(dayOfWeek = Calendar.WEDNESDAY, mealType = "Akşam", dish = "Karnıyarık + Yoğurt", notes = "Patlıcan"),
            // Perşembe
            MealPlan(dayOfWeek = Calendar.THURSDAY, mealType = "Kahvaltı", dish = "Yumurta + Peynir + Zeytin", notes = "Serpme kahvaltı"),
            MealPlan(dayOfWeek = Calendar.THURSDAY, mealType = "Öğle", dish = "Karadeniz Pidesi", notes = "Kıymalı"),
            MealPlan(dayOfWeek = Calendar.THURSDAY, mealType = "Akşam", dish = "Balık Izgara + Salata", notes = "Levrek"),
            // Cuma
            MealPlan(dayOfWeek = Calendar.FRIDAY, mealType = "Kahvaltı", dish = "Acıkan", notes = "Çeşitli hamur işleri"),
            MealPlan(dayOfWeek = Calendar.FRIDAY, mealType = "Öğle", dish = "Lahmacun", notes = "Evde yapım"),
            MealPlan(dayOfWeek = Calendar.FRIDAY, mealType = "Akşam", dish = "Makarna (Fırında)", notes = "Kıymalı soslu"),
            // Cumartesi
            MealPlan(dayOfWeek = Calendar.SATURDAY, mealType = "Kahvaltı", dish = "Kahvaltı Tabağı (Brunch)", notes = "Tüm aile birlikte"),
            MealPlan(dayOfWeek = Calendar.SATURDAY, mealType = "Öğle", dish = "Pizza (Ev yapımı)", notes = "Çocuklarla birlikte"),
            MealPlan(dayOfWeek = Calendar.SATURDAY, mealType = "Akşam", dish = "Mangal", notes = "Dışarıda"),
            // Pazar
            MealPlan(dayOfWeek = Calendar.SUNDAY, mealType = "Kahvaltı", dish = "Serpme Kahvaltı", notes = "Geniş aile kahvaltısı"),
            MealPlan(dayOfWeek = Calendar.SUNDAY, mealType = "Öğle", dish = "Etli Nohut", notes = "Düdüklü tencere"),
            MealPlan(dayOfWeek = Calendar.SUNDAY, mealType = "Akşam", dish = "Hafif Çorba + Salata", notes = "Gün sonu hafif yemek"),
        )
        db.mealPlanDao().upsertAll(mealPlans)

        // ── BÜTÇE ──
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val budgets = listOf(
            Budget(category = "Market", monthlyLimit = 3000.0, spentAmount = 1850.0, monthYear = currentMonth),
            Budget(category = "Faturalar", monthlyLimit = 1500.0, spentAmount = 870.0, monthYear = currentMonth),
            Budget(category = "Eğlence", monthlyLimit = 1000.0, spentAmount = 450.0, monthYear = currentMonth),
            Budget(category = "Sağlık", monthlyLimit = 800.0, spentAmount = 200.0, monthYear = currentMonth),
            Budget(category = "Eğitim", monthlyLimit = 1200.0, spentAmount = 650.0, monthYear = currentMonth),
            Budget(category = "Ulaşım", monthlyLimit = 1000.0, spentAmount = 580.0, monthYear = currentMonth),
            Budget(category = "Giyim", monthlyLimit = 1500.0, spentAmount = 320.0, monthYear = currentMonth),
            Budget(category = "Spor", monthlyLimit = 1500.0, spentAmount = 1300.0, monthYear = currentMonth),
        )
        db.budgetDao().upsertAll(budgets)

        // ── HARCAMALAR ──
        val expenses = listOf(
            Expense(category = "Market", amount = 450.0, description = "Haftalık market alışverişi", expenseDate = daysAgo(2)),
            Expense(category = "Market", amount = 320.0, description = "Meyve-sebze alışverişi", expenseDate = daysAgo(5)),
            Expense(category = "Faturalar", amount = 280.0, description = "Elektrik faturası", expenseDate = daysAgo(3)),
            Expense(category = "Faturalar", amount = 120.0, description = "İnternet faturası", expenseDate = daysAgo(10)),
            Expense(category = "Faturalar", amount = 95.0, description = "Su faturası", expenseDate = daysAgo(7)),
            Expense(category = "Faturalar", amount = 150.0, description = "Doğalgaz faturası", expenseDate = daysAgo(15)),
            Expense(category = "Eğlence", amount = 250.0, description = "Sinema + yemek", expenseDate = daysAgo(4)),
            Expense(category = "Eğlence", amount = 200.0, description = "Tema Park giriş", expenseDate = daysAgo(12)),
            Expense(category = "Sağlık", amount = 200.0, description = "Diş hekimi muayene", expenseDate = daysAgo(6)),
            Expense(category = "Eğitim", amount = 350.0, description = "Zeynep özel ders ücreti", expenseDate = daysAgo(8)),
            Expense(category = "Eğitim", amount = 300.0, description = "Emir kurs ücreti", expenseDate = daysAgo(10)),
            Expense(category = "Ulaşım", amount = 200.0, description = "Akaryat", expenseDate = daysAgo(3)),
            Expense(category = "Ulaşım", amount = 180.0, description = "Metrobus aylık", expenseDate = daysAgo(14)),
            Expense(category = "Spor", amount = 450.0, description = "World Fitness aylık", expenseDate = daysAgo(20)),
            Expense(category = "Spor", amount = 300.0, description = "Yüzme havuzu aylık", expenseDate = daysAgo(18)),
            Expense(category = "Spor", amount = 200.0, description = "Futbol kulübü aylık", expenseDate = daysAgo(15)),
            Expense(category = "Spor", amount = 350.0, description = "Yoga stüdyo aylık", expenseDate = daysAgo(10)),
            Expense(category = "Giyim", amount = 320.0, description = "Zeynep okul kıyafeti", expenseDate = daysAgo(9)),
        )
        db.expenseDao().upsertAll(expenses)

        // ── FATURALAR ──
        val invoices = listOf(
            Invoice(title = "Elektrik Faturası - Eylül", amount = 280.0, category = "Faturalar", dueDate = daysFromNow(5).toString(), status = "pending", createdBy = "Mehmet"),
            Invoice(title = "İnternet Faturası", amount = 120.0, category = "Faturalar", dueDate = daysFromNow(10).toString(), status = "paid", createdBy = "Mehmet"),
            Invoice(title = "Su Faturası", amount = 95.0, category = "Faturalar", dueDate = daysFromNow(3).toString(), status = "pending", createdBy = "Mehmet"),
            Invoice(title = "Doğalgaz Faturası", amount = 150.0, category = "Faturalar", dueDate = daysFromNow(15).toString(), status = "pending", createdBy = "Mehmet"),
            Invoice(title = "Fitness Üye Aidatı", amount = 450.0, category = "Spor", dueDate = daysFromNow(1).toString(), status = "paid", createdBy = "Mehmet"),
            Invoice(title = "Yüzme Havuzu Aidatı", amount = 300.0, category = "Spor", dueDate = daysFromNow(1).toString(), status = "paid", createdBy = "Ayşe"),
            Invoice(title = "Zeynep Kurs Ücreti", amount = 350.0, category = "Eğitim", dueDate = daysFromNow(7).toString(), status = "pending", createdBy = "Ayşe"),
            Invoice(title = "Emir Futbol Ücreti", amount = 200.0, category = "Eğitim", dueDate = daysFromNow(7).toString(), status = "pending", createdBy = "Ayşe"),
        )
        invoices.forEach { db.invoiceDao().upsert(it) }

        // ── NOTLAR ──
        val notes = listOf(
            Note(title = "Ev Kuralları", content = "1. Yemekten sonra bulaşıkları hemen yıka\n2. TV izleme süresi hafta içi 1 saat\n3. Odalar her akşam toplanacak\n4. Telefon yemek masasında yasak", category = "Genel", color = "#2196F3", isPinned = true, createdBy = "Ayşe"),
            Note(title = "Zeynep Okul Programı", content = "Pazartesi: Matematik, Türkçe, İngilizce\nSalı: Fen, Müzik, Beden\nÇarşamba: Matematik, Resim, Türkçe\nPerşembe: Fen, İngilizce, Sosyal\nCuma: Matematik, Türkçe, Rehberlik", category = "Önemli", color = "#E91E63", isPinned = true, createdBy = "Ayşe"),
            Note(title = "Emir Futbol Programı", content = "Salı: 17:00-18:30 Antrenman\nPerşembe: 17:00-18:30 Antrenman\nCumartesi: 10:00-12:00 Maç", category = "Önemli", color = "#FF9800", createdBy = "Mehmet"),
            Note(title = "Tarif: Mercimek Çorbası", content = "Malzemeler:\n- 2 bardak kırmızı mercimek\n- 1 soğan\n- 1 havuç\n- Tuz, karabiber, kimyon\n- Sıvı yağ\n\nYapılışı: Soğanları kavur, havuçları ekle, mercimek ve su ekle, 30 dk pişir, blendsır ile pürüzsüzleştir.", category = "Tarif", color = "#4CAF50", createdBy = "Ayşe"),
            Note(title = "Alışveriş Planı - Bayram", content = "- Bayram şekeri ve lokum\n- Et (3 kg kuzu)\n- Meyve sepeti\n- İçecekler\n- Çocuklara hediye", category = "Alışveriş", color = "#9C27B0", createdBy = "Ayşe"),
            Note(title = "Tatil Notları - Antalya", content = "Otel: Liberty Hotels Lara\nTarih: 15-22 Temmuz\nUçuş: THY 08:30 gidiş, 18:00 dönüş\nAraç kiralama: Sixt\nPlaj havluları unutma!", category = "Önemli", color = "#00BCD4", isPinned = false, createdBy = "Mehmet"),
            Note(title = "Fikir: Ev Tadilatı", content = "- Mutfak tezgahı değiştir\n- Banyo fayansları yenilen\n- Balkon kapatma\n- Boya (salon + yatak odası)", category = "Fikir", color = "#795548", createdBy = "Mehmet"),
            Note(title = "Zeynep Ödev Takibi", content = "[ ] Matematik sayfa 45-48\n[x] Türkçe kompozisyon\n[ ] İngilizce kelime listesi\n[x] Fen deney raporu\n[ ] Resim ödevi (çizim)", category = "Eğitim", color = "#607D8B", createdBy = "Ayşe"),
        )
        notes.forEach { db.noteDao().upsert(it) }

        // ── HATIRLATICILAR ──
        val reminders = listOf(
            Reminder(title = "İlaç zamanı - Mehmet", description = "Tansiyon ilacı alınacak", reminderTime = hoursFromNow(2), category = "Sağlık", priority = "yüksek", repeatType = "daily", alarmSound = "alarm", createdBy = "Ayşe"),
            Reminder(title = "Zeynep'i okuldan al", description = "Saat 15:30'da servis durağına git", reminderTime = hoursFromNow(6), category = "Etkinlik", priority = "yüksek", repeatType = "daily", repeatDays = "1,2,3,4,5", alarmSound = "bell", createdBy = "Mehmet"),
            Reminder(title = "Emir futbol antrenmanı", description = "Saat 17:00'da salon önünde", reminderTime = hoursFromNow(8), category = "Etkinlik", priority = "orta", repeatType = "weekly", repeatDays = "2,4", alarmSound = "chime", createdBy = "Ayşe"),
            Reminder(title = "Fatura son ödeme günü", description = "Elektrik faturası - 280 TL", reminderTime = daysFromNow(3), category = "Fatura", priority = "yüksek", repeatType = "once", alarmSound = "urgent", createdBy = "Mehmet"),
            Reminder(title = "Market listesi hazırla", description = "Bu akşam için alışveriş listesi yaz", reminderTime = hoursFromNow(4), category = "Genel", priority = "orta", repeatType = "once", alarmSound = "default", createdBy = "Ayşe"),
            Reminder(title = "Haftalık temizlik", description = "Ev temizliği ve toz alma", reminderTime = daysFromNow(5), category = "Genel", priority = "düşük", repeatType = "weekly", repeatDays = "6", alarmSound = "bell", createdBy = "Ayşe"),
            Reminder(title = "Doktor randevusu", description = "Zeynep diş kontrolü - Dr. Yılmaz", reminderTime = daysFromNow(3), category = "Sağlık", priority = "yüksek", repeatType = "once", alarmSound = "alarm", createdBy = "Ayşe"),
            Reminder(title = "Aidat öde", description = "Site aidatı 500 TL", reminderTime = daysFromNow(10), category = "Fatura", priority = "orta", repeatType = "monthly", alarmSound = "chime", createdBy = "Mehmet"),
            Reminder(title = "Çöp günü", description = "Geri dönüşüm poşetlerini çıkar", reminderTime = hoursFromNow(10), category = "Genel", priority = "düşük", repeatType = "weekly", repeatDays = "3", alarmSound = "default", createdBy = "Emir"),
            Reminder(title = "Spa randevusu", description = "Ayşe - masaj saat 14:00", reminderTime = daysFromNow(4), category = "Sağlık", priority = "orta", repeatType = "once", alarmSound = "bell", createdBy = "Ayşe"),
        )
        reminders.forEach { db.reminderDao().upsert(it) }

        // ── SU TÜKETİMİ ──
        val waterLogs = listOf(
            WaterLog(memberId = "m1", amountMl = 250, drinkType = "Su", date = today),
            WaterLog(memberId = "m1", amountMl = 250, drinkType = "Su", date = today),
            WaterLog(memberId = "m1", amountMl = 200, drinkType = "Çay", date = today),
            WaterLog(memberId = "m2", amountMl = 250, drinkType = "Su", date = today),
            WaterLog(memberId = "m2", amountMl = 250, drinkType = "Meyve Suyu", date = today),
            WaterLog(memberId = "m2", amountMl = 330, drinkType = "Su", date = today),
            WaterLog(memberId = "m3", amountMl = 200, drinkType = "Su", date = today),
            WaterLog(memberId = "m3", amountMl = 150, drinkType = "Kahve", date = today),
            WaterLog(memberId = "m4", amountMl = 250, drinkType = "Su", date = today),
            WaterLog(memberId = "m4", amountMl = 330, drinkType = "Meyve Suyu", date = today),
        )
        waterLogs.forEach { db.waterLogDao().upsert(it) }

        // ── UYKU KAYITLARI ──
        val sleepLogs = listOf(
            SleepLog(memberId = "m1", bedtime = hoursAgo(10), wakeTime = hoursAgo(2), durationMinutes = 480, quality = "iyi", interruptions = 1, date = today),
            SleepLog(memberId = "m2", bedtime = hoursAgo(11), wakeTime = hoursAgo(3), durationMinutes = 480, quality = "çok iyi", interruptions = 0, date = today),
            SleepLog(memberId = "m3", bedtime = hoursAgo(12), wakeTime = hoursAgo(3), durationMinutes = 540, quality = "iyi", interruptions = 0, date = today),
            SleepLog(memberId = "m4", bedtime = hoursAgo(13), wakeTime = hoursAgo(3), durationMinutes = 600, quality = "çok iyi", interruptions = 0, date = today),
        )
        sleepLogs.forEach { db.sleepLogDao().upsert(it) }

        // ── MESAJLAR ──
        val messages = listOf(
            Message(senderName = "Ayşe", senderId = "m2", content = "Akşam ne yiyelim? 😊", channel = "genel"),
            Message(senderName = "Mehmet", senderId = "m1", content = "Mangal yapılsa güzel olur 🍖", channel = "genel"),
            Message(senderName = "Zeynep", senderId = "m3", content = "Annecim yarın okula erken gitmem lazım, kahvaltıyı erken yapalım mı?", channel = "genel"),
            Message(senderName = "Ayşe", senderId = "m2", content = "Tamam canım, saat 7'de kahvaltı hazır olur 👍", channel = "genel"),
            Message(senderName = "Emir", senderId = "m4", content = "Babacım topu nereye koydun? ⚽", channel = "genel"),
            Message(senderName = "Mehmet", senderId = "m1", content = "Balkondaki dolapta Emir", channel = "genel"),
            Message(senderName = "Ayşe", senderId = "m2", content = "Yarın markete gitmemiz lazım, liste hazırladım 📝", channel = "genel"),
            Message(senderName = "Zeynep", senderId = "m3", content = "Matematik ödevini bitirdim! 🎉", channel = "genel"),
        )
        messages.forEach { db.messageDao().upsert(it) }

        // ── STOK / ENVANTER ──
        val inventory = listOf(
            InventoryItem(name = "Sürekli Çalışan Fırın", category = "Mutfak", quantity = 1, unit = "adet", location = "Mutfak", notes = "Sinja marka, 2022 model"),
            InventoryItem(name = "Bulaşık Makinesi", category = "Mutfak", quantity = 1, unit = "adet", location = "Mutfak"),
            InventoryItem(name = "Çamaşır Makinesi", category = "Banyo", quantity = 1, unit = "adet", location = "Banyo"),
            InventoryItem(name = "Klima", category = "Elektronik", quantity = 2, unit = "adet", location = "Salon + Yatak Odası"),
            InventoryItem(name = "TV 55\"", category = "Elektronik", quantity = 1, unit = "adet", location = "Salon", notes = "Samsung 4K"),
            InventoryItem(name = "Priz (6'lı)", category = "Elektronik", quantity = 4, unit = "adet", location = "Ev geneli"),
            InventoryItem(name = "İlk Yardım Çantası", category = "Sağlık", quantity = 1, unit = "adet", location = "Banyo dolabı", notes = "Bandaj, antiseptik, ateş ölçer"),
            InventoryItem(name = "Düdüklü Tencere", category = "Mutfak", quantity = 1, unit = "adet", location = "Mutfak"),
            InventoryItem(name = "Supap (Lastik)", category = "Spor", quantity = 3, unit = "adet", location = "Emir'in odası"),
            InventoryItem(name = "Yoga Matı", category = "Spor", quantity = 1, unit = "adet", location = "Zeynep'in odası"),
            InventoryItem(name = "Şemsiye", category = "Diğer", quantity = 2, unit = "adet", location = "Balkon"),
            InventoryItem(name = "Pil (AA)", category = "Diğer", quantity = 8, unit = "adet", location = "Çekmece", notes = "Son kullanma: 2026"),
            InventoryItem(name = "Mum (Olası kesinti için)", category = "Diğer", quantity = 6, unit = "adet", location = "Mutfak dolabı"),
            InventoryItem(name = "Termometre", category = "Sağlık", quantity = 1, unit = "adet", location = "Banyo dolabı", notes = "Dijital"),
        )
        db.inventoryDao().upsertAll(inventory)
    }

    private fun hoursAgo(h: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, -h)
        return cal.timeInMillis
    }
}
