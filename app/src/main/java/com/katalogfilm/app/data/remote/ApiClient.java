package com.katalogfilm.app.data.remote;

import com.katalogfilm.app.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String IMG = "https://image.tmdb.org/t/p/";
    private static TmdbApi api;

    public static synchronized TmdbApi get() {
        if (api == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        HttpUrl url = chain.request().url().newBuilder()
                                .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                                .addQueryParameter("language", "id-ID")
                                .addQueryParameter("include_adult", "false")
                                .build();
                        return chain.proceed(chain.request().newBuilder().url(url).build());
                    })
                    .build();
            api = new Retrofit.Builder()
                    .baseUrl("https://api.themoviedb.org/3/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(TmdbApi.class);
        }
        return api;
    }

    public static String poster(String p) { return p == null ? null : IMG + "w342" + p; }
    public static String backdrop(String p) { return p == null ? null : IMG + "w780" + p; }
    public static String profile(String p) { return p == null ? null : IMG + "w185" + p; }
}
