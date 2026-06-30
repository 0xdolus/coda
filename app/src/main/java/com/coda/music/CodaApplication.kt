package com.coda.music

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import com.coda.music.data.provider.HttpDownloader

@HiltAndroidApp
class CodaApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        NewPipe.init(HttpDownloader.getInstance())
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { OkHttpClient() }))
            }
            .crossfade(true)
            .build()
}
