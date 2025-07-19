package com.lesto.lestobackupper.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lesto.lestobackupper.Constants;
import com.lesto.lestobackupper.FileAdapter;
import com.lesto.lestobackupper.R;
import com.lesto.lestobackupper.data.Actions;
import com.lesto.lestobackupper.data.FileItem;
import com.lesto.lestobackupper.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private FileAdapter fileAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fileAdapter = new FileAdapter(getActivity(), new ArrayList<>());

        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = rootView.findViewById(R.id.ListOfFiles);
        recyclerView.setLayoutManager(new LinearLayoutManager(rootView.getContext())); // Or use GridLayoutManager for grid layout
        recyclerView.setAdapter(fileAdapter);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(Constants.LESTO, "reloading RecyclerView for HomeFragment creation");

        Executors.newSingleThreadExecutor().execute(() -> {
            Log.d(Constants.LESTO, "Getting updated file list");
            {
                long time = System.currentTimeMillis();
                List<FileItem> files = Actions.databaseFileList(getContext());
                Log.d(Constants.LESTO, "Getting file list from DB took: " + (System.currentTimeMillis() - time) + " for files " + files.size());
                fileAdapter.setData(files);
            }
            {
                long time = System.currentTimeMillis();
                List<FileItem> files = Actions.localUpdatedFileList(getContext());
                Log.d(Constants.LESTO, "Getting updated file list took: " + (System.currentTimeMillis() - time) + " for files " + files.size());
//                fileAdapter.setData(files);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}