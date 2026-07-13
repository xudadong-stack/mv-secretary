package com.secretary.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a folder for organizing files within a category.
 */
@Entity(tableName = "folders")
public class FolderEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Folder display name */
    @ColumnInfo(name = "name")
    private String name;

    /** Category type: "image", "video", "audio", "other" */
    @ColumnInfo(name = "type")
    private String type;

    /** Parent folder id (0 = root level) */
    @ColumnInfo(name = "parent_id", defaultValue = "0")
    private long parentId;

    /** Creation timestamp (milliseconds) */
    @ColumnInfo(name = "created_at")
    private long createdAt;

    public FolderEntity() {}

    @Ignore
    public FolderEntity(String name, String type, long parentId) {
        this.name = name;
        this.type = type;
        this.parentId = parentId;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getParentId() { return parentId; }
    public void setParentId(long parentId) { this.parentId = parentId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
