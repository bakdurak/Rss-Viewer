package com.example.rss_viewer.release.repositories

import com.example.rss_viewer.release.domain.ArticleDomain
import com.example.rss_viewer.release.utils.ResponseState
import io.reactivex.Observable

interface NewsRepository : Repository {
    fun getNewsByUrl(url: String, start: Int, count: Int)

    fun observeNews(): Observable<ResponseState<List<ArticleDomain>>>
}