package com.katalogfilm.app.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.katalogfilm.app.R;
import com.katalogfilm.app.data.Repository;
import com.katalogfilm.app.data.remote.TmdbModels.Movie;
import com.katalogfilm.app.data.remote.TmdbModels.MovieList;
import com.katalogfilm.app.databinding.FragmentSearchBinding;
import com.katalogfilm.app.util.Genres;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding b;
    private Repository repo;
    private MovieAdapter adapter;
    private int mode = MovieAdapter.GRID;
    private int page = 0, totalPages = 1;
    private boolean loading;
    private int genreId = 0;
    private String query = "";
    private Call<?> current;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable debounce;
    private ActivityResultLauncher<Intent> voice;

    @Override
    public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        voice = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null && b != null) {
                ArrayList<String> res = r.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (res != null && !res.isEmpty()) {
                    b.etSearch.setText(res.get(0));
                    b.etSearch.setSelection(b.etSearch.length());
                }
            }
        });
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentSearchBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        repo = Repository.get(requireContext());
        Utils.insets(b.root, true, false);

        adapter = new MovieAdapter(mode, m -> startActivity(DetailActivity.intent(requireContext(), m.id)));
        b.list.setAdapter(adapter);
        applyMode();
        b.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null && lm.findLastVisibleItemPosition() >= adapter.getItemCount() - 4) load(false);
            }
        });

        // chip genre: "Semua" + daftar genre
        Chip all = Utils.chip(requireContext(), getString(R.string.search_all));
        all.setId(View.generateViewId());
        all.setTag(0);
        b.genreChips.addView(all);
        all.setChecked(true);
        for (int i = 0; i < Genres.IDS.length; i++) {
            Chip c = Utils.chip(requireContext(), Genres.EMOJIS[i] + " " + getResources().getStringArray(R.array.genres_names)[i]);
            c.setId(View.generateViewId());
            c.setTag(Genres.IDS[i]);
            b.genreChips.addView(c);
        }
        b.genreChips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            View chip = group.findViewById(ids.get(0));
            if (chip == null) return;
            genreId = (Integer) chip.getTag();
            SoundManager.click(requireContext());
            load(true);
        });

        b.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable e) {
                query = e.toString().trim();
                b.btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                if (debounce != null) handler.removeCallbacks(debounce);
                debounce = () -> load(true);
                handler.postDelayed(debounce, 450);
            }
        });
        b.etSearch.setOnEditorActionListener((tv, action, ev) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                if (debounce != null) handler.removeCallbacks(debounce);
                load(true);
                return true;
            }
            return false;
        });
        b.btnClear.setOnClickListener(x -> b.etSearch.setText(""));
        b.btnMic.setOnClickListener(x -> { SoundManager.tap(x); startVoice(); });
        b.btnView.setOnClickListener(x -> {
            SoundManager.tap(x);
            mode = mode == MovieAdapter.GRID ? MovieAdapter.LIST : MovieAdapter.GRID;
            applyMode();
        });

        load(true);
    }

    private void applyMode() {
        b.list.setLayoutManager(mode == MovieAdapter.GRID
                ? new GridLayoutManager(getContext(), 2) : new LinearLayoutManager(getContext()));
        adapter.setMode(mode);
        b.btnView.setImageResource(mode == MovieAdapter.GRID ? R.drawable.ic_list : R.drawable.ic_grid);
    }

    private void load(boolean reset) {
        if (b == null) return;
        if (reset) {
            if (current != null) current.cancel();
            page = 0; totalPages = 1; loading = false;
            adapter.setLoading();
            b.emptyText.setVisibility(View.GONE);
        }
        if (loading || page >= totalPages) return;

        final int next = page + 1;
        loading = true;
        b.progress.setVisibility(View.VISIBLE);

        Repository.Result<MovieList> cb = new Repository.Result<MovieList>() {
            @Override public void ok(MovieList d) {
                if (b == null) return;
                loading = false; page = next; totalPages = Math.max(1, d.totalPages);
                b.progress.setVisibility(View.GONE);
                List<Movie> list = new ArrayList<>(d.results);
                if (!query.isEmpty() && genreId > 0) {
                    list.removeIf(m -> m.genreIds == null || !m.genreIds.contains(genreId));
                }
                adapter.submit(list, next > 1);
                b.tvCount.setText(query.isEmpty() ? getString(R.string.search_explore) : getString(R.string.search_results, d.totalResults));
                boolean none = adapter.size() == 0;
                b.emptyText.setVisibility(none ? View.VISIBLE : View.GONE);
                if (none) b.emptyText.setText(getString(R.string.search_empty));
            }
            @Override public void fail(String m) {
                if (b == null) return;
                loading = false;
                b.progress.setVisibility(View.GONE);
                if (next == 1) adapter.submit(null, false);
                b.emptyText.setVisibility(View.VISIBLE);
                b.emptyText.setText(m);
            }
        };
        current = query.isEmpty()
                ? repo.discover(null, "popularity.desc", next, 100, genreId > 0 ? String.valueOf(genreId) : null, cb)
                : repo.search(query, next, cb);
    }

    public void focusInput() {
        if (b == null) return;
        b.etSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(b.etSearch, InputMethodManager.SHOW_IMPLICIT);
    }

    public void startVoice() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.search_voice_prompt));
        try {
            voice.launch(i);
        } catch (ActivityNotFoundException e) {
            if (b != null) Snackbar.make(b.getRoot(), getString(R.string.search_voice_unavailable), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(b.etSearch.getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        if (current != null) current.cancel();
        if (debounce != null) handler.removeCallbacks(debounce);
        super.onDestroyView();
        b = null;
    }
}
