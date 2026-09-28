package com.aile.takip.sync

import com.aile.takip.data.db.AppDatabase
import com.aile.takip.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseSyncService(private val db: AppDatabase) : SyncSink {

    private var database: FirebaseDatabase? = null
    private var auth: FirebaseAuth? = null
    private var familyRef: DatabaseReference? = null
    private val listenerCleanups = mutableListOf<() -> Unit>()

    /** Otomatik senkron motoru; uzaktan veri uygulandığında bilgilendirilir. */
    var autoSync: AutoSyncEngine? = null

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Disconnected)
    val syncState: StateFlow<SyncState> = _syncState

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime

    private val _syncedTables = MutableStateFlow(emptySet<String>())
    val syncedTables: StateFlow<Set<String>> = _syncedTables

    sealed class SyncState {
        data object Disconnected : SyncState()
        data object Connecting : SyncState()
        data object Connected : SyncState()
        data class Syncing(val table: String = "") : SyncState()
        data class Error(val message: String) : SyncState()
    }

    // ==================== KİMLİK: GERÇEK ÜYE (UID) ====================

    /** E-posta/şifre ile giriş sonucu. */
    sealed class AuthResult {
        data class Success(val uid: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    /**
     * Aile grubundaki üyelik durumu.
     *
     * Veritabanı kuralları yalnızca [Member] olan UID'lere okuma/yazma izni verir;
     * bu yüzden senkronizasyon sadece onaylanmış aile bireyleriyle çalışır.
     */
    sealed class Membership {
        data object SignedOut : Membership()
        data class NotMember(val uid: String) : Membership()
        data class Pending(val uid: String) : Membership()
        data class Rejected(val uid: String) : Membership()
        data class Member(val uid: String) : Membership()
        data class Error(val message: String) : Membership()
    }

    /** Onay bekleyen katılım isteği. */
    data class JoinRequest(
        val uid: String,
        val name: String = "",
        val passcodeProof: String = "",
        val requestedAt: Long = 0L
    )

    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> = _uid

    private val _membership = MutableStateFlow<Membership>(Membership.SignedOut)
    val membership: StateFlow<Membership> = _membership

    private val _pendingJoins = MutableStateFlow<List<JoinRequest>>(emptyList())
    val pendingJoins: StateFlow<List<JoinRequest>> = _pendingJoins

    private val _groupPasscodeHash = MutableStateFlow<String?>(null)
    val groupPasscodeHash: StateFlow<String?> = _groupPasscodeHash

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private var groupId: String? = null
    private var membershipRef: DatabaseReference? = null
    private var membershipListener: ValueEventListener? = null
    private var joinsRef: DatabaseReference? = null
    private var joinsListener: ValueEventListener? = null
    private var connectionRef: DatabaseReference? = null
    private var connectionListener: ValueEventListener? = null
    private var tombstoneRef: DatabaseReference? = null
    private var tombstoneListener: ValueEventListener? = null

    private val gson = com.google.gson.Gson()

    private val _conflictsSolved = MutableStateFlow(0)

    /** Çakışmada daha yeni olan yerel verinin korunduğu kayıt sayısı. */
    val conflictsSolved: StateFlow<Int> = _conflictsSolved

    private fun firebaseAuth(): FirebaseAuth? = try {
        if (auth == null) auth = FirebaseAuth.getInstance()
        auth
    } catch (e: Exception) {
        null
    }

    private fun firebaseDb(): FirebaseDatabase? = try {
        if (database == null) database = FirebaseDatabase.getInstance()
        database
    } catch (e: Exception) {
        null
    }

    /** Senkronize edilecek aile grubunu belirler (henüz erişim kontrolü yapmaz). */
    fun setGroup(groupId: String) {
        this.groupId = groupId
    }

    /** Uygulama açılışında mevcut oturumu (UID) yükler. */
    fun loadExistingSession(): String? {
        val current = try {
            firebaseAuth()?.currentUser?.uid
        } catch (e: Exception) {
            null
        }
        _uid.value = current
        if (current == null) _membership.value = Membership.SignedOut
        return current
    }

    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        val a = firebaseAuth() ?: return AuthResult.Error("Firebase Authentication başlatılamadı")
        if (email.isBlank() || !email.contains("@")) return AuthResult.Error("Geçerli bir e-posta girin")
        if (password.length < 6) return AuthResult.Error("Şifre en az 6 karakter olmalı")
        return try {
            val result = a.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid = result.user?.uid ?: return AuthResult.Error("Kullanıcı oluşturulamadı")
            _uid.value = uid
            _authError.value = null
            AuthResult.Success(uid)
        } catch (e: Exception) {
            AuthResult.Error(friendlyAuthError(e)).also { _authError.value = it.message }
        }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        val a = firebaseAuth() ?: return AuthResult.Error("Firebase Authentication başlatılamadı")
        if (email.isBlank() || !email.contains("@")) return AuthResult.Error("Geçerli bir e-posta girin")
        return try {
            val result = a.signInWithEmailAndPassword(email.trim(), password).await()
            val uid = result.user?.uid ?: return AuthResult.Error("Giriş yapılamadı")
            _uid.value = uid
            _authError.value = null
            AuthResult.Success(uid)
        } catch (e: Exception) {
            AuthResult.Error(friendlyAuthError(e)).also { _authError.value = it.message }
        }
    }

    /** Aile hesabından tamamen çıkar. */
    fun signOutUser() {
        try {
            firebaseAuth()?.signOut()
        } catch (e: Exception) {
            // yoksay
        }
        detachGroupListeners()
        _uid.value = null
        _membership.value = Membership.SignedOut
        _pendingJoins.value = emptyList()
        _groupPasscodeHash.value = null
        _syncState.value = SyncState.Disconnected
    }

    /**
     * Yeni aile grubu oluşturur. Kuran kişi grubun ilk üyesi olur.
     *
     * Grup kimliği tahmin edilemez olmalıdır (aile gizli anahtarı), bu yüzden
     * [SyncPreferences.newGroupId] ile üretilir.
     */
    suspend fun createFamily(newGroupId: String, passcode: String, memberName: String): Membership {
        val uid = _uid.value
            ?: return fail(Membership.SignedOut)
        if (newGroupId.isBlank()) return fail(Membership.Error("Grup kimliği oluşturulamadı"))
        if (passcode.length < 4) return fail(Membership.Error("Aile şifresi en az 4 karakter olmalı"))
        val db = firebaseDb() ?: return fail(Membership.Error("Firebase başlatılamadı"))

        groupId = newGroupId
        return try {
            _syncState.value = SyncState.Connecting
            val root = db.getReference("aile_grubu").child(newGroupId)
            val now = System.currentTimeMillis()
            val hash = sha256(passcode)
            root.child("meta").child("ownerUid").setValue(uid).await()
            root.child("meta").child("passcodeHash").setValue(hash).await()
            root.child("meta").child("createdAt").setValue(now).await()
            root.child("meta").child("members").child(uid).setValue(
                mapOf("name" to memberName, "role" to "Kurucu", "joinedAt" to now, "approvedBy" to uid)
            ).await()
            _groupPasscodeHash.value = hash
            becomeMember(root, uid)
        } catch (e: Exception) {
            fail(Membership.Error(e.message ?: "Grup oluşturulamadı"))
        }
    }

    /**
     * Mevcut gruba katılım isteği gönderir.
     *
     * Kullanıcı doğrudan üye olamaz: istek `joins/{uid}` altına yazılır ve
     * **yalnızca mevcut bir aile bireyi onaylayabilir** (kurallar bunu zorunlu kılar).
     */
    suspend fun requestJoin(passcode: String, memberName: String): Membership {
        val uid = _uid.value ?: return fail(Membership.SignedOut)
        val gid = groupId ?: return fail(Membership.Error("Grup ID gerekli"))
        if (passcode.length < 4) return fail(Membership.Error("Aile şifresi en az 4 karakter olmalı"))
        val db = firebaseDb() ?: return fail(Membership.Error("Firebase başlatılamadı"))
        return try {
            val root = db.getReference("aile_grubu").child(gid)
            root.child("joins").child(uid).setValue(
                mapOf(
                    "uid" to uid,
                    "name" to memberName,
                    "passcodeProof" to sha256(passcode),
                    "status" to "pending",
                    "requestedAt" to System.currentTimeMillis()
                )
            ).await()
            attachMembershipListener(root)
            Membership.Pending(uid).also { _membership.value = it }
        } catch (e: Exception) {
            fail(Membership.Error(e.message ?: "Katılım isteği gönderilemedi"))
        }
    }

    /** Üyeliği sunucudan yeniden kontrol eder; üye ise senkronu başlatır. */
    suspend fun refreshMembership(): Membership {
        val uid = _uid.value ?: return fail(Membership.SignedOut)
        val gid = groupId
        if (gid.isNullOrBlank()) return fail(Membership.SignedOut)
        val db = firebaseDb() ?: return fail(Membership.Error("Firebase başlatılamadı"))
        return try {
            val root = db.getReference("aile_grubu").child(gid)
            val memberSnap = root.child("meta").child("members").child(uid).get().await()
            if (memberSnap.exists()) {
                becomeMember(root, uid)
            } else {
                val status = try {
                    root.child("joins").child(uid).child("status").get().await().getValue(String::class.java)
                } catch (e: Exception) {
                    null
                }
                when (status) {
                    "approved" -> becomeMember(root, uid)
                    "rejected" -> Membership.Rejected(uid).also { _membership.value = it; attachMembershipListener(root) }
                    "pending" -> Membership.Pending(uid).also { _membership.value = it; attachMembershipListener(root) }
                    else -> Membership.NotMember(uid).also { _membership.value = it; attachMembershipListener(root) }
                }
            }
        } catch (e: Exception) {
            fail(Membership.Error(e.message ?: "Üyelik kontrol edilemedi"))
        }
    }

    /** Katılım isteğini onaylar (yalnızca mevcut üye yapabilir). */
    suspend fun approveJoin(newUid: String, name: String, role: String = "Üye"): Boolean {
        val me = _uid.value ?: return false
        val gid = groupId ?: return false
        val db = firebaseDb() ?: return false
        return try {
            val root = db.getReference("aile_grubu").child(gid)
            root.child("meta").child("members").child(newUid).setValue(
                mapOf(
                    "name" to name.ifBlank { "Üye" },
                    "role" to role,
                    "joinedAt" to System.currentTimeMillis(),
                    "approvedBy" to me
                )
            ).await()
            root.child("joins").child(newUid).child("status").setValue("approved").await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Katılım isteğini reddeder. */
    suspend fun rejectJoin(candidateUid: String): Boolean {
        val gid = groupId ?: return false
        val db = firebaseDb() ?: return false
        return try {
            db.getReference("aile_grubu").child(gid)
                .child("joins").child(candidateUid).child("status").setValue("rejected").await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Üyeyi gruptan çıkarır (erişimi anında keser). */
    suspend fun removeMember(uid: String): Boolean {
        val gid = groupId ?: return false
        val db = firebaseDb() ?: return false
        return try {
            db.getReference("aile_grubu").child(gid)
                .child("meta").child("members").child(uid).removeValue().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Grubun şifre özetiyle karşılaştırarak isteğin doğru şifreyle geldiğini doğrular. */
    fun passcodeProofMatches(proof: String): Boolean {
        val expected = _groupPasscodeHash.value ?: return false
        return expected == proof
    }

    fun isMember(): Boolean = _membership.value is Membership.Member

    private fun fail(state: Membership): Membership {
        _membership.value = state
        if (state is Membership.Error) _syncState.value = SyncState.Error(state.message)
        return state
    }

    private suspend fun becomeMember(root: DatabaseReference, uid: String): Membership {
        familyRef = root
        _membership.value = Membership.Member(uid)
        attachConnectionListener()
        _groupPasscodeHash.value = try {
            root.child("meta").child("passcodeHash").get().await().getValue(String::class.java)
        } catch (e: Exception) {
            _groupPasscodeHash.value
        }
        attachMembershipListener(root)
        attachJoinsListener(root)
        startListening()
        _syncState.value = SyncState.Connected
        return _membership.value
    }

    private fun attachConnectionListener() {
        detachConnectionListener()
        val ref = firebaseDb()?.getReference(".info/connected") ?: return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                _syncState.value = if (connected) SyncState.Connected else SyncState.Disconnected
            }

            override fun onCancelled(error: DatabaseError) {
                _syncState.value = SyncState.Error(error.message)
            }
        }
        ref.addValueEventListener(listener)
        connectionRef = ref
        connectionListener = listener
    }

    private fun detachConnectionListener() {
        connectionListener?.let { connectionRef?.removeEventListener(it) }
        connectionRef = null
        connectionListener = null
    }

    /**
     * Kendi üyelik durumunu izler: bir aile bireyi isteği onayladığında
     * senkron otomatik olarak başlar.
     */
    private fun attachMembershipListener(root: DatabaseReference) {
        detachMembershipListener()
        val uid = _uid.value ?: return
        val ref = root.child("joins").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                when (snapshot.child("status").getValue(String::class.java)) {
                    "approved" -> GlobalScope.launch(Dispatchers.IO) { refreshMembership() }
                    "rejected" -> _membership.value = Membership.Rejected(uid)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Üye değilken okuma reddedilebilir; yoksay
            }
        }
        ref.addValueEventListener(listener)
        membershipRef = ref
        membershipListener = listener
    }

    private fun detachMembershipListener() {
        membershipListener?.let { membershipRef?.removeEventListener(it) }
        membershipRef = null
        membershipListener = null
    }

    /** Üyelere onay bekleyen katılım isteklerini bildirir. */
    private fun attachJoinsListener(root: DatabaseReference) {
        detachJoinsListener()
        val ref = root.child("joins")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = mutableListOf<JoinRequest>()
                for (child in snapshot.children) {
                    val candidateUid = child.key ?: continue
                    val status = child.child("status").getValue(String::class.java) ?: "pending"
                    if (status != "pending") continue
                    requests.add(
                        JoinRequest(
                            uid = candidateUid,
                            name = child.child("name").getValue(String::class.java) ?: "",
                            passcodeProof = child.child("passcodeProof").getValue(String::class.java) ?: "",
                            requestedAt = child.child("requestedAt").getValue(Long::class.java) ?: 0L
                        )
                    )
                }
                _pendingJoins.value = requests
            }

            override fun onCancelled(error: DatabaseError) {
                _pendingJoins.value = emptyList()
            }
        }
        ref.addValueEventListener(listener)
        joinsRef = ref
        joinsListener = listener
    }

    private fun detachJoinsListener() {
        joinsListener?.let { joinsRef?.removeEventListener(it) }
        joinsRef = null
        joinsListener = null
    }

    private fun detachGroupListeners() {
        stopListening()
        detachTombstoneListener()
        detachMembershipListener()
        detachJoinsListener()
        detachConnectionListener()
        familyRef = null
    }

    private fun friendlyAuthError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("password is invalid", true) || msg.contains("credential is incorrect", true) ->
                "E-posta veya şifre hatalı"
            msg.contains("no user record", true) -> "Bu e-posta ile kayıtlı hesap bulunamadı"
            msg.contains("already in use", true) -> "Bu e-posta zaten kayıtlı"
            msg.contains("network", true) -> "İnternet bağlantısı yok"
            msg.contains("badly formatted", true) -> "Geçersiz e-posta adresi"
            msg.contains("at least 6 characters", true) -> "Şifre en az 6 karakter olmalı"
            msg.contains("too many", true) -> "Çok fazla deneme. Lütfen biraz bekleyin"
            else -> msg.ifBlank { "Kimlik doğrulama hatası" }
        }
    }

    private fun sha256(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun startListening() {
        stopListening()
        for (tableName in SYNCED_TABLES) {
            val ref = familyRef?.child(tableName) ?: continue
            val listener = ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _syncState.value = SyncState.Syncing(tableName)
                    // Uzaktan gelen veri tekrar gönderilmesin (echo koruması)
                    autoSync?.ignoreNextChange(tableName)
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            applyRemoteTable(tableName, snapshot)
                            _lastSyncTime.value = System.currentTimeMillis()
                            val current = _syncedTables.value.toMutableSet()
                            current.add(tableName)
                            _syncedTables.value = current
                            _syncState.value = SyncState.Connected
                        } catch (e: Exception) {
                            _syncState.value = SyncState.Error(e.message ?: "Senkron hatası")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _syncState.value = SyncState.Error(error.message)
                }
            })
            listenerCleanups.add { ref.removeEventListener(listener) }
        }
        attachTombstoneListener()
    }

    /**
     * Uzak tabloyu uygular. Her kayıt için **son değiştiren kazanır** (`syncVersion`)
     * kuralı işletilir:
     *  - yerel kayıt daha yeniyse uzak veri **yazılmaz** ve yerel veri geri gönderilir,
     *  - uzak kayıt daha yeniyse yerel kayıt güncellenir,
     *  - sürümler eşitse içerik karşılaştırılır (deterministik, tüm cihazlar aynı kararı verir).
     */
    private suspend fun applyRemoteTable(table: String, snapshot: DataSnapshot) {
        val local = localRecords(table)

        for (child in snapshot.children) {
            val data = child.value as? Map<*, *> ?: continue
            @Suppress("UNCHECKED_CAST")
            val remoteMap = data as Map<String, Any>
            val id = remoteMap["id"] as? String ?: continue

            val localRecord = local[id]
            if (localRecord != null) {
                val remoteVersion = (remoteMap["syncVersion"] as? Number)?.toLong() ?: 0L
                val winner = ConflictResolver.resolveUpdate(
                    localVersion = localRecord.version,
                    remoteVersion = remoteVersion,
                    localContent = { gson.toJson(localRecord.map) },
                    remoteContent = { gson.toJson(remoteMap) }
                )
                when (winner) {
                    ConflictWinner.LOCAL -> {
                        // Yerel değişiklik kazandı: uzak veriyi ez, yereli geri gönder
                        _conflictsSolved.value += 1
                        push(table, id, localRecord.map)
                        continue
                    }
                    ConflictWinner.EQUAL -> continue
                    ConflictWinner.REMOTE -> Unit
                }
            }
            writeLocal(table, remoteMap)
        }
    }

    /** Uzak kaydı yerel veritabanına yazar. */
    private suspend fun writeLocal(table: String, map: Map<String, Any>) {
        when (table) {
            "members" -> parseMember(map)?.let { db.memberDao().upsert(it) }
            "tasks" -> parseTask(map)?.let { db.taskDao().upsert(it) }
            "shopping" -> parseShopping(map)?.let { db.shoppingDao().upsert(it) }
            "messages" -> parseMessage(map)?.let { db.messageDao().upsert(it) }
            "invoices" -> parseInvoice(map)?.let { db.invoiceDao().upsert(it) }
            "budgets" -> parseBudget(map)?.let { db.budgetDao().upsert(it) }
            "expenses" -> parseExpense(map)?.let { db.expenseDao().upsert(it) }
            "inventory" -> parseInventory(map)?.let { db.inventoryDao().upsert(it) }
            "meal_plans" -> parseMealPlan(map)?.let { db.mealPlanDao().upsert(it) }
            "sports_clubs" -> parseSportsClub(map)?.let { db.sportsClubDao().upsert(it) }
            "workout_logs" -> parseWorkout(map)?.let { db.workoutLogDao().upsert(it) }
            "calorie_logs" -> parseCalorie(map)?.let { db.calorieLogDao().upsert(it) }
            "menstrual_cycles" -> parseMenstrualCycle(map)?.let { db.menstrualCycleDao().upsert(it) }
            "notes" -> parseNote(map)?.let { db.noteDao().upsert(it) }
            "reminders" -> parseReminder(map)?.let { db.reminderDao().upsert(it) }
            "water_logs" -> parseWaterLog(map)?.let { db.waterLogDao().upsert(it) }
            "sleep_logs" -> parseSleepLog(map)?.let { db.sleepLogDao().upsert(it) }
        }
    }

    /** Yerel kaydı siler (mezar taşı uygulanırken kullanılır). */
    private suspend fun deleteLocal(table: String, id: String) {
        when (table) {
            "members" -> db.memberDao().deleteById(id)
            "tasks" -> db.taskDao().deleteById(id)
            "shopping" -> db.shoppingDao().deleteById(id)
            "messages" -> db.messageDao().deleteById(id)
            "invoices" -> db.invoiceDao().deleteById(id)
            "budgets" -> db.budgetDao().deleteById(id)
            "expenses" -> db.expenseDao().deleteById(id)
            "inventory" -> db.inventoryDao().deleteById(id)
            "meal_plans" -> db.mealPlanDao().deleteById(id)
            "sports_clubs" -> db.sportsClubDao().deleteById(id)
            "workout_logs" -> db.workoutLogDao().deleteById(id)
            "calorie_logs" -> db.calorieLogDao().deleteById(id)
            "menstrual_cycles" -> db.menstrualCycleDao().deleteById(id)
            "notes" -> db.noteDao().deleteById(id)
            "reminders" -> db.reminderDao().deleteById(id)
            "water_logs" -> db.waterLogDao().deleteById(id)
            "sleep_logs" -> db.sleepLogDao().deleteById(id)
        }
    }

    /** Yerel kayıtlar: id -> (syncVersion, gönderilebilir içerik). */
    private suspend fun localRecords(table: String): Map<String, LocalRecord> = when (table) {
        "members" -> db.memberDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, memberToMap(it)) }
        "tasks" -> db.taskDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, taskToMap(it)) }
        "shopping" -> db.shoppingDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, shoppingToMap(it)) }
        "messages" -> db.messageDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, messageToMap(it)) }
        "invoices" -> db.invoiceDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, invoiceToMap(it)) }
        "budgets" -> db.budgetDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, budgetToMap(it)) }
        "expenses" -> db.expenseDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, expenseToMap(it)) }
        "inventory" -> db.inventoryDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, inventoryToMap(it)) }
        "meal_plans" -> db.mealPlanDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, mealPlanToMap(it)) }
        "sports_clubs" -> db.sportsClubDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, sportsClubToMap(it)) }
        "workout_logs" -> db.workoutLogDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, workoutToMap(it)) }
        "calorie_logs" -> db.calorieLogDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, calorieToMap(it)) }
        "menstrual_cycles" -> db.menstrualCycleDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, menstrualCycleToMap(it)) }
        "notes" -> db.noteDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, noteToMap(it)) }
        "reminders" -> db.reminderDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, reminderToMap(it)) }
        "water_logs" -> db.waterLogDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, waterLogToMap(it)) }
        "sleep_logs" -> db.sleepLogDao().getAllOnce().associate { it.id to LocalRecord(it.syncVersion, sleepLogToMap(it)) }
        else -> emptyMap()
    }

    // ==================== SİLME YAYILIMI (MEZAR TAŞI) ====================

    /**
     * Silmeleri diğer cihazlara taşır. Bir kayıt silindiğinde veri düğümü
     * kaldırılırsa diğer cihazlar bunu göremez; bu yüzden `meta/tombstones`
     * altında `deletedAt` zamanı bırakılır.
     */
    private fun attachTombstoneListener() {
        detachTombstoneListener()
        val ref = familyRef?.child("meta")?.child("tombstones") ?: return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        applyTombstones(snapshot)
                    } catch (e: Exception) {
                        // yoksay: yerel veri korunur
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // üye değilse okuma reddedilebilir
            }
        }
        ref.addValueEventListener(listener)
        tombstoneRef = ref
        tombstoneListener = listener
    }

    private fun detachTombstoneListener() {
        tombstoneListener?.let { tombstoneRef?.removeEventListener(it) }
        tombstoneRef = null
        tombstoneListener = null
    }

    private suspend fun applyTombstones(snapshot: DataSnapshot) {
        for (tableChild in snapshot.children) {
            val table = tableChild.key ?: continue
            if (table !in SYNCED_TABLES) continue
            val local = localRecords(table)
            for (idChild in tableChild.children) {
                val id = idChild.key ?: continue
                val deletedAt = idChild.child("deletedAt").getValue(Long::class.java) ?: 0L
                val localRecord = local[id] ?: continue // yerelde zaten yok

                // Silmeden sonra düzenlenmişse kayıt geri gelir (son değiştiren kazanır)
                val winner = ConflictResolver.resolveDeletion(localRecord.version, deletedAt)
                autoSync?.ignoreNextChange(table)
                if (winner == ConflictWinner.LOCAL) {
                    _conflictsSolved.value += 1
                    push(table, id, localRecord.map)
                } else {
                    deleteLocal(table, id)
                }
            }
        }
    }

    /** 30 günden eski mezar taşlarını temizler (sınırsız büyümeyi önler). */
    private suspend fun pruneTombstones() {
        val ref = familyRef?.child("meta")?.child("tombstones") ?: return
        val cutoff = System.currentTimeMillis() - TOMBSTONE_TTL_MS
        try {
            val snapshot = ref.get().await()
            for (tableChild in snapshot.children) {
                for (idChild in tableChild.children) {
                    val deletedAt = idChild.child("deletedAt").getValue(Long::class.java) ?: continue
                    if (deletedAt < cutoff) idChild.ref.removeValue()
                }
            }
        } catch (e: Exception) {
            // temizlik başarısız olursa senkron etkilenmez
        }
    }

    override suspend fun push(table: String, id: String, data: Map<String, Any>) {
        familyRef?.child(table)?.child(id)?.setValue(data)
    }

    suspend fun delete(table: String, id: String) {
        familyRef?.child(table)?.child(id)?.removeValue()
    }

    /** Kaydı siler ve diğer cihazların da silmesi için mezar taşı bırakır. */
    override suspend fun deleteWithTombstone(table: String, id: String) {
        try {
            familyRef?.child("meta")?.child("tombstones")?.child(table)?.child(id)?.setValue(
                mapOf(
                    "deletedAt" to System.currentTimeMillis(),
                    "by" to (_uid.value ?: "")
                )
            )
        } catch (e: Exception) {
            // mezar taşı yazılamazsa kayıt yine de silinir
        }
        delete(table, id)
    }

    suspend fun pushAll() {
        _syncState.value = SyncState.Syncing("all")
        val allMembers = db.memberDao().getAllOnce()
        for (m in allMembers) push("members", m.id, memberToMap(m))
        val allTasks = db.taskDao().getAllOnce()
        for (t in allTasks) push("tasks", t.id, taskToMap(t))
        val allShopping = db.shoppingDao().getAllOnce()
        for (s in allShopping) push("shopping", s.id, shoppingToMap(s))
        val allMessages = db.messageDao().getAllOnce()
        for (m in allMessages) push("messages", m.id, messageToMap(m))
        val allInvoices = db.invoiceDao().getAllOnce()
        for (i in allInvoices) push("invoices", i.id, invoiceToMap(i))
        val allBudgets = db.budgetDao().getAllOnce()
        for (b in allBudgets) push("budgets", b.id, budgetToMap(b))
        val allExpenses = db.expenseDao().getAllOnce()
        for (e in allExpenses) push("expenses", e.id, expenseToMap(e))
        val allInventory = db.inventoryDao().getAllOnce()
        for (i in allInventory) push("inventory", i.id, inventoryToMap(i))
        val allMeals = db.mealPlanDao().getAllOnce()
        for (mp in allMeals) push("meal_plans", mp.id, mealPlanToMap(mp))
        val allClubs = db.sportsClubDao().getAllOnce()
        for (c in allClubs) push("sports_clubs", c.id, sportsClubToMap(c))
        val allWorkouts = db.workoutLogDao().getAllOnce()
        for (w in allWorkouts) push("workout_logs", w.id, workoutToMap(w))
        val allCalories = db.calorieLogDao().getAllOnce()
        for (c in allCalories) push("calorie_logs", c.id, calorieToMap(c))
        val allCycles = db.menstrualCycleDao().getAllOnce()
        for (c in allCycles) push("menstrual_cycles", c.id, menstrualCycleToMap(c))
        val allNotes = db.noteDao().getAllOnce()
        for (n in allNotes) push("notes", n.id, noteToMap(n))
        val allReminders = db.reminderDao().getAllOnce()
        for (r in allReminders) push("reminders", r.id, reminderToMap(r))
        val allWater = db.waterLogDao().getAllOnce()
        for (w in allWater) push("water_logs", w.id, waterLogToMap(w))
        val allSleep = db.sleepLogDao().getAllOnce()
        for (s in allSleep) push("sleep_logs", s.id, sleepLogToMap(s))
        familyRef?.child("last_sync")?.setValue(System.currentTimeMillis())
        pruneTombstones()
        _lastSyncTime.value = System.currentTimeMillis()
        _syncState.value = SyncState.Connected
    }

    fun stopListening() {
        for (cleanup in listenerCleanups) cleanup()
        listenerCleanups.clear()
    }

    /** Senkronizasyonu durdurur. Aile hesabı açık kalır. */
    fun disconnect() {
        detachGroupListeners()
        _pendingJoins.value = emptyList()
        _syncState.value = SyncState.Disconnected
        _membership.value = _uid.value?.let { Membership.NotMember(it) } ?: Membership.SignedOut
    }

    // ========== PARSE (model from firebase map) ==========

    fun parseMember(d: Map<String, Any>): FamilyMember? {
        val id = d["id"] as? String ?: return null
        return FamilyMember(id = id, name = d["name"] as? String ?: "", role = d["role"] as? String ?: "uye",
            color = d["color"] as? String ?: "#3498DB", points = (d["points"] as? Number)?.toInt() ?: 0,
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L, syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseTask(d: Map<String, Any>): Task? {
        val id = d["id"] as? String ?: return null
        return Task(id = id, title = d["title"] as? String ?: "", description = d["description"] as? String ?: "",
            category = d["category"] as? String ?: "Genel", priority = d["priority"] as? String ?: "orta",
            assignee = d["assignee"] as? String ?: "", status = d["status"] as? String ?: "bekleyen",
            dueDate = d["dueDate"] as? String ?: "", createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            completedAt = (d["completedAt"] as? Number)?.toLong()?.let { if (it == 0L) null else it },
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseShopping(d: Map<String, Any>): ShoppingItem? {
        val id = d["id"] as? String ?: return null
        return ShoppingItem(id = id, name = d["name"] as? String ?: "",
            quantity = (d["quantity"] as? Number)?.toInt() ?: 1, category = d["category"] as? String ?: "Market",
            checked = d["checked"] as? Boolean ?: false, addedBy = d["addedBy"] as? String ?: "",
            barcode = d["barcode"] as? String ?: "", brand = d["brand"] as? String ?: "",
            description = d["description"] as? String ?: "", imageBase64 = d["imageBase64"] as? String ?: "",
            unitPrice = (d["unitPrice"] as? Number)?.toDouble() ?: 0.0,
            totalPrice = (d["totalPrice"] as? Number)?.toDouble() ?: 0.0,
            store = d["store"] as? String ?: "", notes = d["notes"] as? String ?: "",
            lastPurchaseDate = d["lastPurchaseDate"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L, syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseMessage(d: Map<String, Any>): Message? {
        val id = d["id"] as? String ?: return null
        return Message(id = id, senderName = d["senderName"] as? String ?: "", senderId = d["senderId"] as? String ?: "",
            content = d["content"] as? String ?: "", channel = d["channel"] as? String ?: "genel",
            read = d["read"] as? Boolean ?: false, attachments = d["attachments"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseInvoice(d: Map<String, Any>): Invoice? {
        val id = d["id"] as? String ?: return null
        return Invoice(id = id, title = d["title"] as? String ?: "",
            amount = (d["amount"] as? Number)?.toDouble() ?: 0.0, category = d["category"] as? String ?: "Genel",
            dueDate = d["dueDate"] as? String ?: "", notes = d["notes"] as? String ?: "",
            imageBase64 = d["imageBase64"] as? String,
            status = d["status"] as? String ?: "pending", createdBy = d["createdBy"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L, syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseBudget(d: Map<String, Any>): Budget? {
        val id = d["id"] as? String ?: return null
        return Budget(id = id, category = d["category"] as? String ?: "",
            monthlyLimit = (d["monthlyLimit"] as? Number)?.toDouble() ?: 0.0,
            spentAmount = (d["spentAmount"] as? Number)?.toDouble() ?: 0.0,
            monthYear = d["monthYear"] as? String ?: "", createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseExpense(d: Map<String, Any>): Expense? {
        val id = d["id"] as? String ?: return null
        return Expense(id = id, budgetId = d["budgetId"] as? String ?: "",
            category = d["category"] as? String ?: "",
            amount = (d["amount"] as? Number)?.toDouble() ?: 0.0,
            description = d["description"] as? String ?: "",
            expenseDate = d["expenseDate"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseInventory(d: Map<String, Any>): InventoryItem? {
        val id = d["id"] as? String ?: return null
        return InventoryItem(id = id, name = d["name"] as? String ?: "", category = d["category"] as? String ?: "Genel",
            quantity = (d["quantity"] as? Number)?.toInt() ?: 1, unit = d["unit"] as? String ?: "adet",
            minStock = (d["minStock"] as? Number)?.toInt() ?: 0, location = d["location"] as? String ?: "",
            notes = d["notes"] as? String ?: "", imageBase64 = d["imageBase64"] as? String ?: "",
            lastPrice = (d["lastPrice"] as? Number)?.toDouble() ?: 0.0,
            lastStore = d["lastStore"] as? String ?: "",
            lastPurchaseDate = d["lastPurchaseDate"] as? String ?: "",
            averagePrice = (d["averagePrice"] as? Number)?.toDouble() ?: 0.0,
            priceHistory = d["priceHistory"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseMealPlan(d: Map<String, Any>): MealPlan? {
        val id = d["id"] as? String ?: return null
        return MealPlan(id = id, dayOfWeek = (d["dayOfWeek"] as? Number)?.toInt() ?: 0,
            mealType = d["mealType"] as? String ?: "", dish = d["dish"] as? String ?: "",
            notes = d["notes"] as? String ?: "", createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseSportsClub(d: Map<String, Any>): SportsClub? {
        val id = d["id"] as? String ?: return null
        return SportsClub(id = id, name = d["name"] as? String ?: "", type = d["type"] as? String ?: "Spor Salonu",
            address = d["address"] as? String ?: "", phone = d["phone"] as? String ?: "",
            membershipStart = d["membershipStart"] as? String ?: "", membershipEnd = d["membershipEnd"] as? String ?: "",
            monthlyFee = (d["monthlyFee"] as? Number)?.toDouble() ?: 0.0, isActive = d["isActive"] as? Boolean ?: true,
            memberId = d["memberId"] as? String ?: "", notes = d["notes"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L, syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseWorkout(d: Map<String, Any>): WorkoutLog? {
        val id = d["id"] as? String ?: return null
        return WorkoutLog(id = id, clubId = d["clubId"] as? String ?: "", memberId = d["memberId"] as? String ?: "",
            workoutType = d["workoutType"] as? String ?: "", duration = (d["duration"] as? Number)?.toInt() ?: 0,
            caloriesBurned = (d["caloriesBurned"] as? Number)?.toInt() ?: 0, date = d["date"] as? String ?: "",
            notes = d["notes"] as? String ?: "", createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseCalorie(d: Map<String, Any>): CalorieLog? {
        val id = d["id"] as? String ?: return null
        return CalorieLog(id = id, memberId = d["memberId"] as? String ?: "", mealType = d["mealType"] as? String ?: "",
            foodName = d["foodName"] as? String ?: "", calories = (d["calories"] as? Number)?.toInt() ?: 0,
            protein = (d["protein"] as? Number)?.toDouble() ?: 0.0, carbs = (d["carbs"] as? Number)?.toDouble() ?: 0.0,
            fat = (d["fat"] as? Number)?.toDouble() ?: 0.0, servingSize = d["servingSize"] as? String ?: "",
            date = d["date"] as? String ?: "", createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    fun parseMenstrualCycle(d: Map<String, Any>): MenstrualCycle? {
        val id = d["id"] as? String ?: return null
        return MenstrualCycle(id = id, memberId = d["memberId"] as? String ?: "",
            startDate = d["startDate"] as? String ?: "", endDate = d["endDate"] as? String ?: "",
            cycleLength = (d["cycleLength"] as? Number)?.toInt() ?: 28, periodLength = (d["periodLength"] as? Number)?.toInt() ?: 5,
            symptoms = d["symptoms"] as? String ?: "", mood = d["mood"] as? String ?: "",
            flow = d["flow"] as? String ?: "", notes = d["notes"] as? String ?: "",
            isPersonal = d["isPersonal"] as? Boolean ?: true, createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    // ========== TO MAP ==========

    fun memberToMap(m: FamilyMember) = mapOf("id" to m.id, "name" to m.name, "role" to m.role, "color" to m.color, "points" to m.points, "createdAt" to m.createdAt, "syncVersion" to m.syncVersion)
    fun taskToMap(t: Task) = mapOf("id" to t.id, "title" to t.title, "description" to t.description, "category" to t.category, "priority" to t.priority, "assignee" to t.assignee, "status" to t.status, "dueDate" to t.dueDate, "createdAt" to t.createdAt, "completedAt" to (t.completedAt ?: 0L), "syncVersion" to t.syncVersion)
    fun shoppingToMap(s: ShoppingItem) = mapOf(
        "id" to s.id, "name" to s.name, "quantity" to s.quantity, "category" to s.category,
        "checked" to s.checked, "addedBy" to s.addedBy, "barcode" to s.barcode, "brand" to s.brand,
        "description" to s.description, "imageBase64" to s.imageBase64, "unitPrice" to s.unitPrice,
        "totalPrice" to s.totalPrice, "store" to s.store, "notes" to s.notes,
        "lastPurchaseDate" to s.lastPurchaseDate, "createdAt" to s.createdAt, "syncVersion" to s.syncVersion
    )
    fun messageToMap(m: Message) = mapOf("id" to m.id, "senderName" to m.senderName, "senderId" to m.senderId, "content" to m.content, "channel" to m.channel, "read" to m.read, "attachments" to m.attachments, "createdAt" to m.createdAt, "syncVersion" to m.syncVersion)
    fun invoiceToMap(i: Invoice): Map<String, Any> = buildMap {
        put("id", i.id); put("title", i.title); put("amount", i.amount); put("category", i.category)
        put("dueDate", i.dueDate); put("notes", i.notes); put("status", i.status)
        put("createdBy", i.createdBy); put("createdAt", i.createdAt); put("syncVersion", i.syncVersion)
        // null bırakılırsa anahtar hiç yazılmaz; okurken de null olur (kayıpsız tur)
        i.imageBase64?.let { put("imageBase64", it) }
    }
    fun budgetToMap(b: Budget) = mapOf("id" to b.id, "category" to b.category, "monthlyLimit" to b.monthlyLimit, "spentAmount" to b.spentAmount, "monthYear" to b.monthYear, "createdAt" to b.createdAt, "syncVersion" to b.syncVersion)
    fun expenseToMap(e: Expense) = mapOf("id" to e.id, "budgetId" to e.budgetId, "category" to e.category, "amount" to e.amount, "description" to e.description, "expenseDate" to e.expenseDate, "createdAt" to e.createdAt, "syncVersion" to e.syncVersion)
    fun inventoryToMap(i: InventoryItem) = mapOf(
        "id" to i.id, "name" to i.name, "category" to i.category, "quantity" to i.quantity,
        "unit" to i.unit, "minStock" to i.minStock, "location" to i.location, "notes" to i.notes,
        "imageBase64" to i.imageBase64, "lastPrice" to i.lastPrice, "lastStore" to i.lastStore,
        "lastPurchaseDate" to i.lastPurchaseDate, "averagePrice" to i.averagePrice,
        "priceHistory" to i.priceHistory, "createdAt" to i.createdAt, "syncVersion" to i.syncVersion
    )
    fun mealPlanToMap(mp: MealPlan) = mapOf("id" to mp.id, "dayOfWeek" to mp.dayOfWeek, "mealType" to mp.mealType, "dish" to mp.dish, "notes" to mp.notes, "createdAt" to mp.createdAt, "syncVersion" to mp.syncVersion)
    fun sportsClubToMap(c: SportsClub) = mapOf("id" to c.id, "name" to c.name, "type" to c.type, "address" to c.address, "phone" to c.phone, "membershipStart" to c.membershipStart, "membershipEnd" to c.membershipEnd, "monthlyFee" to c.monthlyFee, "isActive" to c.isActive, "memberId" to c.memberId, "notes" to c.notes, "createdAt" to c.createdAt, "syncVersion" to c.syncVersion)
    fun workoutToMap(w: WorkoutLog) = mapOf("id" to w.id, "clubId" to w.clubId, "memberId" to w.memberId, "workoutType" to w.workoutType, "duration" to w.duration, "caloriesBurned" to w.caloriesBurned, "date" to w.date, "notes" to w.notes, "createdAt" to w.createdAt, "syncVersion" to w.syncVersion)
    fun calorieToMap(c: CalorieLog) = mapOf("id" to c.id, "memberId" to c.memberId, "mealType" to c.mealType, "foodName" to c.foodName, "calories" to c.calories, "protein" to c.protein, "carbs" to c.carbs, "fat" to c.fat, "servingSize" to c.servingSize, "date" to c.date, "createdAt" to c.createdAt, "syncVersion" to c.syncVersion)
    fun menstrualCycleToMap(c: MenstrualCycle) = mapOf("id" to c.id, "memberId" to c.memberId, "startDate" to c.startDate, "endDate" to c.endDate, "cycleLength" to c.cycleLength, "periodLength" to c.periodLength, "symptoms" to c.symptoms, "mood" to c.mood, "flow" to c.flow, "notes" to c.notes, "isPersonal" to c.isPersonal, "createdAt" to c.createdAt, "syncVersion" to c.syncVersion)

    // ========== NOTES ==========

    fun parseNote(d: Map<String, Any>): Note? {
        val id = d["id"] as? String ?: return null
        return Note(
            id = id,
            title = d["title"] as? String ?: "",
            content = d["content"] as? String ?: "",
            category = d["category"] as? String ?: "Genel",
            color = d["color"] as? String ?: "#3498DB",
            isPinned = d["isPinned"] as? Boolean ?: false,
            isArchived = d["isArchived"] as? Boolean ?: false,
            attachments = d["attachments"] as? String ?: "",
            createdBy = d["createdBy"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L
        )
    }

    fun noteToMap(n: Note) = mapOf(
        "id" to n.id,
        "title" to n.title,
        "content" to n.content,
        "category" to n.category,
        "color" to n.color,
        "isPinned" to n.isPinned,
        "isArchived" to n.isArchived,
        "attachments" to n.attachments,
        "createdBy" to n.createdBy,
        "createdAt" to n.createdAt,
        "syncVersion" to n.syncVersion
    )

    // ========== REMINDERS ==========

    fun parseReminder(d: Map<String, Any>): Reminder? {
        val id = d["id"] as? String ?: return null
        return Reminder(
            id = id,
            title = d["title"] as? String ?: "",
            description = d["description"] as? String ?: "",
            reminderTime = (d["reminderTime"] as? Number)?.toLong() ?: 0L,
            repeatType = d["repeatType"] as? String ?: "once",
            repeatDays = d["repeatDays"] as? String ?: "",
            repeatInterval = (d["repeatInterval"] as? Number)?.toInt() ?: 1,
            repeatEndDate = (d["repeatEndDate"] as? Number)?.toLong() ?: 0L,
            category = d["category"] as? String ?: "Genel",
            priority = d["priority"] as? String ?: "orta",
            alarmSound = d["alarmSound"] as? String ?: "default",
            vibrate = d["vibrate"] as? Boolean ?: true,
            snoozeMinutes = (d["snoozeMinutes"] as? Number)?.toInt() ?: 15,
            isCompleted = d["isCompleted"] as? Boolean ?: false,
            isSnoozed = d["isSnoozed"] as? Boolean ?: false,
            snoozeUntil = (d["snoozeUntil"] as? Number)?.toLong() ?: 0L,
            lastFiredAt = (d["lastFiredAt"] as? Number)?.toLong() ?: 0L,
            nextFireAt = (d["nextFireAt"] as? Number)?.toLong() ?: 0L,
            linkedId = d["linkedId"] as? String ?: "",
            linkedType = d["linkedType"] as? String ?: "",
            createdBy = d["createdBy"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L
        )
    }

    fun reminderToMap(r: Reminder) = mapOf(
        "id" to r.id,
        "title" to r.title,
        "description" to r.description,
        "reminderTime" to r.reminderTime,
        "repeatType" to r.repeatType,
        "repeatDays" to r.repeatDays,
        "repeatInterval" to r.repeatInterval,
        "repeatEndDate" to r.repeatEndDate,
        "category" to r.category,
        "priority" to r.priority,
        "alarmSound" to r.alarmSound,
        "vibrate" to r.vibrate,
        "snoozeMinutes" to r.snoozeMinutes,
        "isCompleted" to r.isCompleted,
        "isSnoozed" to r.isSnoozed,
        "snoozeUntil" to r.snoozeUntil,
        "lastFiredAt" to r.lastFiredAt,
        "nextFireAt" to r.nextFireAt,
        "linkedId" to r.linkedId,
        "linkedType" to r.linkedType,
        "createdBy" to r.createdBy,
        "createdAt" to r.createdAt,
        "syncVersion" to r.syncVersion
    )

    // ========== WATER LOGS ==========

    fun parseWaterLog(d: Map<String, Any>): WaterLog? {
        val id = d["id"] as? String ?: return null
        return WaterLog(
            id = id,
            memberId = d["memberId"] as? String ?: "",
            amountMl = (d["amountMl"] as? Number)?.toInt() ?: 250,
            drinkType = d["drinkType"] as? String ?: "Su",
            date = d["date"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L
        )
    }

    fun waterLogToMap(w: WaterLog) = mapOf(
        "id" to w.id,
        "memberId" to w.memberId,
        "amountMl" to w.amountMl,
        "drinkType" to w.drinkType,
        "date" to w.date,
        "createdAt" to w.createdAt,
        "syncVersion" to w.syncVersion
    )

    // ========== SLEEP LOGS ==========

    fun parseSleepLog(d: Map<String, Any>): SleepLog? {
        val id = d["id"] as? String ?: return null
        return SleepLog(
            id = id,
            memberId = d["memberId"] as? String ?: "",
            bedtime = (d["bedtime"] as? Number)?.toLong() ?: 0L,
            wakeTime = (d["wakeTime"] as? Number)?.toLong() ?: 0L,
            durationMinutes = (d["durationMinutes"] as? Number)?.toInt() ?: 0,
            quality = d["quality"] as? String ?: "orta",
            interruptions = (d["interruptions"] as? Number)?.toInt() ?: 0,
            notes = d["notes"] as? String ?: "",
            date = d["date"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L
        )
    }

    fun sleepLogToMap(s: SleepLog) = mapOf(
        "id" to s.id,
        "memberId" to s.memberId,
        "bedtime" to s.bedtime,
        "wakeTime" to s.wakeTime,
        "durationMinutes" to s.durationMinutes,
        "quality" to s.quality,
        "interruptions" to s.interruptions,
        "notes" to s.notes,
        "date" to s.date,
        "createdAt" to s.createdAt,
        "syncVersion" to s.syncVersion
    )

    /** Yerel kaydın sürümü ve gönderilebilir içeriği. */
    private data class LocalRecord(val version: Long, val map: Map<String, Any>)

    private companion object {
        /** Tablo listesi tek yerde tutulur (bkz. DatabaseSyncSource). */
        val SYNCED_TABLES = DatabaseSyncSource.TABLE_NAMES

        /** Mezar taşları bu süreden sonra temizlenir. */
        const val TOMBSTONE_TTL_MS = 30L * 24 * 60 * 60 * 1000
    }
}
