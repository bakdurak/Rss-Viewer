package com.example.rss_viewer.debug.di.components

import com.example.rss_viewer.debug.di.modules.FakeRepositoryModule
import com.example.rss_viewer.debug.repositories.FakeNewsRepository
import com.example.rss_viewer.release.di.components.AppComponent
import com.example.rss_viewer.release.di.modules.NetworkModule
import com.example.rss_viewer.release.di.modules.RepositoryModule
import com.example.rss_viewer.release.di.modules.UtilsModule
import dagger.Component
import javax.inject.Singleton

@Component(modules = [FakeRepositoryModule::class, UtilsModule::class, NetworkModule::class])
@Singleton
interface DebugAppComponent : AppComponent {}
