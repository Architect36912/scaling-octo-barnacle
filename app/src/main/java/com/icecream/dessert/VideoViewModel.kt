package com.icecream.dessert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VideoResult(
    val title: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val duration: String? = null
)

data class VideoState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val videos: List<VideoResult> = emptyList(),
    val error: String? = null,
    val currentVideoUrl: String? = null,
    val isPlaying: Boolean = false,
    val currentTime: Long = 0L
)

class VideoViewModel : ViewModel() {
    private val _state = MutableStateFlow(VideoState())
    val state: StateFlow<VideoState> = _state.asStateFlow()

    // Sample video URLs that are guaranteed to work
    private val sampleVideos = listOf(
        VideoResult(
            title = "Big Buck Bunny",
            url = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
            duration = "9:56"
        ),
        VideoResult(
            title = "Elephant Dream",
            url = "https://storage.googleapis.com/exoplayer-test-media-0/ElephantsDream.mp4",
            duration = "10:54"
        ),
        VideoResult(
            title = "Google Glass",
            url = "https://storage.googleapis.com/exoplayer-test-media-0/google-glass-tech-specs.mp4",
            duration = "0:41"
        ),
        VideoResult(
            title = "Jazz Music",
            url = "https://storage.googleapis.com/exoplayer-test-media-0/jazz_in_paris.mp3",
            duration = "1:24"
        )
    )

    fun searchVideos(query: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isLoading = true,
                    searchQuery = query,
                    error = null
                )

                // Filter videos based on search query
                val filteredVideos = if (query.isEmpty()) {
                    sampleVideos
                } else {
                    sampleVideos.filter { 
                        it.title.contains(query, ignoreCase = true)
                    }
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    videos = filteredVideos
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun playVideo(url: String) {
        _state.value = _state.value.copy(
            currentVideoUrl = url,
            isPlaying = true,
            currentTime = 0L
        )
    }

    fun updateTime(timeMs: Long) {
        _state.value = _state.value.copy(
            currentTime = timeMs
        )
    }

    fun togglePlayPause() {
        _state.value = _state.value.copy(
            isPlaying = !_state.value.isPlaying
        )
    }

    init {
        // Load all videos initially
        searchVideos("")
    }
}
