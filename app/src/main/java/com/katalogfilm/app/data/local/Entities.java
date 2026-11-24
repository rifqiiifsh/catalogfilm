package com.katalogfilm.app.data.local;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Semua tabel database lokal (Room / SQLite). Guest memakai userId = 0. */
public class Entities {

    @Entity(tableName = "users", indices = {@Index(value = {"email"}, unique = true)})
    public static class User {
        @PrimaryKey(autoGenerate = true) public long id;
        public String name;
        public String email;
        public String passHash;
        public boolean publicProfile = true;
        public long createdAt;
    }

    @Entity(tableName = "watchlist", primaryKeys = {"userId", "movieId"})
    public static class WatchItem {
        public long userId;
        public int movieId;
        public String title;
        public String posterPath;
        public String overview;
        public String releaseDate;
        public double rating;
        public long addedAt;
    }

    @Entity(tableName = "reviews", primaryKeys = {"userId", "movieId"})
    public static class Review {
        public long userId;
        public int movieId;
        public String title;
        public String posterPath;
        public float stars;
        public String mood;
        public String text;
        public String tags;   // dipisah "|"
        public long createdAt;
    }

    @Entity(tableName = "history", primaryKeys = {"userId", "movieId"})
    public static class History {
        public long userId;
        public int movieId;
        public String title;
        public String posterPath;
        public String releaseDate;
        public double rating;
        public long viewedAt;
    }

    @Entity(tableName = "notifications")
    public static class Notif {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        public String category;   // "Film" | "Sistem"
        public String title;
        public String body;
        public long createdAt;
        public boolean isRead;
    }
}
