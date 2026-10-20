package com.eunilsung.ods.ui.product.subscribe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.NewSubscription
import com.eunilsung.ods.domain.usecase.SubscribeResult
import com.eunilsung.ods.domain.usecase.SubscriptionUseCases
import com.eunilsung.ods.domain.util.SubscriptionPolicy
import com.eunilsung.ods.domain.util.matchesQuery
import com.eunilsung.ods.domain.util.suitabilityFor
import com.eunilsung.ods.domain.util.todayInSeoul
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 상품 가입 절차 — 고객 → 적합성 → 핵심 설명 → 계좌·금액 → 서명 → 완료.
 *
 * - 적합성: 성향이 없거나 만료됐으면 진단부터 하게 막는다(진단을 마치고 돌아오면 다시 판정한다).
 *   성향보다 위험한 상품은 권유할 수 없으므로, 고객이 스스로 원한다는 확인을 받아야 넘어간다.
 * - 핵심 설명: 항목마다 설명했다는 표시를 받아야 넘어간다.
 * - 계좌·금액: 고객 계좌 가운데 고르고, 최소 금액 이상 만 원 단위여야 한다.
 * 고객을 고르면 그 고객과 상담 중으로 둔다. 고객과 계좌는 기기 DB 를 계속 보므로 진단을 마치거나 계좌를 새로 열면 바로 반영된다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModel(
    private val useCases: SubscriptionUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private val _events = Channel<SubscriptionEvent>(Channel.BUFFERED)
    val events: Flow<SubscriptionEvent> = _events.receiveAsFlow()

    private var customers: List<Customer> = emptyList()
    private var watchJob: Job? = null

    fun onAction(action: SubscriptionActions) {
        when (action) {
            is SubscriptionActions.OnEnter -> enter(action.productId, action.customerId)
            is SubscriptionActions.OnCustomerQueryChange -> _uiState.update {
                it.copy(customerQuery = action.query, customerMatches = match(action.query))
            }
            is SubscriptionActions.OnCustomerSelect -> selectCustomer(action.customerId)
            SubscriptionActions.OnNext -> next()
            SubscriptionActions.OnPrevious -> previous()
            SubscriptionActions.OnAssessClick -> {
                val id = _uiState.value.customer?.id ?: return
                viewModelScope.launch { _events.send(SubscriptionEvent.OpenRiskAssessment(id)) }
            }
            is SubscriptionActions.OnUnsuitableConfirm -> _uiState.update { it.copy(unsuitableConfirmed = action.confirmed) }
            is SubscriptionActions.OnKeyPointToggle -> _uiState.update {
                val points = if (action.index in it.explainedPoints) it.explainedPoints - action.index else it.explainedPoints + action.index
                it.copy(explainedPoints = points)
            }
            is SubscriptionActions.OnAccountSelect -> _uiState.update { it.copy(accountNo = action.accountNo, error = null) }
            is SubscriptionActions.OnAmountChange -> _uiState.update {
                it.copy(amountText = action.text.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS).trimStart('0'), error = null)
            }
            is SubscriptionActions.OnAmountStep -> _uiState.update {
                val next = (it.amount + if (action.up) AMOUNT_STEP else -AMOUNT_STEP).coerceAtLeast(0)
                it.copy(amountText = if (next == 0L) "" else next.toString(), error = null)
            }
            is SubscriptionActions.OnSignatureChange -> _uiState.update { it.copy(signature = action.signature, error = null) }
            is SubscriptionActions.OnSubmit -> submit(action.note)
        }
    }

    private fun enter(productId: String, customerId: String?) {
        if (_uiState.value.product?.id == productId) return
        viewModelScope.launch {
            customers = useCases.observeCustomers().first()
            val product = useCases.observeProduct(productId).first()
            _uiState.value = SubscriptionUiState(
                product = product,
                amountText = product?.minAmount?.toString().orEmpty(),
                customerMatches = match(""),
            )
            if (customerId != null) selectCustomer(customerId)
        }
    }

    private fun selectCustomer(customerId: String) {
        useCases.startConsult(customerId)
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            combine(useCases.observeCustomer(customerId), useCases.observeAccounts(customerId)) { customer, accounts -> customer to accounts }
                .collect { (customer, accounts) ->
                    val today = todayInSeoul()
                    _uiState.update { state ->
                        val product = state.product
                        state.copy(
                            customer = customer,
                            customerQuery = "",
                            customerMatches = emptyList(),
                            suitability = if (customer != null && product != null) customer.suitabilityFor(product, today) else null,
                            needsCoolingOff = customer != null && product != null && SubscriptionPolicy.needsCoolingOff(customer, product, today),
                            accounts = accounts,
                            accountNo = state.accountNo?.takeIf { no -> accounts.any { it.accountNo == no } } ?: accounts.firstOrNull()?.accountNo,
                            step = if (state.step == SubscriptionStep.CUSTOMER && customer != null) SubscriptionStep.SUITABILITY else state.step,
                        )
                    }
                }
        }
    }

    private fun match(query: String): List<Customer> = customers.filter { it.matchesQuery(query) }.take(MAX_MATCHES)

    private fun next() {
        val state = _uiState.value
        if (!state.canProceed) return
        val steps = SubscriptionStep.entries
        val nextStep = steps[(state.step.ordinal + 1).coerceAtMost(SubscriptionStep.SIGN.ordinal)]
        _uiState.update { it.copy(step = nextStep, error = null) }
    }

    private fun previous() {
        val state = _uiState.value
        if (state.step == SubscriptionStep.CUSTOMER || state.step == SubscriptionStep.DONE) return
        val previousStep = SubscriptionStep.entries[state.step.ordinal - 1]
        _uiState.update {
            if (previousStep == SubscriptionStep.CUSTOMER) {
                watchJob?.cancel()
                it.copy(step = previousStep, customer = null, suitability = null, unsuitableConfirmed = false, customerMatches = match(""))
            } else {
                it.copy(step = previousStep, error = null)
            }
        }
    }

    private fun submit(note: String) {
        val state = _uiState.value
        val customer = state.customer ?: return
        val product = state.product ?: return
        val accountNo = state.accountNo ?: return
        if (state.step != SubscriptionStep.SIGN || !state.canProceed) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val request = NewSubscription(customer.id, product.id, accountNo, state.amount, state.unsuitableConfirmed)
            val result = useCases.subscribe(request, state.signature, note)
            _uiState.update {
                when (result) {
                    is SubscribeResult.Subscribed -> it.copy(isSaving = false, step = SubscriptionStep.DONE, result = result.subscription)
                    SubscribeResult.NeedsAssessment -> it.copy(isSaving = false, error = SubscribeError.NEEDS_ASSESSMENT)
                    SubscribeResult.UnsuitableNotConfirmed -> it.copy(isSaving = false, error = SubscribeError.UNSUITABLE)
                    SubscribeResult.InvalidAmount -> it.copy(isSaving = false, error = SubscribeError.INVALID_AMOUNT)
                    SubscribeResult.InvalidAccount -> it.copy(isSaving = false, error = SubscribeError.INVALID_ACCOUNT)
                    SubscribeResult.SignatureRequired -> it.copy(isSaving = false, error = SubscribeError.SIGNATURE)
                    SubscribeResult.NotSignedIn -> it.copy(isSaving = false, error = SubscribeError.NOT_SIGNED_IN)
                    SubscribeResult.NotFound -> it.copy(isSaving = false, error = SubscribeError.NOT_FOUND)
                    SubscribeResult.Unreachable -> it.copy(isSaving = false, error = SubscribeError.UNREACHABLE)
                }
            }
        }
    }

    private companion object {
        const val MAX_MATCHES = 6
        const val MAX_AMOUNT_DIGITS = 12
        const val AMOUNT_STEP = 100_000L
    }
}
