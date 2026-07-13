package com.secretary.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a file stored in the app's private storage.
 */
@Entity(tableName = "files")
public class FileEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Original display name */
    @ColumnInfo(name = "name")
    private String name;

    /** Absolute path to the file in private storage */
    @ColumnInfo(name = "path")
    private String path;

    /** File type: "image", "video", "audio", "other" */
    @ColumnInfo(name = "type")
    private String type;

    /** MIME type of the file */
    @ColumnInfo(name = "mime_type")
    private String mimeType;

    /** File size in bytes */
    @ColumnInfo(name = "size")
    private long size;

    /** Timestamp when the file was imported (milliseconds) */
    @ColumnInfo(name = "added_time")
    private long addedTime;

    /** Folder ID this file belongs to (0 = root / no folder) */
    @ColumnInfo(name = "folder_id", defaultValue = "0")
    private long folderId;

    public FileEntity() {}

    @Ignore
    public FileEntity(String name, String path, String type, String mimeType, long size) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.mimeType = mimeType;
        this.size = size;
        this.addedTime = System.currentTimeMillis();
        this.folderId = 0;
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public long getAddedTime() { return addedTime; }
    public void setAddedTime(long addedTime) { this.addedTime = addedTime; }

    public long getFolderId() { return folderId; }
    public void setFolderId(long folderId) { this.folderId = folderId; }
}
