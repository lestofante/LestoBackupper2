package com.lesto.lestobackupper.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class FolderItem {
    @PrimaryKey @NonNull
    public String localUri;

    public FolderItem(@NonNull String localUri){
        this.localUri = localUri;
    }


    public FolderItem(FolderItem f, String fileHash) {
        this.localUri = f.localUri;
    }

}