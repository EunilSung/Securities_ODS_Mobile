package com.eunilsung.ods.server.repository

import com.eunilsung.ods.server.ApiException
import com.eunilsung.ods.server.ServerJson
import com.eunilsung.ods.server.db.millis
import com.eunilsung.ods.server.db.query
import com.eunilsung.ods.server.db.toTimestamp
import com.eunilsung.ods.server.db.transaction
import com.eunilsung.ods.server.db.update
import com.eunilsung.ods.server.rules.BusinessRules
import com.eunilsung.ods.shared.api.ApiErrorCode
import com.eunilsung.ods.shared.api.NewSubscriptionRequest
import com.eunilsung.ods.shared.api.ProductDto
import com.eunilsung.ods.shared.api.SubscriptionDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

/** 판매 상품과 상품 가입. */
class ProductRepository(private val dataSource: DataSource) {

    fun all(): List<ProductDto> = dataSource.query("SELECT * FROM product ORDER BY sort_order") { it.toProduct() }

    fun count(): Int = dataSource.query("SELECT count(*) FROM product") { it.getInt(1) }.single()

    fun insert(conn: Connection, product: ProductDto, sortOrder: Int) {
        conn.update(
            """
            INSERT INTO product (id, name, type, risk_grade, summary, expected_return, fee_info, min_amount, key_points, is_complex, sort_order)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
            """.trimIndent(),
            product.id, product.name, product.type, product.riskGrade, product.summary, product.expectedReturn,
            product.feeInfo, product.minAmount, ServerJson.encodeToString(KEY_POINTS_SERIALIZER, product.keyPoints),
            product.isComplex, sortOrder,
        )
    }

    /**
     * 가입한다. 화면이 막은 조건을 저장 직전에 한 번 더 본다 — 고객·상품이 있는지(404), 계좌가 그 고객의 것인지,
     * 금액이 최소 금액 이상·만 원 단위인지, 1년 안에 진단한 투자성향이 있는지, 성향보다 위험한 상품이면
     * 고객이 스스로 원한다고 확인했는지(409).
     *
     * 고객 행을 잠그고 확인하므로 확인과 저장 사이에 투자성향이 바뀌지 않는다.
     */
    fun subscribe(request: NewSubscriptionRequest, nowMillis: Long): SubscriptionDto = dataSource.transaction { conn ->
        val customer = conn.query(
            "SELECT risk_profile, risk_assessed_at FROM customer WHERE id = ? FOR UPDATE", request.customerId,
        ) { it.getString("risk_profile") to it.millis("risk_assessed_at") }
            .firstOrNull() ?: throw ApiException.notFound("고객을 찾을 수 없습니다")
        val product = conn.query("SELECT * FROM product WHERE id = ?", request.productId) { it.toProduct() }
            .firstOrNull() ?: throw ApiException.notFound("상품을 찾을 수 없습니다")
        val isOwnAccount = conn.query(
            "SELECT 1 FROM account WHERE account_no = ? AND customer_id = ?", request.accountNo, request.customerId,
        ) { true }.isNotEmpty()

        val (profile, assessedAt) = customer
        when {
            !isOwnAccount -> throw ApiException.conflict(ApiErrorCode.ACCOUNT_MISMATCH, "고객의 계좌가 아닙니다")
            !BusinessRules.isValidAmount(request.amount, product.minAmount) ->
                throw ApiException.conflict(ApiErrorCode.INVALID_AMOUNT, "가입금액이 최소 금액보다 적거나 만 원 단위가 아닙니다")
            !BusinessRules.hasValidRiskProfile(profile, assessedAt, BusinessRules.todayInSeoul(nowMillis)) ->
                throw ApiException.conflict(ApiErrorCode.NEEDS_ASSESSMENT, "투자성향 진단이 필요합니다")
            !BusinessRules.allows(profile!!, product.riskGrade) && !request.unsuitableConfirmed ->
                throw ApiException.conflict(ApiErrorCode.UNSUITABLE_NOT_CONFIRMED, "투자성향보다 위험한 상품입니다. 고객 확인이 필요합니다")
        }

        val subscription = SubscriptionDto(
            id = UUID.randomUUID().toString(),
            customerId = request.customerId,
            productId = product.id,
            productName = product.name,
            accountNo = request.accountNo,
            amount = request.amount,
            subscribedAtMillis = nowMillis,
            unsuitableConfirmed = request.unsuitableConfirmed,
            coolingOffUntilMillis = request.coolingOffUntilMillis,
        )
        conn.update(
            """
            INSERT INTO subscription (id, customer_id, product_id, account_no, amount, subscribed_at, unsuitable_confirmed, cooling_off_until)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            subscription.id, subscription.customerId, subscription.productId, subscription.accountNo, subscription.amount,
            subscription.subscribedAtMillis.toTimestamp(), subscription.unsuitableConfirmed,
            subscription.coolingOffUntilMillis?.toTimestamp(),
        )
        subscription
    }

    private fun ResultSet.toProduct() = ProductDto(
        id = getString("id"),
        name = getString("name"),
        type = getString("type"),
        riskGrade = getString("risk_grade"),
        summary = getString("summary"),
        expectedReturn = getString("expected_return"),
        feeInfo = getString("fee_info"),
        minAmount = getLong("min_amount"),
        keyPoints = ServerJson.decodeFromString(KEY_POINTS_SERIALIZER, getString("key_points")),
        isComplex = getBoolean("is_complex"),
    )

    private companion object {
        val KEY_POINTS_SERIALIZER = ListSerializer(String.serializer())
    }
}

/** 가입 행. `product_name` 열(상품 표와 이은 것)이 있어야 한다. */
internal fun ResultSet.toSubscription() = SubscriptionDto(
    id = getString("id"),
    customerId = getString("customer_id"),
    productId = getString("product_id"),
    productName = getString("product_name"),
    accountNo = getString("account_no"),
    amount = getLong("amount"),
    subscribedAtMillis = millis("subscribed_at")!!,
    unsuitableConfirmed = getBoolean("unsuitable_confirmed"),
    coolingOffUntilMillis = millis("cooling_off_until"),
)
