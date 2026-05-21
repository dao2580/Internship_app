/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: EmbeddedCameraFragment.java
 * Last Modified: 17/10/2025
 */

package vn.edu.usth.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmbeddedCameraFragment extends Fragment {

    private static final String TAG = "EmbeddedCameraFragment";

    private PreviewView previewView;
    private View permissionLayout;
    private View zoomControlLayout;

    private ImageCapture imageCapture;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private CameraControl cameraControl;
    private CameraInfo cameraInfo;
    private ExecutorService cameraExecutor;

    private SeekBar zoomSlider;
    private TextView txtZoomLevel;
    private Button btnZoom1x;
    private Button btnZoom2x;

    private ScaleGestureDetector scaleGestureDetector;
    private float currentZoomRatio = 1.0f;

    private SharedPreferences sharedPreferences;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean allGranted = true;

                        for (Boolean granted : result.values()) {
                            if (!granted) {
                                allGranted = false;
                                break;
                            }
                        }

                        if (allGranted) {
                            startCamera();
                        } else {
                            showPermissionLayout();
                        }
                    }
            );

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_camera, container, false);

        previewView = v.findViewById(R.id.preview_view);
        permissionLayout = v.findViewById(R.id.permission_layout);
        zoomControlLayout = v.findViewById(R.id.zoom_control);

        zoomSlider = v.findViewById(R.id.zoom_slider);
        txtZoomLevel = v.findViewById(R.id.txt_zoom_level);
        btnZoom1x = v.findViewById(R.id.btn_zoom_1x);
        btnZoom2x = v.findViewById(R.id.btn_zoom_2x);

        FloatingActionButton btnCapture = v.findViewById(R.id.btn_capture);
        FloatingActionButton btnSwitch = v.findViewById(R.id.btn_switch_camera);
        FloatingActionButton btnFlash = v.findViewById(R.id.btn_flash);
        MaterialButton btnGrant = v.findViewById(R.id.btn_grant_permission);

        sharedPreferences = requireContext().getSharedPreferences(
                "CamPrefs",
                Context.MODE_PRIVATE
        );

        btnCapture.setOnClickListener(x -> takePhoto());
        btnSwitch.setOnClickListener(x -> switchCamera());
        btnGrant.setOnClickListener(x -> requestNeededPermissions());
        btnFlash.setOnClickListener(x -> toggleFlash());

        setupZoomControl();
        setupPinchToZoom();

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            showPermissionLayout();
        }

        return v;
    }

    private boolean allPermissionsGranted() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;

        if (!cameraGranted) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }

        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNeededPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        permissionLauncher.launch(permissions.toArray(new String[0]));
    }

    private void startCamera() {
        permissionLayout.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                bindCameraUseCases(provider);
            } catch (Exception e) {
                Log.e(TAG, "Camera start failed", e);

                Toast.makeText(
                        requireContext(),
                        R.string.camera_start_failed,
                        Toast.LENGTH_SHORT
                ).show();

                showPermissionLayout();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases(ProcessCameraProvider provider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        try {
            provider.unbindAll();

            Camera camera = provider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
            );

            cameraControl = camera.getCameraControl();
            cameraInfo = camera.getCameraInfo();

            boolean torch = sharedPreferences.getBoolean("flash_mode", false);
            cameraControl.enableTorch(torch);

            initializeZoomControl();

        } catch (Exception e) {
            Log.e(TAG, "Bind failed", e);

            Toast.makeText(
                    requireContext(),
                    R.string.camera_bind_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void setupZoomControl() {
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (cameraInfo == null || cameraControl == null) {
                    return;
                }

                ZoomState zoomState = cameraInfo.getZoomState().getValue();

                if (zoomState == null) {
                    return;
                }

                float min = zoomState.getMinZoomRatio();
                float max = zoomState.getMaxZoomRatio();

                currentZoomRatio = min + (progress / 100f) * (max - min);
                cameraControl.setZoomRatio(currentZoomRatio);

                txtZoomLevel.setText(
                        String.format(Locale.US, "%.1fx", currentZoomRatio)
                );
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        btnZoom1x.setOnClickListener(v -> setZoomRatio(1.0f));
        btnZoom2x.setOnClickListener(v -> setZoomRatio(2.0f));
    }

    private void setupPinchToZoom() {
        scaleGestureDetector = new ScaleGestureDetector(
                requireContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(@NonNull ScaleGestureDetector detector) {
                        if (cameraInfo == null || cameraControl == null) {
                            return true;
                        }

                        ZoomState zoomState = cameraInfo.getZoomState().getValue();

                        if (zoomState == null) {
                            return true;
                        }

                        float min = zoomState.getMinZoomRatio();
                        float max = zoomState.getMaxZoomRatio();

                        currentZoomRatio = Math.max(
                                min,
                                Math.min(currentZoomRatio * detector.getScaleFactor(), max)
                        );

                        cameraControl.setZoomRatio(currentZoomRatio);
                        updateZoomUI(currentZoomRatio, min, max);

                        return true;
                    }
                }
        );

        previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void setZoomRatio(float targetZoom) {
        if (cameraInfo == null || cameraControl == null) {
            return;
        }

        ZoomState zoomState = cameraInfo.getZoomState().getValue();

        if (zoomState == null) {
            return;
        }

        float min = zoomState.getMinZoomRatio();
        float max = zoomState.getMaxZoomRatio();

        currentZoomRatio = Math.max(min, Math.min(targetZoom, max));
        cameraControl.setZoomRatio(currentZoomRatio);

        updateZoomUI(currentZoomRatio, min, max);
    }

    private void updateZoomUI(float zoom, float min, float max) {
        txtZoomLevel.setText(String.format(Locale.US, "%.1fx", zoom));

        if (max > min) {
            zoomSlider.setProgress((int) ((zoom - min) / (max - min) * 100));
        } else {
            zoomSlider.setProgress(0);
        }
    }

    private void initializeZoomControl() {
        if (cameraInfo == null) {
            return;
        }

        ZoomState zoomState = cameraInfo.getZoomState().getValue();

        if (zoomState == null) {
            return;
        }

        float min = zoomState.getMinZoomRatio();
        float max = zoomState.getMaxZoomRatio();

        currentZoomRatio = min;

        zoomSlider.setProgress(0);
        txtZoomLevel.setText(String.format(Locale.US, "%.1fx", min));
        zoomControlLayout.setVisibility(max > min ? View.VISIBLE : View.GONE);
        btnZoom2x.setEnabled(max >= 2.0f);
    }

    private void toggleFlash() {
        boolean newState = !sharedPreferences.getBoolean("flash_mode", false);

        sharedPreferences.edit()
                .putBoolean("flash_mode", newState)
                .apply();

        if (cameraControl != null) {
            cameraControl.enableTorch(newState);
        }
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(
                    requireContext(),
                    R.string.camera_not_ready,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String name = new SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
        ).format(System.currentTimeMillis());

        File file = new File(
                requireContext().getCacheDir(),
                "temp_" + name + ".jpg"
        );

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(file).build();

        imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults outputFileResults
                    ) {
                        Bundle args = new Bundle();
                        args.putString("photo_uri", Uri.fromFile(file).toString());
                        args.putBoolean("is_temp", true);

                        Navigation.findNavController(
                                requireActivity(),
                                R.id.nav_host_fragment
                        ).navigate(R.id.nav_photo_preview, args);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Capture failed", exception);

                        Toast.makeText(
                                requireContext(),
                                R.string.photo_capture_error,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void switchCamera() {
        if (cameraControl != null) {
            cameraControl.enableTorch(false);
        }

        sharedPreferences.edit()
                .putBoolean("flash_mode", false)
                .apply();

        cameraSelector = cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;

        startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (cameraControl != null) {
            cameraControl.enableTorch(false);
        }

        sharedPreferences.edit()
                .putBoolean("flash_mode", false)
                .apply();
    }

    private void showPermissionLayout() {
        permissionLayout.setVisibility(View.VISIBLE);
        previewView.setVisibility(View.GONE);
        zoomControlLayout.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}