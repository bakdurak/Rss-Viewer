package com.example.rss_viewer.release.di.modules

import com.prof.rssparser.Parser
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class UtilsModule {

    @Provides
    @Singleton
    fun provideRssParser(): Parser {
        return Parser(null)
    }
}