package com.eunilsung.ods.ui.product.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.ProductType
import com.eunilsung.ods.domain.usecase.ProductListUseCases
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 상품 목록 — 판매 상품을 종류로 거르고, 누르면 상세를 연다. */
class ProductListViewModel(
    private val useCases: ProductListUseCases,
) : ViewModel() {

    private val typeFilter = MutableStateFlow<ProductType?>(null)

    val uiState: StateFlow<ProductListUiState> = combine(useCases.observeProducts(), typeFilter) { products, filter ->
        ProductListUiState(
            typeFilter = filter,
            types = ProductType.entries.filter { type -> products.any { it.type == type } },
            products = if (filter == null) products else products.filter { it.type == filter },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductListUiState())

    private val _events = Channel<ProductListEvent>(Channel.BUFFERED)
    val events: Flow<ProductListEvent> = _events.receiveAsFlow()

    fun onAction(action: ProductListActions) {
        when (action) {
            is ProductListActions.OnTypeFilter -> typeFilter.value = action.type
            is ProductListActions.OnProductClick -> viewModelScope.launch { _events.send(ProductListEvent.OpenDetail(action.id)) }
        }
    }
}
