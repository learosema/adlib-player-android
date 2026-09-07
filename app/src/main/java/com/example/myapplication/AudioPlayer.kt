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
    var isPlaying = false
        private set
    private val sampleRate = 44100
    private val gain = 4.5f // Balanced software gain factor
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2

    fun play(path: String) {
        stop()
        
        device = adlMidi.init(sampleRate)
        if (device == 0L) {
            Log.e("AudioPlayer", "Failed to initialize ADLMIDI")
            return
        }

        // Set compatibility and performance options before opening the file
        adlMidi.setModeEMIDI(device, 1)         // Enable EMIDI for tricky game MIDIs
        adlMidi.switchEmulator(device, 2)       // 2 = ADLMIDI_EMU_DOSBOX (Fast for slow devices)
        adlMidi.setRunAtPcmRate(device, 1)      // PCM rate for better performance
        adlMidi.setNumChips(device, 1)          // 18 channels total
        adlMidi.setSoftPanEnabled(device, 1)    // Better stereo
        // adlMidi.setHVibrato(device, -1)         // Use bank default deep vibrato
        // adlMidi.setHTremolo(device, -1)         // Use bank default deep tremolo
        // adlMidi.setAutoArpeggio(device, 1)      // Handle auto-arpeggio MIDIs
        // adlMidi.setLoopEnabled(device, 1)       // Enable loop tags

        if (adlMidi.openFile(device, path) != 0) {
            Log.e("AudioPlayer", "Failed to open MIDI file: $path")
            adlMidi.close(device)
            device = 0L
            return
        }

        // Apply remaining dynamics settings
        adlMidi.setVolumeRangeModel(device, 2)  // Native OPL3 volume model
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

        isPlaying = true
        audioTrack?.play()

        thread(start = true, name = "MidiPlaybackThread") {
            val samples = ShortArray(bufferSize / 2)
            while (isPlaying) {
                val read = adlMidi.play(device, samples)
                if (read > 0) {
                    // Apply gain and clipping
                    for (i in 0 until read) {
                        val scaled = (samples[i] * gain).toInt()
                        samples[i] = scaled.coerceIn(-32768, 32767).toShort()
                    }
                    audioTrack?.write(samples, 0, read)
                } else {
                    isPlaying = false
                }
            }
            
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            
            if (device != 0L) {
                adlMidi.close(device)
                device = 0L
            }
        }
    }

    fun stop() {
        isPlaying = false
    }
    
    fun getProgress(): Float {
        if (device == 0L) return 0f
        val total = adlMidi.totalTimeLength(device)
        if (total <= 0) return 0f
        return (adlMidi.positionTell(device) / total).toFloat()
    }
    
    fun seek(progress: Float) {
        if (device == 0L) return
        val total = adlMidi.totalTimeLength(device)
        adlMidi.positionSeek(device, (progress * total).toDouble())
    }
}
