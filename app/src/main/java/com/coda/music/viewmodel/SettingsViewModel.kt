package com.coda.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coda.music.data.model.StreamQuality
import com.coda.music.data.repository.SettingsRepository
import com.coda.music.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Purpose: Exposes settings state and handles user preference changes.
 * Persists via SettingsRepository (DataStore). Quality changes are applied
 * immediately to the current track via PlayerController.reloadWithQuality().
 *
 * Dependencies: SettingsRepository, PlayerController
 * Public API: streamQuality, defaultShuffle, defaultRepeat StateFlows + setters
 * Future TODOs: theme, sleep timer, crossfade
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val playerController: PlayerController
) : ViewModel() {

    val streamQuality: StateFlow<StreamQuality> = settingsRepository.streamQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreamQuality.BEST)

    val defaultShuffle: StateFlow<Boolean> = settingsRepository.defaultShuffle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val defaultRepeat: StateFlow<Boolean> = settingsRepository.defaultRepeat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setStreamQuality(quality: StreamQuality) {
        viewModelScope.launch {
            settingsRepository.setStreamQuality(quality)
            // Apply immediately to currently playing track
            playerController.reloadWithQuality(quality)
        }
    }

    fun setDefaultShuffle(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDefaultShuffle(enabled) }
    }

    fun setDefaultRepeat(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDefaultRepeat(enabled) }
    }
}
