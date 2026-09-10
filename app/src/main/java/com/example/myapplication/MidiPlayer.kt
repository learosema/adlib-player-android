package com.example.myapplication

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class MidiPlayer(private val audioPlayer: AudioPlayer) : SimpleBasePlayer(Looper.getMainLooper()) {
    private var mediaItems = mutableListOf<MediaItem>()
    private var currentIndex = 0
    // Which item index audioPlayer.play() was last actually called for. A single logical
    // "select and play this song" action from a controller can invoke handleSetMediaItems(),
    // handlePrepare() and handleSetPlayWhenReady() back to back, and each of them calls
    // playCurrent() independently. Without this guard, that fires audioPlayer.play() up to
    // three times for the exact same song, each restart immediately cutting off the previous
    // one's native device and AudioTrack.
    private var lastRequestedIndex = -1
    private var playWhenReady = false
    private var playbackState = Player.STATE_IDLE
    private val handler = Handler(Looper.getMainLooper())
    private val positionUpdateIntervalMs = 200L
    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            if (playWhenReady && audioPlayer.isPlaying) {
                invalidateState()
            }
            handler.postDelayed(this, positionUpdateIntervalMs)
        }
    }

    init {
        audioPlayer.onPlaybackStopped = {
            handler.post {
                playbackState = Player.STATE_IDLE
                invalidateState()
            }
        }
        handler.post(positionUpdateRunnable)
    }

    override fun handleRelease(): ListenableFuture<*> {
        handler.removeCallbacks(positionUpdateRunnable)
        return Futures.immediateVoidFuture()
    }

    override fun getState(): State {
        val commands = Player.Commands.Builder()
            .add(Player.COMMAND_PLAY_PAUSE)
            .add(Player.COMMAND_STOP)
            .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
            .add(Player.COMMAND_GET_TIMELINE)
            .add(Player.COMMAND_SET_MEDIA_ITEM)
            .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
            .add(Player.COMMAND_PREPARE)
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .build()

        val safePlaybackState = if (mediaItems.isEmpty()) Player.STATE_IDLE else playbackState

        val stateBuilder = State.Builder()
            .setAvailableCommands(commands)
            .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(safePlaybackState)
            .setCurrentMediaItemIndex(currentIndex)
            .setContentPositionMs(audioPlayer.getCurrentPositionMs())

        if (mediaItems.isNotEmpty()) {
            stateBuilder.setPlaylist(mediaItems.mapIndexed { index, item ->
                SimpleBasePlayer.MediaItemData.Builder(item.mediaId)
                    .setMediaItem(item)
                    .setDurationUs(if (index == currentIndex) audioPlayer.getDurationMs() * 1000 else 0)
                    .build()
            })
        }

        return stateBuilder.build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        this.playWhenReady = playWhenReady
        if (playWhenReady) {
            playbackState = Player.STATE_READY
            playCurrent()
        } else {
            audioPlayer.stop()
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        playbackState = Player.STATE_READY
        if (playWhenReady) playCurrent()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int
    ): ListenableFuture<*> {
        if (mediaItems.isEmpty()) return Futures.immediateVoidFuture()
        
        if (mediaItemIndex != currentIndex) {
            currentIndex = mediaItemIndex.coerceIn(0, mediaItems.size - 1)
            if (playWhenReady) {
                playbackState = Player.STATE_READY
                playCurrent()
            }
        } else {
            audioPlayer.seekTo(positionMs)
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        playWhenReady = false
        playbackState = Player.STATE_IDLE
        audioPlayer.stop()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<*> {
        this.mediaItems = mediaItems
        if (mediaItems.isEmpty()) {
            currentIndex = 0
            audioPlayer.stop()
        } else {
            this.currentIndex = startIndex.coerceIn(0, mediaItems.size - 1)
            if (playWhenReady) playCurrent()
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    private fun playCurrent() {
        if (mediaItems.isEmpty()) return
        // Already playing (or already starting) this exact item - nothing to do. audioPlayer.isPlaying
        // is set synchronously before play() returns, so this reliably reflects an in-flight attempt too.
        if (currentIndex == lastRequestedIndex && audioPlayer.isPlaying) return
        val path = mediaItems[currentIndex].localConfiguration?.uri?.path
        if (path != null) {
            lastRequestedIndex = currentIndex
            audioPlayer.play(path)
        }
    }
}
