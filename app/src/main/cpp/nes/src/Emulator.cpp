#include "Emulator.h"
#include "APU/Constants.h"
#include "Log.h"
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  "NESMod", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "NESMod", __VA_ARGS__)

namespace sn {

Emulator::Emulator()
    : m_audioPlayer(static_cast<int>(1.0 / apu_clock_period_s.count()))
    , m_cpu(m_bus)
    , m_ppu(m_pictureBus, m_screen)
    , m_apu(m_audioPlayer,
            m_cpu.createIRQHandler(),
            [&](Address addr) { return DMCDMA(addr); })
    , m_bus(m_ppu, m_apu, m_ctrl1, m_ctrl2, [&](Byte b) { OAMDMA(b); })
{
    m_ppu.setInterruptCallback([&]() { m_cpu.nmiInterrupt(); });
}

bool Emulator::loadROM(const std::string& path) {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (!m_cartridge.loadFromFile(path)) {
        LOGE("loadROM failed: %s", path.c_str());
        return false;
    }
    m_mapper = Mapper::createMapper(
        static_cast<Mapper::Type>(m_cartridge.getMapper()),
        m_cartridge,
        m_cpu.createIRQHandler(),
        [&]() { m_pictureBus.updateMirroring(); }
    );
    if (!m_mapper) {
        LOGE("Unsupported mapper: %d", m_cartridge.getMapper());
        return false;
    }
    if (!m_bus.setMapper(m_mapper.get()) || !m_pictureBus.setMapper(m_mapper.get())) {
        LOGE("setMapper failed");
        return false;
    }
    m_cpu.reset();
    m_ppu.reset();
    m_screen.create(NESVideoWidth, NESVideoHeight, 1.0f, sf::Color::White);
    m_loaded = true;
    LOGI("ROM loaded: %s", path.c_str());
    return true;
}

void Emulator::stepFrame() {
    if (!m_loaded) return;
    std::lock_guard<std::mutex> lock(m_mutex);
    for (int i = 0; i < CPU_CYCLES_PER_FRAME; i++) {
        m_cpu.step();
        m_ppu.step();
        m_ppu.step();
        m_ppu.step();
        m_apu.step();
    }
}

void Emulator::reset() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_cpu.reset();
    m_ppu.reset();
}

void Emulator::stop() {
    m_loaded = false;
}

uint8_t Emulator::readRAM(uint16_t addr) {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_bus.read(addr);
}

void Emulator::writeRAM(uint16_t addr, uint8_t value) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_bus.write(addr, value);
}

void Emulator::setPlayer1(uint8_t b) { m_ctrl1.setKeyState(b); }
void Emulator::setPlayer2(uint8_t b) { m_ctrl2.setKeyState(b); }

void Emulator::OAMDMA(Byte page) {
    const Byte* ptr = m_bus.getPagePtr(page);
    if (ptr) m_ppu.doDMA(ptr);
}

Byte Emulator::DMCDMA(Address addr) {
    return m_bus.read(addr);
}

} // namespace sn
