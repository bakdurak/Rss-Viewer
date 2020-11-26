package com.example.rss_viewer.release.di.components

import com.example.rss_viewer.debug.repositories.FakeNewsRepository
import com.example.rss_viewer.release.di.modules.NetworkModule
import com.example.rss_viewer.release.di.modules.RepositoryModule
import com.example.rss_viewer.release.di.modules.UtilsModule
import com.example.rss_viewer.release.presenters.NewsPresenter
import com.example.rss_viewer.release.repositories.NewsRepositoryImpl
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [NetworkModule::class, RepositoryModule::class, UtilsModule::class])
interface AppComponent  {

    fun inject(r: NewsRepositoryImpl)

    fun inject(fr: FakeNewsRepository)

    fun inject(p: NewsPresenter)
}