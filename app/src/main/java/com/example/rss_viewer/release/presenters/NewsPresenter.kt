package com.example.rss_viewer.release.presenters

import android.graphics.Bitmap
import android.webkit.URLUtil
import androidx.core.math.MathUtils
import com.example.rss_viewer.release.App
import com.example.rss_viewer.release.domain.ArticleDomain
import com.example.rss_viewer.release.repositories.NewsRepository
import com.example.rss_viewer.release.ui.views.NewsView
import com.example.rss_viewer.release.utils.Helpers
import com.example.rss_viewer.release.utils.ResponseState
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ColorOverlaySubFilter
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubFilter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import moxy.InjectViewState
import moxy.MvpPresenter
import javax.inject.Inject

@InjectViewState
class NewsPresenter : MvpPresenter<NewsView>() {

    @Inject
    lateinit var repository: NewsRepository
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var currentResourceUrl: String? = null
    private var currentOffset: Int = 0
    private val filter = Filter()

    init {
        App.instance.getAppComponent().inject(this)

        val disposable = repository.observeNews()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                when(it) {
                    is ResponseState.Success -> viewState.appendArticles(it.data)
                    is ResponseState.Error -> viewState.showLoadingContentError()
                }
            }, {
                viewState.showLoadingContentError()
            })
        disposables.add(disposable)
    }

    fun onScroll(url: String, itemsCount: Int) {
        // Flush offset
        if (!currentResourceUrl.equals(url)) {
            currentResourceUrl = url
            currentOffset = 0
        }

        repository.getNewsByUrl(url, currentOffset, itemsCount)
        currentOffset += itemsCount
    }

    fun onSubmitUrl(url: String) {
        var formattedUrl = Helpers.toUniformUrl(url)

        if (URLUtil.isHttpUrl(formattedUrl) || URLUtil.isHttpsUrl(formattedUrl)) {
            viewState.setCurrentUrl(formattedUrl)
        }
        else {
            viewState.showWrongUrlError()
        }
    }

    fun applyFilter(
        type: NewsPresenter.Filter,
        firstVisibleItem: Int,
        lastVisible: Int,
        scrollDown: Boolean,
        articles: List<ArticleDomain>
    ) {
        viewState.toggleButtons(false)
        setUpFilter(type)

        val observable: Observable<Map<Int, Bitmap>> = Observable.create {
            val filteredImages = HashMap<Int, Bitmap>()
            for (i in firstVisibleItem..lastVisible) {
                processImage(i, articles, filteredImages)
            }
            it.onNext(filteredImages)
            it.onComplete()
        }

        // First process visible items
        val disposable = observable
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            // Notify view
            .map {
                viewState.updateImages(it, firstVisibleItem, lastVisible)
            }
            .observeOn(Schedulers.computation())
            // Then according to scroll direction
            .map {
                return@map processOutOfWindowImages(
                    scrollDown,
                    firstVisibleItem,
                    lastVisible,
                    articles,
                    false
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            // Notify view
            .map {
                viewState.updateImages(it.images, it.from, it.to)
            }
            .observeOn(Schedulers.computation())
            // Then rest images
            .map {
                return@map processOutOfWindowImages(
                    scrollDown,
                    firstVisibleItem,
                    lastVisible,
                    articles,
                    true
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                viewState.toggleButtons(true)
            }
            .subscribe({
                viewState.updateImages(it.images, it.from, it.to)
            }, {
                viewState.showApplyFiltersError()
            })
        disposables.add(disposable)
    }

    private fun processImage(
        position: Int,
        articles: List<ArticleDomain>,
        dst: MutableMap<Int, Bitmap>
    ) {
        val oldBitmap = articles[position].srcBitMap
        oldBitmap?.let {
            val newBitmap = oldBitmap.copy(oldBitmap.config, true)
            val processedBitmap = filter.processFilter(newBitmap)
            dst[position] = processedBitmap
        }
    }

    private fun processOutOfWindowImages(
        scrollDown: Boolean,
        firstVisibleItem: Int,
        lastVisible: Int,
        articles: List<ArticleDomain>,
        restImages: Boolean
    ): ProcessedImageMeta {
        val filteredImages = HashMap<Int, Bitmap>()
        val begin: Int
        val end: Int
        val downTo = if (restImages) !scrollDown else scrollDown
        if (downTo) {
            begin = MathUtils.clamp(lastVisible + 1, 0, articles.size - 1)
            end = articles.size - 1
            for (i in begin..end) {
                processImage(i, articles, filteredImages)
            }
        }
        else {
            begin = firstVisibleItem - 1
            end = 0
            for (i in begin downTo end) {
                processImage(i, articles, filteredImages)
            }
        }
        return ProcessedImageMeta(filteredImages, begin, end)
    }

    private fun setUpFilter(appliedFilter: Filter) {
        filter.clearSubFilters()
        when (appliedFilter) {
            Filter.BRIGHTNESS -> filter.addSubFilter(BrightnessSubFilter(95))
            Filter.COLOR_OVERLAY -> filter.addSubFilter(ColorOverlaySubFilter(100, .1f, .9f, .1f))
            Filter.SATURATION -> filter.addSubFilter(SaturationSubFilter(5.3f))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
        repository.onDestroy()
    }

    enum class Filter {
        BRIGHTNESS,
        COLOR_OVERLAY,
        SATURATION
    }

    private class ProcessedImageMeta(val images: Map<Int, Bitmap>, val from: Int, val to: Int)
}