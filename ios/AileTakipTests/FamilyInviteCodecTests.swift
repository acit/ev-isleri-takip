import XCTest
@testable import AileTakip

/// FamilyInviteCodec davet kodu çözümleme testleri.
/// QR okuyucusundan gelen ham metin, paylaşılan WhatsApp metni ve bozuk girdilerle
/// davranışı sabitler; QrScanSheet'in "Kodu Kullan" butonunun decode'a dayandığı
/// için bu testler manuel giriş akışını da kapsar.
final class FamilyInviteCodecTests: XCTestCase {

    // MARK: Kompakt biçim

    func testDecodeCompactFormat() {
        let invite = FamilyInviteCodec.decode("AILETAKIP:g=aile_XY23456789ABCD;p=sifre123")
        XCTAssertNotNil(invite)
        XCTAssertEqual(invite?.groupId, "aile_XY23456789ABCD")
        XCTAssertEqual(invite?.passcode, "sifre123")
    }

    func testEncodeProducesCompactFormat() {
        let code = FamilyInviteCodec.encode(groupId: "aile_TESTTESTTEST", passcode: "1234")
        XCTAssertEqual(code, "AILETAKIP:g=aile_TESTTESTTEST;p=1234")
        // Encode → decode gidiş-dönüşü
        let invite = FamilyInviteCodec.decode(code)
        XCTAssertEqual(invite?.groupId, "aile_TESTTESTTEST")
        XCTAssertEqual(invite?.passcode, "1234")
    }

    // MARK: Boşluk / gürültü toleransı

    func testDecodeTrimsWhitespace() {
        let invite = FamilyInviteCodec.decode("  \n AILETAKIP:g=g1;p=p1 \n ")
        XCTAssertNotNil(invite)
        XCTAssertEqual(invite?.groupId, "g1")
        XCTAssertEqual(invite?.passcode, "p1")
    }

    func testDecodeInsideSharedText() {
        // WhatsApp davet metninin ortasına gömülü kod
        let text = "Aile Takip uygulamasına katıl!\nKod: AILETAKIP:g=aile_ABCDEF12345678;p=aile1234\n(beta)"
        let invite = FamilyInviteCodec.decode(text)
        XCTAssertNotNil(invite)
        XCTAssertEqual(invite?.groupId, "aile_ABCDEF12345678")
        XCTAssertEqual(invite?.passcode, "aile1234")
    }

    func testDecodeCaseInsensitivePrefix() {
        let invite = FamilyInviteCodec.decode("ailetakip:g=grup1;p=s1")
        XCTAssertNotNil(invite)
        XCTAssertEqual(invite?.groupId, "grup1")
    }

    // MARK: Serbest metin biçimi

    func testDecodeFreeFormText() {
        let text = """
        Aile Takip'e katılmak için:
        Grup ID: aile_FREEFORM1234
        Aile şifresi: sifre99
        """
        let invite = FamilyInviteCodec.decode(text)
        XCTAssertNotNil(invite)
        XCTAssertEqual(invite?.groupId, "aile_FREEFORM1234")
        XCTAssertEqual(invite?.passcode, "sifre99")
    }

    // MARK: Geçersiz girdiler (nil dönmeli — QrScanSheet "Kodu Kullan" butonu kapalı kalmalı)

    func testRejectsEmptyString() {
        XCTAssertNil(FamilyInviteCodec.decode(""))
        XCTAssertNil(FamilyInviteCodec.decode("   "))
        XCTAssertNil(FamilyInviteCodec.decode("\n"))
    }

    func testRejectsGarbage() {
        XCTAssertNil(FamilyInviteCodec.decode("rastgele metin"))
        XCTAssertNil(FamilyInviteCodec.decode("https://ornek.com/link"))
    }

    func testRejectsIncompleteCompact() {
        XCTAssertNil(FamilyInviteCodec.decode("AILETAKIP:g=sadecegrup"))
        XCTAssertNil(FamilyInviteCodec.decode("AILETAKIP:p=sadeceşifre"))
        XCTAssertNil(FamilyInviteCodec.decode("AILETAKIP:g=;p="))
    }

    func testRejectsFreeFormMissingPasscode() {
        XCTAssertNil(FamilyInviteCodec.decode("Grup ID: aile_sadecegrup (şifre yok)"))
    }
}
