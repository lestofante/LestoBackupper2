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
import com.lesto.lestobackupper.data.db.FileItem;
import com.lesto.lestobackupper.data.db.UniqueFileId;

import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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
        UniqueFileId hash = getFileHash(contentResolver, contentUri);

        List<FileItem> result = db.getFileByHash(hash.md5, hash.sha1); // al potential revision with the same hashes

        if (result != null){
            for (FileItem f : result){
                if (hash.collisionId <= f.uniqueId.collisionId){
                    hash.collisionId = f.uniqueId.collisionId + 1;
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
        return hash;
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

    public static UniqueFileId getFileHash(ContentResolver contentResolver, Uri contentUri) throws NoSuchAlgorithmException, IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            MessageDigest md = MessageDigest.getInstance("MD5");
            // Open an input stream to read the file's contents
            try(FileInputStream inputStream = (FileInputStream) contentResolver.openInputStream(contentUri)) {
                assert inputStream != null;
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                    md.update(buffer, 0, bytesRead);
                }

                long res = 0;

                byte[] hashBytesMD = md.digest();
                assert hashBytesMD.length == 16;

                byte[] hashBytes = digest.digest();
                assert hashBytes.length == 32;

                return new UniqueFileId(hashBytesMD, hashBytes, 0);
            } catch (IOException e) {
                e.printStackTrace();
                throw e;
            }
        } catch (NoSuchAlgorithmException e) {
            Log.d(Constants.LESTO, "no SHA hash!");
            throw e;
        }
    }

//    public static void listFolder(Context context, DocumentFile file, List<FileItem> fileList){
//
//        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
//        FileDatabase fileDb = db.fileDao();
//        if (file != null){
//            if (file.isDirectory()) {
//                for (DocumentFile subfile : file.listFiles()) {
//                    if (subfile.isFile()) {
//                        Log.d("File", "\tFile Name: " + subfile.getName());
//
//                        // Log the retrieved information
//                        //Log.d(Constants.LESTO, "ID: " + id + " " + fileName + " " + filePath);
//
//                        try {
//                            Pair<Boolean, UniqueFileId> id_hash = Actions.getFileUniqueIdAndTrueIfExist(fileDb, context.getContentResolver(), subfile.getUri());
//                            if (!id_hash.first) {
//                                fileList.add(new FileItem(id_hash.second, subfile.getName(), subfile.getUri(), null, 0, true, false));
//                            }
//                        } catch (NoSuchAlgorithmException e) {
//                            e.printStackTrace();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    if (subfile.isDirectory()) {
//                        // RECURSE!
//                        String a = subfile.getUri().toString();
//                        String b = file.getUri().toString();
//                        if (!a.equals(b)) {
//                            listFolder(context, subfile, fileList);
//                        } else {
//                            //weird duplicate case
//                        }
//                    }
//                }
//            }
//            if (file.isFile()) {
//                Log.d("File", "\tFile Name: " + file.getName());
//
//                try {
//                    Pair<Boolean, UniqueFileId> id_hash = Actions.getFileUniqueIdAndTrueIfExist(fileDb, context.getContentResolver(), file.getUri());
//                    if (!id_hash.first) {
//                        assert file.getName() != null;
//                        fileList.add(new FileItem(id_hash.second, file.getName(), file.getUri(), null, 0, true, false));
//                    }
//                } catch (NoSuchAlgorithmException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    public static List<FileItem> localUpdatedFileList(Context context) {

        Log.d(Constants.LESTO, "localUpdatedFileList load DB");
        FileDatabase db = AppDatabase.getInstance(context).fileDao();
        Log.d(Constants.LESTO, "localUpdatedFileList Get all");
        Map<String, FileItem> files = db.getAllByRemoteId();

        ArrayList<FileItem> complete = new ArrayList<>();
        Log.d(Constants.LESTO, "localUpdatedFileList Find new files missing in db " + db.getAllFolder().size() + " " + files.size());
/*
        for (FolderItem folder : db.getAllFolder()){
            Log.d("File", "FolderItem: " + folder.localUri);
            Uri treeUri = Uri.parse(folder.localUri);
            //DocumentFile fileOnDisk = DocumentFile.fromTreeUri(context, treeUri);

            ArrayList<FileItem> tmp = new ArrayList<>();
            DocumentFile fileOnDisk = DocumentFile.fromTreeUri(context, treeUri);
            listFolder(context, fileOnDisk, tmp);
            for (FileItem i: tmp){
                Log.d(Constants.LESTO, "localUpdatedFileList file: " + i.name);
                FileItem db_item = files.remove(i.localId);
                if (db_item == null){
                    Log.d(Constants.LESTO, "^ is NEW: " + i.name);
                    to_add.add(i);
                }
                complete.add(i);
            }
        }
*/
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
        //db.insertAll(newFiles);
        //newFiles.clear();


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
