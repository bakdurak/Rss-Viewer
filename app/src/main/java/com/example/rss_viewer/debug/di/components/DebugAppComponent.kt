package com.example.rss_viewer.debug.di.components

import com.example.rss_viewer.debug.di.modules.FakeRepositoryModule
import com.example.rss_viewer.release.di.components.AppComponent
import dagger.Component
import javax.inject.Singleton

@Component(modules = [FakeRepositoryModule::class])
@Singleton
interface DebugAppComponent : AppComponent {}