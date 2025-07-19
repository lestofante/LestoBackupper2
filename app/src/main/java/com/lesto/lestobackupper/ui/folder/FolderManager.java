package com.lesto.lestobackupper.ui.folder;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.lesto.lestobackupper.Constants;
import com.lesto.lestobackupper.FileAdapter;
import com.lesto.lestobackupper.R;
import com.lesto.lestobackupper.data.Actions;
import com.lesto.lestobackupper.data.AppDatabase;
import com.lesto.lestobackupper.data.FileDatabase;
import com.lesto.lestobackupper.data.FileItem;
import com.lesto.lestobackupper.data.FolderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FolderManager#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FolderManager extends Fragment {
    private FolderAdapter folderAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        folderAdapter = new FolderAdapter(getActivity(), new ArrayList<>(), getContext());

        View rootView = inflater.inflate(R.layout.fragment_folder_manager, container, false);

        Button add_folder = rootView.findViewById(R.id.btn_add_folder);
        add_folder.setOnClickListener(view -> {
            openDocumentTreeLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
        });

        RecyclerView recyclerView = rootView.findViewById(R.id.list_of_folder);
        recyclerView.setLayoutManager(new LinearLayoutManager(rootView.getContext())); // Or use GridLayoutManager for grid layout

        recyclerView.setAdapter(folderAdapter);

        updateList();

        return rootView;
    }

    private ActivityResultLauncher<Intent> openDocumentTreeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri treeUri = data.getData();
                        addFolder(treeUri);
                    }
                }
            });
    private void addFolder(Uri treeUri) {
        Executors.newSingleThreadExecutor().execute(() -> {
            FileDatabase db = AppDatabase.getInstance(getContext()).fileDao();
            try {
                db.insert(new FolderItem(treeUri.toString()));
                updateList();
            }catch(SQLiteConstraintException e){
                //we do not care, the folder already exist
            }
        });
    }

    private void updateList(){
        Executors.newSingleThreadExecutor().execute(() -> {
            Log.d(Constants.LESTO, "Getting updated folder list");
            {
                long time = System.currentTimeMillis();
                FileDatabase db = AppDatabase.getInstance(getContext()).fileDao();
                Log.d(Constants.LESTO, "Getting folder list from DB took: " + (System.currentTimeMillis() - time));
                folderAdapter.setData(db.getAllFolder());
            }
        });
    }
}