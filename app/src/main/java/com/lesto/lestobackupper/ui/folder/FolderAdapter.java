package com.lesto.lestobackupper.ui.folder;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.lesto.lestobackupper.Constants;
import com.lesto.lestobackupper.R;
import com.lesto.lestobackupper.data.db.AppDatabase;
import com.lesto.lestobackupper.data.db.FileDatabase;
import com.lesto.lestobackupper.data.db.FolderItem;

import java.util.List;
import java.util.concurrent.Executors;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    private List<FolderItem> itemList;
    Activity activity;
    Context context;

    public FolderAdapter(Activity activity, List<FolderItem> itemList, Context context) {
        Log.d(Constants.LESTO, "creating new FolderAdapter");
        this.activity = activity;
        this.itemList = itemList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FolderItem item = itemList.get(position);
        holder.recycle(item);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void setData(List<FolderItem> files) {
        activity.runOnUiThread(() -> {
            itemList = files;
            notifyDataSetChanged();
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        Button btn;

        long id = -1;

        public void recycle(FolderItem item) {
            textView.setText(item.localUri);
            btn.setOnClickListener(view -> {
                showConfirmationDialog(item);
            });
        }

        private void showConfirmationDialog(FolderItem item) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Confirmation");
            builder.setMessage("Are you sure?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        {
                            FileDatabase db = AppDatabase.getInstance(context).fileDao();
                            db.delete(item);
                            setData(db.getAllFolder());
                        }
                    });

                    dialog.dismiss(); // Dismiss the dialog
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // User clicked No button
                    // Dismiss the dialog
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.folder_name);
            btn = itemView.findViewById(R.id.remove_folder);
        }

    }
}