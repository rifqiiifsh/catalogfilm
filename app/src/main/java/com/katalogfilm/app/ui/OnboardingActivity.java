package com.katalogfilm.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.katalogfilm.app.R;
import com.katalogfilm.app.databinding.ActivityOnboardingBinding;
import com.katalogfilm.app.databinding.ItemOnboardingBinding;
import com.katalogfilm.app.util.Session;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

public class OnboardingActivity extends BaseActivity {

    private static final int[] ICONS = {R.drawable.ic_movie, R.drawable.ic_star, R.drawable.ic_bookmark};
    private String[] titles;
    private String[] descriptions;

    @Override protected int barMode() { return 1; }

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        ActivityOnboardingBinding b = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Utils.insets(b.bottomBar, false, true);

        titles = getResources().getStringArray(R.array.onboarding_titles);
        descriptions = getResources().getStringArray(R.array.onboarding_descriptions);
        b.pager.setAdapter(new PageAdapter());
        new TabLayoutMediator(b.dots, b.pager, (tab, pos) -> { }).attach();

        b.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                boolean last = pos == titles.length - 1;
                b.btnNext.setText(last ? getString(R.string.onboarding_start) : getString(R.string.onboarding_next));
                b.btnSkip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
            }
        });

        b.btnNext.setOnClickListener(v -> {
            SoundManager.tap(v);
            int p = b.pager.getCurrentItem();
            if (p < titles.length - 1) b.pager.setCurrentItem(p + 1, true);
            else finishOnboarding();
        });
        b.btnSkip.setOnClickListener(v -> { SoundManager.tap(v); finishOnboarding(); });
    }

    private void finishOnboarding() {
        Session.get(this).setOnboardingDone();
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private class PageAdapter extends RecyclerView.Adapter<PageAdapter.VH> {
        static class VH extends RecyclerView.ViewHolder {
            final ItemOnboardingBinding b;
            VH(ItemOnboardingBinding b) { super(b.getRoot()); this.b = b; }
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(ItemOnboardingBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            h.b.icon.setImageResource(ICONS[i]);
            h.b.title.setText(titles[i]);
            h.b.desc.setText(descriptions[i]);
        }
        @Override public int getItemCount() { return titles.length; }
    }
}
