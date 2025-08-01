package com.lesto.lestobackupper.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;


import com.lesto.lestobackupper.Constants;
import com.lesto.lestobackupper.data.db.AppDatabase;
import com.lesto.lestobackupper.data.db.FileDatabase;
import com.lesto.lestobackupper.data.db.FileHash;
import com.lesto.lestobackupper.data.db.FileItem;
import com.lesto.lestobackupper.data.db.UniqueFileId;
import com.lesto.lestobackupper.proto.FileDescription;

import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Actions {

    public static final Uri ALL_IMAGES = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    public static final Uri ALL_VIDEOS = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    public static final Uri ALL_AUDIOS = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    public static final Uri ALL_FILES = MediaStore.Files.getContentUri("external");

    static public Set<String> get_folder_list(ContentResolver contentResolver, Uri currentUri) {
        Log.d(Constants.LESTO, "get_folder_list for " + currentUri);

        // Define the columns you want to retrieve from the media store
        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATA,
        };

        // Query the media store for audio files
        Cursor cursor = contentResolver.query(currentUri, projection, null, null, null);

        Set<String> uniqueSet = new HashSet<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Retrieve data from the cursor
                long id = cursor.getLong(0);
                String filePath = cursor.getString(1);
                filePath = filePath.substring(0, filePath.lastIndexOf('/'));

                // Log the retrieved information
                //Log.d(Constants.LESTO, "ID: " + id + " " + filePath);
                boolean isNew = uniqueSet.add(filePath);
                if (isNew){
                    Log.d(Constants.LESTO, "New path: " + filePath);
                }

            } while (cursor.moveToNext());

            cursor.close();
        }else{
            Log.d(Constants.LESTO, "null cursor");
        }

        Log.d(Constants.LESTO, "get_folder_list loaded " + uniqueSet.size() + " files");
        return uniqueSet;
    }
/*
    static public List<FileItem> get_file_list_MEDIASTORE(ContentResolver contentResolver, Uri currentUri) {
        Log.d(Constants.LESTO, "refresh_file_list for " + currentUri);
        //Log.d(Constants.LESTO, "getExternalStoragePublicDirectory " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));

        // Define the columns you want to retrieve from the media store
        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
        };

        Cursor cursor = contentResolver.query(currentUri, projection, null, null, null);
        List<FileItem> lista = new ArrayList<FileItem>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Retrieve data from the cursor
                long id = cursor.getLong(0);
                String filePath = cursor.getString(1);
                String fileName = cursor.getString(2);

                // Log the retrieved information
                //Log.d(Constants.LESTO, "ID: " + id + " " + fileName + " " + filePath);
                lista.add(new FileItem(id, fileName, currentUri.toString(), filePath, "", 0, "", true, true, false, true));

            } while (cursor.moveToNext());

            cursor.close();
        }else{
            Log.d(Constants.LESTO, "null cursor");
        }

        Log.d(Constants.LESTO, "get_file_list loaded " + lista.size() + " files");

        get_folder_list(contentResolver, currentUri);
        return lista;
    }
*/
    public static long getFileSize(ContentResolver contentResolver, Uri contentUri) throws IOException {
        String[] projection = {OpenableColumns.SIZE};
        try (Cursor cursor = contentResolver.query(contentUri, projection, null, null, null)){
            // Query the content resolver to get the file's size

            if (cursor != null && cursor.moveToFirst()) {
                // Get the column index of the file size
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    // Retrieve the file size
                    return cursor.getLong(sizeIndex);
                }
            }
        }
        throw new IOException("unknown file size");
    }

    @NotNull
    public static UniqueFileId getFileUniqueId(FileDatabase db, ContentResolver contentResolver, Uri contentUri) throws NoSuchAlgorithmException, IOException {
        FileHash hash = getFileHash(contentResolver, contentUri);

        List<FileItem> result = db.getFileByHash(hash.md5, hash.sha256); // al potential revision with the same hashes

        int tmp_collision_id = 0;
        if (result != null){
            for (FileItem f : result){
                if (tmp_collision_id <= f.uniqueId.collisionId){
                    tmp_collision_id = f.uniqueId.collisionId + 1;
                }
                if (f.localUri != null) {
                    if (areEquals(contentResolver, contentUri, f.localUri)) {
                        return f.uniqueId;
                    }
                }else{
                    Log.e("getFileUniqueId", "Trying to compare " + contentUri + " with a file that is not available locally: " + f.name + " stored at: " + f.remoteUri);
                }
            }
        }
        return new UniqueFileId(hash, tmp_collision_id);
    }

     public static boolean areEquals(ContentResolver contentResolver, Uri uri1, Uri uri2) {
        try (InputStream is1 = contentResolver.openInputStream(uri1);
             InputStream is2 = contentResolver.openInputStream(uri2)) {

            if (is1 == null || is2 == null) return false;

            byte[] buffer1 = new byte[4096];
            byte[] buffer2 = new byte[4096];

            int len1, len2;
            while ((len1 = is1.read(buffer1)) != -1) {
                len2 = is2.read(buffer2);
                if (len1 != len2 || !Arrays.equals(buffer1, buffer2)) {
                    return false;
                }
            }

            // Make sure both streams ended
            return is2.read() == -1;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static FileHash getFileHash(ContentResolver contentResolver, Uri contentUri) throws NoSuchAlgorithmException, IOException {
        return getFileHash(contentResolver, contentUri, -1); //-1 == read all file
    }

    public static FileHash getFileHash(ContentResolver contentResolver, Uri contentUri, long filesize) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        MessageDigest md = MessageDigest.getInstance("MD5");
        // Open an input stream to read the file's contents
        try(FileInputStream inputStream = (FileInputStream) contentResolver.openInputStream(contentUri)) {
            assert inputStream != null;
            final int DEFAULT_READ_SIZE = 8192;
            byte[] buffer;
            if (filesize != -1 && filesize < DEFAULT_READ_SIZE)
                buffer = new byte[(int)filesize];
            else
                buffer = new byte[DEFAULT_READ_SIZE];

            long remaining = filesize;
            long size = 0;
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1 && remaining != 0) {
                digest.update(buffer, 0, bytesRead);
                md.update(buffer, 0, bytesRead);
                size += bytesRead;

                if (remaining != -1){
                    //if we are not reading the full file, make sure to read only the remaining
                    remaining -= bytesRead;
                    if (remaining > buffer.length){
                        buffer = new byte[(int)remaining];
                    }
                }
            }

            byte[] hashBytesMD = md.digest();
            assert hashBytesMD.length == 16;

            byte[] hashBytes = digest.digest();
            assert hashBytes.length == 32;

            return new FileHash(hashBytesMD, hashBytes, size);
        }
    }

    public static List<FileItem> localUpdatedFileList(Context context) {

        Log.d(Constants.LESTO, "localUpdatedFileList load DB");
        FileDatabase db = AppDatabase.getInstance(context).fileDao();
        Log.d(Constants.LESTO, "localUpdatedFileList Get all");
        Map<String, FileItem> files = new HashMap<>();
        for (FileItem f : db.getAllByRemoteId()){
            files.put(f.uniqueId.toString(), f);
        }

        ArrayList<FileItem> complete = new ArrayList<>();
        Log.d(Constants.LESTO, "localUpdatedFileList Find new files missing in db " + db.getAllFolder().size() + " " + files.size());

        Log.d(Constants.LESTO, "updating mediastore");
        int new_count = 0, verified_count = 0;
        //List<FileItem> newFiles = new ArrayList<>(100);
        for (Uri mediaUri : listAllImageUris(context)){
            try {
                UniqueFileId unique_id = Actions.getFileUniqueId(db, context.getContentResolver(), mediaUri);

                FileItem db_existing_item = files.remove(unique_id.toString());
                if (db_existing_item == null) {
                    FileItem tmp = new FileItem(unique_id, mediaUri.toString(), mediaUri, null, 0, true, true);
                    db.insert(tmp);
                    //newFiles.add(tmp);
                    //if (newFiles.size() == 100){
                    //   db.insertAll(newFiles);
                    //    newFiles.clear();
                    //}
                    new_count++;
                    Log.d(Constants.LESTO, "localUpdatedFileList NEW: " + tmp.name);
                }else{
                    verified_count++;
                    Log.d(Constants.LESTO, "verified: " + mediaUri.toString());
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                assert false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.i(Constants.LESTO, "localUpdatedFileList Save new files in db, new found: " + new_count + " verified: " + verified_count);

        Log.d(Constants.LESTO, "localUpdatedFileList find all locally deleted files, found: " + files.size());

        // remaining files means they are not local anymore
        for (Map.Entry<String, FileItem> i : files.entrySet()) {
            Log.d(Constants.LESTO, "existing file has been deleted locally: KEY: "+i.getKey() + " VAL "+i.getValue().name + " " +i.getValue().uniqueId);
        }
        for (FileItem i : files.values()){
            if (i.localUri != null) {
                //Log.d(Constants.LESTO, "existing file has been deleted locally: "+i.localId + " "+i.name + " " +i.uniqueId);
            }
            i.localUri = null;
        }

        Log.d(Constants.LESTO, "localUpdatedFileList Save deleted files in db");

        db.updateAll(files.values());

        Log.d(Constants.LESTO, "localUpdatedFileList end, found: " + complete.size());

        return complete;
    }

    @NotNull
    public static Set<Uri> listAllImageUris(Context context) {
        Set<Uri> imageUris = new TreeSet<>();

        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);

        String[] projection = new String[] {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
        };

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                null,
                null,
                null)) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(id)
                    );
                    imageUris.add(contentUri);
                }
            }
        }
        return imageUris;
    }


    public static void update(Context context, FileItem f) {
        FileDatabase db = AppDatabase.getInstance(context).fileDao();
        db.update(f);
    }

    public static FileItem getFromDb(Context context, FileDescription.UniqueFileId fileId) {
        FileDatabase db = AppDatabase.getInstance(context).fileDao();
        List<FileItem> fileByUniqueId = db.getFileByUniqueId(fileId.getHash().getMd5(), fileId.getHash().getSha256(), fileId.getCollisionId());
        assert fileByUniqueId.isEmpty() || fileByUniqueId.size() == 1; //this should be a unique key
        if (fileByUniqueId.isEmpty()){
            return null;
        }else{
            return fileByUniqueId.get(0);
        }
    }
}
