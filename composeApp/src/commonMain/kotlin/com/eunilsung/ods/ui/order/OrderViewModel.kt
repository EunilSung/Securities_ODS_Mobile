package com.eunilsung.ods.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.OrderBook
import com.eunilsung.ods.domain.model.OrderDraft
import com.eunilsung.ods.domain.model.OrderResult
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.domain.usecase.OrderUseCases
import com.eunilsung.ods.domain.util.OrderCheck
import com.eunilsung.ods.domain.util.checkOrder
import com.eunilsung.ods.domain.util.maxOrderQuantity
import com.eunilsung.ods.domain.util.reservedAmount
import com.eunilsung.ods.domain.util.tickDown
import com.eunilsung.ods.domain.util.tickUp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 주문 화면 — 입력(매수·매도, 지정가·시장가, 가격, 수량)과 시세·계좌를 묶어 검사 결과까지 내놓는다.
 *
 * 주문 탭, 현재가에서 연 주문 화면, 넓은 태블릿의 주문 칸이 같은 클래스를 하나씩 쓴다.
 * 검사는 [checkOrder] 가 하고, 주문가능 금액·매도가능 수량은 접수하는 쪽이 다시 확인한다.
 * 확인 화면에서 주문 버튼을 누르면 인증 창을 띄우고, 인증된 뒤에만 보낸다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModel(
    private val orderUseCases: OrderUseCases,
) : ViewModel() {

    private val code = MutableStateFlow<String?>(null)
    private val form = MutableStateFlow(OrderForm())
    private val showsOrderBook = MutableStateFlow(false)
    private val showsWatchlist = MutableStateFlow(false)
    private var initialPriceJob: Job? = null

    private val market: Flow<MarketSnapshot> = code.flatMapLatest { selected ->
        val watchlist = showsWatchlist.flatMapLatest { shows ->
            if (shows) orderUseCases.observeWatchlist() else flowOf(emptyList())
        }
        if (selected == null) return@flatMapLatest watchlist.map { MarketSnapshot(watchlist = it) }
        val orderBook: Flow<OrderBook?> = showsOrderBook.flatMapLatest { shows ->
            if (shows) orderUseCases.observeOrderBook(selected) else flowOf(null)
        }
        combine(
            orderUseCases.observeStock(selected),
            orderBook,
            orderUseCases.observeAccount(),
            orderUseCases.observeSellableQuantity(selected),
            watchlist,
        ) { stock, book, account, sellable, stocks ->
            MarketSnapshot(stock, book, account.buyingPower, sellable, stocks)
        }
    }

    val uiState: StateFlow<OrderUiState> = combine(code, form, market) { selected, input, snapshot ->
        val stock = snapshot.stock?.takeIf { it.code == selected }
        val draft = input.toDraft(selected.orEmpty())
        OrderUiState(
            code = selected.orEmpty(),
            stock = stock,
            orderBook = snapshot.orderBook?.takeIf { it.code == selected },
            watchlist = snapshot.watchlist,
            side = input.side,
            type = input.type,
            priceText = input.priceText,
            quantityText = input.quantityText,
            buyingPower = snapshot.buyingPower,
            sellableQuantity = snapshot.sellableQuantity,
            maxQuantity = stock?.let {
                maxOrderQuantity(input.side, input.type, draft.price, it, snapshot.buyingPower, snapshot.sellableQuantity)
            } ?: 0,
            orderAmount = stock?.let { draft.reservedAmount(it) } ?: 0,
            check = stock?.let { checkOrder(draft, it, snapshot.buyingPower, snapshot.sellableQuantity) } ?: OrderCheck.EMPTY,
            confirm = input.confirm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderUiState())

    fun onAction(action: OrderActions) {
        when (action) {
            is OrderActions.OnEnter -> {
                showsOrderBook.value = true
                showsWatchlist.value = false
                select(action.code, side = action.side, price = action.price, resetInput = true)
            }
            OrderActions.OnEnterTab -> enterTab()
            is OrderActions.OnPaneCodeChange -> {
                showsOrderBook.value = false
                showsWatchlist.value = false
                val selected = action.code
                if (selected == null) {
                    code.value = null
                    form.value = OrderForm(side = form.value.side)
                } else {
                    select(selected)
                }
            }
            is OrderActions.OnSelectStock -> {
                orderUseCases.setLastQuoteCode(action.code)
                select(action.code)
            }
            is OrderActions.OnSideChange -> form.update { it.copy(side = action.side) }
            is OrderActions.OnTypeChange -> form.update { it.copy(type = action.type) }
            is OrderActions.OnPriceChange -> form.update { it.copy(priceText = action.text.digitsOnly()) }
            is OrderActions.OnPriceStep -> stepPrice(action.up)
            OrderActions.OnUseCurrentPrice -> uiState.value.stock?.let { stock ->
                form.update { it.copy(type = OrderType.LIMIT, priceText = stock.price.toString()) }
            }
            is OrderActions.OnPriceSelected -> select(action.code, price = action.price)
            is OrderActions.OnQuantityChange -> form.update { it.copy(quantityText = action.text.digitsOnly()) }
            is OrderActions.OnQuantityStep -> form.update {
                val next = ((it.quantityText.toLongOrNull() ?: 0) + if (action.up) 1 else -1).coerceAtLeast(0)
                it.copy(quantityText = if (next == 0L) "" else next.toString())
            }
            is OrderActions.OnQuantityPercent -> {
                val quantity = uiState.value.maxQuantity * action.percent / 100
                form.update { it.copy(quantityText = if (quantity == 0L) "" else quantity.toString()) }
            }
            OrderActions.OnSubmitClick -> openReview()
            OrderActions.OnConfirm -> updateReview { if (it.isSubmitting) it else it.copy(awaitingAuth = true) }
            OrderActions.OnAuthorized -> submit()
            OrderActions.OnAuthCanceled -> updateReview { it.copy(awaitingAuth = false) }
            OrderActions.OnDismissConfirm -> form.update { input ->
                val review = input.confirm as? OrderConfirmState.Review
                if (review?.isSubmitting == true || review?.awaitingAuth == true) input else input.copy(confirm = null)
            }
        }
    }

    private fun enterTab() {
        showsOrderBook.value = true
        showsWatchlist.value = true
        viewModelScope.launch {
            val selected = orderUseCases.getLastQuoteCode()
                ?: orderUseCases.observeWatchlist().first { it.isNotEmpty() }.first().code
            select(selected)
        }
    }

    /**
     * 종목을 고른다. 다른 종목이면 입력을 비우고, 같은 종목이면 넘어온 값만 바꾼다.
     * 가격이 비면 현재가가 들어오는 대로 채운다.
     */
    private fun select(selected: String, side: OrderSide? = null, price: Long? = null, resetInput: Boolean = false) {
        val sameStock = code.value == selected && !resetInput
        form.update { current ->
            if (sameStock) {
                current.copy(
                    side = side ?: current.side,
                    type = if (price != null) OrderType.LIMIT else current.type,
                    priceText = price?.toString() ?: current.priceText,
                )
            } else {
                OrderForm(side = side ?: current.side, priceText = price?.toString().orEmpty())
            }
        }
        code.value = selected
        if (form.value.priceText.isEmpty()) fillCurrentPrice(selected)
    }

    private fun fillCurrentPrice(selected: String) {
        initialPriceJob?.cancel()
        initialPriceJob = viewModelScope.launch {
            val stock = orderUseCases.observeStock(selected).filterNotNull().first()
            form.update { if (it.priceText.isEmpty() && code.value == selected) it.copy(priceText = stock.price.toString()) else it }
        }
    }

    private fun stepPrice(up: Boolean) {
        val stock = uiState.value.stock ?: return
        form.update {
            val base = it.priceText.toLongOrNull()?.takeIf { price -> price > 0 } ?: stock.price
            val next = (if (up) tickUp(base) else tickDown(base)).coerceIn(stock.lowerLimit, stock.upperLimit)
            it.copy(type = OrderType.LIMIT, priceText = next.toString())
        }
    }

    private fun openReview() {
        val state = uiState.value
        val stock = state.stock ?: return
        if (!state.check.canSubmit) return
        val draft = form.value.toDraft(stock.code)
        form.update {
            it.copy(confirm = OrderConfirmState.Review(draft, stock.name, state.orderAmount, state.check.warnings))
        }
    }

    private fun updateReview(transform: (OrderConfirmState.Review) -> OrderConfirmState.Review) {
        form.update { input ->
            val review = input.confirm as? OrderConfirmState.Review ?: return@update input
            input.copy(confirm = transform(review))
        }
    }

    /** 인증된 뒤에만 부른다. */
    private fun submit() {
        val review = form.value.confirm as? OrderConfirmState.Review ?: return
        if (review.isSubmitting || !review.awaitingAuth) return
        form.update { it.copy(confirm = review.copy(awaitingAuth = false, isSubmitting = true)) }
        viewModelScope.launch {
            val result = orderUseCases.submitOrder(review.draft)
            form.update {
                when (result) {
                    is OrderResult.Accepted -> it.copy(confirm = OrderConfirmState.Accepted(result.order), quantityText = "")
                    is OrderResult.Rejected -> it.copy(confirm = OrderConfirmState.Rejected(result.reason))
                    OrderResult.Unreachable -> it.copy(confirm = OrderConfirmState.Unreachable)
                }
            }
        }
    }

    private fun String.digitsOnly(): String = filter(Char::isDigit).trimStart('0').take(MAX_DIGITS)

    private data class OrderForm(
        val side: OrderSide = OrderSide.BUY,
        val type: OrderType = OrderType.LIMIT,
        val priceText: String = "",
        val quantityText: String = "",
        val confirm: OrderConfirmState? = null,
    ) {
        fun toDraft(code: String) = OrderDraft(
            code = code,
            side = side,
            type = type,
            price = priceText.toLongOrNull() ?: 0,
            quantity = quantityText.toLongOrNull() ?: 0,
        )
    }

    private data class MarketSnapshot(
        val stock: Stock? = null,
        val orderBook: OrderBook? = null,
        val buyingPower: Long = 0,
        val sellableQuantity: Long = 0,
        val watchlist: List<Stock> = emptyList(),
    )

    private companion object {
        const val MAX_DIGITS = 10
    }
}
