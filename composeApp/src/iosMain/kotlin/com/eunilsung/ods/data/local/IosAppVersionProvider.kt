package com.eunilsung.ods.data.local

import com.eunilsung.ods.domain.repository.AppVersionProvider
import platform.Foundation.NSBundle

/** iOS — 앱 번들의 Info.plist 에서 버전을 읽는다(Config.xcconfig 의 MARKETING_VERSION·CURRENT_PROJECT_VERSION). */
class IosAppVersionProvider : AppVersionProvider {
    override fun versionCode(): Long =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)?.toLongOrNull() ?: 0L

    override fun versionName(): String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String).orEmpty()
}
