package com.eunilsung.ods

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.navigator.Navigator
import com.eunilsung.ods.data.local.AndroidActivityHolder
import com.eunilsung.ods.ui.main.MainScreen
import com.eunilsung.ods.ui.theme.AppColors

/** 생체 인증 프롬프트([androidx.biometric.BiometricPrompt])가 [FragmentActivity] 를 요구해 그것을 상속한다. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppSwitcherSnapshotGuard.onCreate(this)
        AndroidActivityHolder.activity = this

        /** 폰은 세로 고정, 태블릿(최소 폭 600dp 이상)은 회전을 허용해 가로 2-pane 을 쓸 수 있게 한다. */
        val isTablet = resources.configuration.smallestScreenWidthDp >= Config.Layout.TABLET_MIN_WIDTH_DP
        requestedOrientation = if (isTablet) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AppColors.Light.Bg.toArgb(), AppColors.Dark.Bg.toArgb()),
            navigationBarStyle = SystemBarStyle.auto(AppColors.Light.Bg.toArgb(), AppColors.Dark.Bg.toArgb())
        )

        setContent {
            Navigator(MainScreen)
        }
    }

    override fun onResume() {
        super.onResume()
        AppSwitcherSnapshotGuard.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        AppSwitcherSnapshotGuard.onPause(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppSwitcherSnapshotGuard.onConfigurationChanged(this)
    }

    override fun onDestroy() {
        if (AndroidActivityHolder.activity === this) AndroidActivityHolder.activity = null
        super.onDestroy()
    }
}
