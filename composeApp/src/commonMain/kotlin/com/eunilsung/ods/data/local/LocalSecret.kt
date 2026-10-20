package com.eunilsung.ods.data.local

import kotlin.io.encoding.Base64

/**
 * 기기 DB·설정에 넣는 개인정보 잠그기 — 넣을 때 잠그고 꺼낼 때 푼다.
 *
 * 태블릿을 잃어버리거나 앱 데이터가 통째로 빠져나가도 고객 이름·연락처·주소·상담 내용·계좌번호가
 * 그대로 읽히지 않게 한다. 키는 플랫폼 보안 저장소(Android Keystore · iOS Keychain)에만 둔다([LocalSecretCipher]).
 * 시세·상품처럼 공개된 값과 코드 값(등급·성향·상태)은 잠그지 않는다.
 *
 * ### 저장 형식
 *
 * `enc1:` + Base64(암호문). 빈 문자열은 그대로 둔다(빈 메모까지 잠글 이유가 없다).
 * 접두어가 없는 값은 잠그기 전에 저장된 평문이라 그대로 돌려준다 — 설정에 남은 예전 값이 그렇다([SecureSettings]).
 *
 * ### 실패했을 때
 *
 * - 잠그지 못하면 **던진다.** 서버에 원본이 없는 로컬 모드라 풀리지 않는 값을 저장하면 그 데이터를 잃는다.
 *   평문으로 두면 잠그는 의미가 없다. 저장이 실패한 것으로 보는 편이 낫다.
 * - 풀지 못하면 [decryptOrNull] 이 null 을 돌려준다. 키를 잃은 경우는 앱 시작 때 [LocalKeyGuard] 가 먼저 가려 낸다.
 */
class LocalSecret(private val cipher: LocalSecretCipher) {

    fun isSealed(stored: String): Boolean = stored.startsWith(PREFIX)

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return plain
        val sealed = cipher.seal(plain.encodeToByteArray()) ?: throw LocalSecretException()
        return PREFIX + Base64.encode(sealed)
    }

    /** 풀지 못하면 null. 잠기지 않은 값은 그대로 돌려준다. */
    fun decryptOrNull(stored: String): String? {
        if (!isSealed(stored)) return stored
        val payload = runCatching { Base64.decode(stored.substring(PREFIX.length)) }.getOrNull() ?: return null
        return cipher.open(payload)?.decodeToString()
    }

    /** 풀지 못하면 빈 문자열 — 화면이 비어 있어도 도는 자리(이름·메모)에 쓴다. */
    fun decrypt(stored: String): String = decryptOrNull(stored) ?: ""

    /** 키를 버린다. 이 키로 잠근 값은 DB 파일에 조각이 남아 있어도 더는 풀 수 없다. 다음 [encrypt] 가 새 키를 만든다. */
    fun destroyKey() = cipher.destroyKey()

    private companion object {
        const val PREFIX = "enc1:"
    }
}

/** 값을 잠그지 못했다. 플랫폼 보안 저장소를 쓸 수 없을 때 난다. */
class LocalSecretException : IllegalStateException("local secret is unavailable")

/** 플랫폼 암호 — 키는 보안 저장소에만 두고 밖으로 꺼내지 않는다. 실패하면 던지지 않고 null 을 돌려준다. */
interface LocalSecretCipher {
    fun seal(plain: ByteArray): ByteArray?
    fun open(payload: ByteArray): ByteArray?

    /** 키를 지운다. 다음 [seal] 이 새 키를 만든다. */
    fun destroyKey()
}
