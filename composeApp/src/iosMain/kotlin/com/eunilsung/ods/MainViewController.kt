package com.eunilsung.ods

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import cafe.adriel.voyager.navigator.Navigator
import com.eunilsung.ods.di.appModule
import com.eunilsung.ods.ui.main.MainScreen
import org.koin.core.context.startKoin
import org.koin.core.error.KoinApplicationAlreadyStartedException

@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController() = ComposeUIViewController(
    configure = {
        onFocusBehavior = OnFocusBehavior.DoNothing
        registerAppSwitcherCover()
        try {
            startKoin {
                modules(appModule, platformModule)
            }
        } catch (_: KoinApplicationAlreadyStartedException) {
        }
    }
) {
    Navigator(MainScreen)
}
