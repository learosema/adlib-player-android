package com.example.myapplication

class AdlMidi {
    companion object {
        init {
            System.loadLibrary("myapplication")
        }
    }

    external fun init(sampleRate: Int): Long
    external fun close(device: Long)
    external fun openFile(device: Long, path: String): Int
    external fun play(device: Long, samples: ShortArray): Int
    external fun positionSeek(device: Long, seconds: Double)
    external fun positionTell(device: Long): Double
    external fun totalTimeLength(device: Long): Double
    external fun setSoftPanEnabled(device: Long, enabled: Int)
    external fun setVolumeRangeModel(device: Long, model: Int)
    external fun setNumChips(device: Long, num: Int)
    external fun setFullRangeBrightness(device: Long, enabled: Int)
    external fun switchEmulator(device: Long, emulator: Int)
    external fun setRunAtPcmRate(device: Long, enabled: Int)
    external fun setModeEMIDI(device: Long, enabled: Int)
    external fun setAutoArpeggio(device: Long, enabled: Int)
    external fun setHVibrato(device: Long, enabled: Int)
    external fun setHTremolo(device: Long, enabled: Int)
    external fun setLoopEnabled(device: Long, enabled: Int)
    external fun selectSongNum(device: Long, songNumber: Int)
    external fun getSongsCount(device: Long): Int
    external fun extractXmiTracks(xmiData: ByteArray): Array<ByteArray>?
    external fun getBankNames(): Array<String>
    external fun setBank(device: Long, bank: Int): Int
}
