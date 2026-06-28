package com.coda.music.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.coda.music.data.model.StreamQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Purpose: Persists user settings via DataStore (datastore-preferences).
 * Single source of truth for all user preferences in the app.
 *
 * Keys:
 *   STREAM_QUALITY  — StreamQuality enum name, default BEST
 *   DEFAULT_SHUFFLE — Boolean, default false
 *   DEFAULT_REPEAT  — Boolean, default false
 *
 * Dependencies: DataStore, Hilt, Kotlin Coroutines
 * Public API: streamQuality, defaultShuffle, defaultRepeat flows + setters
 * Future TODOs: theme preference, sleep timer, crossfade duration
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "coda_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val STREAM_QUALITY  = stringPreferencesKey("stream_quality")
    private val DEFAULT_SHUFFLE = booleanPreferencesKey("default_shuffle")
    private val DEFAULT_REPEAT  = booleanPreferencesKey("default_repeat")

    val streamQuality: Flow<StreamQuality> = context.dataStore.data.map { prefs ->
        val name = prefs[STREAM_QUALITY] ?: StreamQuality.BEST.name
        runCatching { StreamQuality.valueOf(name) }.getOrDefault(StreamQuality.BEST)
    }

    val defaultShuffle: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_SHUFFLE] ?: false
    }

    val defaultRepeat: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_REPEAT] ?: false
    }

    suspend fun setStreamQuality(quality: StreamQuality) {
        context.dataStore.edit { it[STREAM_QUALITY] = quality.name }
    }

    suspend fun setDefaultShuffle(enabled: Boolean) {
        context.dataStore.edit { it[DEFAULT_SHUFFLE] = enabled }
    }

    suspend fun setDefaultRepeat(enabled: Boolean) {
        context.dataStore.edit { it[DEFAULT_REPEAT] = enabled }
    }
}
