package com.lesto.lestobackupper;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;

import android.Manifest;


import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lesto.lestobackupper.data.Actions;
import com.lesto.lestobackupper.data.FileItem;
import com.lesto.lestobackupper.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        Log.d(Constants.LESTO, "Asking permission");
        myRequestPermissions();
    }

    private void myRequestPermissions() {
        String[] permissions_list = new String[] {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        };

        boolean request_permission = false;
        for (String permission : permissions_list) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                request_permission = true;
            }
        }
        if (!request_permission){
            RecyclerView recyclerView = findViewById(R.id.ListOfFiles);
            recyclerView.setLayoutManager(new LinearLayoutManager(this)); // Or use GridLayoutManager for grid layout

            recyclerView.setAdapter(new FileAdapter(Actions.get_file_list(this.getContentResolver(), Actions.ALL_IMAGES)));
            return;
        }

        ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean allPermissionsGranted = true;
            for (String permission : permissions.keySet()) {
                if (!permissions.get(permission)) {
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

            RecyclerView recyclerView = findViewById(R.id.ListOfFiles);
            recyclerView.setLayoutManager(new LinearLayoutManager(this)); // Or use GridLayoutManager for grid layout

            recyclerView.setAdapter(new FileAdapter(Actions.get_file_list(this.getContentResolver(), Actions.ALL_IMAGES)));
        });

        requestPermissionLauncher.launch(permissions_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}