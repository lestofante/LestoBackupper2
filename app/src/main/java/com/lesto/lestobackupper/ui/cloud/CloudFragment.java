package com.lesto.lestobackupper.ui.cloud;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.lesto.lestobackupper.Constants;
import com.lesto.lestobackupper.MainActivity;
import com.lesto.lestobackupper.R;
import com.lesto.lestobackupper.databinding.FragmentCloudBinding;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class CloudFragment extends Fragment {
    private FragmentCloudBinding binding;
    ActivityResultLauncher<Intent> qrScannerLauncher;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        CloudViewModel slideshowViewModel = new ViewModelProvider(this).get(CloudViewModel.class);

        binding = FragmentCloudBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textSlideshow;
        slideshowViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        Button btn = root.findViewById(R.id.button);
        btn.setOnClickListener(view -> {
            Log.d(Constants.LESTO, "string oauth2");
            // Launch QR code scanning app
            requestCode();
        });

        TextView v = root.findViewById(R.id.CloudProvider);

        qrScannerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String contents = data.getStringExtra("SCAN_RESULT");
                            Log.d(Constants.LESTO, "Scanned: " + contents);
                            // Handle the scanned content here
                            performHandshake(contents);
                        }
                    } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        Log.d(Constants.LESTO, "Scan cancelled");
                        // Handle cancellation
                        performHandshake("test");
                    }
                });

        return root;
    }

    private void performHandshake(String contents) {
        try {
            // Example self-signed certificate string
            String certificateString = "-----BEGIN CERTIFICATE-----\n" +
                    "MIIBcjCCARmgAwIBAgIUfaFxJsLQjKOGjM9coI0ROpGzP7kwCgYIKoZIzj0EAwIw\n" +
                    "ITEfMB0GA1UEAwwWcmNnZW4gc2VsZiBzaWduZWQgY2VydDAgFw03NTAxMDEwMDAw\n" +
                    "MDBaGA80MDk2MDEwMTAwMDAwMFowITEfMB0GA1UEAwwWcmNnZW4gc2VsZiBzaWdu\n" +
                    "ZWQgY2VydDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABFlLqHPRQl2pI70LBkCH\n" +
                    "X3Y/DF8B4lGZBt1NvJzEN+dWMvzSelGjN5HJnZtjEW5DZiSWB9rmkAK41jBIrvS/\n" +
                    "Zr+jLTArMCkGA1UdEQQiMCCCE2hlbGxvLndvcmxkLmV4YW1wbGWCCWxvY2FsaG9z\n" +
                    "dDAKBggqhkjOPQQDAgNHADBEAiAM9P/S0l8XCe1MDUpCmaCy5jFUTbgai9njlB0Z\n" +
                    "oX8F/gIgWLu7IIJ6dIB5oVOq+KRSHLdTWQJfWdnek287YBPKELQ=\n" +
                    "-----END CERTIFICATE-----";

            Log.d(Constants.LESTO, "performHandshake");
            // Load the certificate string into an InputStream
            try (InputStream inputStream = new ByteArrayInputStream(certificateString.getBytes())) {

                // Create a CertificateFactory and parse the certificate
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Log.d(Constants.LESTO, "CertificateFactory");
                X509Certificate cert = (X509Certificate) certFactory.generateCertificate(inputStream);
                Log.d(Constants.LESTO, "X509Certificate");

                // Create a KeyStore containing the certificate
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                keyStore.setCertificateEntry("self_signed_certificate", cert);
                Log.d(Constants.LESTO, "keyStore.setCertificateEntry");

                performTask(keyStore);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestCode(){
        // Launch QR code scanning app
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        qrScannerLauncher.launch(intent);

    }

    public void performTask(KeyStore k) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<?> future = executor.submit(() -> {
            try_connect(k);
        });
    }

    private void try_connect(KeyStore k){

        Log.d(Constants.LESTO, "keyStry_connect 1");
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        Log.d(Constants.LESTO, "keyStry_connect 1.1 " + wifiNetInfo);
        Log.d(Constants.LESTO, "keyStry_connect 2 " +cm);
        Network activeNetwork = cm.getActiveNetwork();
        Log.d(Constants.LESTO, "keyStry_connect 3" + activeNetwork);
        NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(activeNetwork);
        Log.d(Constants.LESTO, "keyStry_connect 4" + networkCapabilities);
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            Log.d(Constants.LESTO, "connected to wifi!");
        }

        Log.d(Constants.LESTO, "keyStry_connect");
        try {
//            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            keyStore.load(null, null);
//            Log.d(Constants.LESTO, "keyStore.load");

            // Create a TrustManager that trusts the self-signed certificate
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(k);

            // Create an SSLContext with the custom TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // Create an SSLSocketFactory with the custom SSLContext
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            Log.d(Constants.LESTO, "connecting");
            try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket("192.168.0.102", 4443)) {
                //socket.setSoTimeout(1); // 1 second timeout
                Log.d(Constants.LESTO, "connected");
                // Start handshake
                socket.startHandshake();
                Log.d(Constants.LESTO, "hnadshaked");

                // Write to the socket
                try (OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream())) {
                    Log.d(Constants.LESTO, "OutputStreamWriter");
                    // Read from the socket
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        Log.d(Constants.LESTO, "BufferedReader");
                        for (int i = 0; i < 10; i++) {
                            out.write("Hello, server!\n");
                            out.flush();
                            Log.d(Constants.LESTO, "out.write");
                            //String response = in.readLine();
                            //Log.d(Constants.LESTO, "Server response: " + response);
                            Thread.sleep(1000);
                        }
                    }
                }
            }
            Log.d(Constants.LESTO, "disconnected ");
        } catch (Exception e) {
            Log.e(Constants.LESTO, e.toString());
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}