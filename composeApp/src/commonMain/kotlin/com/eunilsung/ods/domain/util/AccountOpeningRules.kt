package com.eunilsung.ods.domain.util

import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.AccountTypeBlock
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.IdCardInfo
import com.eunilsung.ods.domain.model.IdMismatch
import com.eunilsung.ods.domain.model.Terms
import kotlinx.datetime.LocalDate

/** 계좌 개설 규칙. */
object AccountOpeningPolicy {
    /** 계좌 비밀번호 자릿수. 국내 증권 계좌처럼 숫자 4자리. */
    const val PIN_LENGTH = 4

    const val ISA_MIN_AGE = 19

    /** 같은 숫자만(1111)이거나 1씩 오르내리는(1234·4321) 비밀번호, 생년월일이 들어간 비밀번호는 받지 않는다. */
    fun isWeakPin(pin: String, birthDate: LocalDate): Boolean {
        if (pin.length != PIN_LENGTH || !pin.all(Char::isDigit)) return true
        val steps = pin.zipWithNext { a, b -> b - a }.distinct()
        if (steps.size == 1 && steps.first() in -1..1) return true
        val birth = birthDate.toString().filter(Char::isDigit)
        return birth.contains(pin)
    }

    /** 이 고객이 이 종류의 계좌를 열 수 없는 까닭. 열 수 있으면 null. */
    fun blockFor(type: AccountType, customer: Customer, accounts: List<Account>, today: LocalDate): AccountTypeBlock? = when (type) {
        AccountType.ISA -> when {
            customer.ageOn(today) < ISA_MIN_AGE -> AccountTypeBlock.ISA_AGE
            accounts.any { it.type == AccountType.ISA } -> AccountTypeBlock.ISA_DUPLICATE
            else -> null
        }
        AccountType.PENSION -> if (accounts.any { it.type == AccountType.PENSION }) AccountTypeBlock.PENSION_DUPLICATE else null
        AccountType.BROKERAGE, AccountType.CMA -> null
    }

    fun requiredTermsAgreed(agreed: Set<Terms>): Boolean = Terms.entries.filter { it.required }.all { it in agreed }
}

/** 고객 생년월일로 정해지는 주민등록번호 앞 6자리(YYMMDD). */
fun Customer.rrnFront(): String =
    "${(birthDate.year % 100).toString().padStart(2, '0')}${birthDate.monthNumber.toString().padStart(2, '0')}${birthDate.dayOfMonth.toString().padStart(2, '0')}"

/** 고객 성별·출생 세기로 정해지는 뒷자리 첫 숫자 — 1900년대 남 1·여 2, 2000년대 남 3·여 4, 1800년대 남 9·여 0. */
fun Customer.rrnBackFirst(): Char = when (birthDate.year / 100) {
    19 -> if (gender == Gender.MALE) '1' else '2'
    20 -> if (gender == Gender.MALE) '3' else '4'
    else -> if (gender == Gender.MALE) '9' else '0'
}

/** 신분증과 고객 정보를 맞춰 본다 — 이름, 생년월일(앞 6자리), 성별·세기(뒷자리 첫 숫자). */
fun IdCardInfo.mismatchesWith(customer: Customer): List<IdMismatch> {
    if (rrnFront.length != 6 || !rrnFront.all(Char::isDigit) || !rrnBackFirst.isDigit()) return listOf(IdMismatch.FORMAT)
    val result = mutableListOf<IdMismatch>()
    if (name.trim() != customer.name) result += IdMismatch.NAME
    if (rrnFront != customer.rrnFront()) result += IdMismatch.BIRTH
    if (rrnBackFirst != customer.rrnBackFirst()) result += IdMismatch.GENDER
    return result
}
