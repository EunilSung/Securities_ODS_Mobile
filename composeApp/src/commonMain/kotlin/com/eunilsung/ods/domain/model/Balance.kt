package com.eunilsung.ods.domain.model

/**
 * 보유 종목 한 건과 지금 시세.
 *
 * 평가손익은 수수료·세금을 빼지 않은 단순 손익이다.
 *
 * @param openSellQuantity 미체결 매도 주문에 묶인 수량.
 */
data class Holding(
    val code: String,
    val name: String,
    val quantity: Long,
    val averagePrice: Long,
    val price: Long,
    val previousClose: Long,
    val openSellQuantity: Long = 0,
) {
    val purchaseAmount: Long get() = averagePrice * quantity
    val evaluationAmount: Long get() = price * quantity
    val profit: Long get() = evaluationAmount - purchaseAmount

    /** 수익률(%). 매입금액이 없으면 0. */
    val profitRate: Double get() = if (purchaseAmount == 0L) 0.0 else profit * 100.0 / purchaseAmount

    val sellableQuantity: Long get() = (quantity - openSellQuantity).coerceAtLeast(0)

    /** 오늘 등락(전일 종가 대비). */
    val dayDirection: PriceDirection get() = PriceDirection.of(price, previousClose)

    /** 전일 대비 등락률(%). */
    val dayChangeRate: Double get() = if (previousClose == 0L) 0.0 else (price - previousClose) * 100.0 / previousClose
}

/**
 * 잔고 요약.
 *
 * 총자산은 예수금 + 평가금액이다. 미체결 매수에 묶인 돈은 아직 예수금이므로 총자산을 바꾸지 않고 주문가능 금액만 줄인다.
 */
data class BalanceSummary(
    val cash: Long,
    val buyingPower: Long,
    val purchaseAmount: Long,
    val evaluationAmount: Long,
) {
    val profit: Long get() = evaluationAmount - purchaseAmount
    val profitRate: Double get() = if (purchaseAmount == 0L) 0.0 else profit * 100.0 / purchaseAmount
    val totalAsset: Long get() = cash + evaluationAmount

    companion object {
        fun of(account: Account, holdings: List<Holding>) = BalanceSummary(
            cash = account.cash,
            buyingPower = account.buyingPower,
            purchaseAmount = holdings.sumOf { it.purchaseAmount },
            evaluationAmount = holdings.sumOf { it.evaluationAmount },
        )
    }
}
