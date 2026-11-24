package com.katalogfilm.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.katalogfilm.app.R;
import com.katalogfilm.app.data.Repository;
import com.katalogfilm.app.data.local.Entities;
import com.katalogfilm.app.data.remote.TmdbModels.Movie;
import com.katalogfilm.app.databinding.FragmentWatchlistBinding;
import com.katalogfilm.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class WatchlistFragment extends Fragment {

    private FragmentWatchlistBinding b;
    private List<Entities.WatchItem> current = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentWatchlistBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        Repository repo = Repository.get(requireContext());
        Utils.insets(b.root, true, false);

        MovieAdapter adapter = new MovieAdapter(MovieAdapter.LIST,
                m -> startActivity(DetailActivity.intent(requireContext(), m.id)));
        b.list.setLayoutManager(new LinearLayoutManager(getContext()));
        b.list.setAdapter(adapter);

        repo.watchlist().observe(getViewLifecycleOwner(), items -> {
            current = items == null ? new ArrayList<>() : items;
            List<Movie> ms = new ArrayList<>();
            for (Entities.WatchItem w : current) ms.add(Utils.fromWatch(w));
            adapter.submit(ms, false);
            b.empty.setVisibility(ms.isEmpty() ? View.VISIBLE : View.GONE);
            b.tvCount.setText(ms.isEmpty() ? getString(R.string.watchlist_empty_title) : getString(R.string.watchlist_count, ms.size()));
        });

        // geser kiri/kanan untuk menghapus, dengan opsi Batal
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getBindingAdapterPosition();
                if (pos < 0 || pos >= current.size()) return;
                Entities.WatchItem w = current.get(pos);
                repo.removeWatch(w.movieId);
                Snackbar.make(b.getRoot(), getString(R.string.watchlist_removed, w.title), Snackbar.LENGTH_LONG)
                        .setAction(R.string.cancel, x -> repo.restoreWatch(w)).show();
            }
        }).attachToRecyclerView(b.list);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
