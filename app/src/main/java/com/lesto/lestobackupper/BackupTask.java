package com.lesto.lestobackupper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.lesto.lestobackupper.data.Actions;
import com.lesto.lestobackupper.data.FileItem;
import com.lesto.lestobackupper.proto.FileDescription;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class BackupTask extends Service {

    private static final int NOTIFICATION_ID = 123;
    MyFileObserver fileObserver = new MyFileObserver(new File("/"));

    private final BroadcastReceiver checkBoxReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Constants.NEW_USER_HANDSHAKE)){
            Log.d(Constants.LESTO, "get new credential, reconnecting");
            send_hello();
        }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        fileObserver.startWatching();
        // Register BroadcastReceiver for CheckBox changes
        IntentFilter filter = new IntentFilter("com.example.checkbox.CHANGE_STATE");
        registerReceiver(checkBoxReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create a notification to display the foreground service
        Notification notification = createNotification();

        // Start the service as a foreground service
        startForeground(NOTIFICATION_ID, notification);
        Log.d(Constants.LESTO, "startForeground");

        send_hello();

        // Return START_STICKY to ensure that the service restarts if it's terminated by the system
        return START_STICKY;
    }

    void syncFileList(OutputStream out){
        List<FileItem> files = Actions.localUpdatedFileList(this);
        int sent = 0;
        Log.d(Constants.LESTO, "sent start");
        for (FileItem f : files){
            if (f.should_backup){
                try {
                    if (f.hash.isEmpty()) {
                        f = new FileItem(f, Actions.getFileHash(getContentResolver(), ContentUris.withAppendedId(Uri.parse(f.mediastoreUri), f.id)));
                        Actions.update(this, f);
                    }
                    long len = Actions.getFileSize(getContentResolver(), ContentUris.withAppendedId(Uri.parse(f.mediastoreUri), f.id));
                    FileDescription.FileInfo info = FileDescription.FileInfo.newBuilder()
                            .setId(f.id)
                            .setHash(f.hash)
                            .setSize(len)
                            .setName(f.name)
                            .build();
                    byte[] type_and_size = new byte[3];
                    type_and_size[0] = 0;
                    byte[] data = info.toByteArray();
                    assert (data.length < 32000);
                    type_and_size[1] = (byte) (data.length >> 8);
                    type_and_size[2] = (byte) (data.length);
                    out.write(type_and_size);
                    out.write(info.toByteArray());
                    sent += 1;
                }catch (SocketException e) {
                    Log.w(Constants.LESTO, "Disconnected");
                    return;
                }catch (IOException | NoSuchAlgorithmException e){
                    e.printStackTrace();
                    Log.w(Constants.LESTO, "failed to hash/size " + f.name + ", ignoring");
                }
            }
        }
        Log.d(Constants.LESTO, "sent " + sent);
    }

    void send_hello(){
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(() -> {
            try {
                try_connect();
                Log.d(Constants.LESTO, "still running");
                Thread.sleep(1000*30);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }finally {
                send_hello();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister BroadcastReceiver
        unregisterReceiver(checkBoxReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        Log.d(Constants.LESTO, "onBind called");
        return null;
    }

    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel("channel_id", "Channel Name", NotificationManager.IMPORTANCE_DEFAULT);
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

    private void try_connect(){

        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = cm.getActiveNetwork();
        NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(activeNetwork);

        assert networkCapabilities != null;
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            Log.d(Constants.LESTO, "connected to wifi!");
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(Constants.storeName);
            keyStore.load(null);
            X509Certificate certificate = (X509Certificate)keyStore.getCertificate(Constants.certificateAlias);
            Log.d(Constants.LESTO, "keyStore.load found: " + certificate);

            if (certificate == null){
                Log.d(Constants.LESTO, "No certificate");
                return;
            }

            // Get the Subject Alternative Name (SAN) extension
            Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();

            // Iterate through the alternative names and find URLs
            List<String> hostnames = new ArrayList<>();
            if (altNames != null) {
                for (List<?> entry : altNames) {
                    Integer type = (Integer) entry.get(0);
                    if (type == 2) { // 6 is the type for hostnames
                        hostnames.add((String) entry.get(1));
                    }else if (type == 7) {
                        hostnames.add((String) entry.get(1));
                    }else{
                        String bho = (String) entry.get(1);
                        //Log.d(Constants.LESTO, "altNames type " + type + " is " + bho);
                    }
                }
            } else {
                Log.d(Constants.LESTO, "No Subject Alternative Name (SAN) extension found");
            }

            if (hostnames.isEmpty()){
                Log.d(Constants.LESTO, "No hostname");
                return;
            }

            // Create a TrustManager that trusts the self-signed certificate
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // Create an SSLContext with the custom TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // Create an SSLSocketFactory with the custom SSLContext
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            Log.d(Constants.LESTO, "connecting");
            for (String hostname : hostnames) {
                Log.d(Constants.LESTO, "connecting to " + hostname);
                try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(hostname, 4443)) {
                    socket.setSoTimeout(1000); // 1000 ms timeout
                    socket.setKeepAlive(true); // keep alive
                    Log.d(Constants.LESTO, "connected");
                    // Start handshake
                    socket.startHandshake();
                    Log.d(Constants.LESTO, "handshake");

                    // Write to the socket
                    try (OutputStream out = socket.getOutputStream()) {
                        Log.d(Constants.LESTO, "OutputStreamWriter");
                        // Read from the socket
                        try (InputStream in = socket.getInputStream()) {
                            Log.d(Constants.LESTO, "BufferedReader");
                            syncFileList(out);
                            out.flush();
                            Thread.sleep(100);
                        }
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                    Log.e(Constants.LESTO, e.toString());
                }
                Log.d(Constants.LESTO, "disconnected from " + hostname);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Constants.LESTO, e.toString());
        }

    }
}