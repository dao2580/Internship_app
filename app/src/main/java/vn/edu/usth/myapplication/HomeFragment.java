/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: HomeFragment.java
 * Last Modified: 17/10/2025 0:56
 */

package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;

public class HomeFragment extends Fragment {

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Bundle args = new Bundle();
                    args.putString("photo_uri", uri.toString());
                    args.putLong("timestamp", System.currentTimeMillis());
                    args.putBoolean("is_temp", false);

                    NavController navController =
                            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

                    navController.navigate(R.id.nav_photo_preview, args);
                } else {
                    Toast.makeText(
                            requireContext(),
                            R.string.no_image_selected,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        MaterialCardView takePhotoCard = view.findViewById(R.id.card_take_photo);
        MaterialCardView quizCard = view.findViewById(R.id.card_quiz);
        MaterialCardView importImageCard = view.findViewById(R.id.card_import_image);
        MaterialCardView streamingCard = view.findViewById(R.id.card_streaming);

        takePhotoCard.setOnClickListener(v -> {
            NavController navController =
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.nav_camera);
        });

        quizCard.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("selected_tab", 2);

            NavController navController =
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.nav_history, bundle);
        });

        importImageCard.setOnClickListener(v ->
                pickImageLauncher.launch("image/*")
        );

        streamingCard.setOnClickListener(v ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.nav_streaming)
        );

        return view;
    }
}