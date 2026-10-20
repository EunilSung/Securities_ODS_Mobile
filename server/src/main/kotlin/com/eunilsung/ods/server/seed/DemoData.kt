package com.eunilsung.ods.server.seed

import com.eunilsung.ods.server.auth.PasswordHasher
import com.eunilsung.ods.server.db.transaction
import com.eunilsung.ods.server.repository.CustomerRepository
import com.eunilsung.ods.server.repository.ProductRepository
import com.eunilsung.ods.server.repository.StaffRepository
import com.eunilsung.ods.server.repository.StockRepository
import com.eunilsung.ods.server.repository.VisitRepository
import com.eunilsung.ods.server.rules.BusinessRules
import com.eunilsung.ods.shared.api.AccountDto
import com.eunilsung.ods.shared.api.ConsultationDto
import com.eunilsung.ods.shared.api.CustomerDto
import com.eunilsung.ods.shared.api.ProductDto
import com.eunilsung.ods.shared.api.StockDto
import com.eunilsung.ods.shared.api.VisitDto
import java.time.LocalTime
import javax.sql.DataSource

/**
 * 시연용 데이터를 채운다 — 표가 비어 있을 때만.
 *
 * 표마다 따로 본다. 테스트가 직원만 남기고 나머지를 비우면 나머지가 다시 채워진다.
 * 시각은 채우는 순간을 기준으로 며칠 전·며칠 뒤로 정한다 — 언제 켜도 투자성향 유효기간·일정이 그럴듯하게 보이게.
 */
object DemoData {

    fun ensure(dataSource: DataSource, nowMillis: Long = System.currentTimeMillis()) {
        val staff = StaffRepository(dataSource)
        val customers = CustomerRepository(dataSource)
        val products = ProductRepository(dataSource)
        val stocks = StockRepository(dataSource)
        val visits = VisitRepository(dataSource)

        /** 해시가 일부러 느려서 매번 돌리면 기동이 그만큼 늦어진다. 한 명이라도 있으면 이미 채운 것으로 본다. */
        if (staff.count() == 0) {
            SeedAccounts.ALL.forEach { staff.insertIfAbsent(it, PasswordHasher.hash(SeedAccounts.PASSWORD)) }
        }
        if (products.count() == 0) {
            dataSource.transaction { conn ->
                ProductSeed.products.forEachIndexed { index, row -> products.insert(conn, row.toDto(), index) }
            }
        }
        if (stocks.count() == 0) {
            dataSource.transaction { conn ->
                StockSeed.watchlist.forEachIndexed { index, row -> stocks.insert(conn, row.toDto(), index) }
            }
        }
        if (customers.count() == 0) {
            dataSource.transaction { conn ->
                CustomerSeed.customers.forEach { customers.insertCustomer(conn, it.toDto(nowMillis)) }
                CustomerSeed.accounts.forEach { customers.insertAccount(conn, it.toDto(nowMillis)) }
                CustomerSeed.consultations.forEachIndexed { index, row ->
                    customers.insertConsultation(conn, row.toDto(index, nowMillis))
                }
            }
        }
        if (visits.count() == 0) {
            val today = BusinessRules.todayInSeoul(nowMillis)
            dataSource.transaction { conn ->
                VisitSeed.visits.forEach { row ->
                    val scheduledAt = today.plusDays(row.dayOffset.toLong())
                        .atTime(LocalTime.of(row.hour, row.minute))
                        .atZone(BusinessRules.SEOUL)
                        .toInstant()
                        .toEpochMilli()
                    val customerName = CustomerSeed.customers.first { it.id == row.customerId }.name
                    visits.insert(conn, row.staffId, row.toDto(customerName, scheduledAt))
                }
            }
        }
    }

    private fun ProductSeedRow.toDto() = ProductDto(
        id = id,
        name = name,
        type = type,
        riskGrade = riskGrade,
        summary = summary,
        expectedReturn = expectedReturn,
        feeInfo = feeInfo,
        minAmount = minAmount,
        keyPoints = keyPoints,
        isComplex = isComplex,
    )

    private fun StockSeedRow.toDto() = StockDto(code, name, market, price, previousClose, volume)

    private fun CustomerSeedRow.toDto(nowMillis: Long) = CustomerDto(
        id = id,
        name = name,
        birthDate = birthDate,
        gender = gender,
        phone = phone,
        address = address,
        grade = grade,
        riskProfile = riskProfile,
        riskAssessedAtMillis = riskAssessedDaysAgo?.let { nowMillis - it * DAY_MILLIS },
        registeredAtMillis = nowMillis - registeredDaysAgo * DAY_MILLIS,
    )

    private fun AccountSeedRow.toDto(nowMillis: Long) = AccountDto(
        accountNo = accountNo,
        customerId = customerId,
        type = type,
        openedAtMillis = nowMillis - openedDaysAgo * DAY_MILLIS,
    )

    /** 아이디는 앱 로컬 모드와 같은 `seed-consultation-<순번>` 이다. */
    private fun ConsultationSeedRow.toDto(index: Int, nowMillis: Long) = ConsultationDto(
        id = "seed-consultation-$index",
        customerId = customerId,
        consultedAtMillis = nowMillis - daysAgo * DAY_MILLIS,
        channel = channel,
        topic = topic,
        content = content,
        staffName = staffName,
    )

    private fun VisitSeedRow.toDto(customerName: String, scheduledAtMillis: Long) = VisitDto(
        id = id,
        customerId = customerId,
        customerName = customerName,
        scheduledAtMillis = scheduledAtMillis,
        place = place,
        purpose = purpose,
        memo = memo,
        status = if (dayOffset < 0) "COMPLETED" else VisitRepository.STATUS_SCHEDULED,
    )

    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
}
