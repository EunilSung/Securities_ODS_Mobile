package com.eunilsung.ods.data.mapper

import com.eunilsung.ods.db.SignatureEntity
import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.Consultation
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.CustomerGrade
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.IdCardInfo
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.NewCustomer
import com.eunilsung.ods.domain.model.RiskProfile
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.domain.model.SignatureRecord
import com.eunilsung.ods.shared.api.AccountDto
import com.eunilsung.ods.shared.api.ConsultationDto
import com.eunilsung.ods.shared.api.CustomerDto
import com.eunilsung.ods.shared.api.IdVerificationRequest
import com.eunilsung.ods.shared.api.NewConsultationRequest
import com.eunilsung.ods.shared.api.NewCustomerRequest
import com.eunilsung.ods.shared.api.SignaturePointDto
import com.eunilsung.ods.shared.api.SignatureRecordDto
import com.eunilsung.ods.shared.api.SignatureRequest
import kotlinx.datetime.LocalDate

/**
 * 서버 고객 응답·요청을 도메인 모델과 바꾼다. 모르는 코드 값은 [CustomerMapper] 와 같은 보수적인 값으로 읽는다.
 *
 * 기기 DB 에 넣을 때 잠그는 일은 [CustomerMapper] 가 한다.
 */
class ServerCustomerMapper {
    fun toModel(dto: CustomerDto): Customer = Customer(
        id = dto.id,
        name = dto.name,
        birthDate = LocalDate.parse(dto.birthDate),
        gender = dto.gender.toEnumOr(Gender.MALE),
        phone = dto.phone,
        address = dto.address,
        grade = dto.grade.toEnumOr(CustomerGrade.GENERAL),
        riskProfile = dto.riskProfile?.let { saved -> RiskProfile.entries.firstOrNull { it.name == saved } },
        riskAssessedAtMillis = dto.riskAssessedAtMillis,
        registeredAtMillis = dto.registeredAtMillis,
    )

    fun toModel(dto: AccountDto): Account = Account(
        accountNo = dto.accountNo,
        customerId = dto.customerId,
        type = dto.type.toEnumOr(AccountType.BROKERAGE),
        openedAtMillis = dto.openedAtMillis,
    )

    fun toModel(dto: ConsultationDto): Consultation = Consultation(
        id = dto.id,
        customerId = dto.customerId,
        consultedAtMillis = dto.consultedAtMillis,
        channel = dto.channel.toEnumOr(ConsultChannel.VISIT),
        topic = dto.topic.toEnumOr(ConsultTopic.GENERAL),
        content = dto.content,
        staffName = dto.staffName,
    )

    fun toModel(dto: SignatureRecordDto): SignatureRecord = SignatureRecord(
        id = dto.id,
        customerId = dto.customerId,
        purpose = dto.purpose.toEnumOr(SignaturePurpose.RISK_PROFILE),
        signedAtMillis = dto.signedAtMillis,
        digest = dto.digest,
    )

    /** 서명 기록은 지문뿐이라 잠그지 않고 그대로 넣는다. */
    fun toEntity(record: SignatureRecord): SignatureEntity =
        SignatureEntity(record.id, record.customerId, record.purpose.name, record.signedAtMillis, record.digest)

    fun toRequest(customer: NewCustomer): NewCustomerRequest = NewCustomerRequest(
        name = customer.name.trim(),
        birthDate = customer.birthDate.toString(),
        gender = customer.gender.name,
        phone = customer.phone,
        address = customer.address.trim(),
    )

    fun toRequest(consultation: NewConsultation): NewConsultationRequest = NewConsultationRequest(
        channel = consultation.channel.name,
        topic = consultation.topic.name,
        content = consultation.content.trim(),
    )

    fun toRequest(purpose: SignaturePurpose, signature: Signature, signedAtMillis: Long): SignatureRequest = SignatureRequest(
        purpose = purpose.name,
        signedAtMillis = signedAtMillis,
        strokes = signature.strokes.map { stroke -> stroke.map { SignaturePointDto(it.x, it.y) } },
    )

    /** 주민등록번호는 앞 6자리와 뒷자리 첫 숫자만 싣는다. 사진은 보내지 않는다. */
    fun toRequest(info: IdCardInfo): IdVerificationRequest = IdVerificationRequest(
        type = info.type.name,
        name = info.name.trim(),
        rrnFront = info.rrnFront,
        rrnBackFirst = info.rrnBackFirst.toString(),
    )
}

/** 서버가 실어 보낸 도메인 열거형 이름을 읽는다. 모르는 값이면 [fallback]. */
internal inline fun <reified T : Enum<T>> String.toEnumOr(fallback: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: fallback
