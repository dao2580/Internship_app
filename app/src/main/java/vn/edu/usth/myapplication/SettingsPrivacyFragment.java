package vn.edu.usth.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.switchmaterial.SwitchMaterial;

import vn.edu.usth.myapplication.data.AppRepository;

public class SettingsPrivacyFragment extends Fragment {

    private UserDatabase userDatabase;
    private AppRepository appRepository;

    private LinearLayout layoutCameraPermission;
    private LinearLayout layoutGalleryPermission;
    private LinearLayout layoutClearHistory;

    private SwitchMaterial switchCameraPermission;
    private SwitchMaterial switchGalleryPermission;

    private boolean isUpdatingSwitches = false;

    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    syncPermissionSwitches();

                    Toast.makeText(
                            requireContext(),
                            isGranted
                                    ? R.string.camera_permission_granted
                                    : R.string.camera_permission_denied,
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    syncPermissionSwitches();

                    Toast.makeText(
                            requireContext(),
                            isGranted
                                    ? R.string.gallery_permission_granted
                                    : R.string.gallery_permission_denied,
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_settings_privacy, container, false);

        userDatabase = new UserDatabase(requireContext());
        appRepository = new AppRepository(requireContext());

        TextView btnBack = view.findViewById(R.id.btn_back_privacy);

        layoutCameraPermission = view.findViewById(R.id.layout_camera_permission);
        layoutGalleryPermission = view.findViewById(R.id.layout_gallery_permission);
        layoutClearHistory = view.findViewById(R.id.layout_clear_history);

        switchCameraPermission = view.findViewById(R.id.switch_camera_permission);
        switchGalleryPermission = view.findViewById(R.id.switch_gallery_permission);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        setupListeners();
        syncPermissionSwitches();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        syncPermissionSwitches();
    }

    private void setupListeners() {
        layoutCameraPermission.setOnClickListener(v -> switchCameraPermission.performClick());
        layoutGalleryPermission.setOnClickListener(v -> switchGalleryPermission.performClick());
        layoutClearHistory.setOnClickListener(v -> showClearHistoryDialog());

        switchCameraPermission.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitches) {
                return;
            }

            if (isChecked) {
                requestCameraPermission();
            } else {
                handlePermissionTurnOff(getString(R.string.camera_permission), switchCameraPermission);
            }
        });

        switchGalleryPermission.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitches) {
                return;
            }

            if (isChecked) {
                requestGalleryPermission();
            } else {
                handlePermissionTurnOff(getString(R.string.gallery_permission), switchGalleryPermission);
            }
        });
    }

    private void requestCameraPermission() {
        if (hasPermission(Manifest.permission.CAMERA)) {
            syncPermissionSwitches();
            return;
        }

        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void requestGalleryPermission() {
        String galleryPermission = getGalleryPermission();

        if (hasPermission(galleryPermission)) {
            syncPermissionSwitches();
            return;
        }

        galleryPermissionLauncher.launch(galleryPermission);
    }

    private void handlePermissionTurnOff(String title, SwitchMaterial targetSwitch) {
        setSwitchSilently(targetSwitch, true);

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(R.string.permission_turn_off_message)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> openAppSettings())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showClearHistoryDialog() {
        String email = userDatabase.getLoggedInEmail();

        if (email == null) {
            Toast.makeText(
                    requireContext(),
                    R.string.no_account_logged_in,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.clear, (dialog, which) -> {
                    appRepository.clearHistory(email);

                    Toast.makeText(
                            requireContext(),
                            R.string.history_cleared_success,
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(intent);
    }

    private void syncPermissionSwitches() {
        if (switchCameraPermission == null || switchGalleryPermission == null) {
            return;
        }

        boolean hasCamera = hasPermission(Manifest.permission.CAMERA);
        boolean hasGallery = hasPermission(getGalleryPermission());

        isUpdatingSwitches = true;
        switchCameraPermission.setChecked(hasCamera);
        switchGalleryPermission.setChecked(hasGallery);
        isUpdatingSwitches = false;
    }

    private void setSwitchSilently(SwitchMaterial switchMaterial, boolean checked) {
        isUpdatingSwitches = true;
        switchMaterial.setChecked(checked);
        isUpdatingSwitches = false;
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private String getGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        }

        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }
}