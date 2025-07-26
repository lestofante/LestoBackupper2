package com.lesto.lestobackupper.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity
public class FileItem {
    @PrimaryKey
    public long id;

    public String name;
    public String mediastoreUri;
    public String localUri;
    public String remoteUri;
    public long last_update;
    public String hash;
    public boolean is_local;
    public boolean should_download;
    public boolean is_remote;
    public boolean should_backup;

    public FileItem(long id, String name, String mediastoreUri, String localUri, String remoteUri, long last_update, String hash, boolean is_local, boolean should_download, boolean is_remote, boolean should_backup){
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
        this.should_download = should_download;
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
        this.should_download = f.should_download;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileItem fileItem = (FileItem) o;

        return id == fileItem.id &&
                last_update == fileItem.last_update &&
                should_backup == fileItem.should_backup &&
                is_remote == fileItem.is_remote &&
                is_local == fileItem.is_local &&
                should_download == fileItem.should_download &&
                Objects.equals(name, fileItem.name) &&
                Objects.equals(mediastoreUri, fileItem.mediastoreUri) &&
                Objects.equals(localUri, fileItem.localUri) &&
                Objects.equals(remoteUri, fileItem.remoteUri) &&
                Objects.equals(hash, fileItem.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, mediastoreUri, localUri, remoteUri, hash,
                last_update, should_backup, is_remote, is_local, should_download);
    }
}