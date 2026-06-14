#pragma once
#include <cstdint>
#include <cstddef>
#include "PaletteColors.h"  // sf::Color defined here

namespace sn {

static const int NES_WIDTH  = 256;
static const int NES_HEIGHT = 240;

class VirtualScreen {
public:
    uint32_t pixels[NES_WIDTH * NES_HEIGHT] = {};

    void create(unsigned int, unsigned int, float, sf::Color) {}

    void setPixel(std::size_t x, std::size_t y, sf::Color c) {
        if (x < (size_t)NES_WIDTH && y < (size_t)NES_HEIGHT)
            pixels[y * NES_WIDTH + x] =
                0xFF000000u | ((uint32_t)c.r << 16) | ((uint32_t)c.g << 8) | c.b;
    }

    const uint32_t* getPixels() const { return pixels; }
};

} // namespace sn
