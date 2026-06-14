#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <string>
#include <thread>
#include <atomic>
#include <sstream>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>

#include "nes/include/Emulator.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  "NESMod", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "NESMod", __VA_ARGS__)

static sn::Emulator*      g_emu = nullptr;
static std::atomic<bool>  g_socketRunning{false};
static int                g_serverFd = -1;

// ── Socket Server ────────────────────────────────────────────────────────────
// Protocol (plaintext):
//   READ <hex_addr>           → VALUE <hex>\n
//   WRITE <hex_addr> <hex>    → OK\n
//   RESET                     → OK\n
//   BUTTON1 <hex_bitmask>     → OK\n
//   QUIT                      → BYE\n

static void handleClient(int fd) {
    char buf[256];
    while (true) {
        ssize_t n = recv(fd, buf, sizeof(buf)-1, 0);
        if (n <= 0) break;
        buf[n] = '\0';

        std::istringstream ss(buf);
        std::string cmd;
        ss >> cmd;
        std::string resp = "ERR\n";

        if (cmd == "READ" && g_emu) {
            uint16_t addr; ss >> std::hex >> addr;
            char tmp[20];
            snprintf(tmp, sizeof(tmp), "VALUE %02X\n", g_emu->readRAM(addr));
            resp = tmp;
        } else if (cmd == "WRITE" && g_emu) {
            uint16_t addr, val; ss >> std::hex >> addr >> val;
            g_emu->writeRAM(addr, (uint8_t)val);
            resp = "OK\n";
        } else if (cmd == "RESET" && g_emu) {
            g_emu->reset();
            resp = "OK\n";
        } else if (cmd == "BUTTON1" && g_emu) {
            uint16_t mask; ss >> std::hex >> mask;
            g_emu->setPlayer1((uint8_t)mask);
            resp = "OK\n";
        } else if (cmd == "QUIT") {
            send(fd, "BYE\n", 4, 0);
            break;
        }
        send(fd, resp.c_str(), resp.size(), 0);
    }
    close(fd);
    LOGI("Client disconnected");
}

static void serverLoop(int port) {
    g_serverFd = socket(AF_INET, SOCK_STREAM, 0);
    int opt = 1;
    setsockopt(g_serverFd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    sockaddr_in addr{};
    addr.sin_family      = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port        = htons(port);
    bind(g_serverFd, (sockaddr*)&addr, sizeof(addr));
    listen(g_serverFd, 4);
    LOGI("Socket server port %d", port);

    while (g_socketRunning) {
        sockaddr_in cli{}; socklen_t len = sizeof(cli);
        int cfd = accept(g_serverFd, (sockaddr*)&cli, &len);
        if (cfd < 0) break;
        LOGI("Client connected");
        std::thread(handleClient, cfd).detach();
    }
    close(g_serverFd);
    g_serverFd = -1;
}

// ── JNI ──────────────────────────────────────────────────────────────────────
extern "C" {

JNIEXPORT void JNICALL
Java_com_nesmod_app_NESBridge_nCreate(JNIEnv*, jobject) {
    if (!g_emu) g_emu = new sn::Emulator();
}

JNIEXPORT void JNICALL
Java_com_nesmod_app_NESBridge_nDestroy(JNIEnv*, jobject) {
    delete g_emu; g_emu = nullptr;
}

JNIEXPORT jboolean JNICALL
Java_com_nesmod_app_NESBridge_nLoadROM(JNIEnv* env, jobject, jstring jpath) {
    const char* p = env->GetStringUTFChars(jpath, nullptr);
    bool ok = g_emu && g_emu->loadROM(p);
    env->ReleaseStringUTFChars(jpath, p);
    return ok;
}

JNIEXPORT void JNICALL
Java_com_nesmod_app_NESBridge_nStepFrame(JNIEnv*, jobject) {
    if (g_emu) g_emu->stepFrame();
}

JNIEXPORT void JNICALL
Java_com_nesmod_app_NESBridge_nReset(JNIEnv*, jobject) {
    if (g_emu) g_emu->reset();
}

JNIEXPORT void JNICALL
Java_com_nesmod_app_NESBridge_nCopyFramebuffer(JNIEnv* env, jobject, jobject bitmap) {
    if (!g_emu) return;
    void* pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) return;
    memcpy(pixels, g_emu->getFramebuffer(), 256 * 240 * 4);
    AndroidBitmap_unlockPixels(env, bitmap);
}

JNIEXPORT void JNICALL
Java_com_nesmod_app_NESBridge_nSetButtons(JNIEnv*, jobject, jint player, jint mask) {
    if (!g_emu) return;
    if (player == 1) g_emu->setPlayer1((uint8_t)mask);
    else             g_emu->setPlayer2((uint8_t)mask);
}

JNIEXPORT jint JNICALL
Java_com_nesmod_app_NESBridge_nReadRAM(JNIEnv*, jobject, jint addr) {
    return g_emu ? g_emu->readRAM((uint16_t)addr) : -1;
}

JNIEXPORT void JNICALL
Java_com_nesmod_app_NESBridge_nWriteRAM(JNIEnv*, jobject, jint addr, jint val) {
    if (g_emu) g_emu->writeRAM((uint16_t)addr, (uint8_t)val);
}

JNIEXPORT void JNICALL
Java_com_nesmod_app_NESBridge_nStartSocket(JNIEnv*, jobject, jint port) {
    if (g_socketRunning) return;
    g_socketRunning = true;
    std::thread(serverLoop, (int)port).detach();
}

JNIEXPORT void JNICALL
Java_com_nesmod_app_NESBridge_nStopSocket(JNIEnv*, jobject) {
    g_socketRunning = false;
    if (g_serverFd >= 0) { shutdown(g_serverFd, SHUT_RDWR); close(g_serverFd); }
}

} // extern "C"
