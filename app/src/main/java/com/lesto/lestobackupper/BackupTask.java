package com.lesto.lestobackupper;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;

public class BackupTask extends Service {

    MyFileObserver fileObserver = new MyFileObserver(new File("/"));

    private BroadcastReceiver checkBoxReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle CheckBox state changes here
            boolean isChecked = intent.getBooleanExtra("isChecked", false);
            if (isChecked) {
                // CheckBox is checked, perform tasks
                // Example: Start a background task
                startBackgroundTask();
            } else {
                // CheckBox is unchecked
                // Example: Stop background task
                stopBackgroundTask();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // Register BroadcastReceiver for CheckBox changes
        IntentFilter filter = new IntentFilter("com.example.checkbox.CHANGE_STATE");
        registerReceiver(checkBoxReceiver, filter);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Perform tasks when the service is started (e.g., at startup)
        // Example: Start background task
        startBackgroundTask();
        return START_STICKY;
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

    private void startBackgroundTask() {
        // Example: Start a background task
    }

    private void stopBackgroundTask() {
        // Example: Stop the background task
    }
}