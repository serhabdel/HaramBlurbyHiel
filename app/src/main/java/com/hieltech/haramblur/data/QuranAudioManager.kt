package com.hieltech.haramblur.data

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hieltech.haramblur.data.api.QuranAudioFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentVerseKey: String = "",
    val currentSurahNumber: Int = 0,
    val currentTrackIndex: Int = 0,
    val totalTracks: Int = 0,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isLoading: Boolean = false,
    val chapterAudioUrl: String? = null
)

@Singleton
class QuranAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "QuranAudioManager"
    }

    private var exoPlayer: ExoPlayer? = null
    private var verseAudioFiles: List<QuranAudioFile> = emptyList()

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also { player ->
            exoPlayer = player
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    updatePlaybackState()
                    if (state == Player.STATE_ENDED) {
                        val current = _playbackState.value
                        if (current.currentTrackIndex < current.totalTracks - 1) {
                            playTrackAt(current.currentTrackIndex + 1)
                        } else {
                            _playbackState.value = current.copy(isPlaying = false)
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlaybackState()
                }
            })
        }
    }

    fun playChapterAudio(surahNumber: Int, audioUrl: String) {
        Log.d(TAG, "Playing chapter audio: surah=$surahNumber url=$audioUrl")
        val player = getOrCreatePlayer()
        verseAudioFiles = emptyList()
        player.setMediaItem(MediaItem.fromUri(audioUrl))
        player.prepare()
        player.play()
        _playbackState.value = AudioPlaybackState(
            isPlaying = true,
            currentSurahNumber = surahNumber,
            isLoading = true,
            chapterAudioUrl = audioUrl,
            totalTracks = 1,
            currentTrackIndex = 0
        )
    }

    fun playVerseAudio(surahNumber: Int, audioFiles: List<QuranAudioFile>, startIndex: Int = 0) {
        if (audioFiles.isEmpty()) return
        Log.d(TAG, "Playing verse audio: surah=$surahNumber, ${audioFiles.size} verses, start=$startIndex")
        val player = getOrCreatePlayer()
        verseAudioFiles = audioFiles
        val mediaItems = audioFiles.map { MediaItem.fromUri(it.url) }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()
        _playbackState.value = AudioPlaybackState(
            isPlaying = true,
            currentSurahNumber = surahNumber,
            currentVerseKey = audioFiles.getOrNull(startIndex)?.verseKey ?: "",
            currentTrackIndex = startIndex,
            totalTracks = audioFiles.size,
            isLoading = true
        )
    }

    private fun playTrackAt(index: Int) {
        val player = exoPlayer ?: return
        if (index in 0 until verseAudioFiles.size) {
            player.seekTo(index, 0L)
            player.play()
            _playbackState.value = _playbackState.value.copy(
                currentTrackIndex = index,
                currentVerseKey = verseAudioFiles[index].verseKey,
                isPlaying = true
            )
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekToVerse(index: Int) {
        playTrackAt(index)
    }

    fun skipNext() {
        val current = _playbackState.value
        if (current.currentTrackIndex < current.totalTracks - 1) {
            playTrackAt(current.currentTrackIndex + 1)
        }
    }

    fun skipPrevious() {
        val current = _playbackState.value
        if (current.currentTrackIndex > 0) {
            playTrackAt(current.currentTrackIndex - 1)
        }
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _playbackState.value = AudioPlaybackState()
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        _playbackState.value = AudioPlaybackState()
    }

    fun getCurrentPositionMs(): Long = exoPlayer?.currentPosition ?: 0L
    fun getDurationMs(): Long = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L

    private fun updatePlaybackState() {
        val player = exoPlayer ?: return
        _playbackState.value = _playbackState.value.copy(
            isPlaying = player.isPlaying,
            isLoading = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L)
        )
    }
}

