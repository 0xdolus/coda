package com.coda.music

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import com.coda.music.data.provider.HttpDownloader

@HiltAndroidApp
class CodaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NewPipe.init(HttpDownloader.getInstance())
    }
}
