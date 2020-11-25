package com.example.rss_viewer.release.utils

sealed class ResponseState<out T : Any> {
    data class Success<out T : Any>(val data: T) : ResponseState<T>()
    data class Error(val thr: Throwable) : ResponseState<Nothing>()
}