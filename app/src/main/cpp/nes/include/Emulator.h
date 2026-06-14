#pragma once
#include <string>
#include <memory>
#include <mutex>
#include <atomic>
#include <chrono>

#include "APU/APU.h"
#include "AudioPlayer.h"
#include "CPU.h"
#include "Controller.h"
#include "MainBus.h"
#include "PPU.h"
#include "PictureBus.h"
#include "VirtualScreen.h"
#include "Cartridge.h"
#include "Mapper.h"

namespace sn {

using TimePoint = std::chrono::high_resolution_clock::time_point;
using Duration  = std::chrono::high_resolution_clock::duration;

const int NESVideoWidth  = 256;
const int NESVideoHeight = 240;

class Emulator {
public:
    Emulator();
    ~Emulator() = default;

    bool loadROM(const std::string& path);
    void stepFrame();
    void reset();
    void stop();

    // RAM mod API
    uint8_t  readRAM(uint16_t addr);
    void     writeRAM(uint16_t addr, uint8_t value);

    // Input: bitmask bit0=A bit1=B bit2=Sel bit3=Start bit4=Up bit5=Down bit6=Left bit7=Right
    void setPlayer1(uint8_t buttons);
    void setPlayer2(uint8_t buttons);

    // Framebuffer ARGB8888 256x240
    const uint32_t* getFramebuffer() const { return m_screen.getPixels(); }

    bool isLoaded() const { return m_loaded; }

private:
    void     OAMDMA(Byte page);
    Byte     DMCDMA(Address addr);

    AudioPlayer             m_audioPlayer;
    VirtualScreen           m_screen;

    CPU                     m_cpu;
    PictureBus              m_pictureBus;
    PPU                     m_ppu;
    APU                     m_apu;
    Cartridge               m_cartridge;
    std::unique_ptr<Mapper> m_mapper;
    Controller              m_ctrl1, m_ctrl2;
    MainBus                 m_bus;

    std::atomic<bool>       m_loaded{false};
    std::mutex              m_mutex;

    static constexpr int CPU_CYCLES_PER_FRAME = 29781;
};

} // namespace sn
