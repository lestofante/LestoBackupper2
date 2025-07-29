package com.lesto.lestobackupper.ui.gallery;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lesto.lestobackupper.R;
import com.lesto.lestobackupper.data.db.FileItem;
import com.lesto.lestobackupper.data.db.FileItemDiffCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class GalleryAdapter extends ListAdapter<FileItem, GalleryAdapter.PhotoViewHolder> {

    private final int element_per_row;
    private final NavController navController;
    private Set<Integer> selectedUrisPosition = new TreeSet<>();

    public GalleryAdapter(int element_per_row, NavController navController) {
        super(new FileItemDiffCallback());
        this.element_per_row = element_per_row;
        this.navController = navController;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.photo_item, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int size = screenWidth / element_per_row;

        holder.itemView.getLayoutParams().width = size;
        holder.itemView.getLayoutParams().height = size;

        Context context = holder.itemView.getContext();
        FileItem photo = getItem(position);

        assert photo != null;
//        Uri uri = Uri.parse(photo.mediastoreUri);
        Glide.with(context)
                .load(photo.localUri)
                .placeholder(R.drawable.ic_missing)
                .override(size, size)
                .centerCrop()
                .into(holder.thumbnail);

        holder.itemView.setSelected(selectedUrisPosition.contains(position));

        holder.itemView.setOnClickListener(v -> {
            if (!selectedUrisPosition.isEmpty()) {
                toggleSelection(position, photo);
            }else{
                Bundle bundle = new Bundle();
                bundle.putParcelable("image_uri", photo.localUri);
                navController.navigate(R.id.nav_fullscreen, bundle);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (selectedUrisPosition.isEmpty()) {
                toggleSelection(position, photo);
                return true;
            }
            return false;
        });

        holder.cloudDone.setVisibility(photo.remoteUri != null ? View.VISIBLE : View.GONE);
        holder.cloudUpload.setVisibility(photo.remoteUri == null && photo.should_backup ? View.VISIBLE : View.GONE);
        holder.cloudOff.setVisibility(!photo.should_backup ? View.VISIBLE : View.GONE);
        holder.iconLocal.setVisibility(photo.localUri != null ? View.VISIBLE : View.GONE);
    }

    private void toggleSelection(int position, FileItem fileItem) {

        if (!selectedUrisPosition.add(position)) { //if add return false item already exist; in this case, is a deselect, so remove it
            selectedUrisPosition.remove(position);
        }

        notifyItemChanged(position);
    }

    public List<FileItem> getItemSelected() {
        List<FileItem> selectedItems = new ArrayList<>(selectedUrisPosition.size());
        for (int pos : selectedUrisPosition) {
            if (pos >= 0 && pos < getItemCount()) {
                FileItem item = getItem(pos);
                if (item != null) {
                    selectedItems.add(item);
                }
            }
        }
        return selectedItems;
    }

    public void resetItemSelected() {
        Set<Integer> old = selectedUrisPosition;
        selectedUrisPosition = new TreeSet<>();
        for (int pos : old){
            notifyItemChanged(pos);
        }
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail, cloudDone, cloudOff, cloudUpload, iconLocal;

        public PhotoViewHolder(View view) {
            super(view);
            thumbnail = view.findViewById(R.id.imageThumbnail);
            cloudDone = view.findViewById(R.id.iconCloudDone);
            cloudOff = view.findViewById(R.id.iconCloudOff);
            cloudUpload = view.findViewById(R.id.iconCloudUpload);
            iconLocal = view.findViewById(R.id.iconLocal);
        }
    }

}
