package com.lesto.lestobackupper.ui.gallery;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;

import com.lesto.lestobackupper.data.AppDatabase;
import com.lesto.lestobackupper.data.FileItem;

import static androidx.lifecycle.FlowLiveDataConversions.asLiveData;

public class GalleryViewModel extends AndroidViewModel {
    //private final LiveData<List<FileItem>> photoList;
    private final LiveData<PagingData<FileItem>> photoPagingData;

    public GalleryViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        //photoList = db.fileDao().getAllFiles();


        Pager<Integer, FileItem> pager = new Pager<>(
                new PagingConfig(30, 10, false),
                () -> db.fileDao().pagingSource()
        );

        photoPagingData = asLiveData(pager.getFlow());
    }

    //public LiveData<List<FileItem>> getPhotoList() {
    //    return photoList;
    //}

    public LiveData<PagingData<FileItem>> getPhotoPagingData() {
        return photoPagingData;
    }
}