package com.eunilsung.ods.ui.product.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.usecase.ProductDetailUseCases
import com.eunilsung.ods.domain.util.suitabilityFor
import com.eunilsung.ods.domain.util.todayInSeoul
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 상품 상세 — 상품 설명과 핵심설명 항목, 상담 중 고객에게 맞는지. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModel(
    private val useCases: ProductDetailUseCases,
) : ViewModel() {

    private val productId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProductDetailUiState> = combine(
        productId.filterNotNull().flatMapLatest { useCases.observeProduct(it) },
        useCases.observeActiveCustomerId().flatMapLatest { id -> if (id == null) flowOf(null) else useCases.observeCustomer(id) },
    ) { product, customer ->
        ProductDetailUiState(
            product = product,
            customer = customer,
            suitability = if (product != null && customer != null) customer.suitabilityFor(product, todayInSeoul()) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState())

    private val _events = Channel<ProductDetailEvent>(Channel.BUFFERED)
    val events: Flow<ProductDetailEvent> = _events.receiveAsFlow()

    fun onAction(action: ProductDetailActions) {
        when (action) {
            is ProductDetailActions.OnEnter -> productId.value = action.productId
            ProductDetailActions.OnSubscribeClick -> {
                val id = productId.value ?: return
                viewModelScope.launch { _events.send(ProductDetailEvent.OpenSubscription(id, uiState.value.customer?.id)) }
            }
        }
    }
}
