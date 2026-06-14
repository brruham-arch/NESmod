package com.nesmod.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private static final String ROM_PATH    = "/sdcard/Download/smb.nes";
    private static final int    SOCKET_PORT = 7890;
    private static final int    FRAME_MS    = 1000 / 60;

    private NESBridge   m_nes;
    private Bitmap      m_fb;
    private SurfaceView m_view;
    private Handler     m_handler = new Handler();
    private boolean     m_running = false;

    // D-pad touch state
    private int m_buttons = 0;

    private final Runnable m_loop = new Runnable() {
        @Override public void run() {
            if (!m_running) return;
            m_nes.nSetButtons(1, m_buttons);
            m_nes.nStepFrame();
            m_nes.nCopyFramebuffer(m_fb);
            renderFrame();
            m_handler.postDelayed(this, FRAME_MS);
        }
    };

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);

        m_nes = new NESBridge();
        m_nes.nCreate();
        m_fb  = Bitmap.createBitmap(256, 240, Bitmap.Config.ARGB_8888);

        m_view = new SurfaceView(this);
        m_view.getHolder().addCallback(this);

        // Touch input sederhana — tap kiri/kanan layar = left/right + A
        m_view.setOnTouchListener((v, e) -> {
            int w = v.getWidth();
            float x = e.getX();
            if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
                if (x < w * 0.3f)      m_buttons = NESBridge.BTN_LEFT  | NESBridge.BTN_A;
                else if (x > w * 0.7f) m_buttons = NESBridge.BTN_RIGHT | NESBridge.BTN_A;
                else                   m_buttons = NESBridge.BTN_A;
            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                m_buttons = 0;
            }
            return true;
        });

        setContentView(m_view);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!m_nes.nLoadROM(ROM_PATH)) {
            Toast.makeText(this, "ROM tidak ditemukan:\n" + ROM_PATH, Toast.LENGTH_LONG).show();
            return;
        }
        m_nes.nStartSocket(SOCKET_PORT);
        Toast.makeText(this, "NESMod siap | Socket port " + SOCKET_PORT, Toast.LENGTH_SHORT).show();
        m_running = true;
        m_handler.post(m_loop);
    }

    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        m_running = false;
        m_nes.nStopSocket();
    }

    private void renderFrame() {
        SurfaceHolder h = m_view.getHolder();
        Canvas c = h.lockCanvas();
        if (c == null) return;
        Rect dst = new Rect(0, 0, c.getWidth(), c.getHeight());
        c.drawBitmap(m_fb, new Rect(0, 0, 256, 240), dst, null);
        h.unlockCanvasAndPost(c);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_running = false;
        m_nes.nStopSocket();
        m_nes.nDestroy();
    }
}
