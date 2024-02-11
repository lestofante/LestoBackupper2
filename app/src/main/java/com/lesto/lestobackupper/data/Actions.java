package com.lesto.lestobackupper.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.lesto.lestobackupper.Constants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                lista.add(new FileItem(id, fileName, filePath, 0, "", true, true, false));

            } while (cursor.moveToNext());

            cursor.close();
        }else{
            Log.d(Constants.LESTO, "null cursor");
        }
        return lista;
    }

    public static String getFileHash(ContentResolver contentResolver, Uri contentUri) {

        // Open an input stream to read the file's contents
        try(FileInputStream inputStream = (FileInputStream) contentResolver.openInputStream(contentUri);) {
            // Compute the hash of the file's contents
            if (inputStream != null) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
                byte[] hashBytes = digest.digest();
                inputStream.close();

                // Convert the hash bytes to a hexadecimal string
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<FileItem> localUpdatedFileList(Context context) {

        Log.d(Constants.LESTO, "localUpdatedFileList load DB");
        FileDatabase db = AppDatabase.getInstance(context).fileDao();
        Log.d(Constants.LESTO, "localUpdatedFileList Get all");
        Map<Long, FileItem> files = db.getAll();

        ArrayList<FileItem> complete = new ArrayList<>();
        ArrayList<FileItem> to_add = new ArrayList<>();
        Log.d(Constants.LESTO, "localUpdatedFileList Find new files missing in db");
        for (FileItem i : Actions.get_file_list(context.getContentResolver(), Actions.ALL_IMAGES)){
            FileItem db_item = files.remove(i.id);
            if (db_item == null){
                to_add.add(i);
            }
            complete.add(i);
        }

        Log.d(Constants.LESTO, "localUpdatedFileList Save new files in db");

        db.insertAll(to_add);

        Log.d(Constants.LESTO, "localUpdatedFileList find all locally deleted files");

        // remaining files means they are not local anymore
        for (FileItem i : files.values()){
            i.is_local = false;
            complete.add(i);
        }

        Log.d(Constants.LESTO, "localUpdatedFileList Save deleted files in db");

        db.updateAll(files.values());

        Log.d(Constants.LESTO, "localUpdatedFileList end");

        return complete;
    }

}
