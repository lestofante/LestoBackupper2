package com.lesto.lestobackupper;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lesto.lestobackupper.data.FileItem;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private List<FileItem> itemList;

    public FileAdapter(List<FileItem> itemList) {
        Log.d(Constants.LESTO, "creating new FileAdapter");
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileItem item = itemList.get(position);
        holder.recycle(item);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void setData(List<FileItem> files) {
        itemList = files;
        //notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        CheckBox should_backup;
        CheckBox is_remote;

        CheckBox is_local;

        long id = -1;

        public void recycle(FileItem item) {
            textView.setText(item.name);
            id = item.id;

            // Reset checkbox states to avoid recycling issues
            should_backup.setChecked(item.should_backup);

            is_remote.setChecked(item.is_remote);
            is_remote.setEnabled(false);

            is_local.setChecked(item.is_local);
            is_local.setEnabled(false);
        }

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_view);
            should_backup = itemView.findViewById(R.id.checkbox1);
            is_local = itemView.findViewById(R.id.checkbox2);
            is_remote = itemView.findViewById(R.id.checkbox3);

            should_backup.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    itemList.get(position).should_backup = isChecked;
                    //notifyItemChanged(position);
                }
            });
        }

    }
}