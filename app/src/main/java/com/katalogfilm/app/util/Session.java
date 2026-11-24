package com.katalogfilm.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Penyimpanan preferensi ringan: sesi login, onboarding, pengaturan. */
public class Session {
    private static Session inst;
    private final SharedPreferences sp;

    private Session(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("katalog_film", Context.MODE_PRIVATE);
    }

    public static synchronized Session get(Context c) {
        if (inst == null) inst = new Session(c);
        return inst;
    }

    public boolean isOnboardingDone() { return sp.getBoolean("onboarding", false); }
    public void setOnboardingDone() { sp.edit().putBoolean("onboarding", true).apply(); }

    /** -1 = belum login, 0 = tamu, >0 = id pengguna terdaftar. */
    public long userId() { return sp.getLong("uid", -1); }
    public boolean isLoggedIn() { return userId() >= 0; }
    public boolean isGuest() { return userId() == 0; }
    public void login(long id) { sp.edit().putLong("uid", id).apply(); }
    public void setAuthToken(String token) { sp.edit().putString("auth_token", token).apply(); }
    public String authToken() { return sp.getString("auth_token", ""); }
    public void logout() { sp.edit().remove("uid").remove("auth_token").apply(); }

    public Set<String> genres() {
        return new HashSet<>(sp.getStringSet("genres_" + userId(), new HashSet<>()));
    }
    public void setGenres(Set<String> s) { sp.edit().putStringSet("genres_" + userId(), s).apply(); }

    public boolean soundOn() { return sp.getBoolean("sound", true); }
    public void setSoundOn(boolean v) { sp.edit().putBoolean("sound", v).apply(); }

    public boolean notifOn() { return sp.getBoolean("notif", true); }
    public void setNotifOn(boolean v) { sp.edit().putBoolean("notif", v).apply(); }

    /** 0 = ikuti sistem, 1 = terang, 2 = gelap. */
    public int themeMode() { return sp.getInt("theme", 0); }
    public void setThemeMode(int m) { sp.edit().putInt("theme", m).apply(); }
}
