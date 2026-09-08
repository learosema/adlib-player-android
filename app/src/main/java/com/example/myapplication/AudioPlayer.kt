package com.example.myapplication

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.concurrent.thread

class AudioPlayer {
    private val adlMidi = AdlMidi()
    private var device: Long = 0
    private var audioTrack: AudioTrack? = null
    @Volatile
    var isPlaying = false
        private set
    var onPlaybackStopped: (() -> Unit)? = null
    private val sampleRate = 44100
    private val gain = 4.5f
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2

    private val playbackLock = Any()

    fun play(path: String, songNumber: Int = 0) {
        synchronized(playbackLock) {
            stopInternal()
            isPlaying = true // Set immediately to prevent multiple rapid starts
            
            thread(start = true, name = "MidiPlaybackThread") {
                synchronized(playbackLock) {
                    device = adlMidi.init(sampleRate)
                    if (device == 0L) {
                        isPlaying = false
                        return@thread
                    }

                    adlMidi.setModeEMIDI(device, 1)
                    adlMidi.switchEmulator(device, 2)
                    adlMidi.setRunAtPcmRate(device, 1)
                    adlMidi.setNumChips(device, 1)
                    adlMidi.setSoftPanEnabled(device, 1)

                    val bankNames = adlMidi.getBankNames()
                    Log.d("AudioPlayer", "Available banks: ${bankNames.joinToString(", ")}")
                    
                    // Dune 2 / Westwood often uses bank index 58 (Miles) or something similar in newer libADLMIDI
                    // Let's try to find a bank name containing "Miles" or "Westwood"
                    val milesBank = bankNames.indexOfFirst { it.contains("Miles", ignoreCase = true) }
                    if (milesBank != -1) {
                        Log.d("AudioPlayer", "Selecting Miles bank at index $milesBank")
                        adlMidi.setBank(device, milesBank)
                    }

                    adlMidi.selectSongNum(device, songNumber)
                    val openResult = adlMidi.openFile(device, path)
                    val songsCount = adlMidi.getSongsCount(device)
                    Log.d("AudioPlayer", "openFile result: $openResult, songsCount: $songsCount for $path")

                    // Heuristic: If we requested track 0 but it's a multi-song file, find the longest track.
                    // This is common in Westwood games where track 0 is meta/empty.
                    if (openResult == 0 && songsCount > 1 && songNumber == 0) {
                        var longestTrack = 0
                        var maxDuration = 0.0
                        for (i in 0 until minOf(songsCount, 100)) {
                            adlMidi.selectSongNum(device, i)
                            val duration = adlMidi.totalTimeLength(device)
                            if (duration > maxDuration) {
                                maxDuration = duration
                                longestTrack = i
                            }
                        }
                        if (maxDuration > 0) {
                            Log.d("AudioPlayer", "Auto-selected longest track $longestTrack (duration: $maxDuration s)")
                            adlMidi.selectSongNum(device, longestTrack)
                        }
                    }

                    if (openResult != 0) {
                        adlMidi.close(device)
                        device = 0L
                        isPlaying = false
                        return@thread
                    }

                    adlMidi.setVolumeRangeModel(device, 2)
                    adlMidi.setFullRangeBrightness(device, 1)

                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()

                    val audioFormat = AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()

                    audioTrack = AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()

                    audioTrack?.play()
                }

                val samples = ShortArray(bufferSize / 2)
                var currentDevice: Long
                var currentAudioTrack: AudioTrack?
                
                synchronized(playbackLock) {
                    currentDevice = device
                    currentAudioTrack = audioTrack
                }

                try {
                    while (isPlaying) {
                        val read: Int
                        synchronized(playbackLock) {
                            if (device != currentDevice || !isPlaying) break
                            read = adlMidi.play(currentDevice, samples)
                        }
                        
                        if (read > 0) {
                            for (i in 0 until read) {
                                val scaled = (samples[i] * gain).toInt()
                                samples[i] = scaled.coerceIn(-32768, 32767).toShort()
                            }
                            currentAudioTrack?.write(samples, 0, read)
                        } else {
                            break
                        }
                    }
                } finally {
                    synchronized(playbackLock) {
                        if (device == currentDevice) {
                            isPlaying = false
                            currentAudioTrack?.stop()
                            currentAudioTrack?.release()
                            if (audioTrack == currentAudioTrack) audioTrack = null
                            
                            adlMidi.close(currentDevice)
                            device = 0L
                            onPlaybackStopped?.invoke()
                        } else {
                            // Another thread already took over, just clean up local refs
                            currentAudioTrack?.stop()
                            currentAudioTrack?.release()
                            adlMidi.close(currentDevice)
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        synchronized(playbackLock) {
            stopInternal()
        }
    }

    private fun stopInternal() {
        isPlaying = false
        audioTrack?.stop()
    }
    
    fun getDurationMs(): Long {
        synchronized(playbackLock) {
            if (device == 0L) return 0
            val durationS = adlMidi.totalTimeLength(device)
            return (durationS * 1000).toLong()
        }
    }

    fun getCurrentPositionMs(): Long {
        synchronized(playbackLock) {
            if (device == 0L) return 0
            val posS = adlMidi.positionTell(device)
            return (posS * 1000).toLong()
        }
    }
    
    fun getProgress(): Float {
        synchronized(playbackLock) {
            if (device == 0L) return 0f
            val total = adlMidi.totalTimeLength(device)
            if (total <= 0) return 0f
            return (adlMidi.positionTell(device) / total).toFloat()
        }
    }
    
    fun seek(progress: Float) {
        synchronized(playbackLock) {
            if (device == 0L) return
            val total = adlMidi.totalTimeLength(device)
            adlMidi.positionSeek(device, (progress * total).toDouble())
        }
    }

    fun seekTo(positionMs: Long) {
        synchronized(playbackLock) {
            if (device == 0L) return
            adlMidi.positionSeek(device, positionMs.toDouble() / 1000.0)
        }
    }

    fun selectSong(songNumber: Int) {
        synchronized(playbackLock) {
            if (device != 0L) {
                adlMidi.selectSongNum(device, songNumber)
            }
        }
    }

    fun getSongsCount(): Int {
        synchronized(playbackLock) {
            if (device == 0L) return 1
            return adlMidi.getSongsCount(device)
        }
    }

    fun extractXmiTracks(data: ByteArray): Array<ByteArray>? {
        return adlMidi.extractXmiTracks(data)
    }
}
