package com.katalogfilm.app.ui;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

/** Semua layar edge-to-edge. barMode: 0 = otomatis, 1 = ikon terang (latar gelap), 2 = hanya status bar terang. */
public abstract class BaseActivity extends AppCompatActivity {

    protected int barMode() { return 0; }

    @Override
    protected void onCreate(Bundle b) {
        int mode = barMode();
        if (mode == 1) {
            EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        } else if (mode == 2) {
            EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT));
        } else {
            EdgeToEdge.enable(this);
        }
        super.onCreate(b);
    }
}
