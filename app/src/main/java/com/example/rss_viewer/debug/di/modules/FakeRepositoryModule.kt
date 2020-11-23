package com.example.rss_viewer.debug.di.modules

import com.example.rss_viewer.debug.repositories.FakeNewsRepository
import com.example.rss_viewer.release.repositories.NewsRepository
import dagger.Binds
import dagger.Module

@Module
abstract class FakeRepositoryModule {

    @Binds
    abstract fun bindsFakeNewsRepository(r: FakeNewsRepository): NewsRepository
}