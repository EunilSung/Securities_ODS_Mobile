package com.eunilsung.ods.domain.util

import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.AccountTypeBlock
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.CustomerGrade
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.IdCardInfo
import com.eunilsung.ods.domain.model.IdCardType
import com.eunilsung.ods.domain.model.IdMismatch
import com.eunilsung.ods.domain.model.Terms
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountOpeningRulesTest {
    private fun customer(birth: LocalDate, gender: Gender) = Customer(
        id = "C", name = "홍길동", birthDate = birth, gender = gender, phone = "010-1111-2222", address = "",
        grade = CustomerGrade.GENERAL, riskProfile = null, riskAssessedAtMillis = null, registeredAtMillis = 0,
    )

    @Test
    fun rrnDigitsFollowBirthAndCentury() {
        val man1985 = customer(LocalDate(1985, 3, 14), Gender.MALE)
        assertEquals("850314", man1985.rrnFront())
        assertEquals('1', man1985.rrnBackFirst())
        assertEquals('4', customer(LocalDate(2003, 1, 2), Gender.FEMALE).rrnBackFirst())
    }

    @Test
    fun idInfoMismatchesAreListed() {
        val c = customer(LocalDate(1985, 3, 14), Gender.MALE)
        assertTrue(IdCardInfo(IdCardType.RESIDENT, " 홍길동 ", "850314", '1').mismatchesWith(c).isEmpty())
        assertEquals(listOf(IdMismatch.NAME, IdMismatch.BIRTH, IdMismatch.GENDER), IdCardInfo(IdCardType.DRIVER, "홍길순", "850315", '2').mismatchesWith(c))
        assertEquals(listOf(IdMismatch.FORMAT), IdCardInfo(IdCardType.RESIDENT, "홍길동", "8503", '1').mismatchesWith(c))
    }

    @Test
    fun weakAccountPinsAreRejected() {
        val birth = LocalDate(1985, 3, 14)
        listOf("1111", "1234", "4321", "0314", "8503", "123").forEach { assertTrue(AccountOpeningPolicy.isWeakPin(it, birth), it) }
        listOf("2580", "9173").forEach { assertFalse(AccountOpeningPolicy.isWeakPin(it, birth), it) }
    }

    @Test
    fun isaAndPensionLimits() {
        val today = LocalDate(2026, 10, 19)
        val adult = customer(LocalDate(1990, 1, 1), Gender.MALE)
        val teen = customer(LocalDate(2010, 1, 1), Gender.MALE)
        val isa = Account("A1", "C", AccountType.ISA, 0)
        val pension = Account("A2", "C", AccountType.PENSION, 0)
        assertNull(AccountOpeningPolicy.blockFor(AccountType.ISA, adult, emptyList(), today))
        assertEquals(AccountTypeBlock.ISA_AGE, AccountOpeningPolicy.blockFor(AccountType.ISA, teen, emptyList(), today))
        assertEquals(AccountTypeBlock.ISA_DUPLICATE, AccountOpeningPolicy.blockFor(AccountType.ISA, adult, listOf(isa), today))
        assertEquals(AccountTypeBlock.PENSION_DUPLICATE, AccountOpeningPolicy.blockFor(AccountType.PENSION, adult, listOf(pension), today))
        assertNull(AccountOpeningPolicy.blockFor(AccountType.BROKERAGE, teen, listOf(isa, pension), today))
    }

    @Test
    fun requiredTermsMustAllBeAgreed() {
        assertFalse(AccountOpeningPolicy.requiredTermsAgreed(setOf(Terms.ACCOUNT, Terms.MARKETING)))
        assertTrue(AccountOpeningPolicy.requiredTermsAgreed(setOf(Terms.ACCOUNT, Terms.PERSONAL_INFO, Terms.ELECTRONIC_FINANCE)))
    }
}
