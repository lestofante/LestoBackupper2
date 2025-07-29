package com.lesto.lestobackupper.ui.cloud;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentResolver.MimeTypeInfo;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.lesto.lestobackupper.Constants;
import com.lesto.lestobackupper.R;
import com.lesto.lestobackupper.databinding.FragmentCloudBinding;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.Executors;

public class CloudFragment extends Fragment {
    private static final String NEW_USER_HANDSHAKE = "NEW_USER_HANDSHAKE";
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
            //processDirectoryUri();
        });

        TextView v = root.findViewById(R.id.cloudSelector);

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
/*
    private ActivityResultLauncher<Intent> openDocumentTreeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri treeUri = data.getData();
                        processDirectoryUri(treeUri);
                    }
                }
            });
*/
    private ActivityResultLauncher<Intent> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri fileUri = data.getData();
                        // Write backup data to the file
                        try (OutputStream outputStream = getContext().getContentResolver().openOutputStream(fileUri)) {
                            outputStream.write("lol".getBytes());
                            Log.d("BackupApp", "Backup saved successfully to: " + fileUri);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });

    // Launch the directory picker using ActivityResultLauncher
    /*
    private void launchDirectoryPickerWithLauncher() {
        openDocumentTreeLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
    }
     */

    // Process the selected directory URI
    private void processDirectoryUri() {
        // Get the desired file name and content (e.g., from user input)
        String fileName = "your_file_name.txt"; // Replace with your desired name

        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension("txt");

        // Create a SAF intent to create the backup file
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType(mime) // Replace with appropriate MIME type
                .putExtra(Intent.EXTRA_TITLE, fileName);
        createDocumentLauncher.launch(intent);
/*
        //getActivity().startActivityForResult(intent, 10);

        try {
            // Grant long-term write access if needed
            //intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Uri fileUri = DocumentsContract.createDocument(getContext().getContentResolver(), treeUri, mime, fileName);


            //Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));

            // Write backup data to the file
            try (OutputStream outputStream = getContext().getContentResolver().openOutputStream(fileUri)) {
                outputStream.write(fileContent.getBytes());
                Log.d("BackupApp", "Backup saved successfully to: " + fileUri);
            }
        } catch (IOException | SecurityException e) {
            // Handle errors gracefully, provide user feedback
            Log.e("BackupApp", "Error creating backup file: ", e);
        }

//        // Create the file using ContentResolver
//        ContentResolver contentResolver = getActivity().getContentResolver();
//        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("txt");
//        try {
//            Uri fileUri = DocumentsContract.createDocument(contentResolver, treeUri, mimeType, fileName);
//            // Write content to the file
//            try (OutputStream outputStream = contentResolver.openOutputStream(fileUri)) {
//                outputStream.write(fileContent.getBytes());
//                Log.d(Constants.LESTO, "File created successfully: " + fileUri);
//            } catch (IOException e) {
//                Log.e(Constants.LESTO, "Error creating file: ", e);
//            }
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }

 */
    }

    private void performHandshake(String certificateString) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
//                // Example self-signed certificate string
//                String certificateString = "-----BEGIN CERTIFICATE-----\n" +
//                        "MIIBcjCCARmgAwIBAgIUfaFxJsLQjKOGjM9coI0ROpGzP7kwCgYIKoZIzj0EAwIw\n" +
//                        "ITEfMB0GA1UEAwwWcmNnZW4gc2VsZiBzaWduZWQgY2VydDAgFw03NTAxMDEwMDAw\n" +
//                        "MDBaGA80MDk2MDEwMTAwMDAwMFowITEfMB0GA1UEAwwWcmNnZW4gc2VsZiBzaWdu\n" +
//                        "ZWQgY2VydDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABFlLqHPRQl2pI70LBkCH\n" +
//                        "X3Y/DF8B4lGZBt1NvJzEN+dWMvzSelGjN5HJnZtjEW5DZiSWB9rmkAK41jBIrvS/\n" +
//                        "Zr+jLTArMCkGA1UdEQQiMCCCE2hlbGxvLndvcmxkLmV4YW1wbGWCCWxvY2FsaG9z\n" +
//                        "dDAKBggqhkjOPQQDAgNHADBEAiAM9P/S0l8XCe1MDUpCmaCy5jFUTbgai9njlB0Z\n" +
//                        "oX8F/gIgWLu7IIJ6dIB5oVOq+KRSHLdTWQJfWdnek287YBPKELQ=\n" +
//                        "-----END CERTIFICATE-----";

                Log.d(Constants.LESTO, "performHandshake");
                // Load the certificate string into an InputStream
                try (InputStream inputStream = new ByteArrayInputStream(certificateString.getBytes())) {

                    // Create a CertificateFactory and parse the certificate
                    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                    Log.d(Constants.LESTO, "CertificateFactory");
                    X509Certificate cert = (X509Certificate) certFactory.generateCertificate(inputStream);
                    Log.d(Constants.LESTO, "X509Certificate");

                    // Create a KeyStore containing the certificate
                    KeyStore keyStore = KeyStore.getInstance(Constants.storeName);
                    keyStore.load(null);
                    keyStore.setCertificateEntry(Constants.certificateAlias, cert);
                    Log.d(Constants.LESTO, "keyStore.setCertificateEntry");

                    Intent broadcastIntent = new Intent(Constants.NEW_USER_HANDSHAKE);
                    broadcastIntent.putExtra("result", "Task completed successfully");
                    getContext().sendBroadcast(broadcastIntent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void requestCode(){
        // Launch QR code scanning app
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        try {
            qrScannerLauncher.launch(intent);
        }catch (android.content.ActivityNotFoundException e){
            Snackbar.make(getView(), "Could not find a QR code reader", Snackbar.LENGTH_SHORT).show();
        }
    }

    public static void printDnsServers(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities != null) {
                    LinkProperties linkProperties = connectivityManager.getLinkProperties(activeNetwork);
                    if (linkProperties != null) {
                        List<InetAddress> dnsServers = linkProperties.getDnsServers();
                        for (InetAddress dnsServer : dnsServers) {
                            Log.d(Constants.LESTO, dnsServer.getHostAddress());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}