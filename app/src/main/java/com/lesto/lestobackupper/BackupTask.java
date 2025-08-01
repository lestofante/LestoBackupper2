package com.lesto.lestobackupper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;

import com.google.protobuf.ByteString;
import com.lesto.lestobackupper.data.Actions;
import com.lesto.lestobackupper.data.db.AppDatabase;
import com.lesto.lestobackupper.data.db.FileDatabase;
import com.lesto.lestobackupper.data.db.FileHash;
import com.lesto.lestobackupper.data.db.FileItem;
import com.lesto.lestobackupper.proto.FileDescription;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class BackupTask extends Service {

    private static final int NOTIFICATION_ID = 123;
    MyFileObserver fileObserver = new MyFileObserver(new File("/"));

    @Override
    public void onCreate() {
        super.onCreate();
        fileObserver.startWatching();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create a notification to display the foreground service
        Notification notification = createNotification();

        // Start the service as a foreground service
        startForeground(NOTIFICATION_ID, notification);
        Log.d("BackupTask", "startForeground");

        connect_to_server();

        // Return START_STICKY to ensure that the service restarts if it's terminated by the system
        return START_STICKY;
    }

    void connect_to_server(){
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(() -> {
            try_connect();
            Log.d("BackupTask", "still running");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("BackupTask", "onBind called");
        return null;
    }

    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel("channel_id", "Channel Name", NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("Foreground Service")
                .setContentText("Service is running...")
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground);

        // Build the notification
        return builder.build();
    }

    public static SSLSocketFactory getSocketFactory() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        return sc.getSocketFactory();
    }

    List<FileItem> previousFiles = new ArrayList<>();
    private void try_connect() {
        Log.d("BackupTask", "try_connect");
        try {
            SSLSocketFactory factory = getSocketFactory();

            try (SSLSocket socket = (SSLSocket) factory.createSocket("10.0.2.2", 4443)) {
                Log.d("BackupTask", "connecting");
                socket.startHandshake();

                ArrayList<FileDescription.FileToDownload> listToUpload = new ArrayList<>();
                FileDescription.FileToDownload uploading = null;

                FileDatabase db = AppDatabase.getInstance(getApplicationContext()).fileDao();
                LiveData<List<FileItem>> allFiles = db.getAllFiles();

                new Handler(Looper.getMainLooper()).post(() -> {
                            allFiles.observeForever(files -> {
                                List<FileItem> changed = new ArrayList<>();
                                Map<Long, FileItem> oldMap = previousFiles.stream()
                                        .collect(Collectors.toMap(FileItem::getLocalId, f -> f));

                                for (FileItem f : files) {
                                    FileItem old = oldMap.get(f.getLocalId());
                                    if (!f.equals(old)) {
                                        changed.add(f);
                                    }
                                }

                                try {
                                    sendFileList(socket, changed);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                previousFiles = new ArrayList<>(files);
                            });
                        });
                InputStream inputStream = null;

                while(socket.isConnected()) {
                    if (socket.getInputStream().available() > 0) {
                        FileDescription.ServerMessage serverMsg = FileDescription.ServerMessage.parseDelimitedFrom(socket.getInputStream());
                        switch (serverMsg.getKindCase()) {
                            case FILE_TO_DOWNLOAD -> {
                                FileDescription.FileToDownload fileToDownload = serverMsg.getFileToDownload();
                                listToUpload.add(fileToDownload);
                            }
                            case FILE_OK -> {
                                Log.d("BackupTask", "OK file" + serverMsg.getFileOk().getFileId());
                            }
                            case KIND_NOT_SET -> {
                                Log.d("BackupTask", "Unknown message of kind");
                            }
                        }
                    }

                    while (uploading == null && !listToUpload.isEmpty()) {
                        uploading = listToUpload.remove(0);
                        Pair<FileItem, FileHash> booleanFileHashPair = verify_to_upload(uploading);

                        if (booleanFileHashPair.second == null){
                            FileDescription.FileToDownloadInvalid invalid_unknown = FileDescription.FileToDownloadInvalid.newBuilder()
                                    .setFileId(uploading.getFileId())
                                    .setCurrentMd5("")
                                    .setCurrentSha256("")
                                    .setCurrentSize(0)
                                    .build();
                            FileDescription.ClientMessage clientMessage = FileDescription.ClientMessage.newBuilder().setFileInvalid(invalid_unknown).build();
                            clientMessage.writeDelimitedTo(socket.getOutputStream());
                            uploading = null;
                        }else {
                            if (booleanFileHashPair.second.is_equals(uploading.getFileId().getHash())) {
                                if (inputStream != null){
                                    inputStream.close();
                                }
                                inputStream = getContentResolver().openInputStream(booleanFileHashPair.first.localUri);
                            }else {
                                FileDescription.FileToDownloadInvalid invalid_known = FileDescription.FileToDownloadInvalid.newBuilder()
                                        .setFileId(uploading.getFileId())
                                        .setCurrentMd5(booleanFileHashPair.second.md5)
                                        .setCurrentSha256(booleanFileHashPair.second.sha256)
                                        .setCurrentSize(booleanFileHashPair.second.file_size)
                                        .build();
                                FileDescription.ClientMessage clientMessage = FileDescription.ClientMessage.newBuilder().setFileInvalid(invalid_known).build();
                                clientMessage.writeDelimitedTo(socket.getOutputStream());
                                uploading = null;
                            }
                        }
                    }

                    if (uploading != null) {
                        if (inputStream == null) {
                            Log.d("BackupTask", "Invalid inputStream, closing upload for "+uploading);
                            uploading = null;
                        }else {
                            byte[] buffer = new byte[8192];

                            int bytesRead = inputStream.read(buffer);
                            if (bytesRead != -1) {
                                FileDescription.FileChunk data_chunk = FileDescription.FileChunk.newBuilder()
                                        .setData(ByteString.copyFrom(buffer, 0, bytesRead))
                                        .build();
                                FileDescription.ClientMessage clientMessage = FileDescription.ClientMessage.newBuilder().setFileChunk(data_chunk).build();
                                clientMessage.writeDelimitedTo(socket.getOutputStream());
                            } else {
                                //EOF
                                inputStream.close();
                                inputStream = null;
                            }
                        }
                    }

                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendFileList(SSLSocket socket, List<FileItem> changed) throws IOException {
        Log.d("BackupTask", "sendFileList size  "+changed.size());
        for (FileItem i : changed) {
            FileDescription.NewFileId newFile = FileDescription.NewFileId.newBuilder()
                    .setFileId(i.uniqueId.asUniqueFileId())
                    .build();
            FileDescription.ClientMessage clientMessage = FileDescription.ClientMessage.newBuilder().setFileNew(newFile).build();
            clientMessage.writeDelimitedTo(socket.getOutputStream());
        }
    }

    private void sendFullFileList(SSLSocket socket) throws IOException {
        Log.d("BackupTask", "sendFullFileList");
        FileDatabase db = AppDatabase.getInstance(getApplicationContext()).fileDao();
        LiveData<List<FileItem>> allFiles = db.getAllFiles();
        List<FileItem> value = allFiles.getValue();
        if (value != null){
            for (FileItem i : value) {
                FileDescription.NewFileId newFile = FileDescription.NewFileId.newBuilder()
                        .setFileId(i.uniqueId.asUniqueFileId())
                        .build();
                FileDescription.ClientMessage clientMessage = FileDescription.ClientMessage.newBuilder().setFileNew(newFile).build();
                clientMessage.writeDelimitedTo(socket.getOutputStream());
            }
        }
    }

    private Pair<FileItem, FileHash> verify_to_upload(FileDescription.FileToDownload uploading) throws NoSuchAlgorithmException, IOException {
        Context c = getApplicationContext();
        FileItem fileItem = Actions.getFromDb(c, uploading.getFileId());
        if (fileItem == null) {
            // invalid file, we dont know about it! maybe from some other client? maybe deleted and then DB got erased?
            return new Pair<>(null, null);
        }

        FileHash fileHash = Actions.getFileHash(getContentResolver(), fileItem.localUri, uploading.getCurrentSize());
        return new Pair<>(fileItem, fileHash);
    }
}