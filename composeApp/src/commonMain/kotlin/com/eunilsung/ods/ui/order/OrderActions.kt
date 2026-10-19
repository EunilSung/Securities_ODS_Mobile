package com.eunilsung.ods.ui.order

import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType

sealed interface OrderActions {
    /** 현재가에서 매수·매도를 눌러 주문 화면이 열렸다. 입력을 비우고 [side]·[price] 로 시작한다. */
    data class OnEnter(val code: String, val side: OrderSide, val price: Long?) : OrderActions

    /** 주문 탭으로 열렸다. 마지막으로 본 종목(없으면 관심종목 첫 종목)을 연다. */
    data object OnEnterTab : OrderActions

    /** 넓은 태블릿의 주문 칸 — 가운데 칸에 열린 종목이 바뀌었다. 열린 종목이 없으면 null. */
    data class OnPaneCodeChange(val code: String?) : OrderActions

    /** 주문 탭 위쪽 종목 줄에서 종목을 골랐다. */
    data class OnSelectStock(val code: String) : OrderActions

    data class OnSideChange(val side: OrderSide) : OrderActions
    data class OnTypeChange(val type: OrderType) : OrderActions

    data class OnPriceChange(val text: String) : OrderActions

    /** 가격 −/+ — 한 호가씩 움직인다. */
    data class OnPriceStep(val up: Boolean) : OrderActions

    /** 가격을 현재가로 맞춘다. */
    data object OnUseCurrentPrice : OrderActions

    /** 호가창에서 가격을 눌렀다. 지정가로 바꾸고 그 가격을 넣는다. */
    data class OnPriceSelected(val code: String, val price: Long) : OrderActions

    data class OnQuantityChange(val text: String) : OrderActions
    data class OnQuantityStep(val up: Boolean) : OrderActions

    /** 최대 수량의 [percent]% 를 넣는다(10·25·50·100). */
    data class OnQuantityPercent(val percent: Int) : OrderActions

    /** 주문 버튼을 눌렀다. 확인 화면을 띄운다. */
    data object OnSubmitClick : OrderActions

    /** 확인 화면에서 주문 버튼을 눌렀다. 인증 창을 띄운다. */
    data object OnConfirm : OrderActions

    /** 인증됐다. 주문을 보낸다. */
    data object OnAuthorized : OrderActions

    /** 인증 창을 닫았다. 확인 화면으로 돌아간다. */
    data object OnAuthCanceled : OrderActions

    /** 확인·결과 화면을 닫는다. 보내는 중에는 닫히지 않는다. */
    data object OnDismissConfirm : OrderActions
}
