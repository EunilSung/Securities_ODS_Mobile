package com.eunilsung.ods.data.local

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey

/**
 * 경로를 iCloud·Finder 백업에서 뺀다. 디렉터리에 걸면 안의 파일 전부가 빠진다.
 *
 * DB 는 Keychain 키(`ThisDeviceOnly`, 백업에 따라가지 않는다)로 잠겨 있어, DB 만 새 기기로 복원되면 풀 수 없다.
 * 파일 자체를 백업에서 빼 그 상태를 만들지 않는다. SQLite 는 `-wal`·`-journal` 을 지웠다 다시 만들므로
 * 파일이 아니라 디렉터리에 건다. Android 는 `allowBackup=false` 와 `dataExtractionRules` 가 같은 일을 한다.
 *
 * @return 걸었으면 true. 실패해도 앱은 계속 돈다(백업 제외는 보조 수단이다).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun excludeFromBackup(path: String): Boolean {
    if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return false
    return NSURL.fileURLWithPath(path).setResourceValue(NSNumber(bool = true), forKey = NSURLIsExcludedFromBackupKey, error = null)
}
