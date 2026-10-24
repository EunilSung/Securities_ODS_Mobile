package com.eunilsung.ods.domain.util

import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.CustomerGrade
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.RiskProfile
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomerRulesTest {
    private fun millisOf(date: LocalDate) = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, 12, 0).toInstant(SeoulZone).toEpochMilliseconds()

    private fun customer(
        name: String = "강태준",
        phone: String = "010-0001-4587",
        birth: LocalDate = LocalDate(1968, 3, 14),
        profile: RiskProfile? = RiskProfile.NEUTRAL,
        assessedOn: LocalDate? = LocalDate(2026, 1, 10),
    ) = Customer(
        id = "C1", name = name, birthDate = birth, gender = Gender.MALE, phone = phone, address = "서울 강남구 테헤란로 152",
        grade = CustomerGrade.VIP, riskProfile = profile, riskAssessedAtMillis = assessedOn?.let(::millisOf), registeredAtMillis = 0,
    )

    @Test
    fun findsByNamePartInitialsAndPhoneDigits() {
        val c = customer()
        assertTrue(c.matchesQuery(""))
        assertTrue(c.matchesQuery("태준"))
        assertTrue(c.matchesQuery("ㄱㅌㅈ"))
        assertTrue(c.matchesQuery("ㅌㅈ"))
        assertTrue(c.matchesQuery("4587"))
        assertTrue(c.matchesQuery("0001-4587"))
        assertFalse(c.matchesQuery("ㄱㅁ"))
        assertFalse(c.matchesQuery("9999"))
        assertFalse(c.matchesQuery("4"))
        assertFalse(c.matchesQuery("태준4587"))
    }

    @Test
    fun ageIsInternationalAge() {
        val c = customer(birth = LocalDate(1990, 10, 20))
        assertEquals(35, c.ageOn(LocalDate(2026, 10, 19)))
        assertEquals(36, c.ageOn(LocalDate(2026, 10, 20)))
    }

    @Test
    fun riskProfileIsValidForOneYear() {
        val c = customer(assessedOn = LocalDate(2025, 10, 20))
        assertTrue(c.hasValidRiskProfile(LocalDate(2026, 10, 19)))
        assertFalse(c.hasValidRiskProfile(LocalDate(2026, 10, 20)))
        assertFalse(customer(profile = null, assessedOn = null).hasValidRiskProfile(LocalDate(2026, 1, 1)))
    }

    @Test
    fun masksPhoneAndAddress() {
        assertEquals("010-****-4587", maskPhone("010-0001-4587"))
        assertEquals("011-***-4587", maskPhone("011-223-4587"))
        assertEquals("서울 강남구 ***", maskAddress("서울 강남구 테헤란로 152"))
        assertEquals("서울 강남구", maskAddress("서울 강남구"))
    }

    @Test
    fun parsesPhoneAndBirthDigits() {
        assertEquals("010-1234-5678", formatPhone("01012345678"))
        assertEquals("011-123-4567", formatPhone("0111234567"))
        assertNull(formatPhone("0101234"))
        assertEquals(LocalDate(1985, 3, 14), parseBirthDate("19850314"))
        assertNull(parseBirthDate("19850231"))
        assertNull(parseBirthDate("1985031"))
    }
}
