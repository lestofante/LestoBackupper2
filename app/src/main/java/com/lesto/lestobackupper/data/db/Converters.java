package com.lesto.lestobackupper.data.db;

import android.net.Uri;
import android.util.Base64;

import androidx.room.TypeConverter;

import org.jspecify.annotations.NonNull;

public class Converters {
    @TypeConverter
    public static String fromUri(Uri uri) {
        return uri == null ? null : uri.toString();
    }

    @TypeConverter
    public static Uri toUri(String uriString) {
        return uriString == null ? null : Uri.parse(uriString);
    }

    @TypeConverter
    public static String fromByteArray(byte @NonNull [] bytes) {
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    @TypeConverter
    public static byte @NonNull [] toByteArray(@NonNull String  base64) {
        return Base64.decode(base64, Base64.DEFAULT);
    }
}