cmake_minimum_required(VERSION 3.22)
project(nesmod)
set(CMAKE_CXX_STANDARD 17)

# Source SimpleNES — di-copy ke nes/src & nes/include oleh GitHub Actions
set(NES_SRC
    nes/src/CPU.cpp
    nes/src/MainBus.cpp
    nes/src/PPU.cpp
    nes/src/PictureBus.cpp
    nes/src/Cartridge.cpp
    nes/src/Controller.cpp
    nes/src/Mapper.cpp
    nes/src/MapperNROM.cpp
    nes/src/MapperSxROM.cpp
    nes/src/MapperUxROM.cpp
    nes/src/MapperCNROM.cpp
    nes/src/MapperAxROM.cpp
    nes/src/MapperGxROM.cpp
    nes/src/MapperMMC3.cpp
    nes/src/MapperColorDreams.cpp
    nes/src/Log.cpp
    nes/src/APU/APU.cpp
    nes/src/APU/DMC.cpp
    nes/src/APU/FrameCounter.cpp
    nes/src/APU/Noise.cpp
    nes/src/APU/Pulse.cpp
    nes/src/APU/Triangle.cpp
    nes/src/APU/Units.cpp
    # File kita (pengganti SFML)
    nes/src/Emulator.cpp
    nes/src/AudioPlayer.cpp
    nes/src/VirtualScreen.cpp
)

add_library(nesmod SHARED
    jni_bridge.cpp
    ${NES_SRC}
)

target_include_directories(nesmod PRIVATE
    nes/include
    .
)

target_compile_definitions(nesmod PRIVATE ANDROID_BUILD=1)

target_link_libraries(nesmod android jnigraphics log)
