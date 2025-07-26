package com.lesto.lestobackupper.ui.gallery;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.paging.PagingData;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lesto.lestobackupper.R;
import com.lesto.lestobackupper.data.FileItem;
import com.lesto.lestobackupper.data.FileItemDiffCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class GalleryAdapter extends PagingDataAdapter<FileItem, GalleryAdapter.PhotoViewHolder> {

    private final int element_per_row;
    private final NavController navController;

    private List<FileItem> selectedUris = new ArrayList<>();
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
        Uri uri = Uri.parse(photo.mediastoreUri);
//        try {
//            Bitmap thumbnail = context.getContentResolver().loadThumbnail(
//                    uri,
//                    new Size(size, size),
//                    null
//            );
//
//            holder.thumbnail.setImageBitmap(thumbnail);

            Glide.with(holder.thumbnail.getContext())
                    .load(uri)
                    .override(size, size)
                    .centerCrop()
                    .placeholder(R.drawable.ic_missing)
                    .into(holder.thumbnail);
//        } catch (IOException e) {
 //           e.printStackTrace();
  //          holder.thumbnail.setImageResource(R.drawable.ic_missing);
   //     }

        holder.itemView.setSelected(selectedUrisPosition.contains(position));

        holder.itemView.setOnClickListener(v -> {
            if (!selectedUris.isEmpty()) {
                toggleSelection(position, photo);
            }else{
                Bundle bundle = new Bundle();
                bundle.putParcelable("image_uri", uri);
                navController.navigate(R.id.nav_fullscreen, bundle);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (selectedUris.isEmpty()) {
                toggleSelection(position, photo);
                return true;
            }
            return false;
        });

        holder.cloudDone.setVisibility(photo.is_remote ? View.VISIBLE : View.GONE);
        holder.cloudUpload.setVisibility(!photo.is_remote && photo.should_backup ? View.VISIBLE : View.GONE);
        holder.cloudOff.setVisibility(!photo.should_backup ? View.VISIBLE : View.GONE);
        holder.iconLocal.setVisibility(photo.is_local ? View.VISIBLE : View.GONE);
    }

    private void toggleSelection(int position, FileItem fileItem) {

        if (!selectedUrisPosition.add(position)) { //try adding, if return false is already present, so delete instead
            selectedUrisPosition.remove(position);
        }

        if (selectedUris.contains(fileItem)) {
            selectedUris.remove(fileItem);
        } else {
            selectedUris.add(fileItem);
        }
        notifyItemChanged(position);
    }

    public List<FileItem> getItemSelected() {
        return selectedUris;
    }

    public void resetItemSelected() {
        //List<FileItem> changed = selectedUris;
        //selectedUris = new ArrayList<>();
        //submitList(changed);
        //notifyDataSetChanged();
        selectedUris.clear();
        for (int pos : selectedUrisPosition) {
            notifyItemChanged(pos);
        }
        selectedUrisPosition.clear();
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
