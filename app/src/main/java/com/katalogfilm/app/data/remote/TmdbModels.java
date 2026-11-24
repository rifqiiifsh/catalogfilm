package com.katalogfilm.app.data.remote;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Model respons API TMDB (Gson). */
public class TmdbModels {

    public static class Movie {
        public int id;
        public String title;
        public String overview;
        @SerializedName("poster_path") public String posterPath;
        @SerializedName("backdrop_path") public String backdropPath;
        @SerializedName("vote_average") public double voteAverage;
        @SerializedName("release_date") public String releaseDate;
        @SerializedName("genre_ids") public List<Integer> genreIds;
    }

    public static class MovieList {
        public int page;
        @SerializedName("total_pages") public int totalPages;
        @SerializedName("total_results") public int totalResults;
        public List<Movie> results;
    }

    public static class Genre { public int id; public String name; }

    public static class Cast {
        public int id;
        public String name;
        public String character;
        @SerializedName("profile_path") public String profilePath;
    }

    public static class Crew { public String name; public String job; }

    public static class Credits { public List<Cast> cast; public List<Crew> crew; }

    public static class Video {
        public String key;
        public String site;
        public String type;
        public boolean official;
    }

    public static class Videos { public List<Video> results; }

    public static class Detail {
        public int id;
        public String title;
        public String tagline;
        public String overview;
        public int runtime;
        public List<Genre> genres;
        public Credits credits;
        public Videos videos;
        @SerializedName("poster_path") public String posterPath;
        @SerializedName("backdrop_path") public String backdropPath;
        @SerializedName("vote_average") public double voteAverage;
        @SerializedName("vote_count") public int voteCount;
        @SerializedName("release_date") public String releaseDate;
    }
}
