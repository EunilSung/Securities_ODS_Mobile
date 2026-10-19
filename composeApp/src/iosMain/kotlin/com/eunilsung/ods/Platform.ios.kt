package com.eunilsung.ods

import com.eunilsung.ods.data.local.DatabaseDriverFactory
import com.eunilsung.ods.data.local.IosAppVersionProvider
import com.eunilsung.ods.data.local.IosBiometricAuthenticator
import com.eunilsung.ods.domain.repository.AppVersionProvider
import com.eunilsung.ods.domain.repository.BiometricAuthenticator
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets
import org.koin.dsl.module
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = Platform.IOS
    override val deviceId: String = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown_ios"
    override val deviceModel: String = UIDevice.currentDevice.model
}

val platformModule = module {
    single<Platform> { IOSPlatform() }
    single { DatabaseDriverFactory() }
    single { HttpClient(Darwin) { install(WebSockets) } }
    single<BiometricAuthenticator> { IosBiometricAuthenticator() }
    single<AppVersionProvider> { IosAppVersionProvider() }
}
