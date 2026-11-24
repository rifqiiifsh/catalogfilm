package com.katalogfilm.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.katalogfilm.app.R;
import com.katalogfilm.app.data.remote.ApiClient;
import com.katalogfilm.app.data.remote.TmdbModels.Movie;
import com.katalogfilm.app.util.Genres;
import com.katalogfilm.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

/** Satu adapter, tiga tampilan: kartu horizontal (H), grid, dan daftar. Item null = skeleton loading. */
public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.VH> {

    public static final int H = 0, GRID = 1, LIST = 2;

    public interface OnClick { void on(Movie m); }

    private final List<Movie> items = new ArrayList<>();
    private int mode;
    private final OnClick click;

    public MovieAdapter(int mode, OnClick click) { this.mode = mode; this.click = click; }

    public void setMode(int m) { mode = m; notifyDataSetChanged(); }

    public void setLoading() {
        items.clear();
        for (int i = 0; i < 6; i++) items.add(null);
        notifyDataSetChanged();
    }

    public void submit(List<Movie> list, boolean append) {
        if (!append) {
            items.clear();
            if (list != null) items.addAll(list);
            notifyDataSetChanged();
        } else if (list != null && !list.isEmpty()) {
            int start = items.size();
            items.addAll(list);
            notifyItemRangeInserted(start, list.size());
        }
    }

    public int size() { return items.size(); }

    @Override public int getItemViewType(int p) { return mode; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        int res = t == H ? R.layout.item_movie_h : t == GRID ? R.layout.item_movie_grid : R.layout.item_movie_list;
        return new VH(LayoutInflater.from(p.getContext()).inflate(res, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Movie m = items.get(pos);
        if (m == null) {
            Glide.with(h.poster).clear(h.poster);
            h.poster.setImageResource(R.drawable.bg_poster_placeholder);
            h.title.setText(""); h.meta.setText(""); h.rating.setVisibility(View.INVISIBLE);
            if (h.overview != null) h.overview.setText("");
            h.itemView.setOnClickListener(null);
            h.itemView.setClickable(false);
            return;
        }
        Glide.with(h.poster).load(ApiClient.poster(m.posterPath))
                .placeholder(R.drawable.bg_poster_placeholder)
                .error(R.drawable.bg_poster_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(h.poster);
        h.title.setText(m.title);
        String year = Utils.year(m.releaseDate);
        String g = (m.genreIds != null && !m.genreIds.isEmpty()) ? Genres.name(h.title.getContext(), m.genreIds.get(0)) : null;
        h.meta.setText(g == null ? year : year + "  •  " + g);
        h.rating.setVisibility(View.VISIBLE);
        h.rating.setText(Utils.rating(m.voteAverage));
        if (h.overview != null) {
            h.overview.setText(m.overview == null || m.overview.isEmpty() ? h.title.getContext().getString(R.string.movie_synopsis_missing) : m.overview);
        }
        h.itemView.setOnClickListener(v -> click.on(m));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView poster;
        final TextView title, meta, rating, overview;
        VH(View v) {
            super(v);
            poster = v.findViewById(R.id.poster);
            title = v.findViewById(R.id.title);
            meta = v.findViewById(R.id.meta);
            rating = v.findViewById(R.id.rating);
            overview = v.findViewById(R.id.overview);
        }
    }
}
