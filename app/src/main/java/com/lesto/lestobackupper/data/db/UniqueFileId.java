package com.lesto.lestobackupper.data.db;

import androidx.room.Embedded;
import androidx.room.Ignore;

import com.lesto.lestobackupper.proto.FileDescription;

import org.jspecify.annotations.NonNull;

public class UniqueFileId {
    @NonNull
    @Embedded
    public FileHash hash;
    public int collisionId;

    public UniqueFileId(@NonNull FileHash hash, int collisionId) {
        this.hash = hash;
        this.collisionId = collisionId;
    }

    @Override
    public boolean equals(Object other){
        if (other == this) { //is this myself?
            return true;
        }
        if (other instanceof UniqueFileId other_casted) {
            return other_casted.collisionId == collisionId && other_casted.hash.equals(hash);
        }
        return false;
    }

    @NonNull
    @Override
    public String toString(){
        return "collisionId: " + collisionId + " " + hash;
    }

    public FileDescription.UniqueFileId asUniqueFileId() {
        return FileDescription.UniqueFileId.newBuilder().setCollisionId(collisionId).setHash(hash.asFileHash()).build();
    }
}
