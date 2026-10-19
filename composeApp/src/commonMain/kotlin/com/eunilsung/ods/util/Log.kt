package com.eunilsung.ods.util

import com.eunilsung.ods.Config

object Log {
    fun message(message: String) {
        if (Config.Log.IS_SHOW_LOG) println("### [Debug] $message")
    }
}
