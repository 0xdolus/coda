package com.coda.music.di

import com.coda.music.data.provider.StreamProvider
import com.coda.music.data.source.NewPipeStreamDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {

    @Binds
    @Singleton
    abstract fun bindStreamProvider(impl: NewPipeStreamDataSource): StreamProvider
}
