#pragma once
#include <cstddef>
#include "PaletteColors.h"

namespace sn {
static const int NES_WIDTH  = 256;
static const int NES_HEIGHT = 240;

class VirtualScreen {
public:
    uint32_t pixels[NES_WIDTH * NES_HEIGHT] = {};
    void create(unsigned int, unsigned int, float, sf::Color) {}
    void setPixel(std::size_t x, std::size_t y, sf::Color c) {
        if (x < (size_t)NES_WIDTH && y < (size_t)NES_HEIGHT)
            // Swap R dan B: NES palette disimpan BGR, Bitmap butuh ARGB
            pixels[y * NES_WIDTH + x] =
                0xFF000000u | ((uint32_t)c.b << 16) | ((uint32_t)c.g << 8) | c.r;
    }
    const uint32_t* getPixels() const { return pixels; }
};
} // namespace sn
