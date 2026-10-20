package com.eunilsung.ods.server.repository

import com.eunilsung.ods.server.ApiException
import com.eunilsung.ods.server.ServerJson
import com.eunilsung.ods.server.db.millis
import com.eunilsung.ods.server.db.query
import com.eunilsung.ods.server.db.toTimestamp
import com.eunilsung.ods.server.db.transaction
import com.eunilsung.ods.server.db.update
import com.eunilsung.ods.server.rules.BusinessRules
import com.eunilsung.ods.shared.api.AccountDto
import com.eunilsung.ods.shared.api.ApiErrorCode
import com.eunilsung.ods.shared.api.ConsultationDto
import com.eunilsung.ods.shared.api.CustomerBookResponse
import com.eunilsung.ods.shared.api.CustomerDto
import com.eunilsung.ods.shared.api.SignaturePointDto
import com.eunilsung.ods.shared.api.SignatureRecordDto
import kotlinx.serialization.builtins.ListSerializer
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

/**
 * 지점 고객 장부 — 고객과 그 고객에 딸린 계좌·상담·서명.
 *
 * 고객은 직원 모두가 함께 본다. 담당 직원으로 나누지 않는다(지점 하나의 장부).
 */
class CustomerRepository(private val dataSource: DataSource) {

    /** 장부 전부. 고객은 이름 순, 상담·가입·서명은 최근 것부터. */
    fun book(): CustomerBookResponse = dataSource.connection.use { conn ->
        CustomerBookResponse(
            customers = conn.query("SELECT * FROM customer ORDER BY name, id") { it.toCustomer() },
            accounts = conn.query("SELECT * FROM account ORDER BY opened_at, account_no") { it.toAccount() },
            consultations = conn.query("SELECT * FROM consultation ORDER BY consulted_at DESC, id") { it.toConsultation() },
            subscriptions = conn.query(
                "SELECT s.*, p.name AS product_name FROM subscription s JOIN product p ON p.id = s.product_id ORDER BY s.subscribed_at DESC, s.id"
            ) { it.toSubscription() },
            signatures = conn.query(
                "SELECT id, customer_id, purpose, signed_at, digest FROM signature ORDER BY signed_at DESC, id"
            ) { it.toSignatureRecord() },
        )
    }

    fun count(): Int = dataSource.query("SELECT count(*) FROM customer") { it.getInt(1) }.single()

    fun exists(customerId: String): Boolean =
        dataSource.query("SELECT 1 FROM customer WHERE id = ?", customerId) { true }.isNotEmpty()

    /** 등급은 일반, 투자성향은 아직 없다. */
    fun register(
        name: String,
        birthDate: LocalDate,
        gender: String,
        phone: String,
        address: String,
        nowMillis: Long,
    ): CustomerDto {
        val customer = CustomerDto(
            id = UUID.randomUUID().toString(),
            name = name,
            birthDate = birthDate.toString(),
            gender = gender,
            phone = phone,
            address = address,
            grade = GRADE_GENERAL,
            registeredAtMillis = nowMillis,
        )
        dataSource.connection.use { insertCustomer(it, customer) }
        return customer
    }

    /** 시연용 고객을 그대로 넣는다. */
    fun insertCustomer(conn: Connection, customer: CustomerDto) {
        conn.update(
            """
            INSERT INTO customer (id, name, birth_date, gender, phone, address, grade, risk_profile, risk_assessed_at, registered_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            customer.id, customer.name, LocalDate.parse(customer.birthDate), customer.gender, customer.phone,
            customer.address, customer.grade, customer.riskProfile, customer.riskAssessedAtMillis?.toTimestamp(),
            customer.registeredAtMillis.toTimestamp(),
        )
    }

    fun insertAccount(conn: Connection, account: AccountDto) {
        conn.update(
            "INSERT INTO account (account_no, customer_id, type, opened_at) VALUES (?, ?, ?, ?)",
            account.accountNo, account.customerId, account.type, account.openedAtMillis.toTimestamp(),
        )
    }

    fun insertConsultation(conn: Connection, consultation: ConsultationDto) {
        conn.update(
            "INSERT INTO consultation (id, customer_id, consulted_at, channel, topic, content, staff_name) VALUES (?, ?, ?, ?, ?, ?, ?)",
            consultation.id, consultation.customerId, consultation.consultedAtMillis.toTimestamp(), consultation.channel,
            consultation.topic, consultation.content, consultation.staffName,
        )
    }

    /**
     * 계좌를 연다. 열 수 없는 종류면 409 로 거절한다 — ISA 는 만 19세 이상·한 사람에 하나, 연금저축도 하나.
     *
     * 계좌번호는 `7001-<휴대폰 가운데 4자리>-<순번 2자리>` 다. 같은 앞부분을 가진 계좌 수 + 1 이 순번이라,
     * 앞부분마다 잠가 두 요청이 같은 번호를 받지 않게 한다.
     */
    fun openAccount(customerId: String, type: String, nowMillis: Long): AccountDto = dataSource.transaction { conn ->
        val customer = conn.query("SELECT * FROM customer WHERE id = ? FOR UPDATE", customerId) { it.toCustomer() }
            .firstOrNull() ?: throw ApiException.notFound("고객을 찾을 수 없습니다")
        val types = conn.query("SELECT type FROM account WHERE customer_id = ?", customerId) { it.getString(1) }
        val today = BusinessRules.todayInSeoul(nowMillis)
        when (type) {
            TYPE_ISA -> when {
                BusinessRules.ageOn(LocalDate.parse(customer.birthDate), today) < BusinessRules.ISA_MIN_AGE ->
                    throw ApiException.conflict(ApiErrorCode.ISA_AGE, "ISA 는 만 19세 이상만 열 수 있습니다")
                TYPE_ISA in types -> throw ApiException.conflict(ApiErrorCode.ISA_DUPLICATE, "ISA 는 한 사람에 하나만 열 수 있습니다")
            }
            TYPE_PENSION -> if (TYPE_PENSION in types) {
                throw ApiException.conflict(ApiErrorCode.PENSION_DUPLICATE, "연금저축 계좌가 이미 있습니다")
            }
        }

        val prefix = BusinessRules.accountPrefix(customer.phone)
        conn.query("SELECT pg_advisory_xact_lock(hashtext(?))", prefix) { }
        val sequence = conn.query("SELECT count(*) FROM account WHERE account_no LIKE ?", "$prefix%") { it.getInt(1) }.single() + 1
        val account = AccountDto(
            accountNo = prefix + sequence.toString().padStart(2, '0'),
            customerId = customerId,
            type = type,
            openedAtMillis = nowMillis,
        )
        insertAccount(conn, account)
        account
    }

    /** 고객이 없으면 false. */
    fun saveRiskProfile(customerId: String, profile: String, assessedAtMillis: Long): Boolean =
        dataSource.connection.use { conn ->
            conn.update(
                "UPDATE customer SET risk_profile = ?, risk_assessed_at = ? WHERE id = ?",
                profile, assessedAtMillis.toTimestamp(), customerId,
            ) > 0
        }

    /** 고객이 없으면 null. */
    fun addConsultation(
        customerId: String,
        channel: String,
        topic: String,
        content: String,
        staffName: String,
        nowMillis: Long,
    ): ConsultationDto? {
        if (!exists(customerId)) return null
        val consultation = ConsultationDto(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            consultedAtMillis = nowMillis,
            channel = channel,
            topic = topic,
            content = content,
            staffName = staffName,
        )
        dataSource.connection.use { insertConsultation(it, consultation) }
        return consultation
    }

    /** 서명 그림은 여기에만 남는다. 돌려주는 기록에는 지문만 있다. 고객이 없으면 null. */
    fun addSignature(
        customerId: String,
        purpose: String,
        signedAtMillis: Long,
        strokes: List<List<SignaturePointDto>>,
    ): SignatureRecordDto? {
        if (!exists(customerId)) return null
        val record = SignatureRecordDto(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            purpose = purpose,
            signedAtMillis = signedAtMillis,
            digest = BusinessRules.signatureDigest(strokes),
        )
        dataSource.connection.use { conn ->
            conn.update(
                "INSERT INTO signature (id, customer_id, purpose, signed_at, strokes, digest) VALUES (?, ?, ?, ?, ?::jsonb, ?)",
                record.id, record.customerId, record.purpose, record.signedAtMillis.toTimestamp(),
                ServerJson.encodeToString(STROKES_SERIALIZER, strokes), record.digest,
            )
        }
        return record
    }

    private fun ResultSet.toCustomer() = CustomerDto(
        id = getString("id"),
        name = getString("name"),
        birthDate = getObject("birth_date", LocalDate::class.java).toString(),
        gender = getString("gender"),
        phone = getString("phone"),
        address = getString("address"),
        grade = getString("grade"),
        riskProfile = getString("risk_profile"),
        riskAssessedAtMillis = millis("risk_assessed_at"),
        registeredAtMillis = millis("registered_at")!!,
    )

    private fun ResultSet.toAccount() = AccountDto(
        accountNo = getString("account_no"),
        customerId = getString("customer_id"),
        type = getString("type"),
        openedAtMillis = millis("opened_at")!!,
    )

    private fun ResultSet.toConsultation() = ConsultationDto(
        id = getString("id"),
        customerId = getString("customer_id"),
        consultedAtMillis = millis("consulted_at")!!,
        channel = getString("channel"),
        topic = getString("topic"),
        content = getString("content"),
        staffName = getString("staff_name"),
    )

    private fun ResultSet.toSignatureRecord() = SignatureRecordDto(
        id = getString("id"),
        customerId = getString("customer_id"),
        purpose = getString("purpose"),
        signedAtMillis = millis("signed_at")!!,
        digest = getString("digest"),
    )

    companion object {
        const val GRADE_GENERAL = "GENERAL"
        private const val TYPE_ISA = "ISA"
        private const val TYPE_PENSION = "PENSION"
        private val STROKES_SERIALIZER = ListSerializer(ListSerializer(SignaturePointDto.serializer()))
    }
}
