package com.katalogfilm.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.chip.Chip;
import com.katalogfilm.app.R;
import com.katalogfilm.app.databinding.ActivityGenrePickerBinding;
import com.katalogfilm.app.util.Genres;
import com.katalogfilm.app.util.Session;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

import java.util.HashSet;
import java.util.Set;

public class GenrePickerActivity extends BaseActivity {

    private ActivityGenrePickerBinding b;
    private boolean fromSettings;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityGenrePickerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Utils.insets(b.root, true, true);
        fromSettings = getIntent().getBooleanExtra("from_settings", false);

        Set<String> saved = Session.get(this).genres();
        String[] genreNames = getResources().getStringArray(R.array.genres_names);
        for (int i = 0; i < Genres.IDS.length; i++) {
            Chip c = Utils.chip(this, Genres.EMOJIS[i] + "  " + genreNames[i]);
            c.setTag(Genres.IDS[i]);
            c.setChecked(saved.contains(String.valueOf(Genres.IDS[i])));
            c.setOnCheckedChangeListener((v, checked) -> { SoundManager.click(this); update(); });
            b.chips.addView(c);
        }
        update();
        b.btnContinue.setOnClickListener(v -> save());
    }

    private Set<String> selected() {
        Set<String> out = new HashSet<>();
        for (int i = 0; i < b.chips.getChildCount(); i++) {
            View v = b.chips.getChildAt(i);
            if (v instanceof Chip && ((Chip) v).isChecked()) out.add(String.valueOf(v.getTag()));
        }
        return out;
    }

    private void update() {
        int n = selected().size();
        b.btnContinue.setEnabled(n > 0);
        b.btnContinue.setText(n > 0 ? getString(R.string.continue_count, n) : getString(R.string.genre_minimum));
    }

    private void save() {
        Session.get(this).setGenres(selected());
        SoundManager.success(this);
        if (fromSettings) {
            finish();
        } else {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
    }
}
