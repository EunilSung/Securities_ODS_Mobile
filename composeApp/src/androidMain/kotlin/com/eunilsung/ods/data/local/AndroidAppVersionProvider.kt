package com.eunilsung.ods.data.local

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.eunilsung.ods.domain.repository.AppVersionProvider

/** Android — 패키지 정보에서 버전을 읽는다. */
class AndroidAppVersionProvider(private val context: Context) : AppVersionProvider {

    override fun versionCode(): Long = runCatching {
        val info = packageInfo()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    }.getOrDefault(0L)

    override fun versionName(): String = runCatching { packageInfo().versionName.orEmpty() }.getOrDefault("")

    private fun packageInfo(): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
}
