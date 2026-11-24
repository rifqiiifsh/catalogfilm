package com.katalogfilm.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.katalogfilm.app.BuildConfig;
import com.katalogfilm.app.R;
import com.katalogfilm.app.data.local.AppDao;
import com.katalogfilm.app.data.local.AppDatabase;
import com.katalogfilm.app.data.local.Entities;
import com.katalogfilm.app.data.remote.ApiClient;
import com.katalogfilm.app.data.remote.AuthApi;
import com.katalogfilm.app.data.remote.AuthClient;
import com.katalogfilm.app.data.remote.AuthModels;
import com.katalogfilm.app.data.remote.TmdbModels.Detail;
import com.katalogfilm.app.data.remote.TmdbModels.MovieList;
import com.katalogfilm.app.util.Session;
import com.katalogfilm.app.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Satu-satunya pintu data untuk UI: API TMDB (remote) + Room (lokal).
 * Callback remote dipanggil di main thread; tulis database berjalan di thread latar.
 */
public class Repository {

    public interface Result<T> {
        void ok(T data);
        void fail(String message);
    }

    public static final String PICK = "pick", TRENDING = "trending", NEW = "new", TOP = "top";

    private static Repository inst;
    private final Context appContext;
    private final AppDao dao;
    private final Session session;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private Repository(Context c) {
        Context app = c.getApplicationContext();
        appContext = app;
        dao = AppDatabase.get(app).dao();
        session = Session.get(app);
    }

    public static synchronized Repository get(Context c) {
        if (inst == null) inst = new Repository(c);
        return inst;
    }

    private long uid() { return session.userId(); }

    // =================== REMOTE (TMDB) ===================

    private boolean noKey(Result<?> cb) {
        if (BuildConfig.TMDB_API_KEY.isEmpty()) {
            cb.fail(appContext.getString(R.string.api_key_missing));
            return true;
        }
        return false;
    }

    private <T> Call<T> run(Call<T> call, Result<T> cb) {
        call.enqueue(new retrofit2.Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> r) {
                if (r.isSuccessful() && r.body() != null) cb.ok(r.body());
                else cb.fail(r.code() == 401 ? appContext.getString(R.string.api_key_invalid) : appContext.getString(R.string.api_server_error, r.code()));
            }
            @Override public void onFailure(Call<T> c, Throwable t) {
                if (!c.isCanceled()) cb.fail(appContext.getString(R.string.api_network_error));
            }
        });
        return call;
    }

    private int topVotes(String lang) {
        switch (lang) {
            case "en": return 2000;
            case "id": return 100;
            default: return 300;
        }
    }

    /** Bagian beranda: pick / trending / new / top untuk satu bahasa asli film. */
    public Call<MovieList> section(String section, String lang, String genres, int page, Result<MovieList> cb) {
        if (noKey(cb)) return null;
        String sort = "popularity.desc", from = null, to = null;
        Integer votes = 20;
        if (NEW.equals(section)) {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar c = Calendar.getInstance();
            to = f.format(c.getTime());
            c.add(Calendar.DAY_OF_YEAR, -90);
            from = f.format(c.getTime());
            votes = null;
        } else if (TOP.equals(section)) {
            sort = "vote_average.desc";
            votes = topVotes(lang);
        }
        return run(ApiClient.get().discover(lang, sort, page, from, to, votes, PICK.equals(section) ? genres : null), cb);
    }

    public Call<MovieList> discover(String lang, String sort, int page, Integer votes, String genres, Result<MovieList> cb) {
        if (noKey(cb)) return null;
        return run(ApiClient.get().discover(lang, sort, page, null, null, votes, genres), cb);
    }

    public Call<MovieList> search(String q, int page, Result<MovieList> cb) {
        if (noKey(cb)) return null;
        return run(ApiClient.get().search(q, page), cb);
    }

    public Call<Detail> detail(int id, Result<Detail> cb) {
        if (noKey(cb)) return null;
        return run(ApiClient.get().detail(id, "credits,videos", "id,en,null"), cb);
    }

    // =================== LOCAL (Room) ===================

    public LiveData<Entities.User> user() { return dao.user(uid()); }
    public LiveData<List<Entities.WatchItem>> watchlist() { return dao.watchlist(uid()); }
    public LiveData<Integer> isWatch(int movieId) { return dao.isWatch(uid(), movieId); }
    public LiveData<Integer> watchCount() { return dao.watchCount(uid()); }
    public LiveData<Entities.Review> review(int movieId) { return dao.review(uid(), movieId); }
    public LiveData<Integer> reviewCount() { return dao.reviewCount(uid()); }
    public LiveData<List<Entities.History>> history() { return dao.history(uid()); }
    public LiveData<Integer> historyCount() { return dao.historyCount(uid()); }
    public LiveData<List<Entities.Notif>> notifs() { return dao.notifs(uid()); }
    public LiveData<Integer> unread() { return dao.unread(uid()); }

    private void pushNotif(long u, String category, String title, String body) {
        if (!session.notifOn()) return;
        Entities.Notif n = new Entities.Notif();
        n.userId = u; n.category = category; n.title = title; n.body = body;
        n.createdAt = System.currentTimeMillis();
        dao.addNotif(n);
    }

    private String backendError(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                JSONObject json = new JSONObject(response.errorBody().string());
                String message = json.optString("message", "");
                if (!message.isEmpty()) return message;
            }
        } catch (Exception ignored) { }
        return fallback;
    }

    private void acceptAuth(AuthModels.AuthResponse response, Result<Long> cb) {
        if (response == null || response.user == null || response.user.id <= 0
                || response.token == null || response.token.isEmpty()) {
            cb.fail(appContext.getString(R.string.auth_response_incomplete));
            return;
        }
        Entities.User cached = new Entities.User();
        cached.id = response.user.id;
        cached.name = response.user.name == null ? "" : response.user.name;
        cached.email = response.user.email == null ? "" : response.user.email;
        cached.passHash = "";
        cached.createdAt = System.currentTimeMillis();
        io.execute(() -> {
            dao.cacheRemoteUser(cached);
            session.setAuthToken(response.token);
            main.post(() -> cb.ok(response.user.id));
        });
    }

    public void register(String name, String email, String pass, Result<Long> cb) {
        AuthApi api = AuthClient.get();
        api.register(new AuthModels.RegisterRequest(name.trim(), email.trim().toLowerCase(Locale.ROOT), pass))
                .enqueue(new retrofit2.Callback<AuthModels.AuthResponse>() {
                    @Override public void onResponse(Call<AuthModels.AuthResponse> call,
                            Response<AuthModels.AuthResponse> response) {
                        if (response.isSuccessful()) acceptAuth(response.body(), cb);
                        else cb.fail(backendError(response, appContext.getString(R.string.auth_register_failed)));
                    }
                    @Override public void onFailure(Call<AuthModels.AuthResponse> call, Throwable error) {
                        cb.fail(appContext.getString(R.string.auth_backend_connection_error));
                    }
                });
    }

    public void login(String email, String pass, Result<Long> cb) {
        AuthClient.get().login(new AuthModels.LoginRequest(email.trim().toLowerCase(Locale.ROOT), pass))
                .enqueue(new retrofit2.Callback<AuthModels.AuthResponse>() {
                    @Override public void onResponse(Call<AuthModels.AuthResponse> call,
                            Response<AuthModels.AuthResponse> response) {
                        if (response.isSuccessful()) acceptAuth(response.body(), cb);
                        else cb.fail(backendError(response, appContext.getString(R.string.auth_login_invalid)));
                    }
                    @Override public void onFailure(Call<AuthModels.AuthResponse> call, Throwable error) {
                        cb.fail(appContext.getString(R.string.auth_backend_connection_error));
                    }
                });
    }

    public void loginWithGoogle(String idToken, Result<Long> cb) {
        AuthClient.get().google(new AuthModels.GoogleRequest(idToken))
                .enqueue(new retrofit2.Callback<AuthModels.AuthResponse>() {
                    @Override public void onResponse(Call<AuthModels.AuthResponse> call,
                            Response<AuthModels.AuthResponse> response) {
                        if (response.isSuccessful()) acceptAuth(response.body(), cb);
                        else cb.fail(backendError(response, appContext.getString(R.string.auth_google_failed)));
                    }
                    @Override public void onFailure(Call<AuthModels.AuthResponse> call, Throwable error) {
                        cb.fail(appContext.getString(R.string.auth_backend_google_connection_error));
                    }
                });
    }

    public void forgotPassword(String email, Result<String> cb) {
        AuthClient.get().forgotPassword(new AuthModels.ForgotPasswordRequest(email.trim().toLowerCase(Locale.ROOT)))
                .enqueue(new retrofit2.Callback<AuthModels.MessageResponse>() {
                    @Override public void onResponse(Call<AuthModels.MessageResponse> call,
                            Response<AuthModels.MessageResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            cb.ok(response.body().message == null ? appContext.getString(R.string.auth_forgot_generic) : response.body().message);
                        } else cb.fail(backendError(response, appContext.getString(R.string.auth_forgot_failed)));
                    }
                    @Override public void onFailure(Call<AuthModels.MessageResponse> call, Throwable error) {
                        cb.fail(appContext.getString(R.string.auth_backend_connection_error));
                    }
                });
    }

    public void setPublic(boolean v) {
        final long u = uid();
        io.execute(() -> dao.setPublic(u, v));
    }

    public void addWatch(Detail d) {
        final long u = uid();
        io.execute(() -> {
            Entities.WatchItem w = new Entities.WatchItem();
            w.userId = u; w.movieId = d.id; w.title = d.title; w.posterPath = d.posterPath;
            w.overview = d.overview; w.releaseDate = d.releaseDate; w.rating = d.voteAverage;
            w.addedAt = System.currentTimeMillis();
            dao.addWatch(w);
            pushNotif(u, "Film", appContext.getString(R.string.notification_watch_added), appContext.getString(R.string.notification_watch_body, d.title));
        });
    }

    public void restoreWatch(Entities.WatchItem w) { io.execute(() -> dao.addWatch(w)); }

    public void removeWatch(int movieId) {
        final long u = uid();
        io.execute(() -> dao.removeWatch(u, movieId));
    }

    public void addHistory(Detail d) {
        final long u = uid();
        io.execute(() -> {
            Entities.History h = new Entities.History();
            h.userId = u; h.movieId = d.id; h.title = d.title; h.posterPath = d.posterPath;
            h.releaseDate = d.releaseDate; h.rating = d.voteAverage; h.viewedAt = System.currentTimeMillis();
            dao.addHistory(h);
        });
    }

    public void saveReview(int movieId, String title, String poster, float stars, String mood, String text, String tags) {
        final long u = uid();
        io.execute(() -> {
            Entities.Review r = new Entities.Review();
            r.userId = u; r.movieId = movieId; r.title = title; r.posterPath = poster;
            r.stars = stars; r.mood = mood; r.text = text; r.tags = tags;
            r.createdAt = System.currentTimeMillis();
            dao.saveReview(r);
            pushNotif(u, "Film", appContext.getString(R.string.notification_review_saved), appContext.getString(R.string.notification_review_body, title, stars));
        });
    }

    public void markAllRead() {
        final long u = uid();
        io.execute(() -> dao.readAll(u));
    }

    public void clearHistory() {
        final long u = uid();
        io.execute(() -> dao.clearHistory(u));
    }
}
