package com.example.rss_viewer.release.domain

import android.graphics.Bitmap
import com.prof.rssparser.Article

class ArticleDomain(val imageUrl: String?, val title: String) {
    companion object {
        fun toDomain(article: Article) = ArticleDomain(article.image, article.title!!)
    }

    var srcBitMap: Bitmap? = null
    var currentBitmap: Bitmap? = null

    fun hasImage(): Boolean = imageUrl != null
}