package com.aile.takip.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aile.takip.AileTakipApp
import com.aile.takip.data.model.*
import com.aile.takip.data.repository.FamilyRepository
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import com.aile.takip.sync.AutoSyncEngine
import com.aile.takip.sync.DatabaseSyncSource
import com.aile.takip.sync.FamilyInvite
import com.aile.takip.sync.FamilyInviteCodec
import com.aile.takip.sync.FirebaseSyncService
import com.aile.takip.sync.SyncCoordinator
import com.aile.takip.sync.SyncPreferences
import com.aile.takip.utils.BitmapCache
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as AileTakipApp).db
    private val repo = FamilyRepository(
        db.taskDao(), db.inventoryDao(), db.budgetDao(), db.expenseDao(),
        db.invoiceDao(), db.messageDao(), db.shoppingDao(),
        db.memberDao(), db.mealPlanDao(),
        db.sportsClubDao(), db.workoutLogDao(), db.calorieLogDao(),
        db.menstrualCycleDao(), db.authDao(), db.syncEventDao(),
        db.noteDao(), db.reminderDao(),
        db.waterLogDao(), db.sleepLogDao()
    )

    private val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Firebase Sync
    private val syncService = FirebaseSyncService(db)
    private val syncPrefs = SyncPreferences(app)
    private val autoSyncEngine = AutoSyncEngine(DatabaseSyncSource(db, syncService), syncService)

    val syncState = syncService.syncState
    val lastSyncTime = syncService.lastSyncTime
    val syncedTables = syncService.syncedTables

    /** Çakışmada daha yeni olan yerel kaydın korunduğu (ve geri gönderildiği) sayı. */
    val conflictsSolved = syncService.conflictsSolved
    val syncEnabled = mutableStateOf(false)
    val familyGroupId = mutableStateOf("")
    val familyPasscode = mutableStateOf("")
    val syncError = mutableStateOf<String?>(null)

    // Aile hesabı (gerçek üye kimliği = Firebase UID) ve üyelik durumu
    val familyUid = syncService.uid
    val membership = syncService.membership
    val pendingJoinRequests = syncService.pendingJoins
    val authError = syncService.authError
    val signedInEmail = mutableStateOf("")

    /** Katılım isteğinin doğru aile şifresiyle geldiğini doğrular. */
    fun passcodeProofMatches(proof: String): Boolean = syncService.passcodeProofMatches(proof)

    // Otomatik senkron durumu (her değişiklikte aile bireylerine yayılır)
    val autoSyncEnabled = autoSyncEngine.enabled
    val pendingSyncCount = autoSyncEngine.pendingCount
    val autoPushCount = autoSyncEngine.pushCount
    val lastAutoPushAt = autoSyncEngine.lastPushAt

    // Bu cihazı kullanan aile üyesi
    val myMemberId = mutableStateOf("")

    init {
        // Uzaktan uygulanan veriyi otomatik senkron motoru tekrar göndermesin (echo koruması)
        syncService.autoSync = autoSyncEngine
        // Üyelik onaylandığı anda senkronu otomatik başlat
        // NOT: Bu blok TÜM durum tanımlarından sonra gelmeli. Aksi halde Dispatchers.Main.immediate
        // collect'in ilk değerini kurucu içinde senkron yayar ve henüz oluşmamış (null)
        // MutableState'e yazmaya çalışıp NullPointerException ile çöker.
        viewModelScope.launch {
            syncService.membership.collect { state ->
                when (state) {
                    is FirebaseSyncService.Membership.Member -> {
                        syncEnabled.value = true
                        autoSyncEngine.start()
                    }
                    is FirebaseSyncService.Membership.SignedOut -> syncEnabled.value = false
                    else -> Unit
                }
            }
        }
    }

    // Cross-feature sync coordinator
    private val coordinator = SyncCoordinator(repo)

    // Auth state
    val auth = repo.auth.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    var isAuthenticated = mutableStateOf(false)
    var selectedMemberId = mutableStateOf<String?>(null)
    
    // Theme preferences
    var useDynamicColor = mutableStateOf(true)
    var isDarkMode = mutableStateOf(false)
    var useSystemTheme = mutableStateOf(true)
    var currentLanguage = mutableStateOf("tr")

    // Barcode/QR scan result
    val lastScanResult = mutableStateOf<String?>(null)
    val isLookingUpBarcode = mutableStateOf(false)
    val lastLookupResult = mutableStateOf<com.aile.takip.utils.BarcodeLookupHelper.ProductInfo?>(null)

    // Existing data
    val tasks = repo.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inventory = repo.inventory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = repo.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repo.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val invoices = repo.invoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val messages = repo.messages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val shoppingItems = repo.shoppingItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val members = repo.members.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mealPlans = repo.mealPlans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // New data
    val sportsClubs = repo.sportsClubs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val workoutLogs = repo.workoutLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val calorieLogs = repo.calorieLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val menstrualCycles = repo.menstrualCycles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val unsyncedEvents = repo.unsyncedEvents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notes = repo.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeReminders = repo.activeReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allReminders = repo.allReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val waterLogs = repo.waterLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sleepLogs = repo.sleepLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ===== OPTIMIZED DERIVED STATES =====
    // These avoid recomputing on every recomposition — only changes when source changes.
    val pendingTaskCount by derivedStateOf { tasks.value.count { it.status == "bekleyen" } }
    val pendingInvoiceCount by derivedStateOf { invoices.value.count { it.status == "pending" } }
    val uncheckedShoppingCount by derivedStateOf { shoppingItems.value.count { !it.checked } }
    val activeNoteCount by derivedStateOf { notes.value.count { !it.isArchived } }
    val pinnedNoteCount by derivedStateOf { notes.value.count { it.isPinned && !it.isArchived } }
    val todayCalories by derivedStateOf {
        calorieLogs.value.filter { it.date == todayStr }.sumOf { it.calories }
    }
    val todayWaterMl by derivedStateOf {
        waterLogs.value.filter { it.date == todayStr }.sumOf { it.amountMl.toLong() }.toInt()
    }
    val unreadMessageCount by derivedStateOf { messages.value.size }
    val totalMembers by derivedStateOf { members.value.size }
    val currentMember by derivedStateOf { members.value.find { it.id == myMemberId.value } }

    init {
        // Seed sample data on first run, then check PIN
        viewModelScope.launch {
            try {
                com.aile.takip.data.seeder.SampleDataSeeder.seedIfNeeded(db)
            } catch (e: Exception) {
                // Seeding failed, app continues with empty database
            }
            try {
                val a = repo.getAuthOnce()
                if (a == null || a.pin.isEmpty()) {
                    isAuthenticated.value = true // No PIN set = auto-login
                }
            } catch (e: Exception) {
                // Auth check failed, auto-login
                isAuthenticated.value = true
            }
            // Daha önce kurulmuş aile senkronizasyonunu sürdür
            try {
                bootstrapAutoSync()
            } catch (e: Exception) {
                // Otomatik bağlanma başarısız, uygulama yerel olarak çalışmaya devam eder
            }
        }
    }

    /**
     * Kayıtlı aile grubu varsa uygulama açılışında otomatik bağlanır ve
     * otomatik senkronu (auto-sync) başlatır. Veriler zaten cihazda (Room) durur,
     * bu yüzden bağlantı yokken uygulama tam çalışır.
     */
    private suspend fun bootstrapAutoSync() {
        val uid = syncService.loadExistingSession()
        if (uid == null) return
        val cfg = syncPrefs.current()
        myMemberId.value = cfg.myMemberId
        signedInEmail.value = cfg.myEmail
        autoSyncEngine.setEnabled(cfg.autoSync)
        if (cfg.groupId.isBlank()) return

        familyGroupId.value = cfg.groupId
        familyPasscode.value = cfg.passcode
        syncService.setGroup(cfg.groupId)
        when (syncService.refreshMembership()) {
            is FirebaseSyncService.Membership.Member -> {
                syncEnabled.value = true
                autoSyncEngine.start()
            }
            is FirebaseSyncService.Membership.Pending ->
                syncError.value = "Aile bireyinizin onayı bekleniyor."
            is FirebaseSyncService.Membership.Rejected ->
                syncError.value = "Katılım isteğiniz reddedildi. Aile bireyinizle görüşün."
            else -> Unit
        }
    }

    // ===== AUTH =====
    fun login(pin: String) {
        viewModelScope.launch {
            val a = repo.getAuthOnce()
            if (a == null || a.pin == pin) isAuthenticated.value = true
        }
    }
    fun setupPin(pin: String, name: String, email: String, securityQuestion: String = "", securityAnswer: String = "") {
        viewModelScope.launch { 
            repo.upsertAuth(UserAuth(
                pin = pin, name = name, email = email,
                securityQuestion = securityQuestion, 
                securityAnswer = securityAnswer.lowercase().trim()
            )) 
        }
    }
    fun resetPin() {
        viewModelScope.launch { repo.upsertAuth(UserAuth(id = "main_user", pin = "", name = "", email = "")); isAuthenticated.value = true }
    }
    fun resetPinWithNew(newPin: String) {
        viewModelScope.launch {
            val current = repo.getAuthOnce()
            if (current != null) {
                repo.upsertAuth(current.copy(pin = newPin))
            }
        }
    }

    /** Kullanıcı adı/e-postasını günceller. */
    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            val current = repo.getAuthOnce()
            repo.upsertAuth(
                current?.copy(name = name, email = email) ?: UserAuth(name = name, email = email)
            )
        }
    }
    fun verifySecurityAnswer(answer: String): Boolean {
        val current = auth.value
        return current?.securityAnswer?.lowercase()?.trim() == answer.lowercase().trim()
    }
    fun logout() { isAuthenticated.value = false }

    // ===== THEME PREFERENCES =====
    fun setDynamicColor(enabled: Boolean) {
        useDynamicColor.value = enabled
    }

    fun setDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        useSystemTheme.value = false
    }

    fun useSystemTheme() {
        useSystemTheme.value = true
    }

    // ===== LANGUAGE =====
    fun setLanguage(language: String) {
        currentLanguage.value = language
    }

    // ===== TASKS =====
    fun addTask(title: String, description: String = "", category: String = "Genel", priority: String = "orta", assignee: String = "", dueDate: String = "") {
        viewModelScope.launch {
            val task = Task(title = title, description = description, category = category, priority = priority, assignee = assignee, dueDate = dueDate)
            repo.upsertTask(task)
            coordinator.onTaskCreated(task)
            addSyncEvent("tasks", "insert")
        }
    }
    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val newStatus = if (task.status == "tamamlanan") "bekleyen" else "tamamlanan"
            val updatedTask = task.copy(status = newStatus, completedAt = if (newStatus == "tamamlanan") System.currentTimeMillis() else null)
            repo.upsertTask(updatedTask)
            if (newStatus == "tamamlanan") {
                coordinator.onTaskCompleted(updatedTask)
            }
            addSyncEvent("tasks", "update")
        }
    }
    fun updateTask(task: Task) {
        viewModelScope.launch {
            repo.upsertTask(task)
            // Bağlı hatırlatıcıyı da güncelle/iptal et (modüller arası senkron)
            try { coordinator.onTaskUpdated(task) } catch (_: Exception) {}
            addSyncEvent("tasks", "update")
        }
    }
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repo.deleteTask(task)
            // Bağlı hatırlatıcıları da temizle
            try { coordinator.onTaskDeleted(task) } catch (_: Exception) {}
            addSyncEvent("tasks", "delete")
        }
    }

    // ===== INVENTORY =====
    fun addInventory(name: String, category: String = "Genel", quantity: Int = 1, unit: String = "adet", minStock: Int = 0, location: String = "", imageBase64: String = "") {
        viewModelScope.launch {
            val item = InventoryItem(name = name, category = category, quantity = quantity, unit = unit, minStock = minStock, location = location, imageBase64 = imageBase64)
            repo.upsertInventory(item)
            // Eşik altındaysa stok hatırlatıcısı + alışveriş listesi
            try { coordinator.onInventoryLowStock(item) } catch (_: Exception) {}
            addSyncEvent("inventory", "insert")
        }
    }
    fun updateInventory(item: InventoryItem) {
        viewModelScope.launch {
            repo.upsertInventory(item)
            // Miktar/eşik değişiminde stok hatırlatıcısını yönet
            try { coordinator.onInventoryLowStock(item) } catch (_: Exception) {}
            addSyncEvent("inventory", "update")
        }
    }
    fun deleteInventory(item: InventoryItem) { viewModelScope.launch { repo.deleteInventory(item); addSyncEvent("inventory", "delete") } }

    // ===== BUDGETS =====
    fun addBudget(category: String, limit: Double, monthYear: String) {
        viewModelScope.launch { repo.upsertBudget(Budget(category = category, monthlyLimit = limit, monthYear = monthYear)); addSyncEvent("budgets", "insert") }
    }
    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            repo.upsertBudget(budget)
            addSyncEvent("budgets", "update")
        }
    }
    fun deleteBudget(b: Budget) { viewModelScope.launch { repo.deleteBudget(b); addSyncEvent("budgets", "delete") } }

    // ===== EXPENSES =====
    fun addExpense(category: String, amount: Double, description: String = "", date: String, budgetId: String = "") {
        viewModelScope.launch {
            val expense = Expense(category = category, amount = amount, description = description, expenseDate = date, budgetId = budgetId)
            repo.upsertExpense(expense)
            coordinator.onExpenseRecorded(expense)
            addSyncEvent("expenses", "insert")
        }
    }
    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repo.upsertExpense(expense)
            addSyncEvent("expenses", "update")
        }
    }
    fun deleteExpense(e: Expense) { viewModelScope.launch { repo.deleteExpense(e) } }

    // ===== COORDINATOR HELPERS =====
    suspend fun getLowStockSuggestions() = coordinator.getLowStockSuggestions()
    suspend fun autoAddLowStockToShopping() = coordinator.autoAddLowStockToShopping()
    suspend fun getDailyCalorieSummary(memberId: String) = coordinator.getDailyCalorieSummary(memberId)
    suspend fun getWeeklyCalorieTrend(memberId: String) = coordinator.getWeeklyCalorieTrend(memberId)
    suspend fun getCalorieGoalStatus(memberId: String) = coordinator.getCalorieGoalStatus(memberId)

    // ===== INVOICES =====
    fun addInvoice(title: String, amount: Double, category: String = "Genel", dueDate: String = "", notes: String = "", imageBase64: String? = null) {
        viewModelScope.launch {
            val invoice = Invoice(title = title, amount = amount, category = category, dueDate = dueDate, notes = notes, imageBase64 = imageBase64)
            repo.upsertInvoice(invoice)
            coordinator.onInvoiceCreated(invoice)
            addSyncEvent("invoices", "insert")
        }
    }
    fun toggleInvoiceStatus(invoice: Invoice) {
        viewModelScope.launch {
            val newStatus = if (invoice.status == "paid") "pending" else "paid"
            val updated = invoice.copy(status = newStatus)
            repo.upsertInvoice(updated)
            // Ödeme/geri alma bağlı hatırlatıcıya yansır (ödendiyse iptal)
            try { coordinator.onInvoiceUpdated(updated) } catch (_: Exception) {}
            addSyncEvent("invoices", "update")
        }
    }
    fun updateInvoice(invoice: Invoice) {
        viewModelScope.launch {
            repo.upsertInvoice(invoice)
            // Bağlı hatırlatıcıyı yenile/ödendi ise iptal et
            try { coordinator.onInvoiceUpdated(invoice) } catch (_: Exception) {}
            addSyncEvent("invoices", "update")
        }
    }
    fun deleteInvoice(i: Invoice) {
        viewModelScope.launch {
            repo.deleteInvoice(i)
            // Bağlı hatırlatıcıları da temizle
            try { coordinator.onInvoiceDeleted(i) } catch (_: Exception) {}
            addSyncEvent("invoices", "delete")
        }
    }

    // ===== FİŞ/FATURA OCR =====
    /** Fiş tarama sonucu (UI otomatik doldurma için). */
    val receiptScanResult = mutableStateOf<ReceiptOcrView?>(null)
    val isScanningReceipt = mutableStateOf(false)

    /** UI'a taşınabilir sadeleştirilmiş OCR sonucu. */
    data class ReceiptOcrView(
        val title: String,
        val amount: String,
        val dueDate: String,
        val category: String,
        val confidence: Float,
        val rawText: String
    )

    /**
     * Fiş/fatura fotoğrafını OCR ile okur; sonucu [receiptScanResult]'a yazar.
     * Barkod internette bulunamadığında fiş fotoğrafı yeterli olur.
     */
    fun scanReceiptImage(context: android.content.Context, uri: android.net.Uri) {
        if (isScanningReceipt.value) return
        isScanningReceipt.value = true
        receiptScanResult.value = null
        val appContext = context.applicationContext
        com.aile.takip.utils.ReceiptOcr.scan(appContext, uri) { result ->
            isScanningReceipt.value = false
            receiptScanResult.value = result.parsed?.let { p ->
                ReceiptOcrView(
                    title = p.title,
                    amount = p.amount?.let { a ->
                        if (a % 1.0 == 0.0) a.toInt().toString() else String.format(java.util.Locale.US, "%.2f", a)
                    } ?: "",
                    dueDate = p.dueDate,
                    category = p.category,
                    confidence = p.confidence,
                    rawText = result.rawText.takeLast(2000)
                )
            } ?: ReceiptOcrView("", "", "", "", 0f, result.error ?: result.rawText.takeLast(500))
        }
    }

    // ===== YEMEK PLANI ↔ AKILLI ALIŞVERİŞ =====
    /** Haftalık eksik malzeme önerileri; null = henüz hesaplanmadı, boş = eksik yok. */
    val smartSuggestions = mutableStateOf<List<com.aile.takip.sync.AggregateMissing>?>(null)
    val isLoadingSuggestions = mutableStateOf(false)

    // Haftalık market listesi + önceki hafta karşılaştırması
    val marketDiff = mutableStateOf<com.aile.takip.utils.MarketListStore.WeekDiff?>(null)
    val isGeneratingMarketList = mutableStateOf(false)

    private val marketPersistence by lazy { com.aile.takip.sync.MarketListPersistence(app.applicationContext) }

    /**
     * Bu haftanın market listesini üretir (eksiklerden), kaydeder ve
     * önceki haftayla karşılaştırma özetini döner.
     */
    fun generateMarketList(onDone: (com.aile.takip.utils.MarketListStore.WeekDiff?) -> Unit = {}) {
        if (isGeneratingMarketList.value) return
        isGeneratingMarketList.value = true
        viewModelScope.launch {
            val diff = try {
                coordinator.generateWeeklyMarketList(marketPersistence)
            } catch (_: Exception) {
                null
            }
            marketDiff.value = diff
            isGeneratingMarketList.value = false
            onDone(diff)
        }
    }

    /** Yemek planındaki eksik malzemeleri envanterle gerçek eşleştirmeyle hesaplar. */
    fun loadSmartSuggestions() {
        if (isLoadingSuggestions.value) return
        isLoadingSuggestions.value = true
        viewModelScope.launch {
            try {
                smartSuggestions.value = coordinator.weeklyMissingIngredients()
            } catch (_: Exception) {
                smartSuggestions.value = emptyList()
            }
            isLoadingSuggestions.value = false
        }
    }

    /**
     * Eksik malzemeleri alışveriş listesine ekler.
     * [names] boşsa tümünü ekler; eklenen malzeme sayısını döner (UI bildirimi için).
     */
    fun addSuggestionsToShopping(names: Collection<String> = emptyList(), onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val added = try {
                coordinator.addMissingToShopping(names)
            } catch (_: Exception) {
                0
            }
            if (added > 0) addSyncEvent("shopping", "insert")
            // Listenin güncel halini yansıt: eklenenler artık "eksik" önerisi olmamalı
            loadSmartSuggestions()
            onDone(added)
        }
    }

    // ===== MESSAGES =====
    fun sendMessage(content: String, senderName: String = "Ben", attachments: String = "") {
        viewModelScope.launch { repo.upsertMessage(Message(senderName = senderName, senderId = "self", content = content, attachments = attachments)); addSyncEvent("messages", "insert") }
    }
    fun deleteMessage(m: Message) { viewModelScope.launch { repo.deleteMessage(m) } }

    // ===== SHOPPING =====
    fun addShoppingItem(name: String, quantity: Int = 1, category: String = "Market", addedBy: String = "", barcode: String = "", brand: String = "", description: String = "", unitPrice: Double = 0.0, store: String = "", imageBase64: String = "") {
        viewModelScope.launch {
            val totalPrice = unitPrice * quantity
            repo.upsertShopping(ShoppingItem(
                name = name, quantity = quantity, category = category, addedBy = addedBy,
                barcode = barcode, brand = brand, description = description,
                unitPrice = unitPrice, totalPrice = totalPrice, store = store, imageBase64 = imageBase64
            ))
            addSyncEvent("shopping", "insert")
        }
    }
    fun toggleShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            val newChecked = !item.checked
            repo.upsertShopping(item.copy(checked = newChecked));
            if (newChecked) {
                coordinator.onShoppingItemBought(item)
            }
            addSyncEvent("shopping", "update")
        }
    }
    fun updateShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            repo.upsertShopping(item)
            addSyncEvent("shopping", "update")
        }
    }
    fun deleteShopping(item: ShoppingItem) { viewModelScope.launch { repo.deleteShopping(item); addSyncEvent("shopping", "delete") } }

    // ===== MEMBERS =====
    fun addMember(name: String, role: String = "Üye", color: String = "#3498DB") {
        viewModelScope.launch {
            repo.upsertMember(FamilyMember(name = name, role = role, color = color))
            addSyncEvent("members", "insert")
        }
    }

    /** Var olan üyenin adı, rolü, rengi vb. güncellenir. */
    fun updateMember(member: FamilyMember) {
        viewModelScope.launch {
            repo.upsertMember(member.copy(syncVersion = System.currentTimeMillis()))
            addSyncEvent("members", "update")
        }
    }

    fun addPoints(member: FamilyMember, points: Int) {
        viewModelScope.launch {
            repo.upsertMember(member.copy(points = member.points + points, syncVersion = System.currentTimeMillis()))
            addSyncEvent("members", "update")
        }
    }

    fun deleteMember(m: FamilyMember) {
        viewModelScope.launch {
            repo.deleteMember(m)
            if (myMemberId.value == m.id) {
                myMemberId.value = ""
                syncPrefs.setDeviceUser("", "")
            }
            addSyncEvent("members", "delete")
        }
    }

    /** Bu cihazı kullanan aile üyesini seçer (kim neyi yaptı bilgisi için). */
    fun setDeviceUser(member: FamilyMember?) {
        myMemberId.value = member?.id ?: ""
        viewModelScope.launch { syncPrefs.setDeviceUser(member?.id ?: "", member?.name ?: "") }
    }

    // ===== MEAL PLANS =====
    fun addMealPlan(dayOfWeek: Int, mealType: String, dish: String, notes: String = "") {
        viewModelScope.launch {
            val mealPlan = MealPlan(dayOfWeek = dayOfWeek, mealType = mealType, dish = dish, notes = notes)
            repo.upsertMealPlan(mealPlan)
            coordinator.onMealPlanCreated(mealPlan)
            addSyncEvent("meal_plans", "insert")
        }
    }
    fun deleteMealPlan(mp: MealPlan) { viewModelScope.launch { repo.deleteMealPlan(mp); addSyncEvent("meal_plans", "delete") } }

    // ===== SPORTS CLUBS =====
    fun addClub(name: String, type: String, address: String = "", phone: String = "", fee: Double = 0.0, memberId: String = "", startDate: String = "", endDate: String = "") {
        viewModelScope.launch {
            repo.upsertClub(SportsClub(name = name, type = type, address = address, phone = phone, monthlyFee = fee, memberId = memberId, membershipStart = startDate, membershipEnd = endDate))
            addSyncEvent("sports_clubs", "insert")
        }
    }
    fun deleteClub(c: SportsClub) { viewModelScope.launch { repo.deleteClub(c); addSyncEvent("sports_clubs", "delete") } }

    // ===== WORKOUT LOGS =====
    fun addWorkout(clubId: String, memberId: String, workoutType: String, duration: Int, calories: Int, date: String, notes: String = "") {
        viewModelScope.launch {
            repo.upsertWorkout(WorkoutLog(clubId = clubId, memberId = memberId, workoutType = workoutType, duration = duration, caloriesBurned = calories, date = date, notes = notes))
            addSyncEvent("workout_logs", "insert")
        }
    }
    fun deleteWorkout(w: WorkoutLog) { viewModelScope.launch { repo.deleteWorkout(w) } }

    // ===== CALORIE LOGS =====
    fun addCalorie(memberId: String, mealType: String, foodName: String, calories: Int, protein: Double = 0.0, carbs: Double = 0.0, fat: Double = 0.0, serving: String = "", date: String = todayStr) {
        viewModelScope.launch {
            repo.upsertCalorie(CalorieLog(memberId = memberId, mealType = mealType, foodName = foodName, calories = calories, protein = protein, carbs = carbs, fat = fat, servingSize = serving, date = date))
            addSyncEvent("calorie_logs", "insert")
        }
    }
    fun deleteCalorie(c: CalorieLog) { viewModelScope.launch { repo.deleteCalorie(c) } }

    // ===== MENSTRUAL CYCLES =====
    fun addCycle(memberId: String, startDate: String, cycleLength: Int = 28, periodLength: Int = 5, symptoms: String = "", mood: String = "", flow: String = "", isPersonal: Boolean = true, notes: String = "") {
        viewModelScope.launch {
            repo.upsertCycle(MenstrualCycle(memberId = memberId, startDate = startDate, cycleLength = cycleLength, periodLength = periodLength, symptoms = symptoms, mood = mood, flow = flow, isPersonal = isPersonal, notes = notes))
            addSyncEvent("menstrual_cycles", "insert")
        }
    }
    fun deleteCycle(c: MenstrualCycle) { viewModelScope.launch { repo.deleteCycle(c) } }

    // ===== SYNC =====
    private fun addSyncEvent(table: String, action: String) {
        viewModelScope.launch {
            repo.upsertSyncEvent(SyncEvent(tableName = table, recordId = UUID.randomUUID().toString(), action = action))
        }
    }

    // Export all data as JSON for sync
    fun exportAllData(): String {
        val data = mapOf(
            "tasks" to tasks.value,
            "inventory" to inventory.value,
            "budgets" to budgets.value,
            "expenses" to expenses.value,
            "invoices" to invoices.value,
            "messages" to messages.value,
            "shopping" to shoppingItems.value,
            "members" to members.value,
            "mealPlans" to mealPlans.value,
            "sportsClubs" to sportsClubs.value,
            "workoutLogs" to workoutLogs.value,
            "calorieLogs" to calorieLogs.value,
            "menstrualCycles" to menstrualCycles.value,
            "exportDate" to todayStr,
            "appVersion" to "3.1.0"
        )
        return com.google.gson.Gson().toJson(data)
    }

    // ===== FIREBASE SYNC =====

    // ---- Aile hesabı (gerçek üye kimliği) ----

    fun signInFamilyAccount(email: String, password: String) {
        viewModelScope.launch {
            syncError.value = null
            when (val r = syncService.signInWithEmail(email, password)) {
                is FirebaseSyncService.AuthResult.Success -> {
                    signedInEmail.value = email.trim()
                    syncPrefs.setAccount(email.trim())
                    bootstrapAutoSync()
                }
                is FirebaseSyncService.AuthResult.Error -> syncError.value = r.message
            }
        }
    }

    fun signUpFamilyAccount(email: String, password: String) {
        viewModelScope.launch {
            syncError.value = null
            when (val r = syncService.signUpWithEmail(email, password)) {
                is FirebaseSyncService.AuthResult.Success -> {
                    signedInEmail.value = email.trim()
                    syncPrefs.setAccount(email.trim())
                    bootstrapAutoSync()
                }
                is FirebaseSyncService.AuthResult.Error -> syncError.value = r.message
            }
        }
    }

    fun signOutFamilyAccount() {
        autoSyncEngine.stop()
        syncService.signOutUser()
        syncEnabled.value = false
        signedInEmail.value = ""
        viewModelScope.launch {
            syncPrefs.setEnabled(false)
            syncPrefs.setAccount("")
        }
    }

    // ---- Grup kurma / katılma ----

    /**
     * Yeni aile grubu oluşturur. Grup kimliği tahmin edilemez şekilde üretilir.
     * Kuran kişi ilk üye olur ve mevcut veriler gruba taşınır.
     */
    fun createFamilyGroup(passcode: String) {
        viewModelScope.launch {
            syncError.value = null
            if (syncService.uid.value == null) {
                syncError.value = "Önce aile hesabıyla giriş yapın"
                return@launch
            }
            if (passcode.length < 4) {
                syncError.value = "Aile şifresi en az 4 karakter olmalı"
                return@launch
            }
            val newGroupId = SyncPreferences.newGroupId()
            val myName = currentMember?.name?.ifBlank { null } ?: "Aile Bireyi"

            when (val result = syncService.createFamily(newGroupId, passcode, myName)) {
                is FirebaseSyncService.Membership.Member -> {
                    familyGroupId.value = newGroupId
                    familyPasscode.value = passcode
                    syncPrefs.saveGroup(newGroupId, passcode)
                    syncEnabled.value = true
                    // Kurucu cihaz: mevcut veriyi gruba taşı
                    autoSyncEngine.pause()
                    try {
                        syncService.pushAll()
                    } finally {
                        autoSyncEngine.resume()
                    }
                    autoSyncEngine.setEnabled(syncPrefs.current().autoSync)
                    autoSyncEngine.start()
                }
                is FirebaseSyncService.Membership.Error -> syncError.value = result.message
                else -> syncError.value = "Grup oluşturulamadı"
            }
        }
    }

    /**
     * Var olan gruba katılma isteği gönderir.
     *
     * Kullanıcı doğrudan üye olamaz: istek gönderilir ve **mevcut bir aile bireyi
     * onaylayana kadar** hiçbir veri okunamaz/yazılamaz (kurallar bunu zorunlu kılar).
     */
    fun requestJoinFamily(groupId: String, passcode: String) {
        viewModelScope.launch {
            syncError.value = null
            if (groupId.isBlank()) {
                syncError.value = "Grup ID gerekli"
                return@launch
            }
            if (passcode.length < 4) {
                syncError.value = "Aile şifresi en az 4 karakter olmalı"
                return@launch
            }
            if (syncService.uid.value == null) {
                syncError.value = "Önce aile hesabıyla giriş yapın"
                return@launch
            }
            familyGroupId.value = groupId.trim()
            familyPasscode.value = passcode
            syncService.setGroup(groupId.trim())

            // Zaten onaylanmış üye miyiz?
            when (val state = syncService.refreshMembership()) {
                is FirebaseSyncService.Membership.Member -> {
                    syncPrefs.saveGroup(groupId.trim(), passcode)
                    syncEnabled.value = true
                    autoSyncEngine.start()
                    return@launch
                }
                is FirebaseSyncService.Membership.Error -> {
                    syncError.value = state.message
                    return@launch
                }
                else -> Unit
            }

            val myName = currentMember?.name?.ifBlank { null } ?: "Aile Bireyi"
            when (val result = syncService.requestJoin(passcode, myName)) {
                is FirebaseSyncService.Membership.Pending -> {
                    syncPrefs.saveGroup(groupId.trim(), passcode)
                    syncError.value = "Katılım isteğiniz gönderildi. Aile bireyinizin onayı bekleniyor."
                }
                is FirebaseSyncService.Membership.Error -> syncError.value = result.message
                else -> syncError.value = "Katılım isteği gönderilemedi"
            }
        }
    }

    // ---- Üye yönetimi (yalnızca mevcut üyeler yapabilir) ----

    fun approveJoinRequest(request: FirebaseSyncService.JoinRequest) {
        viewModelScope.launch {
            if (!syncService.approveJoin(request.uid, request.name)) {
                syncError.value = "Katılım isteği onaylanamadı"
            }
        }
    }

    fun rejectJoinRequest(uid: String) {
        viewModelScope.launch { syncService.rejectJoin(uid) }
    }

    /** Bir üyenin grup erişimini kaldırır. */
    fun removeFamilyAccess(uid: String) {
        viewModelScope.launch {
            if (!syncService.removeMember(uid)) {
                syncError.value = "Üyenin erişimi kaldırılamadı"
            }
        }
    }

    /** Cihazdaki tüm veriyi gruba elle gönderir. */
    fun syncToFirebase() {
        viewModelScope.launch {
            autoSyncEngine.pause()
            try {
                syncService.pushAll()
            } finally {
                autoSyncEngine.resume()
            }
        }
    }

    /** Otomatik senkronu açar/kapatır. Veriler her durumda cihazda kalır. */
    fun setAutoSync(enabled: Boolean) {
        autoSyncEngine.setEnabled(enabled)
        viewModelScope.launch { syncPrefs.setAutoSync(enabled) }
    }

    fun disconnectFirebase() {
        autoSyncEngine.stop()
        syncService.disconnect()
        syncEnabled.value = false
        viewModelScope.launch { syncPrefs.setEnabled(false) }
    }

    fun clearSyncError() {
        syncError.value = null
    }

    // ===== AİLE DAVETİ / PAYLAŞIM =====

    /** QR/kod olarak okunabilen kısa davet kodu. */
    fun familyInviteCode(): String =
        FamilyInviteCodec.encode(familyGroupId.value, familyPasscode.value)

    /** WhatsApp/mesaj ile paylaşılacak okunur davet metni. */
    fun familyInviteText(): String =
        FamilyInviteCodec.shareText(familyGroupId.value, familyPasscode.value)

    /** QR koddan veya paylaşılan metinden davet bilgilerini çözer. */
    fun parseInvite(raw: String): FamilyInvite? = FamilyInviteCodec.decode(raw)

    // ===== NOTES =====
    fun addNote(title: String, content: String = "", category: String = "Genel", color: String = "#3498DB", attachments: String = "", createdBy: String = "") {
        viewModelScope.launch {
            repo.upsertNote(Note(title = title, content = content, category = category, color = color, attachments = attachments, createdBy = createdBy))
            addSyncEvent("notes", "insert")
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repo.upsertNote(note)
            addSyncEvent("notes", "update")
        }
    }

    fun togglePinNote(note: Note) {
        viewModelScope.launch {
            repo.upsertNote(note.copy(isPinned = !note.isPinned))
            addSyncEvent("notes", "update")
        }
    }

    fun archiveNote(note: Note) {
        viewModelScope.launch {
            repo.upsertNote(note.copy(isArchived = !note.isArchived))
            addSyncEvent("notes", "update")
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repo.deleteNote(note)
            addSyncEvent("notes", "delete")
        }
    }

    // ===== REMINDERS =====
    fun addReminder(
        title: String,
        description: String = "",
        reminderTime: Long,
        repeatType: String = "once",
        category: String = "Genel",
        priority: String = "orta",
        alarmSound: String = "default",
        vibrate: Boolean = true,
        snoozeMinutes: Int = 15,
        repeatDays: String = "",
        repeatInterval: Int = 1,
        repeatEndDate: Long = 0L,
        linkedId: String = "",
        linkedType: String = "",
        createdBy: String = ""
    ) {
        viewModelScope.launch {
            repo.upsertReminder(
                Reminder(
                    title = title,
                    description = description,
                    reminderTime = reminderTime,
                    repeatType = repeatType,
                    repeatDays = repeatDays,
                    repeatInterval = repeatInterval,
                    repeatEndDate = repeatEndDate,
                    category = category,
                    priority = priority,
                    alarmSound = alarmSound,
                    vibrate = vibrate,
                    snoozeMinutes = snoozeMinutes,
                    linkedId = linkedId,
                    linkedType = linkedType,
                    createdBy = createdBy,
                    nextFireAt = reminderTime
                )
            )
            addSyncEvent("reminders", "insert")
        }
    }

    fun completeReminder(reminder: Reminder) {
        viewModelScope.launch {
            repo.upsertReminder(reminder.copy(isCompleted = true))
            addSyncEvent("reminders", "update")
        }
    }

    fun snoozeReminder(reminder: Reminder) {
        viewModelScope.launch {
            val minutes = reminder.snoozeMinutes
            val snoozeUntil = System.currentTimeMillis() + (minutes * 60 * 1000)
            repo.upsertReminder(reminder.copy(isSnoozed = true, snoozeUntil = snoozeUntil))
            addSyncEvent("reminders", "update")
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            repo.upsertReminder(reminder)
            addSyncEvent("reminders", "update")
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repo.deleteReminder(reminder)
            addSyncEvent("reminders", "delete")
        }
    }

    // ===== WATER LOGS =====
    fun addWater(memberId: String, amountMl: Int = 250, drinkType: String = "Su", date: String = todayStr) {
        viewModelScope.launch {
            repo.upsertWater(WaterLog(memberId = memberId, amountMl = amountMl, drinkType = drinkType, date = date))
            addSyncEvent("water_logs", "insert")
        }
    }

    fun deleteWater(w: WaterLog) {
        viewModelScope.launch {
            repo.deleteWater(w)
            addSyncEvent("water_logs", "delete")
        }
    }

    fun totalWater(memberId: String, date: String = todayStr) = repo.totalWater(memberId, date)

    // ===== SLEEP LOGS =====
    fun addSleep(memberId: String, bedtime: Long, wakeTime: Long, quality: String = "orta", interruptions: Int = 0, notes: String = "", date: String = todayStr) {
        viewModelScope.launch {
            val durationMinutes = ((wakeTime - bedtime) / (1000 * 60)).toInt()
            repo.upsertSleep(
                SleepLog(
                    memberId = memberId,
                    bedtime = bedtime,
                    wakeTime = wakeTime,
                    durationMinutes = durationMinutes,
                    quality = quality,
                    interruptions = interruptions,
                    notes = notes,
                    date = date
                )
            )
            addSyncEvent("sleep_logs", "insert")
        }
    }

    fun deleteSleep(s: SleepLog) {
        viewModelScope.launch {
            repo.deleteSleep(s)
            addSyncEvent("sleep_logs", "delete")
        }
    }

    // ===== BARCODE LOOKUP =====
    fun lookupBarcode(barcode: String) {
        viewModelScope.launch {
            isLookingUpBarcode.value = true
            try {
                val result = com.aile.takip.utils.BarcodeLookupHelper.lookupBarcode(barcode)
                lastLookupResult.value = result
            } catch (e: Exception) {
                lastLookupResult.value = com.aile.takip.utils.BarcodeLookupHelper.ProductInfo(
                    barcode = barcode,
                    name = "",
                    found = false
                )
            } finally {
                isLookingUpBarcode.value = false
            }
        }
    }

    fun clearLookupResult() {
        lastLookupResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        autoSyncEngine.shutdown()
        syncService.stopListening()
        BitmapCache.clear()
    }
}

