package com.lesto.lestobackupper.data.db;

import androidx.room.Ignore;

import com.lesto.lestobackupper.proto.FileDescription;

import org.jspecify.annotations.NonNull;

public class FileHash{
    public @NonNull String md5;
    public @NonNull String sha256;
    public long file_size;

    @Ignore
    public FileHash(byte @NonNull [] md5, byte @NonNull [] sha256, long file_size) {
        this.md5 = Converters.fromByteArray(md5);
        this.sha256 = Converters.fromByteArray(sha256);
        this.file_size = file_size;
    }

    public FileHash(@NonNull String md5, @NonNull String sha256, long file_size) {
        this.md5 = md5;
        this.sha256 = sha256;
        this.file_size = file_size;
    }

    @Override
    public boolean equals(Object other){
        if (other == this) { //is this myself?
            return true;
        }
        if (other instanceof FileHash other_casted) {
            return other_casted.md5.equals(md5) && other_casted.sha256.equals(sha256);
        }
        return false;
    }

    public boolean is_equals(FileDescription.FileHash other) {
        return other.getMd5().equals(md5) && other.getSha256().equals(sha256);
    }

    @NonNull
    @Override
    public String toString(){
        return "md5: " + md5 + " sha256: " + sha256 + " file_size: "+file_size;
    }

    public FileDescription.FileHash asFileHash() {
        return FileDescription.FileHash.newBuilder().setMd5(md5).setSha256(sha256).setFileSize(file_size).build();
    }
}
