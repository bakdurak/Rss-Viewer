package com.example.rss_viewer.release.di.modules

import com.example.rss_viewer.release.repositories.remote.api.NewsResourcesApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): NewsResourcesApi {
        val gson: Gson = GsonBuilder().serializeNulls().create()

        return Retrofit.Builder()
            .baseUrl(NewsResourcesApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(NewsResourcesApi::class.java)
    }
}