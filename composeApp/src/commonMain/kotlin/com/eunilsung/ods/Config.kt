package com.eunilsung.ods

object Config {
    /** 디버그 로그 on/off — 기본 꺼짐. */
    object Log {
        const val IS_SHOW_LOG: Boolean = false
    }

    /**
     * 서버 연동.
     *
     * 비어 있으면 서버 없이 기기 안 시연용 데이터만으로 돈다(`data/local/repository`).
     * 주소가 생기면 `di/Modules.kt` 에서 서버 구현(`data/repository`)으로 바꿔 끼운다.
     */
    object Server {
        const val BASE_URL: String = ""
        val IS_ENABLED: Boolean = BASE_URL.isNotBlank()
    }

    /** 로컬 데이터베이스(SQLDelight). [NAME] 을 바꾸면 새 DB 로 시작한다. */
    object Database {
        const val NAME: String = "OdsDatabase_v1.db"
    }

    /** 화면 구성. 이 폭 이상이면 좌우 2-pane(태블릿·폴더블)으로 그린다. */
    object Layout {
        const val TABLET_MIN_WIDTH_DP: Int = 600

        /** 현재가 화면이 이 폭 이상이면 차트와 호가를 좌우로 나란히 놓는다(태블릿 오른쪽 칸 가로). */
        const val QUOTE_SIDE_BY_SIDE_MIN_WIDTH_DP: Int = 640
    }

    /** 앱 전환기(최근 앱) 화면 가리기 — 고객 정보가 앱 전환기 썸네일에 그림으로 남지 않게 한다. */
    object AppSwitcherSnapshot {
        /**
         * 스냅샷 가리기 on/off.
         *  - Android 13 이상: `setRecentsScreenshotEnabled(false)`. 12 이하는 [IS_SECURE_WHILE_PAUSED_ENABLED].
         *  - iOS: 앱이 비활성이 되는 순간 창 맨 위에 첫 화면과 같은 가림막을 올리고 다시 활성이 되면 내린다.
         */
        const val IS_ENABLED: Boolean = true

        /**
         * Android 에서 **앱이 가려져 일시정지된 동안만** 캡처를 막는다(onPause 에 `FLAG_SECURE`, onResume 에 해제).
         *
         * 12 이하는 스냅샷만 끄는 공개 API 가 없어 쓰는 차선책이다. 13 이상에서도 권한 요청·생체 인증 창처럼 반투명 시스템
         * 화면이 앱 위에 떠 있으면 OS 가 실제 화면을 찍으므로 그 경우를 막는다.
         */
        const val IS_SECURE_WHILE_PAUSED_ENABLED: Boolean = true
    }
}
