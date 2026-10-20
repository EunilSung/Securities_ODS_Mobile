package com.eunilsung.ods.server.repository

import com.eunilsung.ods.server.db.query
import com.eunilsung.ods.server.db.transaction
import com.eunilsung.ods.server.db.update
import com.eunilsung.ods.shared.api.QuoteDto
import com.eunilsung.ods.shared.api.StockDto
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

/** 관심종목 시세. */
class StockRepository(private val dataSource: DataSource) {

    fun watchlist(): List<StockDto> = dataSource.query("SELECT * FROM stock ORDER BY sort_order") { it.toStock() }

    fun count(): Int = dataSource.query("SELECT count(*) FROM stock") { it.getInt(1) }.single()

    fun insert(conn: Connection, stock: StockDto, sortOrder: Int) {
        conn.update(
            "INSERT INTO stock (code, name, market, price, previous_close, volume, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?)",
            stock.code, stock.name, stock.market, stock.price, stock.previousClose, stock.volume, sortOrder,
        )
    }

    /**
     * 한 종목의 시세를 [next] 가 정한 값으로 바꾼다. 행을 잠그고 읽어 고치므로 두 체결이 같은 값에서 출발하지 않는다.
     * 종목이 없으면 null.
     */
    fun move(code: String, next: (StockDto) -> QuoteDto): QuoteDto? = dataSource.transaction { conn ->
        val stock = conn.query("SELECT * FROM stock WHERE code = ? FOR UPDATE", code) { it.toStock() }.firstOrNull()
            ?: return@transaction null
        val quote = next(stock)
        conn.update("UPDATE stock SET price = ?, volume = ? WHERE code = ?", quote.price, quote.volume, code)
        quote
    }

    private fun ResultSet.toStock() = StockDto(
        code = getString("code"),
        name = getString("name"),
        market = getString("market"),
        price = getLong("price"),
        previousClose = getLong("previous_close"),
        volume = getLong("volume"),
    )
}
