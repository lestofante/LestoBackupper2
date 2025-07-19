package com.lesto.lestobackupper.data;

import android.net.Uri;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;

import java.nio.charset.StandardCharsets;

@Entity
public class FileItem {
    @PrimaryKey
    public long id;

    public String name;

    public String mediastoreUri;
    public String localUri;
    public String remoteUri;

    public String hash;

    public long last_update;

    public boolean should_backup;
    public boolean is_remote;
    public boolean is_local;

    public FileItem(long id, String name, String mediastoreUri, String localUri, String remoteUri, long last_update, String hash, boolean should_backup, boolean is_local, boolean is_remote){
        this.id = id;
        this.name = name;
        this.mediastoreUri = mediastoreUri;
        this.localUri = localUri;
        this.remoteUri = remoteUri;
        this.hash = hash;
        this.is_local = is_local;
        this.is_remote = is_remote;
        this.should_backup = should_backup;
        this.last_update = last_update;
    }

    public FileItem(FileItem f, String fileHash) {
        this.id = f.id;
        this.name = f.name;
        this.mediastoreUri = f.mediastoreUri;
        this.localUri = f.localUri;
        this.remoteUri = f.remoteUri;
        this.hash = fileHash;
        this.is_local = f.is_local;
        this.is_remote = f.is_remote;
        this.should_backup = f.should_backup;
        this.last_update = f.last_update;
    }
}