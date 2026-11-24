package com.katalogfilm.app.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        Entities.User.class, Entities.WatchItem.class, Entities.Review.class,
        Entities.History.class, Entities.Notif.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AppDao dao();

    private static volatile AppDatabase inst;

    public static AppDatabase get(Context c) {
        if (inst == null) {
            synchronized (AppDatabase.class) {
                if (inst == null) {
                    inst = Room.databaseBuilder(c.getApplicationContext(), AppDatabase.class, "katalog_film.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return inst;
    }
}
