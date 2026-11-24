package com.katalogfilm.app.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.Chip;
import com.katalogfilm.app.R;
import com.katalogfilm.app.data.local.Entities;
import com.katalogfilm.app.data.remote.TmdbModels;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public class Utils {

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static void insets(View v, boolean top, boolean bottom) { insets(v, top, bottom, false); }

    /** Tambahkan padding sesuai status bar / navigation bar (edge-to-edge). */
    public static void insets(View v, boolean top, boolean bottom, boolean ime) {
        final int pt = v.getPaddingTop(), pb = v.getPaddingBottom();
        final int types = ime
                ? (WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime())
                : WindowInsetsCompat.Type.systemBars();
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, wi) -> {
            Insets i = wi.getInsets(types);
            view.setPadding(view.getPaddingLeft(), top ? pt + i.top : pt,
                    view.getPaddingRight(), bottom ? pb + i.bottom : pb);
            return wi;
        });
        ViewCompat.requestApplyInsets(v);
    }

    public static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("kf-salt::" + s).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return s;
        }
    }

    /** Chip pilihan (checkable) bergaya aplikasi. */
    public static Chip chip(Context c, String text) {
        Chip ch = new Chip(c);
        ch.setText(text);
        ch.setCheckable(true);
        ch.setCheckedIconVisible(false);
        ch.setEnsureMinTouchTargetSize(false);
        ch.setChipBackgroundColorResource(R.color.chip_bg);
        ch.setChipStrokeColorResource(R.color.chip_stroke);
        ch.setChipStrokeWidth(dp(c, 1));
        ch.setChipCornerRadius(dp(c, 20));
        ch.setChipMinHeight(dp(c, 40));
        ch.setTextColor(c.getColorStateList(R.color.chip_text));
        return ch;
    }

    /** Chip tampilan saja (tidak bisa dipilih). */
    public static Chip tag(Context c, String text) {
        Chip ch = new Chip(c);
        ch.setText(text);
        ch.setCheckable(false);
        ch.setClickable(false);
        ch.setEnsureMinTouchTargetSize(false);
        ch.setChipBackgroundColorResource(R.color.brand_tint);
        ch.setChipStrokeWidth(0);
        ch.setChipCornerRadius(dp(c, 16));
        ch.setTextColor(ColorStateList.valueOf(c.getColor(R.color.brand)));
        return ch;
    }

    public static String year(String date) {
        return (date == null || date.length() < 4) ? "—" : date.substring(0, 4);
    }

    public static String runtime(int m) {
        return m <= 0 ? "" : (m / 60) + "j " + (m % 60) + "m";
    }

    public static String rating(double r) {
        return r <= 0 ? "—" : String.format(Locale.US, "%.1f", r);
    }

    public static String ago(long t) {
        return DateUtils.getRelativeTimeSpanString(t, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
    }

    public static void applyTheme(int mode) {
        AppCompatDelegate.setDefaultNightMode(mode == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                : mode == 2 ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static TmdbModels.Movie fromWatch(Entities.WatchItem w) {
        TmdbModels.Movie m = new TmdbModels.Movie();
        m.id = w.movieId; m.title = w.title; m.posterPath = w.posterPath;
        m.voteAverage = w.rating; m.releaseDate = w.releaseDate; m.overview = w.overview;
        return m;
    }

    public static TmdbModels.Movie fromHistory(Entities.History h) {
        TmdbModels.Movie m = new TmdbModels.Movie();
        m.id = h.movieId; m.title = h.title; m.posterPath = h.posterPath;
        m.voteAverage = h.rating; m.releaseDate = h.releaseDate;
        return m;
    }
}
