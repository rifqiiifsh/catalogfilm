package com.katalogfilm.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.katalogfilm.app.R;
import com.katalogfilm.app.data.Repository;
import com.katalogfilm.app.databinding.ActivitySettingsBinding;
import com.katalogfilm.app.util.Session;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

public class SettingsActivity extends BaseActivity {

    private String[] themes;

    private ActivitySettingsBinding b;
    private Session session;
    private Repository repo;
    private TextView genreValue;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Utils.insets(b.root, true, true);
        themes = getResources().getStringArray(R.array.settings_themes);
        session = Session.get(this);
        repo = Repository.get(this);

        b.btnBack.setOnClickListener(v -> finish());

        if (session.isGuest()) {
            showAccount(getString(R.string.guest_name), getString(R.string.guest_mode));
        } else {
            repo.user().observe(this, u -> { if (u != null) showAccount(u.name, u.email); });
        }

        addHeader(getString(R.string.settings_preferences));
        addSwitch(getString(R.string.settings_effect_sound), session.soundOn(), (cb, on) -> {
            session.setSoundOn(on);
            if (on) SoundManager.click(this);
        });
        addSwitch(getString(R.string.settings_notifications), session.notifOn(), (cb, on) -> session.setNotifOn(on));
        final TextView themeValue = addRow(getString(R.string.settings_theme), themes[session.themeMode()], null);
        ((View) themeValue.getParent()).setOnClickListener(v -> {
            SoundManager.tap(v);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.settings_theme_title)
                    .setSingleChoiceItems(themes, session.themeMode(), (d, which) -> {
                        session.setThemeMode(which);
                        themeValue.setText(themes[which]);
                        d.dismiss();
                        Utils.applyTheme(which);
                    }).show();
        });
        genreValue = addRow(getString(R.string.settings_favorite_genres), genreCount(), null);
        ((View) genreValue.getParent()).setOnClickListener(v -> {
            SoundManager.tap(v);
            startActivity(new Intent(this, GenrePickerActivity.class).putExtra("from_settings", true));
        });

        addHeader(getString(R.string.settings_data));
        ((View) addRow(getString(R.string.settings_clear_history), null, null).getParent()).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.settings_clear_history_title)
                        .setMessage(R.string.settings_clear_history_message)
                         .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.delete, (d, w) -> {
                            repo.clearHistory();
                            Snackbar.make(b.getRoot(), R.string.settings_history_deleted, Snackbar.LENGTH_SHORT).show();
                        }).show());

        addHeader(getString(R.string.settings_other));
        ((View) addRow(getString(R.string.settings_about), getString(R.string.app_version), null).getParent()).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.settings_about_message)
                        .setPositiveButton(R.string.close, null).show());

        TextView out = addRow(session.isGuest() ? getString(R.string.settings_guest_logout) : getString(R.string.logout), null, null);
        out.setTextColor(getColor(R.color.brand));
        ((View) out.getParent()).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.logout_title)
                        .setMessage(R.string.logout_message)
                         .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.logout, (d, w) -> {
                            session.logout();
                            startActivity(new Intent(this, LoginActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        }).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (genreValue != null) genreValue.setText(genreCount());
    }

    private String genreCount() { return getString(R.string.selected_count, session.genres().size()); }

    private void showAccount(String name, String sub) {
        b.tvName.setText(name);
        b.tvEmail.setText(sub);
        b.tvAvatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
    }

    private void addHeader(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(getColor(R.color.brand));
        t.setTextSize(13);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.START);
        t.setPadding(Utils.dp(this, 20), Utils.dp(this, 24), Utils.dp(this, 20), Utils.dp(this, 4));
        b.list.addView(t);
    }

    /** Baris pengaturan; mengembalikan TextView judul (parent-nya adalah barisnya). */
    private TextView addRow(String title, String value, View.OnClickListener click) {
        View v = getLayoutInflater().inflate(R.layout.item_setting, b.list, false);
        TextView t = v.findViewById(R.id.title);
        TextView val = v.findViewById(R.id.value);
        t.setText(title);
        val.setText(value == null ? "" : value);
        val.setVisibility(value == null ? View.GONE : View.VISIBLE);
        if (click != null) v.setOnClickListener(click);
        b.list.addView(v);
        return value == null ? t : val;
    }

    private void addSwitch(String title, boolean checked, CompoundButton.OnCheckedChangeListener l) {
        TextView t = addRow(title, null, null);
        View row = (View) t.getParent();
        row.findViewById(R.id.chevron).setVisibility(View.GONE);
        MaterialSwitch sw = row.findViewById(R.id.sw);
        sw.setVisibility(View.VISIBLE);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(l);
        row.setOnClickListener(x -> sw.toggle());
    }
}
