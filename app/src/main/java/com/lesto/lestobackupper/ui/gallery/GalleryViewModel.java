package com.lesto.lestobackupper.ui.gallery;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.lesto.lestobackupper.data.db.AppDatabase;
import com.lesto.lestobackupper.data.db.FileItem;

import java.util.List;

public class GalleryViewModel extends AndroidViewModel {
    private final LiveData<List<FileItem>> photoList;

    public GalleryViewModel(@NonNull Application application) {
        super(application);

        AppDatabase db = AppDatabase.getInstance(application);
        photoList = db.fileDao().getAllFiles();
        assert photoList != null;
        if (photoList.getValue() == null){
            Log.d(getClass().getName(), "photoList is null, reloading database");
        }else{
            Log.d(getClass().getName(), "photoList size: " + photoList.getValue().size());
        }
    }
    public LiveData<List<FileItem>> getPhotoList() {
        return photoList;
    }
}