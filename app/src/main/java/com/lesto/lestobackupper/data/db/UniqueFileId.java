package com.lesto.lestobackupper.data.db;

import org.jspecify.annotations.NonNull;

public class UniqueFileId {
    public @NonNull String md5;

    public @NonNull String sha1;

    public int collisionId;

    public UniqueFileId(byte @NonNull [] md5, byte @NonNull [] sha1, int collisionId) {
        this.md5 = Converters.fromByteArray(md5);
        this.sha1 = Converters.fromByteArray(sha1);
        this.collisionId = collisionId;
    }

    public UniqueFileId(@NonNull String md5, @NonNull String sha1, int collisionId) {
        this.md5 = md5;
        this.sha1 = sha1;
        this.collisionId = collisionId;
    }

    @NonNull
    @Override
    public String toString(){
        return md5 + sha1 + collisionId;
    }
}
