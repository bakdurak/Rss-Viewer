package com.example.rss_viewer.release.presenters

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.webkit.URLUtil
import androidx.core.math.MathUtils
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.rss_viewer.R
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
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@InjectViewState
class NewsPresenter : MvpPresenter<NewsView>() {

    @Inject
    lateinit var repository: NewsRepository
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var currentResourceUrl: String? = null
    private var currentOffset: Int = 0
    private val filter = Filter()
    @Volatile
    private var currentFilter: NewsPresenter.Filter? = null
    private lateinit var glide: RequestManager

    init {
        App.instance.getAppComponent().inject(this)
        observeNews()
    }

    fun onScroll(url: String, itemsCount: Int, glide: RequestManager) {
        this.glide = glide
        // Flush state
        if (!currentResourceUrl.equals(url)) {
            currentResourceUrl = url
            currentOffset = 0
            currentFilter = null
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
        currentFilter = type

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
            .map { viewState.updateImages(it, firstVisibleItem, lastVisible) }
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
            .map { viewState.updateImages(it.images, it.from, it.to) }
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
            .doFinally { viewState.toggleButtons(true) }
            .subscribe({
                viewState.updateImages(it.images, it.from, it.to)
            }, {
                viewState.showApplyFiltersError()
            })
        disposables.add(disposable)
    }

    private fun observeNews() {
        val disposable = repository.observeNews()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({response ->
                when(response) {
                    is ResponseState.Success -> {
                        val articleCnt = response.data.size
                        val repositoryDrained = articleCnt == 0
                        if (repositoryDrained) {
                            viewState.appendArticles(response.data)
                        }
                        else {
                            val disposable = downloadImages(response.data)
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.computation())
                                .map {
                                    val shouldApplyFilter = currentFilter != null
                                    if (shouldApplyFilter) {
                                        return@map ArticleMeta(processImage(it), true)
                                    }
                                    else {
                                        return@map ArticleMeta(it, false)
                                    }
                                }
                                .buffer(articleCnt)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({articles ->
                                    val shouldApplyFilter = currentFilter != null
                                    val unprocessedArticles = articles.filter { !it.withFilters }
                                    // Attention: consider case when UI thread busy with something
                                    // while computation thread(above) does it's job
                                    // then UI thread cannot set currentFilter variable on time
                                    // therefore some images will not be processed with filters.
                                    // Because at the current time we are in UI thread
                                    // then now we will definitely process images
                                    // if some were not processed because of above reason
                                    if (shouldApplyFilter && unprocessedArticles.isNotEmpty()) {
                                        val disposable = Observable.create<ArticleDomain> {emitter ->
                                            unprocessedArticles.forEach {
                                                emitter.onNext(processImage(it.article))
                                            }
                                        }
                                            .subscribeOn(Schedulers.computation())
                                            .buffer(unprocessedArticles.size)
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({
                                                viewState.appendArticles(it)
                                            }, {
                                                viewState.showLoadingContentError()
                                            })
                                        disposables.add(disposable)
                                    }
                                    else {
                                        viewState.appendArticles(ArticleMeta.toArticleDomain(articles))
                                    }
                                }, {
                                    viewState.showLoadingContentError()
                                })
                            disposables.add(disposable)
                        }
                    }
                    is ResponseState.Error -> viewState.showLoadingContentError()
                }
            }, {
                viewState.showLoadingContentError()
            })
        disposables.add(disposable)
    }

    private fun downloadImages(articles: List<ArticleDomain>): Observable<ArticleDomain> {
        return Observable.create {emitter ->
            articles.forEach {article ->
                if (article.hasImage()) {
                    glide
                        .asBitmap()
                        .load(article.imageUrl)
                        .listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Bitmap>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                // TODO: Get rid of android dependencies
                                val placeholderDrawable = App
                                    .instance
                                    .resources
                                    .getDrawable(R.drawable.network_error_placeholder)
                                placeholderDrawable as BitmapDrawable
                                article.srcBitMap = placeholderDrawable.bitmap
                                article.currentBitmap = article.srcBitMap
                                emitter.onNext(article)
                                return false
                            }

                            override fun onResourceReady(
                                resource: Bitmap?,
                                model: Any?,
                                target: Target<Bitmap>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                article.srcBitMap = resource
                                article.currentBitmap = article.srcBitMap
                                emitter.onNext(article)
                                return false
                            }
                        })
                        .submit()
                }
                else {
                    // TODO: Get rid of android dependencies
                    val placeholderDrawable = App
                        .instance
                        .resources
                        .getDrawable(R.drawable.no_image_placeholder)
                    placeholderDrawable as BitmapDrawable
                    article.srcBitMap = placeholderDrawable.bitmap
                    article.currentBitmap = article.srcBitMap
                    emitter.onNext(article)
                }
            }
        }
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

    private fun processImage(article: ArticleDomain): ArticleDomain {
        val oldBitmap = article.srcBitMap
        val newBitmap = oldBitmap!!.copy(oldBitmap.config, true)
        val processedBitmap = filter.processFilter(newBitmap)
        article.currentBitmap = processedBitmap
        return article
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

    private class ArticleMeta(val article: ArticleDomain, val withFilters: Boolean) {
        companion object {
            fun toArticleDomain(articles: List<ArticleMeta>): List<ArticleDomain> {
                val refinedArticles = mutableListOf<ArticleDomain>()
                articles.forEach {
                    refinedArticles.add(it.article)
                }
                return refinedArticles
            }
        }
    }
}