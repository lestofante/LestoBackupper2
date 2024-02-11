package com.lesto.lestobackupper.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class FileItem {
    @PrimaryKey
    public long id;

    public String name;
    public String path;

    public String hash;

    public long last_update;

    public boolean should_backup;
    public boolean is_remote;
    public boolean is_local;

    public FileItem(long id, String name, String path, long last_update, String hash, boolean should_backup, boolean is_local, boolean is_remote){
        this.id = id;
        this.name = name;
        this.path = path;
        this.hash = hash;
        this.is_local = is_local;
        this.is_remote = is_remote;
        this.should_backup = should_backup;
        this.last_update = last_update;
    }
}