package com.eunilsung.ods

interface Platform {
    /** [ANDROID] 또는 [IOS]. */
    val name: String
    val deviceId: String
    val deviceModel: String

    val isAndroid: Boolean get() = name == ANDROID

    companion object {
        const val ANDROID = "ANDROID"
        const val IOS = "IPHONE"
    }
}
