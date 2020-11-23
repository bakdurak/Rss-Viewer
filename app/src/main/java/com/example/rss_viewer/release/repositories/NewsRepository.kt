package com.example.rss_viewer.release.repositories

import com.prof.rssparser.Channel
import io.reactivex.Observable

interface NewsRepository {
    fun getNewsByUrl(url: String): Observable<Channel>
}