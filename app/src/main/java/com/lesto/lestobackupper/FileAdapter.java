package com.lesto.lestobackupper;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lesto.lestobackupper.data.FileItem;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private List<FileItem> itemList;

    public FileAdapter(List<FileItem> itemList) {
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        CheckBox checkbox1;
        CheckBox checkbox2;

        long id = -1;

        public void recycle(FileItem item) {
            textView.setText(item.name);
            id = item.id;
            // Reset checkbox states to avoid recycling issues
            checkbox1.setChecked(false);
            checkbox2.setChecked(false);
        }

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_view);
            checkbox1 = itemView.findViewById(R.id.checkbox1);
            checkbox2 = itemView.findViewById(R.id.checkbox2);

            checkbox1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        // CheckBox is checked
                        // Perform your desired action here
                        Log.d(Constants.LESTO, id + ": checkbox1 is checked");
                    } else {
                        // CheckBox is unchecked
                        // Perform your desired action here
                        Log.d(Constants.LESTO, id + ": checkbox1 is unchecked");
                    }
                }
            });

            checkbox2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        // CheckBox is checked
                        // Perform your desired action here
                        Log.d(Constants.LESTO, "checkbox2 is checked");
                    } else {
                        // CheckBox is unchecked
                        // Perform your desired action here
                        Log.d(Constants.LESTO, "checkbox2 is unchecked");
                    }
                }
            });
        }

    }
}