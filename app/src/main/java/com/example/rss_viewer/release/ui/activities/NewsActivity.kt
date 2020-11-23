package com.example.rss_viewer.release.ui.activities

import android.os.Bundle
import com.example.rss_viewer.R
import moxy.MvpAppCompatActivity

class NewsActivity : MvpAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_news)
    }
}