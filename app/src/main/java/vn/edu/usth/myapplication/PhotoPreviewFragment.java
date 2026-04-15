package vn.edu.usth.myapplication;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PhotoPreviewFragment extends Fragment {

    private static final String TAG = "PhotoPreview";
    private static final String ARG_PHOTO_URI = "photo_uri";
    private static final String ARG_IS_TEMP = "is_temp";

    private ImageView imgPreview;
    private TextView txtDetectedObjects;
    private FloatingActionButton btnSave;
    private ExtendedFloatingActionButton btnProceedTranslation;

    private String photoUri;
    private boolean isTemp = false;
    private Bitmap currentBitmap;

    private final List<String> detectedObjectsList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            photoUri = getArguments().getString(ARG_PHOTO_URI);
            isTemp = getArguments().getBoolean(ARG_IS_TEMP, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_photo_preview, container, false);

        imgPreview = v.findViewById(R.id.img_preview);
        txtDetectedObjects = v.findViewById(R.id.txt_detected_objects);
        FloatingActionButton btnBack = v.findViewById(R.id.btn_back_to_camera);
        btnSave = v.findViewById(R.id.btn_save_photo);
        btnProceedTranslation = v.findViewById(R.id.btn_proceed_translation);

        btnBack.setOnClickListener(view ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp()
        );

        btnSave.setVisibility(isTemp ? View.VISIBLE : View.GONE);
        btnSave.setOnClickListener(view -> {
            if (currentBitmap != null) {
                savePhotoToGallery(currentBitmap);
            } else {
                Toast.makeText(requireContext(), "No image to save", Toast.LENGTH_SHORT).show();
            }
        });

        btnProceedTranslation.setOnClickListener(view -> proceedToTranslation());

        if (photoUri != null) {
            loadAndDetectObjects(photoUri);
        }

        return v;
    }

    private void loadAndDetectObjects(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            Bitmap bitmap;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(requireContext().getContentResolver(), uri)
                );
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(
                        requireContext().getContentResolver(),
                        uri
                );
            }

            currentBitmap = bitmap;
            imgPreview.setImageBitmap(bitmap);

            detectObjects(bitmap);

        } catch (Exception e) {
            Log.e(TAG, "Failed to load image", e);
            Toast.makeText(
                    requireContext(),
                    "Failed to load image: " + buildErrorMessage(e),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void detectObjects(Bitmap bitmap) {
        txtDetectedObjects.setText("Analyzing...");

        new Thread(() -> {
            try {
                Bitmap processed = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                List<YoloV8Classifier.Result> results =
                        YoloV8Classifier.getInstance(requireContext()).detect(processed);

                Set<String> labels = new LinkedHashSet<>();
                for (YoloV8Classifier.Result r : results) {
                    labels.add(r.label);
                }

                requireActivity().runOnUiThread(() -> {
                    txtDetectedObjects.setText(
                            labels.isEmpty()
                                    ? "No objects detected"
                                    : "Detected: " + String.join(", ", labels)
                    );

                    imgPreview.setImageBitmap(processed);
                    detectedObjectsList.clear();
                    detectedObjectsList.addAll(labels);
                });

            } catch (Exception e) {
                Log.e(TAG, "detectObjects FAILED", e);

                final String errorText = buildErrorMessage(e);

                requireActivity().runOnUiThread(() -> {
                    txtDetectedObjects.setText("Detect failed: " + errorText);
                    Toast.makeText(
                            requireContext(),
                            "Detect failed: " + errorText,
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    private void savePhotoToGallery(Bitmap bitmap) {
        new Thread(() -> {
            String name = "CamStudy_" + System.currentTimeMillis() + ".jpg";
            OutputStream fos = null;

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                    values.put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/CamStudy"
                    );

                    Uri imageUri = requireContext().getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values
                    );

                    if (imageUri != null) {
                        fos = requireContext().getContentResolver().openOutputStream(imageUri);
                    }
                } else {
                    File dir = new File(
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES
                            ),
                            "CamStudy"
                    );

                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    File image = new File(dir, name);
                    fos = new FileOutputStream(image);

                    requireContext().sendBroadcast(
                            new android.content.Intent(
                                    android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.fromFile(image)
                            )
                    );
                }

                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                }

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(
                                requireContext(),
                                "Photo saved to gallery!",
                                Toast.LENGTH_SHORT
                        ).show()
                );

            } catch (Exception e) {
                Log.e(TAG, "Failed to save photo", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(
                                requireContext(),
                                "Failed to save photo: " + buildErrorMessage(e),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }).start();
    }

    private void proceedToTranslation() {
        Bundle b = new Bundle();
        b.putStringArray("detected_objects", detectedObjectsList.toArray(new String[0]));
        b.putString("photo_uri", photoUri);

        NavController navController =
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.action_photoPreviewFragment_to_translationFragment, b);
    }

    private String buildErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + msg;
    }
}