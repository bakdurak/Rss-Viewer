package com.example.rss_viewer.release.ui.views

import android.graphics.Bitmap
import com.example.rss_viewer.release.domain.ArticleDomain
import com.example.rss_viewer.release.presenters.NewsPresenter
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.AddToEndStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType

interface NewsView : MvpView {
    // TODO: Flush queue for the method when new url is passed (custom strategy)
    @StateStrategyType(value = AddToEndStrategy::class)
    fun appendArticles(articles: List<ArticleDomain>)

    @StateStrategyType(value = OneExecutionStateStrategy::class)
    fun showLoadingContentError()

    // TODO: Flush queue for the method when new filter is passed (custom strategy)
    @StateStrategyType(value = AddToEndStrategy::class)
    fun updateImages(positionToBitmap: Map<Int, Bitmap>, from: Int, to: Int)

    @StateStrategyType(value = AddToEndSingleStrategy::class)
    fun toggleButtons(enabled: Boolean)

    @StateStrategyType(value = AddToEndSingleStrategy::class)
    fun setCurrentUrl(url: String)

    @StateStrategyType(value = OneExecutionStateStrategy::class)
    fun showWrongUrlError()

    @StateStrategyType(value = OneExecutionStateStrategy::class)
    fun showApplyFiltersError()
}