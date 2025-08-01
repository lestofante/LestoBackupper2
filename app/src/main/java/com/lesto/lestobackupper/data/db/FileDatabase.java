package com.lesto.lestobackupper.data.db;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.MapColumn;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Update;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Dao
public interface FileDatabase {

//    @Query("SELECT localId as IDD, * FROM fileitem")
//    Map<@MapColumn(columnName = "IDD")Long, FileItem> getAll();

//    @Query("SELECT md5 || sha256 || collisionId as ID1, * FROM fileitem")
//    Map<@MapColumn(columnName = "ID1")String, FileItem> getAllByRemoteId();

    @Query("SELECT * FROM fileitem")
    List<FileItem> getAllByRemoteId();

    @Query("SELECT * FROM fileitem")
    LiveData<List<FileItem>> getAllFiles();

    @Query("SELECT * FROM fileitem WHERE localId = :id")
    FileItem getFile(long id);

    @Query("SELECT * FROM fileitem WHERE md5 = :md5 and sha256 = :sha256")
    List<FileItem> getFileByHash(@NonNull String md5, @NonNull String sha256);

    @Query("SELECT * FROM fileitem WHERE md5 = :md5 and sha256 = :sha256 and collisionId = :collisionId")
    List<FileItem> getFileByUniqueId(@NonNull String md5, @NonNull String sha256, long collisionId);

    @Query("SELECT * FROM folderitem")
    List<FolderItem> getAllFolder();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(FolderItem folder);

    @Insert
    void insert(FileItem file);

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

    @Query("DELETE FROM folderitem")
    void clearFolders();

    @Query("DELETE FROM fileitem")
    void clearFiles();
}

