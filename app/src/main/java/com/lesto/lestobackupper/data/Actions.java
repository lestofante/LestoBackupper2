package com.lesto.lestobackupper.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.lesto.lestobackupper.Constants;

import java.util.ArrayList;
import java.util.List;

public class Actions {

    public static final Uri ALL_IMAGES = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    public static final Uri ALL_VIDEOS = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    public static final Uri ALL_AUDIOS = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    public static final Uri ALL_FILES = MediaStore.Files.getContentUri("external");

    static public List<FileItem> get_file_list(ContentResolver contentResolver, Uri currentUri) {
        Log.d(Constants.LESTO, "refresh_file_list for " + currentUri);

        // Define the columns you want to retrieve from the media store
        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
        };

        // Query the media store for audio files
        Cursor cursor = contentResolver.query(currentUri, projection, null, null, null);

        List<FileItem> lista = new ArrayList<FileItem>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Retrieve data from the cursor
                long id = cursor.getLong(0);
                String filePath = cursor.getString(1);
                String fileName = cursor.getString(2);

                // Log the retrieved information
                //Log.d(Constants.LESTO, "ID: " + id + " " + filePath);
                lista.add(new FileItem(id, fileName, filePath));

            } while (cursor.moveToNext());

            cursor.close();
        }else{
            Log.d(Constants.LESTO, "null cursor");
        }
        return lista;
    }

}
