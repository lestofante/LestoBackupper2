package com.lesto.lestobackupper.data;

import androidx.recyclerview.widget.DiffUtil;

import org.jspecify.annotations.NonNull;

public class FileItemDiffCallback extends DiffUtil.ItemCallback<FileItem> {
    @Override
    public boolean areItemsTheSame(@NonNull FileItem oldItem, @NonNull FileItem newItem) {
        return oldItem.id == newItem.id;
    }

    @Override
    public boolean areContentsTheSame(@NonNull FileItem oldItem, @NonNull FileItem newItem) {
        return oldItem.equals(newItem);
    }
}