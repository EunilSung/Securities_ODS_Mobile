package com.eunilsung.ods.ui.product.list

import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.ProductType

/** 상품 목록의 상태. */
data class ProductListUiState(
    /** 고른 종류. null 이면 전체. */
    val typeFilter: ProductType? = null,
    /** 판매 중인 상품 가운데 있는 종류만(필터 단추). */
    val types: List<ProductType> = emptyList(),
    val products: List<Product> = emptyList(),
)
