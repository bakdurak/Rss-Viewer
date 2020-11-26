package com.example.rss_viewer.release.repositories

import com.example.rss_viewer.release.App
import com.example.rss_viewer.release.domain.ArticleDomain
import com.example.rss_viewer.release.utils.ResponseState
import com.prof.rssparser.Channel
import com.prof.rssparser.Parser
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.*
import java.lang.Exception
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor() : NewsRepository {

    @Inject
    lateinit var api: Parser
    private var channelSubject: BehaviorSubject<ResponseState<List<ArticleDomain>>> = BehaviorSubject.create()
    private var cachedChannel: Channel? = null
    private var currentResourceUrl: String? = null
    private val repositoryScope = CoroutineScope(Dispatchers.Main)

    init {
        App.instance.getAppComponent().inject(this)
    }

    override fun getNewsByUrl(url: String, start: Int, count: Int) {
        val differentResource = !url.equals(currentResourceUrl)

        if (differentResource) {
            currentResourceUrl = url

            repositoryScope.launch {
                try {
                    cachedChannel = withContext(Dispatchers.IO) {
                        api.getChannel(url)
                    }

                    val domainArticles = mapChannelToArticles(start, count)
                    channelSubject.onNext(ResponseState.Success(domainArticles))
                } catch(e: Exception) {
                    channelSubject.onNext(ResponseState.Error(e))
                }
            }
        }
        else {
            val domainArticles = mapChannelToArticles(start, count)
            channelSubject.onNext(ResponseState.Success(domainArticles))
        }
    }

    private fun mapChannelToArticles(start: Int, count: Int): List<ArticleDomain> {
        val outOfBounds = start > (cachedChannel!!.articles.size - 1)
        if (outOfBounds) {
            return emptyList()
        }

        val end = if ((start + count) > cachedChannel!!.articles.size) {
            cachedChannel!!.articles.size
        } else {
            start + count
        }

        val slice = cachedChannel!!.articles.subList(start, end)
        val domainArticles = mutableListOf<ArticleDomain>()
        slice.forEach {
            domainArticles.add(ArticleDomain.toDomain(it))
        }
        return domainArticles
    }

    override fun observeNews(): Observable<ResponseState<List<ArticleDomain>>> {
        return channelSubject
    }

    override fun onDestroy() {
        repositoryScope.cancel()
    }
}