package com.example.rss_viewer.debug.repositories

import com.example.rss_viewer.release.repositories.NewsRepository
import com.prof.rssparser.Channel
import io.reactivex.Observable

class FakeNewsRepository : NewsRepository {

    override fun getNewsByUrl(url: String): Observable<Channel> {
        TODO("Not yet implemented")
    }
}