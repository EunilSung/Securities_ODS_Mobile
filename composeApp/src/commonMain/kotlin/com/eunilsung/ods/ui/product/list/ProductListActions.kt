package com.eunilsung.ods.ui.product.list

import com.eunilsung.ods.domain.model.ProductType

sealed interface ProductListActions {
    /** 종류로 거른다. null 이면 전체. */
    data class OnTypeFilter(val type: ProductType?) : ProductListActions

    data class OnProductClick(val id: String) : ProductListActions
}

/** 상품 목록이 한 번씩 처리할 일. */
sealed interface ProductListEvent {
    data class OpenDetail(val id: String) : ProductListEvent
}
