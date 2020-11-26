package com.example.rss_viewer.debug.repositories

import com.example.rss_viewer.release.domain.ArticleDomain
import com.example.rss_viewer.release.repositories.NewsRepository
import com.example.rss_viewer.release.utils.ResponseState
import io.reactivex.Observable
import javax.inject.Inject

class FakeNewsRepository @Inject constructor() : NewsRepository {

    override fun getNewsByUrl(url: String, start: Int, count: Int) {
        TODO("Not yet implemented")
    }

    override fun observeNews(): Observable<ResponseState<List<ArticleDomain>>> {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        TODO("Not yet implemented")
    }
}