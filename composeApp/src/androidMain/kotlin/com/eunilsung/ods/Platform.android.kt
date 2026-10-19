package com.eunilsung.ods

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.eunilsung.ods.data.local.AndroidAppVersionProvider
import com.eunilsung.ods.data.local.AndroidBiometricAuthenticator
import com.eunilsung.ods.data.local.DatabaseDriverFactory
import com.eunilsung.ods.domain.repository.AppVersionProvider
import com.eunilsung.ods.domain.repository.BiometricAuthenticator
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import org.koin.dsl.module

class AndroidPlatform(private val context: Context) : Platform {
    override val name: String = Platform.ANDROID
    override val deviceId: String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    override val deviceModel: String = Build.MODEL
}

val platformModule = module {
    single<Platform> { AndroidPlatform(get()) }
    single { DatabaseDriverFactory(get()) }
    single { HttpClient(OkHttp) { install(WebSockets) } }
    single<BiometricAuthenticator> { AndroidBiometricAuthenticator(get()) }
    single<AppVersionProvider> { AndroidAppVersionProvider(get()) }
}
