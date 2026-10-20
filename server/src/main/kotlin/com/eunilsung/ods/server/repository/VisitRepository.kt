package com.eunilsung.ods.server.repository

import com.eunilsung.ods.server.db.millis
import com.eunilsung.ods.server.db.query
import com.eunilsung.ods.server.db.toTimestamp
import com.eunilsung.ods.server.db.update
import com.eunilsung.ods.shared.api.VisitDto
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

/**
 * 방문 일정 — 직원마다 자기 것만 있다.
 *
 * 모든 조회·수정에 직원 아이디를 조건으로 건다. 남의 일정은 "없는 일정" 과 똑같이 보인다 —
 * 있다고만 알려 줘도 남의 일정 아이디를 더듬어 찾을 수 있다.
 */
class VisitRepository(private val dataSource: DataSource) {

    fun visitsOf(staffId: String): List<VisitDto> = dataSource.query(
        "$SELECT_VISIT WHERE v.staff_id = ? ORDER BY v.scheduled_at, v.id", staffId,
    ) { rs ->
        VisitDto(
            id = rs.getString("id"),
            customerId = rs.getString("customer_id"),
            customerName = rs.getString("customer_name"),
            scheduledAtMillis = rs.millis("scheduled_at")!!,
            place = rs.getString("place"),
            purpose = rs.getString("purpose"),
            memo = rs.getString("memo"),
            status = rs.getString("status"),
        )
    }

    fun count(): Int = dataSource.query("SELECT count(*) FROM visit") { it.getInt(1) }.single()

    /** 고객이 없으면 null. */
    fun add(
        staffId: String,
        customerId: String,
        scheduledAtMillis: Long,
        place: String,
        purpose: String,
        memo: String,
    ): VisitDto? {
        val customerName = dataSource.query("SELECT name FROM customer WHERE id = ?", customerId) { it.getString(1) }
            .firstOrNull() ?: return null
        val visit = VisitDto(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            customerName = customerName,
            scheduledAtMillis = scheduledAtMillis,
            place = place,
            purpose = purpose,
            memo = memo,
            status = STATUS_SCHEDULED,
        )
        dataSource.connection.use { insert(it, staffId, visit) }
        return visit
    }

    fun insert(conn: Connection, staffId: String, visit: VisitDto) {
        conn.update(
            """
            INSERT INTO visit (id, staff_id, customer_id, scheduled_at, place, purpose, memo, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            visit.id, staffId, visit.customerId, visit.scheduledAtMillis.toTimestamp(), visit.place, visit.purpose,
            visit.memo, visit.status,
        )
    }

    /** 이 직원의 일정이 아니면(없는 일정 포함) false. */
    fun updateStatus(staffId: String, visitId: String, status: String): Boolean =
        dataSource.connection.use { conn ->
            conn.update("UPDATE visit SET status = ? WHERE id = ? AND staff_id = ?", status, visitId, staffId) > 0
        }

    companion object {
        const val STATUS_SCHEDULED = "SCHEDULED"
        private const val SELECT_VISIT =
            "SELECT v.*, c.name AS customer_name FROM visit v JOIN customer c ON c.id = v.customer_id"
    }
}
