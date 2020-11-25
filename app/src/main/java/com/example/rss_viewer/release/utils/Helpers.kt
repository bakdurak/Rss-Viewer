package com.example.rss_viewer.release.utils

class Helpers {
    companion object {
        fun toUniformUrl(srcUrl: String): String {
            val formattedUrl = srcUrl.trim()
            var mayBeHttp = ""
            if (srcUrl.length >= 7) {
                mayBeHttp = formattedUrl.substring(0, 7)
            }
            var mayBeHttps = ""
            if (srcUrl.length >= 8) {
                mayBeHttps = formattedUrl.substring(0, 8)
            }
            return if (!mayBeHttp.equals("http://") && !mayBeHttps.equals("https://")) {
                "http://$formattedUrl"
            } else {
                formattedUrl
            }
        }
    }
}