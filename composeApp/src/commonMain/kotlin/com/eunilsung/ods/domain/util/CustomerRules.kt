package com.eunilsung.ods.domain.util

import com.eunilsung.ods.domain.model.Customer
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** 업무 날짜는 한국 시간으로 따진다. */
val SeoulZone: TimeZone = TimeZone.of("Asia/Seoul")

fun todayInSeoul(nowMillis: Long = Clock.System.now().toEpochMilliseconds()): LocalDate =
    Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(SeoulZone).date

/** 만 나이. */
fun Customer.ageOn(today: LocalDate): Int {
    var age = today.year - birthDate.year
    if (today.monthNumber < birthDate.monthNumber ||
        (today.monthNumber == birthDate.monthNumber && today.dayOfMonth < birthDate.dayOfMonth)
    ) age--
    return age
}

/**
 * 투자성향 유효기간. 진단한 날부터 [VALID_YEARS] 년이 지나면 다시 진단해야 상품을 권유할 수 있다.
 */
object RiskProfilePolicy {
    const val VALID_YEARS = 1
}

/** 투자성향이 지금 유효한지 — 진단한 적이 없거나 1년이 지났으면 유효하지 않다. */
fun Customer.hasValidRiskProfile(today: LocalDate): Boolean {
    val assessedAt = riskAssessedAtMillis ?: return false
    if (riskProfile == null) return false
    val expiresOn = todayInSeoul(assessedAt).plus(DatePeriod(years = RiskProfilePolicy.VALID_YEARS))
    return today < expiresOn
}

/**
 * 고객 찾기 — 이름 일부, 한글 초성("ㄱㅁㅈ"), 전화번호 숫자 일부(끝 네 자리 등)로 찾는다. 빈 검색어는 모두 맞는다.
 */
fun Customer.matchesQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    if (name.contains(q, ignoreCase = true)) return true
    if (q.all { it in CHOSUNG }) return name.chosung().contains(q)
    val digits = q.filter(Char::isDigit)
    return digits.length >= 2 && digits.length == q.count { !it.isWhitespace() && it != '-' } &&
        phone.filter(Char::isDigit).contains(digits)
}

private val CHOSUNG = listOf('ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ')

/** 한글 음절은 초성으로, 나머지 글자는 그대로 둔다. */
internal fun String.chosung(): String = map { char ->
    if (char in '가'..'힣') CHOSUNG[(char - '가') / (21 * 28)] else char
}.joinToString("")

/** 휴대폰 번호 가리기 — 가운데 자리를 `*` 로. 예) 010-1234-5678 → 010-****-5678 */
fun maskPhone(phone: String): String {
    val parts = phone.split("-")
    if (parts.size != 3) return phone.mapIndexed { i, c -> if (c.isDigit() && i in 3 until phone.length - 4) '*' else c }.joinToString("")
    return "${parts[0]}-${"*".repeat(parts[1].length)}-${parts[2]}"
}

/** 주소 가리기 — 앞의 시·구 두 마디만 남긴다. 예) 서울 강남구 테헤란로 152 → 서울 강남구 *** */
fun maskAddress(address: String): String {
    val words = address.trim().split(" ").filter { it.isNotBlank() }
    return if (words.size <= 2) address else words.take(2).joinToString(" ") + " ***"
}

/** 숫자만 받은 휴대폰 번호를 하이픈 넣은 모양으로. 10·11자리가 아니면 null. */
fun formatPhone(digits: String): String? = when {
    !digits.all(Char::isDigit) -> null
    digits.length == 11 -> "${digits.take(3)}-${digits.substring(3, 7)}-${digits.takeLast(4)}"
    digits.length == 10 -> "${digits.take(3)}-${digits.substring(3, 6)}-${digits.takeLast(4)}"
    else -> null
}

/** "19850314" 를 날짜로. 형식이 틀리거나 없는 날이면 null. */
fun parseBirthDate(digits: String): LocalDate? {
    if (digits.length != 8 || !digits.all(Char::isDigit)) return null
    return runCatching { LocalDate(digits.take(4).toInt(), digits.substring(4, 6).toInt(), digits.takeLast(2).toInt()) }.getOrNull()
}
