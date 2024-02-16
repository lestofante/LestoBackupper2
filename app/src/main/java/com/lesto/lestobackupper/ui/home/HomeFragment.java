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

    private FragmentHomeBinding binding;
    private FileAdapter fileAdapter = new FileAdapter(new ArrayList<>());

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
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

        Log.d(Constants.LESTO, "adapter set");

//        FileItem test[] = {
//                new FileItem(0, "0", "0", 0, "0", false, false, false),
//                new FileItem(1, "1", "0", 0, "0", false, false, false),
//                new FileItem(2, "2", "0", 0, "0", false, false, false),
//                new FileItem(3, "3", "0", 0, "0", false, false, false),
//                new FileItem(4, "4", "0", 0, "0", false, false, false),
//                new FileItem(5, "5", "0", 0, "0", false, false, false),
//        };

        Executors.newSingleThreadExecutor().execute(() -> {
            Log.d(Constants.LESTO, "Getting updated file list");
            List<FileItem> files = Actions.localUpdatedFileList(getContext());
            Log.d(Constants.LESTO, "Setting the file list to the view");
            fileAdapter.setData(files);
            Log.d(Constants.LESTO, "reloading RecyclerView END");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}