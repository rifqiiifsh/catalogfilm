package com.katalogfilm.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.OvershootInterpolator;

import com.katalogfilm.app.databinding.ActivitySplashBinding;
import com.katalogfilm.app.util.Session;
import com.katalogfilm.app.util.SoundManager;

public class SplashActivity extends BaseActivity {

    @Override protected int barMode() { return 1; }

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        ActivitySplashBinding b = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.logo.setAlpha(0f); b.logo.setScaleX(0.5f); b.logo.setScaleY(0.5f);
        b.logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(900)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();

        b.title.setAlpha(0f); b.title.setTranslationY(24f);
        b.title.animate().alpha(1f).translationY(0f).setStartDelay(500).setDuration(600).start();
        b.tagline.setAlpha(0f); b.tagline.setTranslationY(16f);
        b.tagline.animate().alpha(1f).translationY(0f).setStartDelay(800).setDuration(600).start();

        SoundManager.splash(this);
        new Handler(Looper.getMainLooper()).postDelayed(this::next, 2400);
    }

    private void next() {
        if (isFinishing() || isDestroyed()) return;
        Session s = Session.get(this);
        Class<?> target = !s.isOnboardingDone() ? OnboardingActivity.class
                : !s.isLoggedIn() ? LoginActivity.class : MainActivity.class;
        startActivity(new Intent(this, target));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
