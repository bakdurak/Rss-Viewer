package com.example.rss_viewer.release.ui.adapters

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.example.rss_viewer.R
import com.example.rss_viewer.release.App
import com.example.rss_viewer.release.domain.ArticleDomain


class ArticleViewHolder(view: View, private val glide: RequestManager): RecyclerView.ViewHolder(view) {
//    // Hold references to default placeholders to share bitmaps between image views
//    // In flyweight fashion.
//    // Because same filter should be applied to all images not particular it's allowed
//    companion object {
//        val NO_IMAGE_PLACEHOLDER = initNoImageDrawable(R.drawable.no_image_placeholder)
//        val LOADING_ARTICLE_PLACEHOLDER = initNoImageDrawable(R.drawable.loading_article_placeholder)
//        val NETWORK_ERROR_PLACEHOLDER = initNoImageDrawable(R.drawable.network_error_placeholder)
//
//        private fun initNoImageDrawable(resId: Int): Drawable {
//            val resources = App.instance.applicationContext.resources
//            return resources.getDrawable(resId)
//        }
//    }

    companion object {
        val resources = App.instance.resources

        private fun getBitmapById(resId: Int): Bitmap {
            val placeholder = resources.getDrawable(resId)
            placeholder as BitmapDrawable
            return placeholder.bitmap
        }
    }

    private var image: ImageView = view.findViewById(R.id.item_article_image)
    private var title: TextView = view.findViewById(R.id.item_article_content)
    private lateinit var mArticle: ArticleDomain

    fun bind(article: ArticleDomain) {
        mArticle = article
        title.text = article.title

        if (article.srcBitMap != null && article.currentBitmap != null) {
            image.setImageBitmap(article.currentBitmap)
            return
        }

        if (article.hasImage()) {
            glide
                .asBitmap()
                .load(article.imageUrl)
                .placeholder(R.drawable.loading_article_placeholder)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        article.srcBitMap = getBitmapById(R.drawable.network_error_placeholder)
                        article.currentBitmap = article.srcBitMap
                        // Avoid loading image artefact
                        val oldArticleFromClosure = mArticle != article
                        if (!oldArticleFromClosure) {
                            image.setImageBitmap(article.srcBitMap)
                        }
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        article.srcBitMap = resource
                        article.currentBitmap = article.srcBitMap
                        // Avoid loading image artefact
                        val oldArticleFromClosure = mArticle != article
                        if (!oldArticleFromClosure) {
                            image.setImageBitmap(article.srcBitMap)
                        }
                    }
                })
        }
        else {
            val bitmap = getBitmapById(R.drawable.no_image_placeholder)
            article.srcBitMap = bitmap
            article.currentBitmap = bitmap
            image.setImageBitmap(bitmap)
        }
    }

    // Don't clear() resource to avoid to complicated logic in presenter.
    // Release resource only after glide's associated component is destroyed (activity, fragment)
    fun recycle() {
        image.setImageResource(R.drawable.loading_article_placeholder)
    }
}