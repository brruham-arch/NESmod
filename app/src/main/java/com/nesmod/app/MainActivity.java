package com.nesmod.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private static final String ROM_PATH    = "/sdcard/Download/smb.nes";
    private static final int    SOCKET_PORT = 7890;

    private NESBridge   m_nes;
    private Bitmap      m_fb;
    private SurfaceView m_view;
    private Thread      m_thread;
    private volatile boolean m_running = false;
    private volatile int     m_buttons = 0;
    private Paint       m_paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Rect m_gameArea = new Rect();
    private int  m_scrW, m_scrH;

    private Rect m_btnLeft, m_btnRight, m_btnUp, m_btnDown;
    private Rect m_btnA, m_btnB, m_btnStart, m_btnSelect;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        m_nes = new NESBridge();
        m_nes.nCreate();
        m_fb  = Bitmap.createBitmap(256, 240, Bitmap.Config.ARGB_8888);

        m_view = new SurfaceView(this);
        m_view.getHolder().addCallback(this);
        m_view.setOnTouchListener((v, e) -> { handleTouch(e); return true; });
        setContentView(m_view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    startActivityForResult(new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())), 100);
                } catch (Exception e) {
                    startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), 100);
                }
                return;
            }
        }
    }

    private void handleTouch(MotionEvent e) {
        int newBtns = 0;
        int count = e.getPointerCount();
        int action = e.getActionMasked();
        int upIdx  = (action == MotionEvent.ACTION_POINTER_UP) ? e.getActionIndex() : -1;

        for (int i = 0; i < count; i++) {
            if (i == upIdx) continue;
            int x = (int) e.getX(i);
            int y = (int) e.getY(i);
            if (hit(m_btnLeft,   x, y)) newBtns |= NESBridge.BTN_LEFT;
            if (hit(m_btnRight,  x, y)) newBtns |= NESBridge.BTN_RIGHT;
            if (hit(m_btnUp,     x, y)) newBtns |= NESBridge.BTN_UP;
            if (hit(m_btnDown,   x, y)) newBtns |= NESBridge.BTN_DOWN;
            if (hit(m_btnA,      x, y)) newBtns |= NESBridge.BTN_A;
            if (hit(m_btnB,      x, y)) newBtns |= NESBridge.BTN_B;
            if (hit(m_btnStart,  x, y)) newBtns |= NESBridge.BTN_START;
            if (hit(m_btnSelect, x, y)) newBtns |= NESBridge.BTN_SELECT;
        }
        if (action == MotionEvent.ACTION_UP) newBtns = 0;
        m_buttons = newBtns;
    }

    private boolean hit(Rect r, int x, int y) {
        return r != null && r.contains(x, y);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        startEmulator();
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) startEmulator();
        } else {
            startEmulator();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {
        m_scrW = w; m_scrH = hh;
        setupLayout(w, hh);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder h) {
        stopEmulator();
    }

    private void setupLayout(int w, int h) {
        int ctrlH = h / 3;
        int gameH = h - ctrlH;
        int gameW = gameH * 256 / 240;
        if (gameW > w) { gameW = w; gameH = gameW * 240 / 256; }
        int gx = (w - gameW) / 2;
        m_gameArea = new Rect(gx, 0, gx + gameW, gameH);

        int cy = gameH;
        int bs = Math.min(ctrlH / 3, w / 9);

        // D-pad kiri
        int px = bs * 2, py = cy + ctrlH / 2;
        m_btnUp    = new Rect(px,      py-bs,   px+bs,   py);
        m_btnDown  = new Rect(px,      py+bs,   px+bs,   py+bs*2);
        m_btnLeft  = new Rect(px-bs,   py,      px,      py+bs);
        m_btnRight = new Rect(px+bs,   py,      px+bs*2, py+bs);

        // A B kanan
        int ax = w - bs * 4, ay = cy + ctrlH / 2;
        m_btnB = new Rect(ax,      ay,      ax+bs*2, ay+bs);
        m_btnA = new Rect(ax+bs*2, ay-bs/2, ax+bs*4, ay+bs/2);

        // Start Select tengah
        int sx = w / 2 - bs, sy = cy + ctrlH * 2 / 3;
        m_btnSelect = new Rect(sx,      sy, sx+bs,    sy+bs/2);
        m_btnStart  = new Rect(sx+bs+8, sy, sx+bs*2+8, sy+bs/2);
    }

    private void startEmulator() {
        if (m_running) return;
        if (!m_nes.nLoadROM(ROM_PATH)) {
            Toast.makeText(this, "ROM tidak ditemukan:\n" + ROM_PATH, Toast.LENGTH_LONG).show();
            return;
        }
        m_nes.nStartSocket(SOCKET_PORT);
        if (m_scrW > 0) setupLayout(m_scrW, m_scrH);
        m_running = true;

        m_thread = new Thread(() -> {
            final long TARGET_NS = 1_000_000_000L / 60;
            while (m_running) {
                long t0 = System.nanoTime();
                m_nes.nSetButtons(1, m_buttons);
                m_nes.nStepFrame();
                m_nes.nCopyFramebuffer(m_fb);
                renderFrame();
                long elapsed = System.nanoTime() - t0;
                long sleep   = (TARGET_NS - elapsed) / 1_000_000;
                if (sleep > 1) try { Thread.sleep(sleep); } catch (Exception ignored) {}
            }
        });
        m_thread.setName("NES-Loop");
        m_thread.start();

        Toast.makeText(this, "NESMod | port " + SOCKET_PORT, Toast.LENGTH_SHORT).show();
    }

    private void stopEmulator() {
        m_running = false;
        m_nes.nStopSocket();
        if (m_thread != null) try { m_thread.join(500); } catch (Exception ignored) {}
    }

    private void renderFrame() {
        SurfaceHolder holder = m_view.getHolder();
        Canvas c = holder.lockCanvas();
        if (c == null) return;
        try {
            c.drawColor(Color.BLACK);
            c.drawBitmap(m_fb, new Rect(0,0,256,240), m_gameArea, null);
            drawControls(c);
        } finally {
            holder.unlockCanvasAndPost(c);
        }
    }

    private void drawControls(Canvas c) {
        // D-pad
        drawBtn(c, m_btnLeft,   "◀", Color.argb(160,60,60,60));
        drawBtn(c, m_btnRight,  "▶", Color.argb(160,60,60,60));
        drawBtn(c, m_btnUp,     "▲", Color.argb(160,60,60,60));
        drawBtn(c, m_btnDown,   "▼", Color.argb(160,60,60,60));
        // A B
        drawBtn(c, m_btnA,      "A", Color.argb(200,180,30,30));
        drawBtn(c, m_btnB,      "B", Color.argb(200,30,30,180));
        // Start Select
        drawBtn(c, m_btnSelect, "SEL", Color.argb(160,80,80,80));
        drawBtn(c, m_btnStart,  "STA", Color.argb(160,80,80,80));
    }

    private void drawBtn(Canvas c, Rect r, String label, int color) {
        if (r == null) return;
        m_paint.setColor(color);
        m_paint.setStyle(Paint.Style.FILL);
        c.drawRoundRect(r.left, r.top, r.right, r.bottom, 12, 12, m_paint);
        m_paint.setColor(Color.WHITE);
        m_paint.setTextAlign(Paint.Align.CENTER);
        m_paint.setTextSize(Math.min(r.height(), r.width()) * 0.5f);
        c.drawText(label, r.centerX(), r.centerY() + m_paint.getTextSize() * 0.35f, m_paint);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopEmulator();
        m_nes.nDestroy();
    }
}
