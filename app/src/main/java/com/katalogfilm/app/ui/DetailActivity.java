package com.katalogfilm.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.katalogfilm.app.R;
import com.katalogfilm.app.data.Repository;
import com.katalogfilm.app.data.remote.ApiClient;
import com.katalogfilm.app.data.remote.TmdbModels.Cast;
import com.katalogfilm.app.data.remote.TmdbModels.Crew;
import com.katalogfilm.app.data.remote.TmdbModels.Detail;
import com.katalogfilm.app.data.remote.TmdbModels.Genre;
import com.katalogfilm.app.data.remote.TmdbModels.Video;
import com.katalogfilm.app.databinding.ActivityDetailBinding;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class DetailActivity extends BaseActivity {

    public static Intent intent(Context c, int id) {
        return new Intent(c, DetailActivity.class).putExtra("id", id);
    }

    private ActivityDetailBinding b;
    private Repository repo;
    private int movieId;
    private Detail detail;
    private boolean saved;
    private Call<Detail> call;
    private final CastAdapter castAdapter = new CastAdapter();

    @Override protected int barMode() { return 2; }

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Utils.insets(b.topBar, true, false);
        Utils.insets(b.sheet, false, true);

        repo = Repository.get(this);
        movieId = getIntent().getIntExtra("id", 0);

        b.castList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        b.castList.setAdapter(castAdapter);

        b.btnBack.setOnClickListener(v -> finish());
        b.btnShare.setOnClickListener(v -> share());
        b.btnPlay.setOnClickListener(v -> { SoundManager.tap(v); openTrailer(); });
        b.btnTrailer.setOnClickListener(v -> { SoundManager.tap(v); openTrailer(); });
        b.btnRetry.setOnClickListener(v -> load());
        b.btnWatch.setOnClickListener(v -> {
            if (detail == null) return;
            SoundManager.tap(v);
            if (saved) repo.removeWatch(movieId); else repo.addWatch(detail);
        });
        b.btnReview.setOnClickListener(v -> {
            if (detail == null) return;
            SoundManager.tap(v);
            RateReviewSheet.newInstance(detail.id, detail.title, detail.posterPath)
                    .show(getSupportFragmentManager(), "review");
        });

        repo.isWatch(movieId).observe(this, n -> {
            saved = n != null && n > 0;
            b.btnWatch.setText(saved ? getString(R.string.watchlist_saved) : getString(R.string.watchlist_title));
            b.btnWatch.setIconResource(saved ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
        });

        repo.review(movieId).observe(this, r -> {
            if (r == null) { b.cardReview.setVisibility(View.GONE); return; }
            b.cardReview.setVisibility(View.VISIBLE);
            b.rbMine.setRating(r.stars);
            b.tvMyMood.setText(r.mood == null ? "" : r.mood);
            b.tvMyMood.setVisibility(r.mood == null || r.mood.isEmpty() ? View.GONE : View.VISIBLE);
            b.tvMyText.setText(r.text == null ? "" : r.text);
            b.tvMyText.setVisibility(r.text == null || r.text.isEmpty() ? View.GONE : View.VISIBLE);
            b.tvMyTags.setText(r.tags == null ? "" : r.tags.replace("|", "  •  "));
            b.tvMyTags.setVisibility(r.tags == null || r.tags.isEmpty() ? View.GONE : View.VISIBLE);
            b.btnReview.setText(R.string.review_edit);
        });

        load();
    }

    private void load() {
        b.progress.setVisibility(View.VISIBLE);
        b.errorBox.setVisibility(View.GONE);
        call = repo.detail(movieId, new Repository.Result<Detail>() {
            @Override public void ok(Detail d) {
                if (isDestroyed()) return;
                b.progress.setVisibility(View.GONE);
                show(d);
            }
            @Override public void fail(String m) {
                if (isDestroyed()) return;
                b.progress.setVisibility(View.GONE);
                b.errorMsg.setText(m);
                b.errorBox.setVisibility(View.VISIBLE);
            }
        });
    }

    private void show(Detail d) {
        detail = d;
        String bd = d.backdropPath != null ? ApiClient.backdrop(d.backdropPath) : ApiClient.poster(d.posterPath);
        Glide.with(this).load(bd).into(b.backdrop);

        b.tvTitle.setText(d.title);
        String meta = Utils.year(d.releaseDate);
        if (d.runtime > 0) meta += "  •  " + Utils.runtime(d.runtime);
        b.tvMeta.setText(meta);
        b.ratingBar.setRating((float) (d.voteAverage / 2.0));
        b.tvRating.setText(getString(R.string.rating_display, Utils.rating(d.voteAverage), d.voteCount));

        b.genres.removeAllViews();
        if (d.genres != null) for (Genre g : d.genres) b.genres.addView(Utils.tag(this, g.name));

        boolean hasTag = d.tagline != null && !d.tagline.isEmpty();
        b.tvTagline.setVisibility(hasTag ? View.VISIBLE : View.GONE);
        if (hasTag) b.tvTagline.setText("“" + d.tagline + "”");

        b.tvOverview.setText(d.overview == null || d.overview.isEmpty()
                ? getString(R.string.overview_missing) : d.overview);

        String director = null;
        List<Cast> cast = new ArrayList<>();
        if (d.credits != null) {
            if (d.credits.crew != null) for (Crew c : d.credits.crew) {
                if ("Director".equals(c.job)) { director = c.name; break; }
            }
            if (d.credits.cast != null) {
                for (int i = 0; i < Math.min(15, d.credits.cast.size()); i++) cast.add(d.credits.cast.get(i));
            }
        }
        b.tvDirector.setVisibility(director == null ? View.GONE : View.VISIBLE);
        if (director != null) b.tvDirector.setText(getString(R.string.director_format, director));
        castAdapter.submit(cast);
        b.tvCastTitle.setVisibility(cast.isEmpty() ? View.GONE : View.VISIBLE);
        b.castList.setVisibility(cast.isEmpty() ? View.GONE : View.VISIBLE);

        b.sheet.setVisibility(View.VISIBLE);
        repo.addHistory(d);
    }

    private String trailerKey() {
        if (detail == null || detail.videos == null || detail.videos.results == null) return null;
        String fallback = null;
        for (Video v : detail.videos.results) {
            if (!"YouTube".equalsIgnoreCase(v.site)) continue;
            if ("Trailer".equalsIgnoreCase(v.type)) return v.key;
            if (fallback == null) fallback = v.key;
        }
        return fallback;
    }

    private void openTrailer() {
        String key = trailerKey();
        if (key == null) {
            Snackbar.make(b.getRoot(), getString(R.string.trailer_missing), Snackbar.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + key)));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(b.getRoot(), getString(R.string.no_trailer_app), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void share() {
        if (detail == null) return;
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_movie_format, detail.title, detail.id));
        startActivity(Intent.createChooser(i, getString(R.string.share_movie_title)));
    }

    @Override
    protected void onDestroy() {
        if (call != null) call.cancel();
        super.onDestroy();
    }

    // ---- adapter pemeran
    private static class CastAdapter extends RecyclerView.Adapter<CastAdapter.VH> {
        private final List<Cast> items = new ArrayList<>();

        void submit(List<Cast> l) { items.clear(); items.addAll(l); notifyDataSetChanged(); }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView photo; final TextView name, role;
            VH(View v) {
                super(v);
                photo = v.findViewById(R.id.photo);
                name = v.findViewById(R.id.name);
                role = v.findViewById(R.id.role);
            }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_cast, p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            Cast c = items.get(i);
            Glide.with(h.photo).load(ApiClient.profile(c.profilePath))
                    .placeholder(R.drawable.bg_poster_placeholder)
                    .error(R.drawable.bg_poster_placeholder)
                    .into(h.photo);
            h.name.setText(c.name);
            h.role.setText(c.character == null ? "" : c.character);
        }

        @Override public int getItemCount() { return items.size(); }
    }
}
