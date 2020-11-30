package com.example.rss_viewer.release.ui.activities

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.example.rss_viewer.R
import com.example.rss_viewer.release.domain.ArticleDomain
import com.example.rss_viewer.release.presenters.NewsPresenter
import com.example.rss_viewer.release.ui.adapters.ArticleAdapter
import com.example.rss_viewer.release.ui.views.NewsView
import com.example.rss_viewer.release.utils.Helpers
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import moxy.MvpAppCompatActivity
import moxy.presenter.InjectPresenter
import java.util.concurrent.TimeUnit


class NewsActivity : MvpAppCompatActivity(), NewsView {

    companion object {
        private const val DEFAULT_RSS_URL = "https://lenta.ru/rss/articles"
        private const val LOAD_ITEMS_CNT = 8
        private const val DISABLED_BUTTON_ALPHA = 0.65f
        private const val LOAD_ITEMS_THRESHOLD = 15

        init {
            System.loadLibrary("NativeImageProcessor")
        }
    }

    @InjectPresenter
    lateinit var presenter: NewsPresenter
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArticleAdapter
    private lateinit var rootView: View
    private lateinit var brightnessButton: Button
    private lateinit var colorOverlayButton: Button
    private lateinit var saturationButton: Button
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var rssUrlEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var glide: RequestManager
    private var scrollDown = true
    private var currentRssUrl = DEFAULT_RSS_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar!!.hide()

        setContentView(R.layout.activity_news)

        glide = Glide.with(this)

        rootView = findViewById(android.R.id.content)
        brightnessButton = findViewById(R.id.activity_news_button_brightness)
        colorOverlayButton = findViewById(R.id.activity_news_button_color_overlay)
        saturationButton = findViewById(R.id.activity_news_button_saturation)
        rssUrlEditText = findViewById(R.id.activity_news_rss_resource_edit_text)
        submitButton = findViewById(R.id.activity_news_button_submit)

        brightnessButton.setOnClickListener(this::onBrightnessClick)
        colorOverlayButton.setOnClickListener(this::onColorOverlayClick)
        saturationButton.setOnClickListener(this::onSaturationClick)
        submitButton.setOnClickListener(this::onSubmitClick)

        recyclerView = findViewById(R.id.activity_news_recycler_view)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                onScrolled(dx, dy)
            }
        })
        recyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        adapter = ArticleAdapter(
            LOAD_ITEMS_THRESHOLD,
            this::onScrollCallback,
            this::stopLoadConditionCallback,
            glide
        )
        recyclerView.adapter = adapter

        presenter.onScroll(currentRssUrl, LOAD_ITEMS_CNT, glide)
    }

    override fun appendArticles(articles: List<ArticleDomain>) {
        adapter.appendItems(articles)
    }

    private fun onScrollCallback() {
        presenter.onScroll(currentRssUrl, LOAD_ITEMS_CNT, glide)
    }

    private fun stopLoadConditionCallback(nextItems: List<ArticleDomain>): Boolean = nextItems.isNotEmpty()

    // TODO: For better visible window bounds prediction we could use exponential smoothing
    //  for scroll velocity vector to get precise bounds for images to be processed first
    // Scroll direction for bitmap processing order prediction
    private fun onScrolled(dx: Int, dy: Int) {
        if (dy > 0) {
            scrollDown = false
        }
        else {
            scrollDown = true
        }
    }

    private fun onSubmitClick(v: View) {
        val url = rssUrlEditText.text.toString()
        if (url.isEmpty() || Helpers.toUniformUrl(url).equals(currentRssUrl)) {
            return
        }
        presenter.onSubmitUrl(url)
    }

    override fun setCurrentUrl(url: String) {
        currentRssUrl = url

        // Recycle old items
        recyclerView.adapter = null

        adapter = ArticleAdapter(
            LOAD_ITEMS_THRESHOLD,
            this::onScrollCallback,
            this::stopLoadConditionCallback,
            glide
        )
        recyclerView.adapter = adapter

        presenter.onScroll(currentRssUrl, LOAD_ITEMS_CNT, glide)
    }

    override fun showWrongUrlError() {
        Snackbar.make(
            rootView,
            resources.getString(R.string.invalid_url),
            Snackbar.LENGTH_LONG)
            .show()
    }

    private fun onBrightnessClick(v: View) {
        presenter.applyFilter(
            NewsPresenter.Filter.BRIGHTNESS,
            layoutManager.findFirstVisibleItemPosition(),
            layoutManager.findLastVisibleItemPosition(),
            scrollDown,
            adapter.items
        )
    }

    private fun onColorOverlayClick(v: View) {
        presenter.applyFilter(
            NewsPresenter.Filter.COLOR_OVERLAY,
            layoutManager.findFirstVisibleItemPosition(),
            layoutManager.findLastVisibleItemPosition(),
            scrollDown,
            adapter.items
        )
    }

    private fun onSaturationClick(v: View) {
        presenter.applyFilter(
            NewsPresenter.Filter.SATURATION,
            layoutManager.findFirstVisibleItemPosition(),
            layoutManager.findLastVisibleItemPosition(),
            scrollDown,
            adapter.items
        )
    }

    override fun toggleButtons(enabled: Boolean) {
        brightnessButton.isEnabled = enabled
        colorOverlayButton.isEnabled = enabled
        saturationButton.isEnabled = enabled

        val alpha = if (enabled) 1f else DISABLED_BUTTON_ALPHA
        brightnessButton.alpha = alpha
        colorOverlayButton.alpha = alpha
        saturationButton.alpha = alpha
    }

    override fun updateImages(positionToBitmap: Map<Int, Bitmap>, from: Int, to: Int) {
        adapter.updateImages(positionToBitmap, from, to)
    }

    override fun showLoadingContentError() {
        Snackbar.make(
            rootView,
            resources.getString(R.string.loading_content_error),
            Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showApplyFiltersError() {
        Snackbar.make(
            rootView,
            resources.getString(R.string.apply_filers_error),
            Snackbar.LENGTH_LONG)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()

        recyclerView.adapter = null
    }
}