#include <jni.h>
#include <string>
#include <adlmidi.h>

#define BW_MidiSequencer AdlMidiSequencer
#include <midiseq/midi_sequencer.hpp>

#ifndef XMIDI_CONVERT_NOCONVERSION
#define XMIDI_CONVERT_NOCONVERSION 0x00
#endif

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_myapplication_AdlMidi_init(JNIEnv *env, jobject thiz, jint sample_rate) {
    auto *device = adl_init(sample_rate);
    return reinterpret_cast<jlong>(device);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_close(JNIEnv *env, jobject thiz, jlong device_ptr) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_close(device);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_myapplication_AdlMidi_openFile(JNIEnv *env, jobject thiz, jlong device_ptr, jstring path) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (!device) return -1;

    const char *nativePath = env->GetStringUTFChars(path, nullptr);
    int result = adl_openFile(device, nativePath);
    env->ReleaseStringUTFChars(path, nativePath);
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_myapplication_AdlMidi_play(JNIEnv *env, jobject thiz, jlong device_ptr, jshortArray samples) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (!device) return 0;

    jsize len = env->GetArrayLength(samples);
    jshort *nativeSamples = env->GetShortArrayElements(samples, nullptr);

    int result = adl_play(device, len, nativeSamples);

    env->ReleaseShortArrayElements(samples, nativeSamples, 0);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_positionSeek(JNIEnv *env, jobject thiz, jlong device_ptr, jdouble seconds) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_positionSeek(device, seconds);
    }
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_example_myapplication_AdlMidi_positionTell(JNIEnv *env, jobject thiz, jlong device_ptr) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        return adl_positionTell(device);
    }
    return 0.0;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_example_myapplication_AdlMidi_totalTimeLength(JNIEnv *env, jobject thiz, jlong device_ptr) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        return adl_totalTimeLength(device);
    }
    return 0.0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_setSoftPanEnabled(JNIEnv *env, jobject thiz, jlong device_ptr, jint enabled) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_setSoftPanEnabled(device, enabled);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_setVolumeRangeModel(JNIEnv *env, jobject thiz, jlong device_ptr, jint model) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_setVolumeRangeModel(device, model);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_setNumChips(JNIEnv *env, jobject thiz, jlong device_ptr, jint num) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_setNumChips(device, num);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_setFullRangeBrightness(JNIEnv *env, jobject thiz, jlong device_ptr, jint enabled) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_setFullRangeBrightness(device, enabled);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_switchEmulator(JNIEnv *env, jobject thiz, jlong device_ptr, jint emulator) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_switchEmulator(device, emulator);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_setRunAtPcmRate(JNIEnv *env, jobject thiz, jlong device_ptr, jint enabled) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_setRunAtPcmRate(device, enabled);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_setModeEMIDI(JNIEnv *env, jobject thiz, jlong device_ptr, jint enabled) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_setModeEMIDI(device, enabled);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_setAutoArpeggio(JNIEnv *env, jobject thiz, jlong device_ptr, jint enabled) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_setAutoArpeggio(device, enabled);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_setHVibrato(JNIEnv *env, jobject thiz, jlong device_ptr, jint enabled) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_setHVibrato(device, enabled);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_setHTremolo(JNIEnv *env, jobject thiz, jlong device_ptr, jint enabled) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_setHTremolo(device, enabled);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_setLoopEnabled(JNIEnv *env, jobject thiz, jlong device_ptr, jint enabled) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_setLoopEnabled(device, enabled);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_AdlMidi_selectSongNum(JNIEnv *env, jobject thiz, jlong device_ptr, jint song_number) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        adl_selectSongNum(device, song_number);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_myapplication_AdlMidi_getSongsCount(JNIEnv *env, jobject thiz, jlong device_ptr) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        int count = adl_getSongsCount(device);
        return count > 0 ? count : 1;
    }
    return 1;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_myapplication_AdlMidi_getBankNames(JNIEnv *env, jobject thiz) {
    int count = adl_getBanksCount();
    const char *const *names = adl_getBankNames();

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(count, stringClass, nullptr);

    for (int i = 0; i < count; ++i) {
        jstring name = env->NewStringUTF(names[i]);
        env->SetObjectArrayElement(result, i, name);
        env->DeleteLocalRef(name);
    }
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_myapplication_AdlMidi_setBank(JNIEnv *env, jobject thiz, jlong device_ptr, jint bank) {
    auto *device = reinterpret_cast<ADL_MIDIPlayer *>(device_ptr);
    if (device) {
        return adl_setBank(device, bank);
    }
    return -1;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_myapplication_AdlMidi_extractXmiTracks(JNIEnv *env, jobject thiz, jbyteArray xmi_data) {
    if (!xmi_data) return nullptr;

    jsize len = env->GetArrayLength(xmi_data);
    jbyte *nativeData = env->GetByteArrayElements(xmi_data, nullptr);

    AdlMidiSequencer::RawSongsList song_buf;
    int res = AdlMidiSequencer::Convert_xmi2midi_multi(reinterpret_cast<uint8_t *>(nativeData), static_cast<uint32_t>(len), song_buf, XMIDI_CONVERT_NOCONVERSION);

    env->ReleaseByteArrayElements(xmi_data, nativeData, JNI_ABORT);

    if (res < 0) return nullptr;

    jclass byteArrayClass = env->FindClass("[B");
    if (!byteArrayClass) return nullptr;

    jobjectArray result = env->NewObjectArray(static_cast<jsize>(song_buf.size), byteArrayClass, nullptr);

    for (size_t i = 0; i < song_buf.size; ++i) {
        jbyteArray innerArray = env->NewByteArray(static_cast<jsize>(song_buf[i].size));
        env->SetByteArrayRegion(innerArray, 0, static_cast<jsize>(song_buf[i].size), reinterpret_cast<const jbyte *>(song_buf[i].data));
        env->SetObjectArrayElement(result, static_cast<jsize>(i), innerArray);
        env->DeleteLocalRef(innerArray);
    }

    return result;
}
