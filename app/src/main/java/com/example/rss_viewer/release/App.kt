package com.example.rss_viewer.release

import android.app.Application
import com.example.rss_viewer.BuildConfig
import com.example.rss_viewer.debug.di.components.DaggerDebugAppComponent
import com.example.rss_viewer.release.di.components.AppComponent
import com.example.rss_viewer.release.di.components.DaggerAppComponent

class App : Application() {
    companion object {
        lateinit var instance: App
    }

    private lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        instance = this
        appComponent = buildAppComponent()
    }

    private fun buildAppComponent(): AppComponent {
        val env = BuildConfig.ENV
        return if (env.equals("debug")) {
            DaggerDebugAppComponent.builder()
                .build()
        } else {
            DaggerAppComponent.builder()
                .build()
        }
    }
}