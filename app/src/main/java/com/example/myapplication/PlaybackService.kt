package com.example.myapplication

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    val audioPlayer = AudioPlayer()
    private lateinit var midiPlayer: MidiPlayer
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        midiPlayer = MidiPlayer(audioPlayer)
        mediaSession = MediaSession.Builder(this, midiPlayer).build()
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AdlibPlayer::PlaybackWakeLock")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val path = intent?.getStringExtra("PATH")
        if (path != null) {
            startPlayback(path)
        }
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    fun startPlayback(path: String) {
        val mediaItem = MediaItem.Builder()
            .setMediaId(path)
            .setUri(Uri.parse(path))
            .build()
        midiPlayer.setMediaItem(mediaItem)
        midiPlayer.prepare()
        midiPlayer.playWhenReady = true
        
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
        }
    }

    fun stopPlayback() {
        midiPlayer.stop()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopPlayback()
    }
}
