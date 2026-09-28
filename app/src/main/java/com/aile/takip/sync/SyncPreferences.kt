package com.aile.takip.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom

private val Context.syncDataStore by preferencesDataStore(name = "aile_sync_config")

/**
 * Cihazın aile grubu ayarları.
 *
 * - [groupId] / [passcode] : Aileye katılım anahtarı (yalnızca aile bireylerinde olur)
 * - [autoSync]             : Her değişiklikte otomatik senkron aktif mi
 * - [enabled]              : Senkronizasyon daha önce kuruldu mu (açılışta otomatik bağlanır)
 * - [myMemberId]           : Bu cihazı kullanan aile üyesi
 * - [myEmail]              : Giriş yapılan aile hesabı (otomatik doldurma için)
 */
data class SyncConfig(
    val groupId: String = "",
    val passcode: String = "",
    val autoSync: Boolean = true,
    val enabled: Boolean = false,
    val myMemberId: String = "",
    val myMemberName: String = "",
    val myEmail: String = ""
)

/**
 * Aile grubu yapılandırmasını cihazda kalıcı olarak saklar (DataStore).
 * Veriler cihazda kalır; yalnızca grup kimliği ve şifre burada tutulur.
 */
class SyncPreferences(private val context: Context) {

    private object Keys {
        val GROUP_ID = stringPreferencesKey("group_id")
        val PASSCODE = stringPreferencesKey("passcode")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val ENABLED = booleanPreferencesKey("enabled")
        val MY_MEMBER_ID = stringPreferencesKey("my_member_id")
        val MY_MEMBER_NAME = stringPreferencesKey("my_member_name")
        val MY_EMAIL = stringPreferencesKey("my_email")
    }

    val config: Flow<SyncConfig> = context.syncDataStore.data.map { p ->
        SyncConfig(
            groupId = p[Keys.GROUP_ID] ?: "",
            passcode = p[Keys.PASSCODE] ?: "",
            autoSync = p[Keys.AUTO_SYNC] ?: true,
            enabled = p[Keys.ENABLED] ?: false,
            myMemberId = p[Keys.MY_MEMBER_ID] ?: "",
            myMemberName = p[Keys.MY_MEMBER_NAME] ?: "",
            myEmail = p[Keys.MY_EMAIL] ?: ""
        )
    }

    suspend fun current(): SyncConfig = config.first()

    suspend fun saveGroup(groupId: String, passcode: String) {
        context.syncDataStore.edit {
            it[Keys.GROUP_ID] = groupId
            it[Keys.PASSCODE] = passcode
            it[Keys.ENABLED] = true
        }
    }

    suspend fun setAutoSync(enabled: Boolean) {
        context.syncDataStore.edit { it[Keys.AUTO_SYNC] = enabled }
    }

    suspend fun setDeviceUser(memberId: String, memberName: String) {
        context.syncDataStore.edit {
            it[Keys.MY_MEMBER_ID] = memberId
            it[Keys.MY_MEMBER_NAME] = memberName
        }
    }

    /** Senkronizasyonu kapatır ama grup bilgisini hızlı yeniden bağlanmak için saklar. */
    suspend fun setEnabled(enabled: Boolean) {
        context.syncDataStore.edit { it[Keys.ENABLED] = enabled }
    }

    /** Aile hesabı e-postasını saklar (otomatik doldurma için). */
    suspend fun setAccount(email: String) {
        context.syncDataStore.edit { it[Keys.MY_EMAIL] = email }
    }

    /** Gruptan tamamen çıkar. */
    suspend fun clearGroup() {
        context.syncDataStore.edit {
            it.remove(Keys.GROUP_ID)
            it.remove(Keys.PASSCODE)
            it[Keys.ENABLED] = false
        }
    }

    companion object {
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()

        /**
         * Tahmin edilmesi güç grup kimliği üretir.
         *
         * Grup kimliği ailenin gizli anahtarıdır: yalnızca davet ile paylaşılır ve
         * ilk kuran kişi grubun sahibi olur. Bu yüzden kısa/tahmin edilebilir
         * (örn. aile_1234) kimlikler kullanılmaz.
         */
        fun newGroupId(): String {
            val sb = StringBuilder("aile_")
            repeat(16) { sb.append(ALPHABET[random.nextInt(ALPHABET.length)]) }
            return sb.toString()
        }
    }
}
