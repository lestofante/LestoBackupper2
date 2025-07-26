package com.lesto.lestobackupper.ui.image;

import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.material.navigation.NavigationView;
import com.lesto.lestobackupper.R;

public class FullscreenImageFragment extends Fragment {

    private static final String ARG_IMAGE_URI = "image_uri";
    private Uri imageUri;

    public FullscreenImageFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        NavigationView navView = requireActivity().findViewById(R.id.nav_view);

        if (toolbar != null) toolbar.setVisibility(View.GONE);
        if (navView != null) navView.setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        super.onStop();
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        NavigationView navView = requireActivity().findViewById(R.id.nav_view);

        if (toolbar != null) toolbar.setVisibility(View.VISIBLE);
        if (navView != null) navView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUri = getArguments().getParcelable(ARG_IMAGE_URI);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_full_screen_image, container, false);

        ImageView imageView = view.findViewById(R.id.fullscreen_image);
        imageView.setImageURI(imageUri); // or use Glide/ImageDecoder for SAF support

        return view;
    }
}