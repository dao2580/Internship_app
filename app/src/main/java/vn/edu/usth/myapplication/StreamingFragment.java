package vn.edu.usth.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.usth.myapplication.data.AppRepository;

public class StreamingFragment extends Fragment {

    private static final String TAG = "StreamingFragment";
    private static final long FRAME_INTERVAL_MS = 500;   // phân tích mỗi 500ms
    private static final long DEBOUNCE_SAVE_MS = 3000;   // không lưu cùng 1 từ trong 3 giây

    private PreviewView previewView;
    private BoundingBoxOverlayView overlayView;
    private MaterialButton btnBack, btnSaveAll;
    private TextView txtStreamingHint;

    private ExecutorService cameraExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long lastFrameTime = 0;

    // Debounce: label → timestamp lần lưu gần nhất
    private final Map<String, Long> lastSavedTime = new HashMap<>();

    // Kết quả detect hiện tại để nút Save dùng
    private List<BoundingBoxOverlayView.DetectionItem> currentDetections = new ArrayList<>();

    // Repository
    private AppRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_streaming, container, false);

        previewView = v.findViewById(R.id.streaming_preview);
        overlayView = v.findViewById(R.id.streaming_overlay);
        btnBack = v.findViewById(R.id.btn_streaming_back);
        btnSaveAll = v.findViewById(R.id.btn_save_detected);
        txtStreamingHint = v.findViewById(R.id.txt_streaming_hint);

        repository = new AppRepository(requireContext());

        btnBack.setOnClickListener(x ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp());

        btnSaveAll.setOnClickListener(x -> saveCurrentDetections());

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    100
            );
        }

        return v;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                provider.unbindAll();
                provider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                );
            } catch (Exception e) {
                Log.e(TAG, "startCamera failed", e);
                mainHandler.post(() ->
                        Toast.makeText(requireContext(),
                                "Không thể mở camera: " + buildErrorMessage(e),
                                Toast.LENGTH_LONG).show());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void analyzeFrame(@NonNull ImageProxy image) {
        long now = System.currentTimeMillis();
        if (now - lastFrameTime < FRAME_INTERVAL_MS) {
            image.close();
            return;
        }
        lastFrameTime = now;

        Bitmap bitmap = toBitmap(image);
        image.close();

        if (bitmap == null) return;

        final int imgW = bitmap.getWidth();
        final int imgH = bitmap.getHeight();

        List<YoloV8Classifier.Result> results;
        try {
            results = YoloV8Classifier.getInstance(requireContext()).detectTop3(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "YOLOv8 detect failed", e);

            final String errorText = buildErrorMessage(e);

            mainHandler.post(() -> {
                overlayView.clear();
                currentDetections = new ArrayList<>();
                txtStreamingHint.setText("Detect lỗi: " + errorText);
                btnSaveAll.setEnabled(false);

                Toast.makeText(
                        requireContext(),
                        "YOLOv8 detect failed: " + errorText,
                        Toast.LENGTH_LONG
                ).show();
            });
            return;
        }

        List<BoundingBoxOverlayView.DetectionItem> items = new ArrayList<>();
        for (YoloV8Classifier.Result r : results) {
            String vi = VocabMap.getVI(r.label);
            items.add(new BoundingBoxOverlayView.DetectionItem(r, vi));
        }

        mainHandler.post(() -> {
            overlayView.setDetections(items, imgW, imgH);
            currentDetections = items;

            if (items.isEmpty()) {
                txtStreamingHint.setText("Hướng camera vào vật thể...");
                btnSaveAll.setEnabled(false);
            } else {
                StringBuilder sb = new StringBuilder();
                for (BoundingBoxOverlayView.DetectionItem d : items) {
                    sb.append(d.result.label)
                            .append(" / ")
                            .append(d.labelVi)
                            .append("  ");
                }
                txtStreamingHint.setText(sb.toString().trim());
                btnSaveAll.setEnabled(true);
            }
        });
    }

    /**
     * Lưu tất cả detection hiện tại vào Room (có debounce — không lưu trùng trong 3 giây)
     */
    private void saveCurrentDetections() {
        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email == null) return;

        if (currentDetections.isEmpty()) {
            Toast.makeText(requireContext(), "Chưa detect được vật thể nào", Toast.LENGTH_SHORT).show();
            return;
        }

        int savedCount = 0;
        long now = System.currentTimeMillis();

        for (BoundingBoxOverlayView.DetectionItem item : currentDetections) {
            String label = item.result.label;
            Long lastTime = lastSavedTime.get(label);

            // Debounce: bỏ qua nếu đã lưu từ này trong 3 giây qua
            if (lastTime != null && now - lastTime < DEBOUNCE_SAVE_MS) continue;

            lastSavedTime.put(label, now);
            repository.saveLearnedWord(
                    email,
                    label,
                    item.labelVi,
                    item.labelVi,
                    "vi",
                    "streaming"
            );
            savedCount++;
        }

        if (savedCount > 0) {
            Toast.makeText(requireContext(),
                    "Đã lưu " + savedCount + " từ vào My Words!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(),
                    "Các từ này đã được lưu gần đây", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Convert ImageProxy → Bitmap =====
    private Bitmap toBitmap(ImageProxy image) {
        try {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer yBuf = planes[0].getBuffer();
            ByteBuffer uBuf = planes[1].getBuffer();
            ByteBuffer vBuf = planes[2].getBuffer();

            int ySize = yBuf.remaining();
            int uSize = uBuf.remaining();
            int vSize = vBuf.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuf.get(nv21, 0, ySize);
            vBuf.get(nv21, ySize, vSize);
            uBuf.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(
                    nv21,
                    ImageFormat.NV21,
                    image.getWidth(),
                    image.getHeight(),
                    null
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(
                    new Rect(0, 0, image.getWidth(), image.getHeight()),
                    80,
                    out
            );

            byte[] bytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        } catch (Exception e) {
            Log.e(TAG, "toBitmap failed", e);
            return null;
        }
    }

    private String buildErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + msg;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        overlayView.clear();
    }
}