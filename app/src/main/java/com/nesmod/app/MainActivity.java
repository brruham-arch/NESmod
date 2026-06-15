package com.nesmod.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
    private int         m_buttons = 0;
    private Paint       m_paint   = new Paint();

    // Button areas (akan dihitung saat surface ready)
    private Rect m_gameArea   = new Rect();
    private int  m_scrW, m_scrH;

    // Virtual button rects
    private Rect m_btnLeft, m_btnRight, m_btnUp, m_btnDown;
    private Rect m_btnA, m_btnB, m_btnStart, m_btnSelect;

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
        // ARGB_8888 — framebuffer dari C sudah ARGB
        m_fb = Bitmap.createBitmap(256, 240, Bitmap.Config.ARGB_8888);

        m_view = new SurfaceView(this);
        m_view.getHolder().addCallback(this);
        m_view.setOnTouchListener((v, e) -> {
            handleTouch(e);
            return true;
        });
        setContentView(m_view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                    );
                    startActivityForResult(intent, 100);
                } catch (Exception e) {
                    startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), 100);
                }
            }
        }
    }

    private void handleTouch(MotionEvent e) {
        int action = e.getActionMasked();
        m_buttons = 0;
        // Cek semua pointer aktif
        for (int i = 0; i < e.getPointerCount(); i++) {
            if (action == MotionEvent.ACTION_UP && i == e.getActionIndex()) continue;
            int x = (int) e.getX(i);
            int y = (int) e.getY(i);
            if (contains(m_btnLeft,   x, y)) m_buttons |= NESBridge.BTN_LEFT;
            if (contains(m_btnRight,  x, y)) m_buttons |= NESBridge.BTN_RIGHT;
            if (contains(m_btnUp,     x, y)) m_buttons |= NESBridge.BTN_UP;
            if (contains(m_btnDown,   x, y)) m_buttons |= NESBridge.BTN_DOWN;
            if (contains(m_btnA,      x, y)) m_buttons |= NESBridge.BTN_A;
            if (contains(m_btnB,      x, y)) m_buttons |= NESBridge.BTN_B;
            if (contains(m_btnStart,  x, y)) m_buttons |= NESBridge.BTN_START;
            if (contains(m_btnSelect, x, y)) m_buttons |= NESBridge.BTN_SELECT;
        }
        if (action == MotionEvent.ACTION_UP && e.getPointerCount() == 1) m_buttons = 0;
    }

    private boolean contains(Rect r, int x, int y) {
        return r != null && r.contains(x, y);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        startEmulator();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
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

    private void setupLayout(int w, int h) {
        // Game area: atas, 70% tinggi layar, center horizontal
        int gameH = (int)(h * 0.68f);
        int gameW = gameH * 256 / 240;
        if (gameW > w) { gameW = w; gameH = gameW * 240 / 256; }
        int gx = (w - gameW) / 2;
        m_gameArea = new Rect(gx, 0, gx + gameW, gameH);

        // Controller area: bawah layar
        int cy = gameH + 4;
        int ch = h - cy;
        int bs = Math.min(ch / 3, w / 10); // button size

        // D-pad kiri
        int dx = bs, dy = cy + ch/2 - bs/2;
        m_btnLeft  = new Rect(dx,        dy,        dx+bs,    dy+bs);
        m_btnRight = new Rect(dx+bs*2,   dy,        dx+bs*3,  dy+bs);
        m_btnUp    = new Rect(dx+bs,     dy-bs,     dx+bs*2,  dy);
        m_btnDown  = new Rect(dx+bs,     dy+bs,     dx+bs*2,  dy+bs*2);

        // A B kanan
        int ax = w - bs*4, ay = cy + ch/2 - bs/2;
        m_btnB = new Rect(ax,      ay,      ax+bs,   ay+bs);
        m_btnA = new Rect(ax+bs*2, ay-bs/2, ax+bs*3, ay+bs/2);

        // Start/Select tengah bawah
        int mx = w/2 - bs, my = h - bs - 4;
        m_btnSelect = new Rect(mx,      my, mx+bs,    my+bs/2);
        m_btnStart  = new Rect(mx+bs+4, my, mx+bs*2+4, my+bs/2);
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
        m_handler.post(m_loop);
    }

    private void renderFrame() {
        SurfaceHolder holder = m_view.getHolder();
        Canvas c = holder.lockCanvas();
        if (c == null) return;

        // Background hitam
        c.drawColor(Color.BLACK);

        // Game
        c.drawBitmap(m_fb, new Rect(0,0,256,240), m_gameArea, null);

        // D-pad
        m_paint.setColor(Color.argb(180, 50, 50, 50));
        m_paint.setStyle(Paint.Style.FILL);
        drawBtn(c, m_btnLeft,   "◀");
        drawBtn(c, m_btnRight,  "▶");
        drawBtn(c, m_btnUp,     "▲");
        drawBtn(c, m_btnDown,   "▼");

        // A B
        m_paint.setColor(Color.argb(180, 200, 50, 50));
        drawBtn(c, m_btnA, "A");
        m_paint.setColor(Color.argb(180, 50, 50, 200));
        drawBtn(c, m_btnB, "B");

        // Start Select
        m_paint.setColor(Color.argb(180, 80, 80, 80));
        drawBtn(c, m_btnSelect, "SEL");
        drawBtn(c, m_btnStart,  "STA");

        holder.unlockCanvasAndPost(c);
    }

    private void drawBtn(Canvas c, Rect r, String label) {
        if (r == null) return;
        c.drawRoundRect(r.left, r.top, r.right, r.bottom, 8, 8, m_paint);
        m_paint.setColor(Color.WHITE);
        m_paint.setTextAlign(Paint.Align.CENTER);
        m_paint.setTextSize(r.height() * 0.45f);
        m_paint.setStyle(Paint.Style.FILL);
        c.drawText(label, r.centerX(), r.centerY() + m_paint.getTextSize()/3, m_paint);
        // Reset untuk button berikutnya
        m_paint.setStyle(Paint.Style.FILL);
    }

    @Override public void surfaceDestroyed(SurfaceHolder h) {
        m_running = false;
        m_nes.nStopSocket();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_running = false;
        m_nes.nStopSocket();
        m_nes.nDestroy();
    }
}
