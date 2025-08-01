package com.lesto.lestobackupper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.lesto.lestobackupper.data.Actions;
import com.lesto.lestobackupper.data.db.AppDatabase;
import com.lesto.lestobackupper.databinding.ActivityMainBinding;
import com.lesto.lestobackupper.ui.image.FullscreenImageFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(this);
                    //db.fileDao().clearFolders();
                    db.fileDao().clearFiles();
                });


        Log.d(Constants.LESTO, "CIAO");

        ExecutorService executorService2 = Executors.newSingleThreadExecutor();
        executorService2.execute(() -> {
            while (true) {
                try {
                    Actions.localUpdatedFileList(this);
                    Thread.sleep(1000*300);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_gallery, R.id.nav_cloud, R.id.nav_folder)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        Log.d(Constants.LESTO, "Asking permission");
        myRequestPermissions();

        Intent serviceIntent = new Intent(getBaseContext(), BackupTask.class);
        ContextCompat.startForegroundService(getBaseContext(), serviceIntent);
    }

    private void myRequestPermissions() {
        String[] permissions_list = new String[] {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.INTERNET,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };

        boolean request_permission = false;
        for (String permission : permissions_list) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                request_permission = true;
            }
        }
        if (!request_permission){
            Log.d(Constants.LESTO, "no need to request permission");
            /*
            RecyclerView recyclerView = findViewById(R.id.ListOfFiles);
            recyclerView.setLayoutManager(new LinearLayoutManager(this)); // Or use GridLayoutManager for grid layout

            recyclerView.setAdapter(new FileAdapter(Actions.get_file_list(this.getContentResolver(), Actions.ALL_IMAGES)));
             */
            return;
        }

        ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean allPermissionsGranted = true;
            for (String permission : permissions.keySet()) {
                if (Boolean.FALSE.equals(permissions.get(permission))) {
                    allPermissionsGranted = false;
                    Log.d(Constants.LESTO, "Permission " + permission + " KO");
                    break;
                }
            }
            if (allPermissionsGranted) {
                // Permissions are granted
                Log.d(Constants.LESTO, "ALL permission ok");
            } else {
                // Permissions are denied
                Log.d(Constants.LESTO, "Some permission ko");
            }
        });

        requestPermissionLauncher.launch(permissions_list);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        Fragment current = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
//        if (current instanceof FullscreenImageFragment) {
//            // Skip inflating menu here
//            return false;
//        }
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}