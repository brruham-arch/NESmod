package com.nesmod.app;

import android.graphics.Bitmap;

public class NESBridge {
    static { System.loadLibrary("nesmod"); }

    public native void    nCreate();
    public native void    nDestroy();
    public native boolean nLoadROM(String path);
    public native void    nStepFrame();
    public native void    nReset();
    public native void    nCopyFramebuffer(Bitmap bitmap);
    public native void    nSetButtons(int player, int mask);
    public native int     nReadRAM(int addr);
    public native void    nWriteRAM(int addr, int value);
    public native void    nStartSocket(int port);
    public native void    nStopSocket();

    // SMB1 RAM addresses
    public static final int ADDR_LIVES   = 0x075A;
    public static final int ADDR_POWER   = 0x0756; // 0=small 1=big 2=fire
    public static final int ADDR_WORLD   = 0x075F;
    public static final int ADDR_LEVEL   = 0x075C;
    public static final int ADDR_MARIO_X = 0x0086;
    public static final int ADDR_MARIO_Y = 0x00CE;
    public static final int ADDR_COINS   = 0x07ED;
    public static final int ADDR_ENEMY0  = 0x0016;
    public static final int ADDR_ENEMY1  = 0x0017;
    public static final int ADDR_ENEMY2  = 0x0018;

    // Button bitmask
    public static final int BTN_A      = 0x01;
    public static final int BTN_B      = 0x02;
    public static final int BTN_SELECT = 0x04;
    public static final int BTN_START  = 0x08;
    public static final int BTN_UP     = 0x10;
    public static final int BTN_DOWN   = 0x20;
    public static final int BTN_LEFT   = 0x40;
    public static final int BTN_RIGHT  = 0x80;
}
