package com.lesto.lestobackupper.data.db;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(indices = {@Index(value = {"md5", "sha1", "collisionId"}, unique = true)})
public class FileItem {

    @PrimaryKey(autoGenerate = true)
    long localId;

    @NonNull
    @Embedded
    public UniqueFileId uniqueId;

    @NonNull
    public String name;
    public Uri localUri; // null: not local. May be a SAF or MediaStore
    public Uri remoteUri; // null: not remote
    public long last_update;
    public boolean should_download;
    public boolean should_backup;

    public FileItem(@NonNull UniqueFileId uniqueId, @NonNull String name, Uri localUri, Uri remoteUri, long last_update, boolean should_download, boolean should_backup){
        this.uniqueId = uniqueId;
        this.name = name;
        this.localUri = localUri;
        this.remoteUri = remoteUri;
        this.should_backup = should_backup;
        this.last_update = last_update;
        this.should_download = should_download;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileItem fileItem = (FileItem) o;

        return  localId == fileItem.localId &&
                uniqueId.equals(fileItem.uniqueId) &&
                last_update == fileItem.last_update &&
                should_backup == fileItem.should_backup &&
                should_download == fileItem.should_download &&

                Objects.equals(name, fileItem.name) &&
                Objects.equals(localUri, fileItem.localUri) &&
                Objects.equals(remoteUri, fileItem.remoteUri);
    }

    @Override
    public int hashCode() {
        return (int)localId; //good enough
    }

    public long gerLocalId() {
        return localId;
    }
}