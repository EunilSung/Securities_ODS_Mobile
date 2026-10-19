package com.eunilsung.ods

import android.app.Activity
import android.app.ActivityManager
import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager
import androidx.compose.ui.graphics.toArgb
import com.eunilsung.ods.ui.theme.AppColors

/**
 * Android 최근 앱 화면에 시세·잔고·주문이 그림으로 남지 않게 한다([Config.AppSwitcherSnapshot]).
 *
 * 두 가지를 함께 쓴다.
 *  - **Android 13 이상 — 스냅샷 끄기**(`setRecentsScreenshotEnabled(false)`). 뒤로 가면 화면 대신 앱 배경색이 저장된다.
 *    그 색은 첫 화면과 같게 맞춘다([applyRecentsBackground]) — OS 가 색만 칠하므로 로고는 넣을 수 없다.
 *  - **일시정지 동안 보안 창**(onPause 에 `FLAG_SECURE`, onResume 에 해제).
 *    - 12 이하는 스냅샷만 끄는 공개 API 가 없어 이것이 유일한 방법이다(부분 보호).
 *    - 13 이상에서도 필요하다. 생체 인증·권한 요청 같은 **반투명 시스템 화면이 앱 위에** 있으면 OS 가 스냅샷 끄기를 보지 않고
 *      실제 화면을 찍는다. 그 창이 뜨는 순간 앱은 일시정지되므로 여기서 보안 창으로 바꿔 둔다.
 *
 * 생명주기 훅은 플랫폼 진입점([MainActivity])에만 둔다 — Compose 의 생명주기 효과로 옮기면 Compose 가 없는 순간을 놓친다.
 * Talk 의 같은 이름 클래스를 옮겨 왔다.
 */
internal object AppSwitcherSnapshotGuard {

    /** 무엇을 켤지. 순수 판정이라 기기 없이 확인할 수 있다. */
    data class Plan(val disableRecentsScreenshot: Boolean, val secureWhilePaused: Boolean)

    fun planFor(sdkInt: Int, enabled: Boolean, secureWhilePausedEnabled: Boolean): Plan =
        if (!enabled) Plan(disableRecentsScreenshot = false, secureWhilePaused = false)
        else Plan(
            disableRecentsScreenshot = sdkInt >= Build.VERSION_CODES.TIRAMISU,
            secureWhilePaused = secureWhilePausedEnabled,
        )

    private val plan: Plan
        get() = planFor(
            sdkInt = Build.VERSION.SDK_INT,
            enabled = Config.AppSwitcherSnapshot.IS_ENABLED,
            secureWhilePausedEnabled = Config.AppSwitcherSnapshot.IS_SECURE_WHILE_PAUSED_ENABLED,
        )

    /** 프로세스가 되살아나면 새 액티비티라 onCreate 마다 건다. */
    fun onCreate(activity: Activity) {
        applyRecentsBackground(activity)
        if (!plan.disableRecentsScreenshot) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.setRecentsScreenshotEnabled(false)
        }
    }

    /** 다크 모드가 바뀌어도 액티비티는 다시 만들어지지 않는다(manifest 의 configChanges 에 uiMode 가 있다). */
    fun onConfigurationChanged(activity: Activity) = applyRecentsBackground(activity)

    /**
     * 최근 앱 카드에 그려질 배경색을 첫 화면과 같게 맞춘다.
     *
     * **Android 13 미만에서는 하지 않는다.** 이 색은 스냅샷을 끌 때(13 이상)만 보이고, 색을 정하는
     * `TaskDescription.Builder` 도 13 에서 처음 공개됐다.
     */
    private fun applyRecentsBackground(activity: Activity) {
        if (!Config.AppSwitcherSnapshot.IS_ENABLED) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val isDark = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val background = (if (isDark) AppColors.Dark.Bg else AppColors.Light.Bg).toArgb()
        val description = ActivityManager.TaskDescription.Builder()
            .setBackgroundColor(background)
            .setStatusBarColor(background)
            .setNavigationBarColor(background)
            .build()
        runCatching { activity.setTaskDescription(description) }
    }

    fun onPause(activity: Activity) {
        if (!plan.secureWhilePaused) return
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    /** 다시 앞으로 오면 풀어 사용자 캡처를 되돌린다. */
    fun onResume(activity: Activity) {
        if (!plan.secureWhilePaused) return
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
