package com.lesto.lestobackupper;

import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.util.concurrent.Executor;

public class MyFileObserver extends FileObserver {

    private File path;

    public MyFileObserver(File path) {
        super(path);
        this.path = path;
    }

    @Override
    public void onEvent(int event, String path) {
        Log.d(Constants.LESTO, "New file event: " + event + " " + this.path + "/" + path);
    }
}