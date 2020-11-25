package com.example.rss_viewer.release.ui.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.rss_viewer.R
import com.example.rss_viewer.release.domain.ArticleDomain

class ArticleAdapter(
    threshold: Int,
    loaderCallback: Loader,
    stopLoadConditionCallback: StopLoadCondition<ArticleDomain>,
    private val glide: RequestManager)
    : LazyLoadAdapter<ArticleDomain>(
    threshold,
    loaderCallback,
    stopLoadConditionCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val item = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return ArticleViewHolder(item, glide)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        holder as ArticleViewHolder
        holder.bind(items[position])
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        holder as ArticleViewHolder
        holder.recycle()
    }

    fun updateImages(positionToBitmap: Map<Int, Bitmap>, from: Int, to: Int): Boolean {
        if (positionToBitmap.isEmpty()) {
            return false
        }

        for ((k, v) in positionToBitmap) {
            items[k].currentBitmap = v
        }

        val reverseOrder = from > to
        var count: Int
        count = if (reverseOrder) {
            from - to + 1
        } else {
            to - from + 1
        }
        val start = if (from < to) from else to
        notifyItemRangeChanged(start, count)
        return true
    }
}