package com.example.rss_viewer.release.ui.adapters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.rss_viewer.R
import com.example.rss_viewer.release.domain.ArticleDomain


class ArticleViewHolder(view: View, private val glide: RequestManager): RecyclerView.ViewHolder(view) {
    private var image: ImageView = view.findViewById(R.id.item_article_image)
    private var title: TextView = view.findViewById(R.id.item_article_content)

    fun bind(article: ArticleDomain) {
        title.text = article.title

        if (article.srcBitMap != null && article.currentBitmap != null) {
            image.setImageBitmap(article.currentBitmap)
        }
    }

    // Don't clear() resource to avoid to complicated logic in presenter.
    // Release resource only after glide's associated component is destroyed (activity, fragment)
    fun recycle() {
        image.setImageResource(R.drawable.loading_article_placeholder)
    }
}