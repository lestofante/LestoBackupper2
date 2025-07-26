package com.lesto.lestobackupper.ui.gallery;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.lesto.lestobackupper.R;
import com.lesto.lestobackupper.data.AppDatabase;
import com.lesto.lestobackupper.data.FileItem;
import com.lesto.lestobackupper.databinding.FragmentGalleryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private GalleryAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        binding.galleryRecyclerView.setHasFixedSize(true);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.gallery_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.add_to_cloud) {
                    List<FileItem> selected = adapter.getItemSelected();
                    for (FileItem item : selected) {
                        item.should_backup = true;
                    }
                    adapter.resetItemSelected();
                    return true;
                }
                if (menuItem.getItemId() == R.id.add_to_local) {
                    List<FileItem> selected = adapter.getItemSelected();
                    for (FileItem item : selected) {
                        item.should_download = true;
                    }
                    adapter.resetItemSelected();
                    return true;
                }
                if (menuItem.getItemId() == R.id.remove_from_cloud) {
                    List<FileItem> selected = adapter.getItemSelected();
                    for (FileItem item : selected) {
                        item.should_backup = false;
                    }
                    adapter.resetItemSelected();
                    return true;
                }
                if (menuItem.getItemId() == R.id.remove_from_local) {
                    List<FileItem> selected = adapter.getItemSelected();
                    for (FileItem item : selected) {
                        item.should_download = false;
                    }
                    adapter.resetItemSelected();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        // Setup RecyclerView
        int element_per_row = 3;
        binding.galleryRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), element_per_row));

        NavController navController = NavHostFragment.findNavController(this);
        adapter = new GalleryAdapter(element_per_row, navController);
        binding.galleryRecyclerView.setAdapter(adapter);

        // Setup ViewModel
        GalleryViewModel viewModel = new ViewModelProvider(this).get(GalleryViewModel.class);
        viewModel.getPhotoPagingData().observe(getViewLifecycleOwner(), newPhotos -> {
            adapter.submitData(getViewLifecycleOwner().getLifecycle(), newPhotos);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}