package com.secretary.room;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public interface FolderDao {

    /** Get all root-level folders for a category */
    @Query("SELECT * FROM folders WHERE type = :type AND parent_id = 0 ORDER BY created_at DESC")
    Flowable<List<FolderEntity>> getRootFoldersByType(String type);

    /** Get sub-folders inside a parent folder */
    @Query("SELECT * FROM folders WHERE parent_id = :parentId ORDER BY created_at DESC")
    Flowable<List<FolderEntity>> getSubFolders(long parentId);

    /** Get a single folder by id */
    @Query("SELECT * FROM folders WHERE id = :id")
    Single<FolderEntity> getFolderById(long id);

    @Insert
    Single<Long> insert(FolderEntity folder);

    @Update
    Single<Integer> update(FolderEntity folder);

    @Delete
    Single<Integer> delete(FolderEntity folder);

    @Query("DELETE FROM folders WHERE id = :id")
    Single<Integer> deleteById(long id);

    /** Delete all folders of a given type (used when deleting a category) */
    @Query("DELETE FROM folders WHERE type = :type")
    Single<Integer> deleteByType(String type);
}
