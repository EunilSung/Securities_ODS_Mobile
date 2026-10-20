package com.eunilsung.ods.server.repository

import com.eunilsung.ods.server.db.query
import com.eunilsung.ods.server.db.update
import com.eunilsung.ods.shared.api.StaffDto
import java.sql.ResultSet
import javax.sql.DataSource

/** 로그인 판정에 필요한 것 — 직원과 그 비밀번호 해시. 해시는 서버 밖으로 나가지 않는다. */
data class StaffCredential(
    val staff: StaffDto,
    val passwordHash: String,
)

/** 직원 저장소. 아이디는 소문자로 통일해 대소문자 차이로 다른 사람이 되지 않게 한다. */
class StaffRepository(private val dataSource: DataSource) {

    fun find(staffId: String): StaffDto? = findCredential(staffId)?.staff

    fun findCredential(staffId: String): StaffCredential? =
        dataSource.query("SELECT * FROM staff WHERE id = ?", normalize(staffId)) {
            StaffCredential(it.toStaff(), it.getString("password_hash"))
        }.firstOrNull()

    fun count(): Int = dataSource.query("SELECT count(*) FROM staff") { it.getInt(1) }.single()

    /** 없을 때만 넣는다. 이미 있으면 그대로 둔다. */
    fun insertIfAbsent(staff: StaffDto, passwordHash: String) {
        dataSource.connection.use { conn ->
            conn.update(
                "INSERT INTO staff (id, name, branch, position, password_hash) VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING",
                normalize(staff.id), staff.name, staff.branch, staff.position, passwordHash,
            )
        }
    }

    private fun ResultSet.toStaff() = StaffDto(
        id = getString("id"),
        name = getString("name"),
        branch = getString("branch"),
        position = getString("position"),
    )

    private fun normalize(staffId: String): String = staffId.trim().lowercase()
}
