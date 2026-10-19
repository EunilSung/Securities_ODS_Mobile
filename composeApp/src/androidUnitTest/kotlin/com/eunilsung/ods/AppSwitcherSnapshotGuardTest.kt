package com.eunilsung.ods

import kotlin.test.Test
import kotlin.test.assertEquals

class AppSwitcherSnapshotGuardTest {
    @Test
    fun android13AndUpTurnsOffRecentsScreenshot() {
        assertEquals(
            AppSwitcherSnapshotGuard.Plan(disableRecentsScreenshot = true, secureWhilePaused = true),
            AppSwitcherSnapshotGuard.planFor(sdkInt = 33, enabled = true, secureWhilePausedEnabled = true),
        )
    }

    @Test
    fun olderAndroidOnlyHasSecureWhilePaused() {
        assertEquals(
            AppSwitcherSnapshotGuard.Plan(disableRecentsScreenshot = false, secureWhilePaused = true),
            AppSwitcherSnapshotGuard.planFor(sdkInt = 32, enabled = true, secureWhilePausedEnabled = true),
        )
    }

    @Test
    fun disabledDoesNothing() {
        assertEquals(
            AppSwitcherSnapshotGuard.Plan(disableRecentsScreenshot = false, secureWhilePaused = false),
            AppSwitcherSnapshotGuard.planFor(sdkInt = 35, enabled = false, secureWhilePausedEnabled = true),
        )
    }
}
