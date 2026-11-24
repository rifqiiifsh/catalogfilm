package com.katalogfilm.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.katalogfilm.app.R;
import com.katalogfilm.app.data.Repository;
import com.katalogfilm.app.data.local.Entities;
import com.katalogfilm.app.data.remote.TmdbModels.Movie;
import com.katalogfilm.app.databinding.FragmentProfileBinding;
import com.katalogfilm.app.util.Genres;
import com.katalogfilm.app.util.Session;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding b;
    private Repository repo;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentProfileBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        repo = Repository.get(requireContext());
        Utils.insets(b.root, true, false);
        Session session = Session.get(requireContext());

        b.btnSettings.setOnClickListener(x -> {
            SoundManager.tap(x);
            startActivity(new Intent(requireContext(), SettingsActivity.class));
        });

        // identitas
        if (session.isGuest()) {
            showName(getString(R.string.guest_name), getString(R.string.guest_mode));
            b.rowPublic.setVisibility(View.GONE);
        } else {
            repo.user().observe(getViewLifecycleOwner(), u -> {
                if (u == null) return;
                showName(u.name, u.email);
                b.switchPublic.setOnCheckedChangeListener(null);
                b.switchPublic.setChecked(u.publicProfile);
                b.switchPublic.setOnCheckedChangeListener((cb, on) -> { SoundManager.click(requireContext()); repo.setPublic(on); });
            });
        }

        // statistik
        repo.historyCount().observe(getViewLifecycleOwner(), n -> b.tvWatched.setText(String.valueOf(n == null ? 0 : n)));
        repo.reviewCount().observe(getViewLifecycleOwner(), n -> b.tvReviews.setText(String.valueOf(n == null ? 0 : n)));
        repo.watchCount().observe(getViewLifecycleOwner(), n -> b.tvWatchlist.setText(String.valueOf(n == null ? 0 : n)));

        // terakhir dilihat
        MovieAdapter adapter = new MovieAdapter(MovieAdapter.H,
                m -> startActivity(DetailActivity.intent(requireContext(), m.id)));
        b.historyList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        b.historyList.setAdapter(adapter);
        repo.history().observe(getViewLifecycleOwner(), items -> {
            List<Movie> ms = new ArrayList<>();
            if (items != null) for (Entities.History h : items) ms.add(Utils.fromHistory(h));
            adapter.submit(ms, false);
            b.tvHistoryEmpty.setVisibility(ms.isEmpty() ? View.VISIBLE : View.GONE);
        });

        refreshGenres();
    }

    private void showName(String name, String sub) {
        b.tvName.setText(name);
        b.tvEmail.setText(sub);
        b.tvAvatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
    }

    private void refreshGenres() {
        if (b == null) return;
        b.genreChips.removeAllViews();
        for (String g : Session.get(requireContext()).genres()) {
            try {
                int id = Integer.parseInt(g);
                String emoji = "";
                for (int i = 0; i < Genres.IDS.length; i++) if (Genres.IDS[i] == id) emoji = Genres.EMOJIS[i] + " ";
                b.genreChips.addView(Utils.tag(requireContext(), emoji + Genres.name(requireContext(), id)));
            } catch (NumberFormatException ignored) { }
        }
        b.tvGenreEmpty.setVisibility(b.genreChips.getChildCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshGenres();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refreshGenres();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
