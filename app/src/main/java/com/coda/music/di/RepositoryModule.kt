package com.coda.music.di

import com.coda.music.data.repository.MusicRepository
import com.coda.music.data.repository.NewPipeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: NewPipeRepository): MusicRepository
}
