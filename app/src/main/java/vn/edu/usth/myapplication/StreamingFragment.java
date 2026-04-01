package vn.edu.usth.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamingFragment extends Fragment {

    private static final String TAG = "StreamingFragment";
    private PreviewView previewView;
    private BoundingBoxOverlayView overlayView;
    private ExecutorService cameraExecutor;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private static final Map<String, String> LABEL_VI = new HashMap<>();
    static {
        LABEL_VI.put("person","người"); LABEL_VI.put("bicycle","xe đạp");
        LABEL_VI.put("car","ô tô"); LABEL_VI.put("motorcycle","xe máy");
        LABEL_VI.put("airplane","máy bay"); LABEL_VI.put("bus","xe buýt");
        LABEL_VI.put("train","tàu hỏa"); LABEL_VI.put("truck","xe tải");
        LABEL_VI.put("boat","thuyền"); LABEL_VI.put("traffic light","đèn giao thông");
        LABEL_VI.put("fire hydrant","vòi cứu hỏa"); LABEL_VI.put("stop sign","biển dừng");
        LABEL_VI.put("bench","ghế băng"); LABEL_VI.put("bird","con chim");
        LABEL_VI.put("cat","con mèo"); LABEL_VI.put("dog","con chó");
        LABEL_VI.put("horse","con ngựa"); LABEL_VI.put("sheep","con cừu");
        LABEL_VI.put("cow","con bò"); LABEL_VI.put("elephant","con voi");
        LABEL_VI.put("bear","con gấu"); LABEL_VI.put("zebra","ngựa vằn");
        LABEL_VI.put("giraffe","hươu cao cổ"); LABEL_VI.put("backpack","balo");
        LABEL_VI.put("umbrella","ô dù"); LABEL_VI.put("handbag","túi xách");
        LABEL_VI.put("suitcase","vali"); LABEL_VI.put("bottle","chai nước");
        LABEL_VI.put("wine glass","ly rượu"); LABEL_VI.put("cup","cái cốc");
        LABEL_VI.put("fork","cái nĩa"); LABEL_VI.put("knife","con dao");
        LABEL_VI.put("spoon","cái thìa"); LABEL_VI.put("bowl","cái bát");
        LABEL_VI.put("banana","quả chuối"); LABEL_VI.put("apple","quả táo");
        LABEL_VI.put("sandwich","bánh sandwich"); LABEL_VI.put("orange","quả cam");
        LABEL_VI.put("broccoli","bông cải xanh"); LABEL_VI.put("carrot","cà rốt");
        LABEL_VI.put("hot dog","xúc xích"); LABEL_VI.put("pizza","bánh pizza");
        LABEL_VI.put("donut","bánh donut"); LABEL_VI.put("cake","bánh ngọt");
        LABEL_VI.put("chair","cái ghế"); LABEL_VI.put("couch","ghế sofa");
        LABEL_VI.put("potted plant","cây cảnh"); LABEL_VI.put("bed","cái giường");
        LABEL_VI.put("dining table","bàn ăn"); LABEL_VI.put("toilet","nhà vệ sinh");
        LABEL_VI.put("tv","tivi"); LABEL_VI.put("laptop","máy tính xách tay");
        LABEL_VI.put("mouse","chuột máy tính"); LABEL_VI.put("remote","điều khiển từ xa");
        LABEL_VI.put("keyboard","bàn phím"); LABEL_VI.put("cell phone","điện thoại");
        LABEL_VI.put("microwave","lò vi sóng"); LABEL_VI.put("oven","lò nướng");
        LABEL_VI.put("toaster","máy nướng bánh"); LABEL_VI.put("sink","bồn rửa");
        LABEL_VI.put("refrigerator","tủ lạnh"); LABEL_VI.put("book","quyển sách");
        LABEL_VI.put("clock","đồng hồ"); LABEL_VI.put("vase","bình hoa");
        LABEL_VI.put("scissors","cái kéo"); LABEL_VI.put("teddy bear","gấu bông");
        LABEL_VI.put("hair drier","máy sấy tóc"); LABEL_VI.put("toothbrush","bàn chải đánh răng");
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_streaming, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = view.findViewById(R.id.streaming_preview);
        overlayView = view.findViewById(R.id.streaming_overlay);

        view.findViewById(R.id.streaming_btn_back).setOnClickListener(v ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp());

        // Bấm "Học từ này" -> navigate sang TranslationFragment
        overlayView.setOnObjectSelectedListener((labelEn, labelVi) -> {
            Bundle args = new Bundle();
            args.putStringArray("detected_objects", new String[]{labelEn});
            args.putString("user_input_text", labelEn);
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.nav_translation, args);
        });

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 201);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 201 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(requireContext(), "Cần quyền Camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider cp = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new android.util.Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build();
                analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                cp.unbindAll();
                cp.bindToLifecycle(getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } catch (Exception e) {
                Log.e(TAG, "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close(); return;
        }
        try {
            Bitmap bitmap = yuvToBitmap(imageProxy);
            if (bitmap == null) return;

            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            if (rotation != 0) {
                Matrix m = new Matrix(); m.postRotate(rotation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
            }

            // detectTop3 dùng cho streaming
            List<YOLOv5Classifier.Result> results =
                    YOLOv5Classifier.getInstance(requireContext()).detectTop3(bitmap);

            final int bmpW = bitmap.getWidth(), bmpH = bitmap.getHeight();
            requireActivity().runOnUiThread(() -> {
                int vW = overlayView.getWidth(), vH = overlayView.getHeight();
                if (vW == 0 || vH == 0) return;
                float sx = (float) vW / bmpW, sy = (float) vH / bmpH;
                List<BoundingBoxOverlayView.DetectedBox> boxes = new ArrayList<>();
                for (YOLOv5Classifier.Result r : results) {
                    RectF scaled = new RectF(
                            r.left*sx, r.top*sy, r.right*sx, r.bottom*sy);
                    String vi = LABEL_VI.containsKey(r.label) ? LABEL_VI.get(r.label) : r.label;
                    boxes.add(new BoundingBoxOverlayView.DetectedBox(scaled, r.label, vi, r.conf));
                }
                overlayView.setBoxes(boxes);
            });
        } catch (Exception e) {
            Log.e(TAG, "Analysis error", e);
        } finally {
            imageProxy.close(); isProcessing.set(false);
        }
    }

    private Bitmap yuvToBitmap(@NonNull ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuf = planes[0].getBuffer();
        ByteBuffer uBuf = planes[1].getBuffer();
        ByteBuffer vBuf = planes[2].getBuffer();
        int ySize = yBuf.remaining(), uSize = uBuf.remaining(), vSize = vBuf.remaining();
        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuf.get(nv21, 0, ySize);
        vBuf.get(nv21, ySize, vSize);
        uBuf.get(nv21, ySize + vSize, uSize);
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21,
                image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 85, out);
        byte[] b = out.toByteArray();
        return BitmapFactory.decodeByteArray(b, 0, b.length);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (overlayView != null) overlayView.clear();
    }
}