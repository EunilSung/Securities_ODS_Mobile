package com.eunilsung.ods.ui.openorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.Order
import com.eunilsung.ods.domain.model.OrderChangeResult
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.domain.usecase.OpenOrderUseCases
import com.eunilsung.ods.domain.util.OrderCheck
import com.eunilsung.ods.domain.util.amendExtraAmount
import com.eunilsung.ods.domain.util.checkAmend
import com.eunilsung.ods.domain.util.tickDown
import com.eunilsung.ods.domain.util.tickUp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 미체결 화면 — 미체결 주문과 각 종목의 현재가, 정정·취소 창을 내놓는다.
 *
 * 정정은 남은 수량 전체의 가격을 바꾸고([checkAmend] 로 검사), 취소는 남은 수량 전체를 취소한다.
 * 보내기를 누르면 인증 창을 띄우고, 인증된 뒤에만 보낸다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OpenOrdersViewModel(
    private val useCases: OpenOrderUseCases,
) : ViewModel() {

    private val step = MutableStateFlow<ChangeStep?>(null)

    private val amendStock: Flow<Stock?> = step
        .map { (it as? ChangeStep.Editing)?.order?.code }
        .distinctUntilChanged()
        .flatMapLatest { code -> if (code == null) flowOf(null) else useCases.observeStock(code) }

    val uiState: StateFlow<OpenOrdersUiState> = combine(
        useCases.observeOpenOrders(),
        useCases.observeWatchlist(),
        useCases.observeAccount(),
        step,
        amendStock,
    ) { orders, stocks, account, current, stock ->
        OpenOrdersUiState(
            orders = orders,
            stocks = stocks.associateBy { it.code },
            buyingPower = account.buyingPower,
            change = current?.toState(stock, account.buyingPower),
            isLoaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OpenOrdersUiState())

    fun onAction(action: OpenOrdersActions) {
        when (action) {
            is OpenOrdersActions.OnAmendClick -> step.value = ChangeStep.Editing(action.order, action.order.price.toString())
            is OpenOrdersActions.OnCancelClick -> step.value = ChangeStep.Cancelling(action.order)
            is OpenOrdersActions.OnAmendPriceChange -> updateEditing { it.copy(priceText = action.text.filter(Char::isDigit).trimStart('0').take(10)) }
            is OpenOrdersActions.OnAmendPriceStep -> stepPrice(action.up)
            OpenOrdersActions.OnAmendUseCurrentPrice -> {
                val stock = (uiState.value.change as? OrderChangeState.Amend)?.stock ?: return
                updateEditing { it.copy(priceText = stock.price.toString()) }
            }
            OpenOrdersActions.OnConfirm -> requestAuth()
            OpenOrdersActions.OnAuthorized -> submit()
            OpenOrdersActions.OnAuthCanceled -> step.update { current ->
                when (current) {
                    is ChangeStep.Editing -> current.copy(awaitingAuth = false)
                    is ChangeStep.Cancelling -> current.copy(awaitingAuth = false)
                    else -> current
                }
            }
            OpenOrdersActions.OnDismiss -> step.update { if (it?.isSubmitting == true || it?.awaitingAuth == true) it else null }
        }
    }

    private fun updateEditing(transform: (ChangeStep.Editing) -> ChangeStep.Editing) {
        step.update { current -> if (current is ChangeStep.Editing && !current.isSubmitting) transform(current) else current }
    }

    private fun stepPrice(up: Boolean) {
        val stock = (uiState.value.change as? OrderChangeState.Amend)?.stock ?: return
        updateEditing {
            val base = it.priceText.toLongOrNull()?.takeIf { price -> price > 0 } ?: it.order.price
            val next = (if (up) tickUp(base) else tickDown(base)).coerceIn(stock.lowerLimit, stock.upperLimit)
            it.copy(priceText = next.toString())
        }
    }

    /** 보내기 전에 검사를 통과했는지 보고 인증 창을 띄운다. */
    private fun requestAuth() {
        when (val current = step.value) {
            is ChangeStep.Editing -> {
                val amend = uiState.value.change as? OrderChangeState.Amend ?: return
                if (current.isSubmitting || amend.stock == null || !amend.check.canSubmit) return
                if (current.priceText.toLongOrNull() == null) return
                step.value = current.copy(awaitingAuth = true)
            }
            is ChangeStep.Cancelling -> if (!current.isSubmitting) step.value = current.copy(awaitingAuth = true)
            else -> Unit
        }
    }

    /** 인증된 뒤에만 부른다. */
    private fun submit() {
        when (val current = step.value) {
            is ChangeStep.Editing -> {
                if (!current.awaitingAuth) return
                val newPrice = current.priceText.toLongOrNull() ?: return
                step.value = current.copy(awaitingAuth = false, isSubmitting = true)
                viewModelScope.launch {
                    val result = useCases.amendOrder(current.order.orderNo, newPrice)
                    step.value = ChangeStep.Finished(
                        when (result) {
                            is OrderChangeResult.Done -> result.order?.let { OrderChangeState.Amended(it) } ?: OrderChangeState.Unreachable
                            is OrderChangeResult.Rejected -> OrderChangeState.Rejected(result.reason)
                            OrderChangeResult.Unreachable -> OrderChangeState.Unreachable
                        }
                    )
                }
            }
            is ChangeStep.Cancelling -> {
                if (!current.awaitingAuth) return
                step.value = current.copy(awaitingAuth = false, isSubmitting = true)
                viewModelScope.launch {
                    val result = useCases.cancelOrder(current.order.orderNo)
                    step.value = ChangeStep.Finished(
                        when (result) {
                            is OrderChangeResult.Done -> OrderChangeState.Canceled(current.order)
                            is OrderChangeResult.Rejected -> OrderChangeState.Rejected(result.reason)
                            OrderChangeResult.Unreachable -> OrderChangeState.Unreachable
                        }
                    )
                }
            }
            else -> Unit
        }
    }

    /** 창의 진행 상태. 검사 결과는 시세·주문가능 금액이 바뀔 때마다 [toState] 에서 다시 낸다. */
    private sealed interface ChangeStep {
        val isSubmitting: Boolean
        val awaitingAuth: Boolean

        data class Editing(
            val order: Order,
            val priceText: String,
            override val awaitingAuth: Boolean = false,
            override val isSubmitting: Boolean = false,
        ) : ChangeStep

        data class Cancelling(
            val order: Order,
            override val awaitingAuth: Boolean = false,
            override val isSubmitting: Boolean = false,
        ) : ChangeStep

        data class Finished(val state: OrderChangeState) : ChangeStep {
            override val isSubmitting: Boolean get() = false
            override val awaitingAuth: Boolean get() = false
        }

        fun toState(stock: Stock?, buyingPower: Long): OrderChangeState = when (this) {
            is Editing -> {
                val newPrice = priceText.toLongOrNull() ?: 0
                OrderChangeState.Amend(
                    order = order,
                    stock = stock?.takeIf { it.code == order.code },
                    priceText = priceText,
                    check = stock?.takeIf { it.code == order.code }?.let { checkAmend(order, newPrice, it, buyingPower) } ?: OrderCheck.EMPTY,
                    extraAmount = amendExtraAmount(order, newPrice),
                    awaitingAuth = awaitingAuth,
                    isSubmitting = isSubmitting,
                )
            }
            is Cancelling -> OrderChangeState.Cancel(order, awaitingAuth, isSubmitting)
            is Finished -> state
        }
    }
}
