package com.katalogfilm.app.util;

import android.content.Context;
import com.katalogfilm.app.R;

/** Daftar genre TMDB; label lokal diambil dari strings.xml. */
public final class Genres {
    private Genres() { }
    public static final int[] IDS = {28, 12, 16, 35, 80, 99, 18, 10751, 14, 36, 27, 10402, 9648, 10749, 878, 53, 10752, 37};
    public static final String[] EMOJIS = {"💥", "🧭", "🎨", "😂", "🕵️", "🎞️", "🎭", "👪", "🧙", "🏛️", "👻", "🎵", "🔍", "❤️", "🚀", "😱", "🪖", "🤠"};

    public static String name(Context context, int id) {
        String[] names = context.getResources().getStringArray(R.array.genres_names);
        for (int i = 0; i < IDS.length; i++) if (IDS[i] == id) return names[i];
        return context.getString(R.string.genre_unknown);
    }
}
