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

    // Identifies which play() call is the newest request. Using a counter instead of the
    // native device pointer avoids ABA bugs: a freed native pointer can be handed back out
    // by the allocator for the very next adl_init() call.
    private var playbackGeneration = 0L

    // Which generation's device/audioTrack are currently sitting in the shared fields, if any.
    // This is deliberately a SEPARATE thing from playbackGeneration: a thread can successfully
    // publish its device and then be superseded before it runs a single loop iteration. When it
    // tears down, it must only be allowed to null out the shared fields if they still hold ITS
    // device - otherwise a live, newer device (or worse, nothing, leaving a dangling pointer to
    // memory it just freed) could be wiped out or left dangling by the wrong thread.
    private var publishedGeneration = -1L

    fun play(path: String, songNumber: Int = 0) {
        val myGeneration: Long
        synchronized(playbackLock) {
            stopInternal()
            isPlaying = true // Set immediately to prevent multiple rapid starts
            myGeneration = ++playbackGeneration
        }

        thread(start = true, name = "MidiPlaybackThread") {
            var localDevice = 0L
            var localTrack: AudioTrack? = null

            // Releases only resources THIS thread allocated, and only clears the shared
            // device/audioTrack fields if they still hold what THIS thread published there.
            fun cleanup() {
                synchronized(playbackLock) {
                    // "Do I still own the shared fields" and "am I still the newest request"
                    // are different questions. A thread that just got superseded by a newer
                    // play() call still needs to clear the fields it published (if the newer
                    // thread hasn't published yet) - but must NOT report a real stop, since
                    // playback isn't stopping, it's transitioning to the new song.
                    val isNewestRequest = myGeneration == playbackGeneration
                    if (publishedGeneration == myGeneration) {
                        device = 0L
                        audioTrack = null
                        publishedGeneration = -1L
                    }
                    if (isNewestRequest) {
                        isPlaying = false
                        onPlaybackStopped?.invoke()
                    }
                    localTrack?.stop()
                    localTrack?.release()
                    if (localDevice != 0L) adlMidi.close(localDevice)
                }
            }

            try {
                synchronized(playbackLock) {
                    if (myGeneration != playbackGeneration) return@thread

                    localDevice = adlMidi.init(sampleRate)
                    if (localDevice == 0L) {
                        cleanup()
                        return@thread
                    }

                    adlMidi.setModeEMIDI(localDevice, 1)
                    adlMidi.switchEmulator(localDevice, 2)
                    adlMidi.setRunAtPcmRate(localDevice, 1)
                    adlMidi.setNumChips(localDevice, 1)
                    adlMidi.setSoftPanEnabled(localDevice, 1)

                    val bankNames = adlMidi.getBankNames()
                    Log.d("AudioPlayer", "Available banks: ${bankNames.joinToString(", ")}")

                    // Dune 2 / Westwood often uses bank index 58 (Miles) or something similar in newer libADLMIDI
                    // Let's try to find a bank name containing "Miles" or "Westwood"
                    val milesBank = bankNames.indexOfFirst { it.contains("Miles", ignoreCase = true) }
                    if (milesBank != -1) {
                        Log.d("AudioPlayer", "Selecting Miles bank at index $milesBank")
                        adlMidi.setBank(localDevice, milesBank)
                    }

                    adlMidi.selectSongNum(localDevice, songNumber)
                    val openResult = adlMidi.openFile(localDevice, path)
                    val songsCount = adlMidi.getSongsCount(localDevice)
                    Log.d("AudioPlayer", "openFile result: $openResult, songsCount: $songsCount for $path")

                    // Heuristic: If we requested track 0 but it's a multi-song file, find the longest track.
                    // This is common in Westwood games where track 0 is meta/empty.
                    if (openResult == 0 && songsCount > 1 && songNumber == 0) {
                        var longestTrack = 0
                        var maxDuration = 0.0
                        for (i in 0 until minOf(songsCount, 100)) {
                            adlMidi.selectSongNum(localDevice, i)
                            val duration = adlMidi.totalTimeLength(localDevice)
                            if (duration > maxDuration) {
                                maxDuration = duration
                                longestTrack = i
                            }
                        }
                        if (maxDuration > 0) {
                            Log.d("AudioPlayer", "Auto-selected longest track $longestTrack (duration: $maxDuration s)")
                            adlMidi.selectSongNum(localDevice, longestTrack)
                        }
                    }

                    if (openResult != 0) {
                        cleanup()
                        return@thread
                    }

                    adlMidi.setVolumeRangeModel(localDevice, 2)
                    adlMidi.setFullRangeBrightness(localDevice, 1)

                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()

                    val audioFormat = AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()

                    localTrack = AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()

                    if (myGeneration != playbackGeneration) {
                        // Superseded while we were setting up; nothing published yet, just
                        // release what we made ourselves.
                        cleanup()
                        return@thread
                    }

                    localTrack?.play()
                    device = localDevice
                    audioTrack = localTrack
                    publishedGeneration = myGeneration
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Failed to start playback for $path", e)
                cleanup()
                return@thread
            }

            val samples = ShortArray(bufferSize / 2)

            try {
                while (isPlaying) {
                    val read: Int
                    synchronized(playbackLock) {
                        if (myGeneration != playbackGeneration || !isPlaying) return@thread
                        read = adlMidi.play(localDevice, samples)
                    }

                    if (read > 0) {
                        for (i in 0 until read) {
                            val scaled = (samples[i] * gain).toInt()
                            samples[i] = scaled.coerceIn(-32768, 32767).toShort()
                        }
                        localTrack?.write(samples, 0, read)
                    } else {
                        break
                    }
                }
            } finally {
                cleanup()
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
