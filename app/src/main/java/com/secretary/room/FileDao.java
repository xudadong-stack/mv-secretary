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
public interface FileDao {

    @Query("SELECT * FROM files ORDER BY added_time DESC")
    Flowable<List<FileEntity>> getAllFiles();

    @Query("SELECT * FROM files WHERE type = :type ORDER BY added_time DESC")
    Flowable<List<FileEntity>> getFilesByType(String type);

    /** Get files in a specific type AND folder */
    @Query("SELECT * FROM files WHERE type = :type AND folder_id = :folderId ORDER BY added_time DESC")
    Flowable<List<FileEntity>> getFilesByTypeAndFolder(String type, long folderId);

    /** Get ALL files in a folder regardless of type */
    @Query("SELECT * FROM files WHERE folder_id = :folderId ORDER BY added_time DESC")
    Flowable<List<FileEntity>> getFilesByFolder(long folderId);

    /** Get files in a root folder (no folder assigned) for a type */
    @Query("SELECT * FROM files WHERE type = :type AND folder_id = 0 ORDER BY added_time DESC")
    Flowable<List<FileEntity>> getFilesByTypeRoot(String type);

    @Query("SELECT * FROM files WHERE id = :id")
    Single<FileEntity> getFileById(long id);

    @Insert
    Single<Long> insert(FileEntity file);

    @Update
    Single<Integer> update(FileEntity file);

    @Delete
    Single<Integer> delete(FileEntity file);

    @Query("DELETE FROM files WHERE id = :id")
    Single<Integer> deleteById(long id);

    @Query("DELETE FROM files WHERE path = :path")
    Single<Integer> deleteByPath(String path);

    /** Move all files from a folder to root when folder is deleted */
    @Query("UPDATE files SET folder_id = 0 WHERE folder_id = :folderId")
    Single<Integer> moveFilesToRoot(long folderId);
}
