package com.eunilsung.ods.domain.repository

/** 설치된 앱의 버전. 구현은 플랫폼마다 둔다. */
interface AppVersionProvider {
    /** 빌드 번호. 못 읽으면 0. */
    fun versionCode(): Long

    /** 사용자에게 보일 버전명. 못 읽으면 빈 문자열. */
    fun versionName(): String
}
