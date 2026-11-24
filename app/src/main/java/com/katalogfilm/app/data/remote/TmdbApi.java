package com.katalogfilm.app.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbApi {

    @GET("discover/movie")
    Call<TmdbModels.MovieList> discover(
            @Query("with_original_language") String lang,
            @Query("sort_by") String sort,
            @Query("page") int page,
            @Query("primary_release_date.gte") String from,
            @Query("primary_release_date.lte") String to,
            @Query("vote_count.gte") Integer minVotes,
            @Query("with_genres") String genres);

    @GET("search/movie")
    Call<TmdbModels.MovieList> search(@Query("query") String q, @Query("page") int page);

    @GET("movie/{id}")
    Call<TmdbModels.Detail> detail(
            @Path("id") int id,
            @Query("append_to_response") String append,
            @Query("include_video_language") String videoLang);
}
