package com.eunilsung.ods.ui.consult

sealed interface ConsultPaneActions {
    /** 상담 종료를 눌렀다. 상담 칸을 비운다. */
    data object OnEndConsult : ConsultPaneActions
}
