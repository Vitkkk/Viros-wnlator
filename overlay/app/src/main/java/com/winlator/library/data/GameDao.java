package com.winlator.library.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GameDao {
    @Query("SELECT * FROM games ORDER BY favorite DESC, name COLLATE NOCASE ASC")
    List<GameEntity> getAll();

    @Query("SELECT * FROM games WHERE executableUri = :uri LIMIT 1")
    GameEntity findByExecutableUri(String uri);

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    GameEntity findById(long id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(GameEntity game);

    @Update
    void update(GameEntity game);

    @Query("DELETE FROM games WHERE id = :id")
    void removeFromLibrary(long id);

    @Query("DELETE FROM games WHERE folderUri LIKE :treePrefix || '%' AND executableUri NOT IN (:seenExecutableUris)")
    void removeMissingFromTree(String treePrefix, List<String> seenExecutableUris);
}
