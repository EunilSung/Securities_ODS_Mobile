package com.eunilsung.ods.ui.openorder

import com.eunilsung.ods.domain.model.Order

sealed interface OpenOrdersActions {
    /** 정정을 눌렀다. 정정 창을 연다. */
    data class OnAmendClick(val order: Order) : OpenOrdersActions

    /** 취소를 눌렀다. 취소 확인 창을 연다. */
    data class OnCancelClick(val order: Order) : OpenOrdersActions

    data class OnAmendPriceChange(val text: String) : OpenOrdersActions

    /** 정정 가격 −/+ — 한 호가씩 움직인다. */
    data class OnAmendPriceStep(val up: Boolean) : OpenOrdersActions

    /** 정정 가격을 현재가로 맞춘다. */
    data object OnAmendUseCurrentPrice : OpenOrdersActions

    /** 정정·취소 창에서 보내기를 눌렀다. 인증 창을 띄운다. */
    data object OnConfirm : OpenOrdersActions

    /** 인증됐다. 정정·취소를 보낸다. */
    data object OnAuthorized : OpenOrdersActions

    /** 인증 창을 닫았다. 정정·취소 창으로 돌아간다. */
    data object OnAuthCanceled : OpenOrdersActions

    /** 창을 닫는다. 보내는 중에는 닫히지 않는다. */
    data object OnDismiss : OpenOrdersActions
}
