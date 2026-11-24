package com.katalogfilm.app.ui;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.katalogfilm.app.R;
import com.katalogfilm.app.databinding.ActivityMainBinding;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

public class MainActivity extends BaseActivity {

    private static final String[] TAGS = {"home", "search", "watch", "profile"};
    private static final int[] IDS = {R.id.nav_home, R.id.nav_search, R.id.nav_watchlist, R.id.nav_profile};

    private ActivityMainBinding b;
    private int current = 0;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Utils.insets(b.bottomNav, false, true);

        FragmentManager fm = getSupportFragmentManager();
        if (s == null) {
            Fragment[] fs = {new HomeFragment(), new SearchFragment(), new WatchlistFragment(), new ProfileFragment()};
            FragmentTransaction t = fm.beginTransaction();
            for (int i = 0; i < fs.length; i++) {
                t.add(R.id.container, fs[i], TAGS[i]);
                if (i != 0) t.hide(fs[i]);
            }
            t.commit();
        } else {
            current = s.getInt("tab", 0);
        }

        b.bottomNav.setOnItemSelectedListener(item -> {
            for (int i = 0; i < IDS.length; i++) {
                if (IDS[i] == item.getItemId()) { show(i); break; }
            }
            SoundManager.click(this);
            return true;
        });
        b.bottomNav.setSelectedItemId(IDS[current]);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (current != 0) {
                    b.bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", current);
    }

    private void show(int idx) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction t = fm.beginTransaction();
        for (int i = 0; i < TAGS.length; i++) {
            Fragment f = fm.findFragmentByTag(TAGS[i]);
            if (f == null) continue;
            if (i == idx) t.show(f); else t.hide(f);
        }
        t.commit();
        current = idx;
    }

    /** Dipanggil dari beranda: buka tab Cari, opsional langsung pencarian suara. */
    public void openSearch(boolean voice) {
        b.bottomNav.setSelectedItemId(R.id.nav_search);
        Fragment f = getSupportFragmentManager().findFragmentByTag("search");
        if (f instanceof SearchFragment) {
            if (voice) ((SearchFragment) f).startVoice();
            else ((SearchFragment) f).focusInput();
        }
    }
}
