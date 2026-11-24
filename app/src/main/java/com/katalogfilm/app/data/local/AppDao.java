package com.katalogfilm.app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AppDao {

    // ---- users
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertUser(Entities.User u);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void cacheRemoteUser(Entities.User u);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    Entities.User findUser(String email);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    LiveData<Entities.User> user(long id);

    @Query("UPDATE users SET publicProfile = :v WHERE id = :id")
    void setPublic(long id, boolean v);

    // ---- watchlist
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addWatch(Entities.WatchItem w);

    @Query("DELETE FROM watchlist WHERE userId = :u AND movieId = :m")
    void removeWatch(long u, int m);

    @Query("SELECT * FROM watchlist WHERE userId = :u ORDER BY addedAt DESC")
    LiveData<List<Entities.WatchItem>> watchlist(long u);

    @Query("SELECT COUNT(*) FROM watchlist WHERE userId = :u AND movieId = :m")
    LiveData<Integer> isWatch(long u, int m);

    @Query("SELECT COUNT(*) FROM watchlist WHERE userId = :u")
    LiveData<Integer> watchCount(long u);

    // ---- reviews
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveReview(Entities.Review r);

    @Query("SELECT * FROM reviews WHERE userId = :u AND movieId = :m LIMIT 1")
    LiveData<Entities.Review> review(long u, int m);

    @Query("SELECT COUNT(*) FROM reviews WHERE userId = :u")
    LiveData<Integer> reviewCount(long u);

    // ---- history
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addHistory(Entities.History h);

    @Query("SELECT * FROM history WHERE userId = :u ORDER BY viewedAt DESC LIMIT 20")
    LiveData<List<Entities.History>> history(long u);

    @Query("SELECT COUNT(*) FROM history WHERE userId = :u")
    LiveData<Integer> historyCount(long u);

    @Query("DELETE FROM history WHERE userId = :u")
    void clearHistory(long u);

    // ---- notifications
    @Insert
    void addNotif(Entities.Notif n);

    @Query("SELECT * FROM notifications WHERE userId = :u ORDER BY createdAt DESC")
    LiveData<List<Entities.Notif>> notifs(long u);

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :u AND isRead = 0")
    LiveData<Integer> unread(long u);

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :u")
    void readAll(long u);
}
