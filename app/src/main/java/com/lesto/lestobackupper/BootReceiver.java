package com.lesto.lestobackupper;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

public class BootReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Start the service when the device boots up
            Intent serviceIntent = new Intent(context, BackupTask.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}
