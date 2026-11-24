package com.katalogfilm.app.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.view.HapticFeedbackConstants;
import android.view.View;

import com.katalogfilm.app.R;

/** Efek suara UI (klik, sukses) dan suara splash. Hormati pengaturan "Suara efek". */
public class SoundManager {
    private static SoundPool pool;
    private static int click, success;

    public static void init(Context c) {
        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(aa).build();
        click = pool.load(c, R.raw.sfx_click, 1);
        success = pool.load(c, R.raw.sfx_success, 1);
    }

    private static void play(Context c, int id, float vol) {
        if (pool == null || !Session.get(c).soundOn()) return;
        pool.play(id, vol, vol, 1, 0, 1f);
    }

    public static void click(Context c) { play(c, click, 0.6f); }
    public static void success(Context c) { play(c, success, 0.9f); }

    /** Klik + getar halus. */
    public static void tap(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        click(v.getContext());
    }

    public static void splash(Context c) {
        if (!Session.get(c).soundOn()) return;
        MediaPlayer mp = MediaPlayer.create(c, R.raw.sfx_splash);
        if (mp == null) return;
        mp.setOnCompletionListener(MediaPlayer::release);
        mp.start();
    }
}
