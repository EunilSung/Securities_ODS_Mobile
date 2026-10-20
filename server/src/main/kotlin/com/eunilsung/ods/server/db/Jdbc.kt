package com.eunilsung.ods.server.db

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import javax.sql.DataSource

/**
 * 한 트랜잭션으로 묶는다. [block] 이 던지면 되돌린다 — 규칙 확인과 쓰기 사이에 다른 요청이 끼어들어도
 * 확인한 그대로 저장된다(확인하는 쪽이 행을 잠근다).
 */
fun <T> DataSource.transaction(block: (Connection) -> T): T = connection.use { conn ->
    conn.autoCommit = false
    try {
        block(conn).also { conn.commit() }
    } catch (e: Throwable) {
        conn.rollback()
        throw e
    }
}

/** 한 번 묻고 행마다 [map] 한다. [params] 는 `?` 자리에 차례로 들어간다. */
fun <T> Connection.query(sql: String, vararg params: Any?, map: (ResultSet) -> T): List<T> =
    prepareStatement(sql).use { st ->
        params.forEachIndexed { index, value -> st.setObject(index + 1, value) }
        st.executeQuery().use { rs -> buildList { while (rs.next()) add(map(rs)) } }
    }

/** 바뀐 행 수를 돌려준다. */
fun Connection.update(sql: String, vararg params: Any?): Int =
    prepareStatement(sql).use { st ->
        params.forEachIndexed { index, value -> st.setObject(index + 1, value) }
        st.executeUpdate()
    }

fun <T> DataSource.query(sql: String, vararg params: Any?, map: (ResultSet) -> T): List<T> =
    connection.use { it.query(sql, *params, map = map) }

/** epoch 밀리초를 TIMESTAMPTZ 자리에 넣을 값으로. */
fun Long.toTimestamp(): Timestamp = Timestamp(this)

/** TIMESTAMPTZ 열을 epoch 밀리초로. 비어 있으면 null. */
fun ResultSet.millis(column: String): Long? = getTimestamp(column)?.time
