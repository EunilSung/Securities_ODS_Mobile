package com.eunilsung.ods.ui.product.detail

import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.Suitability

/** 상품 상세의 상태. */
data class ProductDetailUiState(
    val product: Product? = null,
    /** 상담 중인 고객. 없으면 적합성을 보이지 않는다. */
    val customer: Customer? = null,
    /** 상담 중 고객에게 이 상품이 맞는지. */
    val suitability: Suitability? = null,
)
