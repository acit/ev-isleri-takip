package com.aile.takip.sync

/** Paylaşılan aile davet bilgisi (grup ID + aile şifresi). */
data class FamilyInvite(
    val groupId: String,
    val passcode: String
)

/**
 * Aile davetini okunabilir metne ve QR/kod dizesine çevirir.
 *
 * Davet kodu, bir cihazdan diğerine yalnızca aile içinde paylaşılacak
 * iki bilgiyi (grup kimliği + aile şifresi) taşır.
 */
object FamilyInviteCodec {

    private const val SCHEME = "AILETAKIP"
    private const val PARAM_SEPARATOR = ";"

    /** Tek satırda, QR/okuyucu dostu davet kodu. */
    fun encode(groupId: String, passcode: String): String =
        "$SCHEME:g=$groupId${PARAM_SEPARATOR}p=$passcode"

    /** WhatsApp/mesaj ile paylaşılacak okunur davet metni. */
    fun shareText(groupId: String, passcode: String): String = buildString {
        appendLine("\uD83C\uDFE0 Aile Takip daveti")
        appendLine()
        appendLine("Aile grubumuza katılın:")
        appendLine("Grup ID: $groupId")
        appendLine("Aile şifresi: $passcode")
        appendLine()
        appendLine("Uygulama > Profil > Senkronizasyon ekranından bu bilgileri girin.")
        appendLine("(Katılımınız bir aile bireyi onayladıktan sonra senkron başlar.)")
    }

    /**
     * QR koddan, davet kodundan veya paylaşılan metinden davet bilgilerini çözer.
     *
     * Kabul edilen biçimler:
     *  - `AILETAKIP:g=<grup>;p=<şifre>`
     *  - "Grup ID: <grup>" ve "Aile şifresi: <şifre>" içeren serbest metin
     */
    fun decode(raw: String): FamilyInvite? {
        if (raw.isBlank()) return null

        val compact = Regex("$SCHEME:g=([^;\\s]+);p=(\\S+)").find(raw)
        if (compact != null) {
            val group = compact.groupValues[1]
            val pass = compact.groupValues[2]
            if (group.isNotBlank() && pass.isNotBlank()) return FamilyInvite(group, pass)
        }

        val group = Regex("Grup ID[:\\s]+(\\S+)", RegexOption.IGNORE_CASE)
            .find(raw)?.groupValues?.get(1)
        val pass = Regex("şifresi[:\\s]+(\\S+)", RegexOption.IGNORE_CASE)
            .find(raw)?.groupValues?.get(1)

        return if (!group.isNullOrBlank() && !pass.isNullOrBlank()) {
            FamilyInvite(group, pass)
        } else {
            null
        }
    }
}
