package com.example.rss_viewer.release.di.modules

import com.example.rss_viewer.release.repositories.NewsRepository
import com.example.rss_viewer.release.repositories.NewsRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
abstract class RepositoryModule {

    @Binds
    abstract fun bindsNewsRepository(r: NewsRepositoryImpl): NewsRepository
}