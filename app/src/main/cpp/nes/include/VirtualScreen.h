#pragma once
#include <cstdint>
#include <cstddef>

// Pengganti sf::Color
namespace sf {
struct Color {
    uint8_t r, g, b, a;
    Color(uint8_t r=0, uint8_t g=0, uint8_t b=0, uint8_t a=255)
        : r(r), g(g), b(b), a(a) {}
    static const Color White;
    static const Color Black;
};
}

namespace sn {

static const int NES_WIDTH  = 256;
static const int NES_HEIGHT = 240;

// Framebuffer ARGB8888 — langsung di-copy ke Android Bitmap
class VirtualScreen {
public:
    uint32_t pixels[NES_WIDTH * NES_HEIGHT] = {};

    void create(unsigned int w, unsigned int h, float, sf::Color) {
        // tidak perlu apa-apa
    }

    void setPixel(std::size_t x, std::size_t y, sf::Color c) {
        if (x < NES_WIDTH && y < NES_HEIGHT)
            pixels[y * NES_WIDTH + x] =
                (0xFF000000u) | ((uint32_t)c.r << 16) | ((uint32_t)c.g << 8) | c.b;
    }

    const uint32_t* getPixels() const { return pixels; }
};

} // namespace sn
