package com.lesto.lestobackupper.data;

import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.MapColumn;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Dao
public interface FileDatabase {

    @Query("SELECT * FROM fileitem ORDER BY last_update DESC")
    PagingSource<Integer, FileItem> pagingSource();

    @Query("SELECT id as IDD, * FROM fileitem")
    Map<@MapColumn(columnName = "IDD")Long, FileItem> getAll();

    @Query("SELECT * FROM fileitem")
    LiveData<List<FileItem>> getAllFiles();

    @Query("SELECT * FROM folderitem")
    List<FolderItem> getAllFolder();

    @Insert
    void insert(FolderItem folder);

    @Insert
    void insertAll(Collection<FileItem> users);

    @Delete
    void delete(FileItem user);

    @Update
    void updateAll(Collection<FileItem> values);

    @Update
    void update(FileItem f);

    @Delete
    void delete(FolderItem localUri);
}

