package com.nesmod.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
        m_view.setOnTouchListener((v, e) -> {
            int w = v.getWidth();
            float x = e.getX();
            if (e.getAction() == MotionEvent.ACTION_DOWN ||
                e.getAction() == MotionEvent.ACTION_MOVE) {
                if (x < w * 0.3f)      m_buttons = NESBridge.BTN_LEFT  | NESBridge.BTN_A;
                else if (x > w * 0.7f) m_buttons = NESBridge.BTN_RIGHT | NESBridge.BTN_A;
                else                   m_buttons = NESBridge.BTN_A;
            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                m_buttons = 0;
            }
            return true;
        });
        setContentView(m_view);

        // Minta akses semua file (Android 11+)
        requestStoragePermission();
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivityForResult(intent, 100);
            }
        } else {
            requestPermissions(new String[]{
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }, 101);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        // Setelah user grant, surface sudah ready — langsung load ROM
        if (m_view.getHolder().getSurface().isValid()) {
            startEmulator();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                startEmulator();
            }
            // else tunggu onActivityResult
        } else {
            startEmulator();
        }
    }

    private void startEmulator() {
        if (m_running) return;
        if (!m_nes.nLoadROM(ROM_PATH)) {
            Toast.makeText(this, "ROM tidak ditemukan:\n" + ROM_PATH, Toast.LENGTH_LONG).show();
            return;
        }
        m_nes.nStartSocket(SOCKET_PORT);
        Toast.makeText(this, "NESMod siap | port " + SOCKET_PORT, Toast.LENGTH_SHORT).show();
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
        c.drawBitmap(m_fb, new Rect(0,0,256,240),
            new Rect(0,0,c.getWidth(),c.getHeight()), null);
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
