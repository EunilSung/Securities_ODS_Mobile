package com.eunilsung.ods.ui.product.detail

sealed interface ProductDetailActions {
    data class OnEnter(val productId: String) : ProductDetailActions

    /** 가입 진행을 눌렀다. 상담 중 고객이 있으면 그 고객으로 시작한다. */
    data object OnSubscribeClick : ProductDetailActions
}

/** 상품 상세가 한 번씩 처리할 일. */
sealed interface ProductDetailEvent {
    data class OpenSubscription(val productId: String, val customerId: String?) : ProductDetailEvent
}
