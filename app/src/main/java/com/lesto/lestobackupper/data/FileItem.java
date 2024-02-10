package com.lesto.lestobackupper.data;

public class FileItem {

    public long id;
    public String name;
    public String path;

    public FileItem(long id, String name, String path){
        this.id = id;
        this.name = name;
        this.path = path;
    }
}