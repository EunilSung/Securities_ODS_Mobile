package com.eunilsung.ods.util

import korlibs.crypto.SHA256
import korlibs.crypto.SecureRandom

/** 비밀번호 해시 — salt + SHA-256. 비밀번호 자체는 어디에도 저장하지 않는다. */
object PinHash {

    /** 랜덤 16바이트 salt(hex). 비밀번호를 등록할 때마다 새로 만든다. */
    fun newSalt(): String = SecureRandom.nextBytes(ByteArray(16)).toHex()

    /** `salt:secret` 을 SHA-256 한 hex 문자열. */
    fun hash(secret: String, saltHex: String): String =
        SHA256.digest("$saltHex:$secret".encodeToByteArray()).hex

    private fun ByteArray.toHex(): String =
        joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
