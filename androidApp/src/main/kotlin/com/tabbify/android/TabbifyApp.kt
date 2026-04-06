package com.tabbify.android

import android.app.Application
import com.tabbify.platform.appContext
import com.tabbify.di.initKoin
import org.koin.android.ext.koin.androidContext

class TabbifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        initKoin {
            androidContext(this@TabbifyApp)
        }
    }
}
