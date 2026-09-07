#include <jni.h>
#include <string>
#include <adlmidi.h>

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
