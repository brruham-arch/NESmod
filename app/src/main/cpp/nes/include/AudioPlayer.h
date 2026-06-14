#pragma once
#include <cstdint>

// Dummy AudioPlayer — APU tetap berjalan tapi audio tidak dikeluarkan
// Bisa diganti nanti dengan AudioTrack JNI
class AudioPlayer {
public:
    AudioPlayer(int sampleRate = 44100) {}
    void start() {}
    void stop()  {}
    bool queueAudio(const float* samples, int count) { return true; }
    int  getSampleRate() const { return 44100; }
};
