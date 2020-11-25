package com.example.rss_viewer.release.ui.adapters

import androidx.recyclerview.widget.RecyclerView

/**
 * Warning: restoring state after application kill not implemented.
 * Using the class this manner could lead to unexpected behaviour
 */
abstract class LazyLoadAdapter<T>(
    private val threshold: Int,
    private val onLoadCallback: Loader,
    private val stopLoadConditionCallback: StopLoadCondition<T>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: MutableList<T> = arrayListOf()
    protected set
    private var loading: Boolean = false
    private var needMoreItems: Boolean = true

    fun interface Loader {
        fun onLoad()
    }

    fun interface StopLoadCondition<T> {
        fun needMore(nextItems: List<T>) : Boolean
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (loading) {
            return
        }

        val endIsNear = (items.size - position) <= threshold
        if (endIsNear && needMoreItems) {
            loading = true
            onLoadCallback.onLoad()
        }
    }

    fun appendItems(nextItems: List<T>) {
        if (!stopLoadConditionCallback.needMore(nextItems)) {
            needMoreItems = false
        }

        val initItems = items.isEmpty()
        if (initItems) {
            resetItems(nextItems)
            return
        }

        loading = false

        // Items ran out
        if (nextItems.isEmpty()) {
            return
        }

        val start = items.size
        items.addAll(nextItems)
        notifyItemRangeInserted(start, nextItems.size)
    }

    fun resetItems(items: List<T>) {
        needMoreItems = true
        if (!stopLoadConditionCallback.needMore(items)) {
            needMoreItems = false
        }
        loading = false
        this.items = items.toMutableList()
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): T {
        return items[position]
    }
}