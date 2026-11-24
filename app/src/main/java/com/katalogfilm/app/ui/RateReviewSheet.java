package com.katalogfilm.app.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.katalogfilm.app.R;
import com.katalogfilm.app.data.Repository;
import com.katalogfilm.app.data.remote.ApiClient;
import com.katalogfilm.app.databinding.SheetReviewBinding;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Layar "Rate & Review": bintang, perasaan, teks ulasan, dan tag. */
public class RateReviewSheet extends BottomSheetDialogFragment {



    public static RateReviewSheet newInstance(int id, String title, String poster) {
        RateReviewSheet s = new RateReviewSheet();
        Bundle a = new Bundle();
        a.putInt("id", id);
        a.putString("title", title);
        a.putString("poster", poster);
        s.setArguments(a);
        return s;
    }

    private SheetReviewBinding b;
    private boolean prefilled;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = SheetReviewBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        if (getDialog() instanceof BottomSheetDialog) {
            ((BottomSheetDialog) getDialog()).getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        Bundle a = requireArguments();
        final int id = a.getInt("id");
        final String title = a.getString("title");
        final String poster = a.getString("poster");
        Repository repo = Repository.get(requireContext());

        b.tvMovie.setText(title);
        Glide.with(this).load(ApiClient.poster(poster)).placeholder(R.drawable.bg_poster_placeholder).into(b.ivPoster);

        for (String m : getResources().getStringArray(R.array.review_moods)) b.moodChips.addView(Utils.chip(requireContext(), m));
        for (String t : getResources().getStringArray(R.array.review_tags)) b.tagChips.addView(Utils.chip(requireContext(), t));

        b.btnSubmit.setEnabled(false);
        b.ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            b.btnSubmit.setEnabled(rating > 0);
            if (fromUser) SoundManager.click(requireContext());
        });

        // isi ulang jika pernah mengulas film ini
        repo.review(id).observe(getViewLifecycleOwner(), r -> {
            if (r == null || prefilled) return;
            prefilled = true;
            b.ratingBar.setRating(r.stars);
            b.etReview.setText(r.text);
            for (int i = 0; i < b.moodChips.getChildCount(); i++) {
                Chip c = (Chip) b.moodChips.getChildAt(i);
                if (c.getText().toString().equals(r.mood)) c.setChecked(true);
            }
            List<String> old = r.tags == null ? new ArrayList<>() : Arrays.asList(r.tags.split("\\|"));
            for (int i = 0; i < b.tagChips.getChildCount(); i++) {
                Chip c = (Chip) b.tagChips.getChildAt(i);
                if (old.contains(c.getText().toString())) c.setChecked(true);
            }
        });

        b.btnSubmit.setOnClickListener(x -> {
            float stars = b.ratingBar.getRating();
            if (stars <= 0) return;
            String mood = "";
            int cid = b.moodChips.getCheckedChipId();
            if (cid != View.NO_ID) {
                Chip c = b.moodChips.findViewById(cid);
                if (c != null) mood = c.getText().toString();
            }
            List<String> tags = new ArrayList<>();
            for (int chipId : b.tagChips.getCheckedChipIds()) {
                Chip c = b.tagChips.findViewById(chipId);
                if (c != null) tags.add(c.getText().toString());
            }
            String text = String.valueOf(b.etReview.getText()).trim();
            repo.saveReview(id, title, poster, stars, mood, text, TextUtils.join("|", tags));
            SoundManager.success(requireContext());
            Toast.makeText(requireContext(), R.string.review_saved, Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
