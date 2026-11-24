package com.katalogfilm.app;

import android.app.Application;

import com.katalogfilm.app.util.Session;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.applyTheme(Session.get(this).themeMode());
        SoundManager.init(this);
    }
}
