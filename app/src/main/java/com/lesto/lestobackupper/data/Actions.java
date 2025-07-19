package com.lesto.lestobackupper.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;


import androidx.room.MapColumn;

import com.lesto.lestobackupper.Constants;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    static public List<FileItem> get_file_list(ContentResolver contentResolver, Uri currentUri) {
        Log.d(Constants.LESTO, "refresh_file_list for " + currentUri);
        //Log.d(Constants.LESTO, "getExternalStoragePublicDirectory " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));

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
                //Log.d(Constants.LESTO, "ID: " + id + " " + fileName + " " + filePath);
                lista.add(new FileItem(id, fileName, currentUri.toString(), filePath, "", 0, "", true, true, false));

            } while (cursor.moveToNext());

            cursor.close();
        }else{
            Log.d(Constants.LESTO, "null cursor");
        }

        Log.d(Constants.LESTO, "get_file_list loaded " + lista.size() + " files");

        get_folder_list(contentResolver, currentUri);
        return lista;
    }

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
    public static String getFileHash(ContentResolver contentResolver, Uri contentUri) throws NoSuchAlgorithmException, IOException {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Open an input stream to read the file's contents
            try(FileInputStream inputStream = (FileInputStream) contentResolver.openInputStream(contentUri)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
                byte[] hashBytes = digest.digest();
                return new String(hashBytes, StandardCharsets.US_ASCII);//ascii is always a valid representation
            } catch (IOException e) {
                e.printStackTrace();
                throw e;
            }
        } catch (NoSuchAlgorithmException e) {
            Log.d(Constants.LESTO, "no SHA hash!");
            throw e;
        }
    }

    public static List<FileItem> databaseFileList(Context context) {
        FileDatabase db = AppDatabase.getInstance(context).fileDao();
        return new ArrayList<>(db.getAll().values());
    }

    public static Map<Long, FileItem> databaseFile(Context context) {
        FileDatabase db = AppDatabase.getInstance(context).fileDao();
        return db.getAll();
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

        Log.d(Constants.LESTO, "localUpdatedFileList Save new files in db, found: " + to_add.size());

        db.insertAll(to_add);

        Log.d(Constants.LESTO, "localUpdatedFileList find all locally deleted files, found: " + files.size());

        // remaining files means they are not local anymore
        for (FileItem i : files.values()){
            i.is_local = false;
            complete.add(i);
        }

        Log.d(Constants.LESTO, "localUpdatedFileList Save deleted files in db");

        db.updateAll(files.values());

        Log.d(Constants.LESTO, "localUpdatedFileList end, found: " + complete.size());

        return complete;
    }

    public static void update(Context context, FileItem f) {
        FileDatabase db = AppDatabase.getInstance(context).fileDao();
        db.update(f);
    }

    public static class CloudConfig{
        public final String MY_CLIENT_ID;
        public final Uri MY_REDIRECT_URI;

        public CloudConfig(String client_id, Uri uri){
            this.MY_CLIENT_ID = client_id;
            MY_REDIRECT_URI = uri;
        }
    }

//    public static void requestAuth2(Context context, CloudConfig c){
//        AuthorizationServiceConfiguration.fetchFromIssuer(
//            Uri.parse("https://idp.example.com"),
//            new AuthorizationServiceConfiguration.RetrieveConfigurationCallback() {
//                public void onFetchConfigurationCompleted(
//                        @Nullable AuthorizationServiceConfiguration serviceConfiguration,
//                        @Nullable AuthorizationException ex) {
//                    if (ex != null) {
//                        Log.e(Constants.LESTO, "failed to fetch configuration");
//                        return;
//                    }
//
//                    // use serviceConfiguration as needed
//                    AuthorizationRequest.Builder authRequestBuilder =
//                        new AuthorizationRequest.Builder(
//                            serviceConfiguration, // the authorization service configuration
//                            c.MY_CLIENT_ID, // the client ID, typically pre-registered and static
//                            ResponseTypeValues.CODE, // the response_type value: we want a code
//                            c.MY_REDIRECT_URI); // the redirect URI to which the auth response is sent
//                    AuthorizationService authService = new AuthorizationService(context);
//                    Intent authIntent = authService.getAuthorizationRequestIntent(authRequestBuilder.build());
//                    context.startActivityForResult(authIntent, RC_AUTH);
//                }
//            });
//    }
}
