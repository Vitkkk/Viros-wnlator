package com.winlator.library.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {GameEntity.class}, version = 1, exportSchema = false)
public abstract class GameDatabase extends RoomDatabase {
    private static volatile GameDatabase instance;

    public abstract GameDao gameDao();

    public static GameDatabase get(Context context) {
        if (instance == null) {
            synchronized (GameDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        GameDatabase.class,
                        "viros-library.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
