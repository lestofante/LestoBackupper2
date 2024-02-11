package com.lesto.lestobackupper.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.MapColumn;
import androidx.room.Query;
import androidx.room.Update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Dao
public interface FileDatabase {

    @Query("SELECT id as IDD, * FROM fileitem")
    Map<@MapColumn(columnName = "IDD")Long, FileItem> getAll();

    @Insert
    void insertAll(Collection<FileItem> users);

    @Delete
    void delete(FileItem user);

    @Update
    void updateAll(Collection<FileItem> values);
}

