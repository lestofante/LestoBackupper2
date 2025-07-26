package com.lesto.lestobackupper;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BackupWorker extends Worker {

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // TODO: Your backup logic here

        try {
            // Backup logic
            return Result.success();
        } catch (Exception e) {
            return Result.retry(); // or Result.failure();
        }
    }
}