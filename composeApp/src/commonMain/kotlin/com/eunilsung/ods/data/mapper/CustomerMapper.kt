package com.eunilsung.ods.data.mapper

import com.eunilsung.ods.data.local.seed.AccountSeedRow
import com.eunilsung.ods.data.local.seed.ConsultationSeedRow
import com.eunilsung.ods.data.local.seed.CustomerSeedRow
import com.eunilsung.ods.db.AccountEntity
import com.eunilsung.ods.db.ConsultationEntity
import com.eunilsung.ods.db.CustomerEntity
import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.Consultation
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.CustomerGrade
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.RiskProfile
import kotlinx.datetime.LocalDate

/** 기기 DB 행·시연용 데이터를 고객 도메인 모델로 바꾼다. 모르는 코드 값은 가장 보수적인 값으로 읽는다. */
class CustomerMapper {
    fun toModel(entity: CustomerEntity): Customer = Customer(
        id = entity.id,
        name = entity.name,
        birthDate = LocalDate.parse(entity.birthDate),
        gender = enumOr(entity.gender, Gender.MALE),
        phone = entity.phone,
        address = entity.address,
        grade = enumOr(entity.grade, CustomerGrade.GENERAL),
        riskProfile = entity.riskProfile?.let { saved -> RiskProfile.entries.firstOrNull { it.name == saved } },
        riskAssessedAtMillis = entity.riskAssessedAt,
        registeredAtMillis = entity.registeredAt,
    )

    fun toEntity(customer: Customer): CustomerEntity = CustomerEntity(
        id = customer.id,
        name = customer.name,
        birthDate = customer.birthDate.toString(),
        gender = customer.gender.name,
        phone = customer.phone,
        address = customer.address,
        grade = customer.grade.name,
        riskProfile = customer.riskProfile?.name,
        riskAssessedAt = customer.riskAssessedAtMillis,
        registeredAt = customer.registeredAtMillis,
    )

    fun toModel(entity: ConsultationEntity): Consultation = Consultation(
        id = entity.id,
        customerId = entity.customerId,
        consultedAtMillis = entity.consultedAt,
        channel = enumOr(entity.channel, ConsultChannel.VISIT),
        topic = enumOr(entity.topic, ConsultTopic.GENERAL),
        content = entity.content,
        staffName = entity.staffName,
    )

    fun toEntity(consultation: Consultation): ConsultationEntity = ConsultationEntity(
        id = consultation.id,
        customerId = consultation.customerId,
        consultedAt = consultation.consultedAtMillis,
        channel = consultation.channel.name,
        topic = consultation.topic.name,
        content = consultation.content,
        staffName = consultation.staffName,
    )

    fun toModel(entity: AccountEntity): Account = Account(
        accountNo = entity.accountNo,
        customerId = entity.customerId,
        type = enumOr(entity.type, AccountType.BROKERAGE),
        openedAtMillis = entity.openedAt,
    )

    fun toEntity(account: Account): AccountEntity = AccountEntity(
        accountNo = account.accountNo,
        customerId = account.customerId,
        type = account.type.name,
        openedAt = account.openedAtMillis,
    )

    internal fun toEntity(row: CustomerSeedRow, nowMillis: Long): CustomerEntity = CustomerEntity(
        id = row.id,
        name = row.name,
        birthDate = row.birthDate,
        gender = row.gender,
        phone = row.phone,
        address = row.address,
        grade = row.grade,
        riskProfile = row.riskProfile,
        riskAssessedAt = row.riskAssessedDaysAgo?.let { nowMillis - it * DAY_MILLIS },
        registeredAt = nowMillis - row.registeredDaysAgo * DAY_MILLIS,
    )

    internal fun toEntity(row: AccountSeedRow, nowMillis: Long): AccountEntity = AccountEntity(
        accountNo = row.accountNo,
        customerId = row.customerId,
        type = row.type,
        openedAt = nowMillis - row.openedDaysAgo * DAY_MILLIS,
    )

    internal fun toEntity(row: ConsultationSeedRow, index: Int, nowMillis: Long): ConsultationEntity = ConsultationEntity(
        id = "seed-consultation-$index",
        customerId = row.customerId,
        consultedAt = nowMillis - row.daysAgo * DAY_MILLIS,
        channel = row.channel,
        topic = row.topic,
        content = row.content,
        staffName = row.staffName,
    )

    private inline fun <reified T : Enum<T>> enumOr(value: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
