package com.katalogfilm.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.katalogfilm.app.R;
import com.katalogfilm.app.data.Repository;
import com.katalogfilm.app.data.remote.TmdbModels.MovieList;
import com.katalogfilm.app.databinding.FragmentHomeBinding;
import com.katalogfilm.app.databinding.ViewSectionBinding;
import com.katalogfilm.app.util.Session;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

import retrofit2.Call;

public class HomeFragment extends Fragment {

    private static final String[] LANGS = {"en", "id", "ko", "hi"};
    private static final String[] SECTIONS = {Repository.PICK, Repository.TRENDING, Repository.NEW, Repository.TOP};

    private FragmentHomeBinding b;
    private Repository repo;
    private String[] regions;
    private String[] titles;
    private String lang = "en";
    private final MovieAdapter[] adapters = new MovieAdapter[4];
    private final Call<?>[] calls = new Call<?>[4];
    private int pending;
    private boolean failed;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentHomeBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        repo = Repository.get(requireContext());
        regions = getResources().getStringArray(R.array.home_regions);
        titles = getResources().getStringArray(R.array.home_sections);
        Utils.insets(b.root, true, false);

        ViewSectionBinding[] secs = {b.secPick, b.secTrending, b.secNew, b.secTop};
        for (int i = 0; i < 4; i++) {
            adapters[i] = new MovieAdapter(MovieAdapter.H,
                    m -> startActivity(DetailActivity.intent(requireContext(), m.id)));
            secs[i].list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            secs[i].list.setAdapter(adapters[i]);
            secs[i].title.setText(titles[i]);
        }

        for (int i = 0; i < regions.length; i++) {
            Chip c = Utils.chip(requireContext(), regions[i]);
            c.setId(View.generateViewId());
            c.setTag(i);
            b.regionChips.addView(c);
            if (i == 0) c.setChecked(true);
        }
        b.regionChips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            View chip = group.findViewById(ids.get(0));
            if (chip == null) return;
            lang = LANGS[(Integer) chip.getTag()];
            SoundManager.click(requireContext());
            load();
        });

        b.searchBar.setOnClickListener(x -> ((MainActivity) requireActivity()).openSearch(false));
        b.btnMic.setOnClickListener(x -> ((MainActivity) requireActivity()).openSearch(true));
        b.btnBell.setOnClickListener(x -> startActivity(new Intent(requireContext(), NotificationsActivity.class)));
        b.btnRetry.setOnClickListener(x -> load());
        b.refresh.setColorSchemeResources(R.color.brand);
        b.refresh.setOnRefreshListener(this::load);

        repo.unread().observe(getViewLifecycleOwner(),
                n -> b.badge.setVisibility(n != null && n > 0 ? View.VISIBLE : View.GONE));
        load();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // genre favorit bisa berubah dari Pengaturan → muat ulang bagian "Pilihan Untukmu"
        if (!hidden && b != null) {
            boolean hasPick = !Session.get(requireContext()).genres().isEmpty();
            if (hasPick != (b.secPick.getRoot().getVisibility() == View.VISIBLE)) load();
        }
    }

    private void load() {
        if (b == null) return;
        for (Call<?> c : calls) if (c != null) c.cancel();
        failed = false;
        b.errorBox.setVisibility(View.GONE);
        b.content.setVisibility(View.VISIBLE);

        String genres = TextUtils.join("|", Session.get(requireContext()).genres());
        boolean hasPick = !genres.isEmpty();
        b.secPick.getRoot().setVisibility(hasPick ? View.VISIBLE : View.GONE);

        pending = 0;
        for (int i = 0; i < 4; i++) {
            if (i == 0 && !hasPick) continue;
            final int idx = i;
            pending++;
            adapters[i].setLoading();
            calls[i] = repo.section(SECTIONS[i], lang, genres, 1, new Repository.Result<MovieList>() {
                @Override public void ok(MovieList d) {
                    if (b == null) return;
                    adapters[idx].submit(d.results, false);
                    done();
                }
                @Override public void fail(String m) {
                    if (b == null) return;
                    adapters[idx].submit(null, false);
                    if (!failed) {
                        failed = true;
                        b.content.setVisibility(View.GONE);
                        b.errorMsg.setText(m);
                        b.errorBox.setVisibility(View.VISIBLE);
                    }
                    done();
                }
            });
        }
    }

    private void done() {
        pending--;
        if (pending <= 0) b.refresh.setRefreshing(false);
    }

    @Override
    public void onDestroyView() {
        for (Call<?> c : calls) if (c != null) c.cancel();
        super.onDestroyView();
        b = null;
    }
}
