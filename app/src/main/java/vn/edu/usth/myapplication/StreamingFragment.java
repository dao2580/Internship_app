package vn.edu.usth.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.usth.myapplication.data.AppRepository;

public class StreamingFragment extends Fragment {

    private static final String TAG = "StreamingFragment";
    private static final int REQ_CAMERA = 100;

    private static final long FRAME_INTERVAL_MS = 200;
    private static final long DEBOUNCE_SAVE_MS = 3000;
    private static final long BOX_HOLD_MS = 3000;
    private static final float TRACK_MATCH_IOU_THRESHOLD = 0.40f;

    private static final int MAX_NEAR_RESULTS = 3;

    // Box quá nhỏ thì xem là vật ở xa
    private static final float MIN_NEAR_BOX_AREA_RATIO = 0.05f;

    // Chỉ là "ưu tiên" camera trước, không bắt buộc
    // Nếu emulator của bạn hay lỗi camera trước thì có thể đổi tạm thành false
    private static final boolean USE_FRONT_CAMERA = true;

    private static final String[][] LANGS = {
            {"Vietnamese", "vi"},
            {"English", "en"},
            {"Chinese", "zh"},
            {"Japanese", "ja"},
            {"Korean", "ko"},
            {"French", "fr"},
            {"German", "de"},
            {"Spanish", "es"},
            {"Thai", "th"},
            {"Russian", "ru"}
    };

    private PreviewView previewView;
    private BoundingBoxOverlayView overlayView;
    private MaterialButton btnBack, btnSaveAll;
    private TextView txtStreamingHint;
    private AutoCompleteTextView spinnerTargetLanguage;

    private ExecutorService cameraExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private long lastFrameTime = 0L;
    private int lastImageW = 1;
    private int lastImageH = 1;

    private final Map<String, Long> lastSavedTime = new HashMap<>();
    private final List<BoundingBoxOverlayView.DetectionItem> currentDetections = new ArrayList<>();

    private final Map<String, String> languageMap = new LinkedHashMap<>();
    private final List<String> languageNames = new ArrayList<>();

    private final Map<String, TrackedDetection> trackedDetections = new LinkedHashMap<>();
    private final Map<String, String> translationCache = new HashMap<>();
    private final Set<String> pendingTranslationKeys = new HashSet<>();

    private AppRepository repository;
    private AzureTranslatorService translatorService;
    private OfflineTranslatorService offlineTranslatorService;

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean isOfflineModelReady = false;
    private boolean isUsingFrontCamera = false;
    private String currentTargetCode = "vi";

    private static class TrackedDetection {
        String id;
        YoloV8Classifier.Result result;
        String labelVi;
        String translatedText;
        long lastSeenAt;
    }

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
        spinnerTargetLanguage = v.findViewById(R.id.spinner_streaming_target_language);

        repository = new AppRepository(requireContext());
        translatorService = new AzureTranslatorService();

        setupTargetLanguageDropdown();
        setupTts();

        overlayView.setOnSpeakClickListener(this::speakDetection);

        btnBack.setOnClickListener(x ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp()
        );

        btnSaveAll.setOnClickListener(x -> saveCurrentDetections());

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    REQ_CAMERA
            );
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (spinnerTargetLanguage != null) {
            applyDefaultTargetLanguage(true);
        }
    }

    private void setupTargetLanguageDropdown() {
        languageMap.clear();
        languageNames.clear();

        for (String[] row : LANGS) {
            languageMap.put(row[0], row[1]);
            languageNames.add(row[0]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                languageNames
        );

        spinnerTargetLanguage.setAdapter(adapter);

        applyDefaultTargetLanguage(false);

        spinnerTargetLanguage.setOnItemClickListener((parent, view, position, id) -> {
            String name = languageNames.get(position);
            String code = languageMap.get(name);
            if (code == null || code.equals(currentTargetCode)) return;

            currentTargetCode = code;
            prepareOfflineTranslator(currentTargetCode);
            setTtsLanguage(currentTargetCode);
            refreshVisibleTranslations();
        });
    }

    private void applyDefaultTargetLanguage(boolean refreshTranslations) {
        String defaultCode = SettingsPreferences.getDefaultLanguageCode(requireContext());
        String defaultName = SettingsPreferences.getLanguageNameFromCode(defaultCode);

        boolean changed = !defaultCode.equals(currentTargetCode);
        currentTargetCode = defaultCode;

        spinnerTargetLanguage.setText(defaultName, false);
        prepareOfflineTranslator(currentTargetCode);
        setTtsLanguage(currentTargetCode);

        if (refreshTranslations && changed) {
            refreshVisibleTranslations();
        }
    }

    private void setupTts() {
        tts = new TextToSpeech(requireContext(), status -> {
            ttsReady = (status == TextToSpeech.SUCCESS);
            if (ttsReady) {
                setTtsLanguage(currentTargetCode);
            }
        });
    }

    private void prepareOfflineTranslator(String targetCode) {
        try {
            if (offlineTranslatorService != null) {
                offlineTranslatorService.close();
            }
        } catch (Exception ignored) {
        }

        offlineTranslatorService = null;
        isOfflineModelReady = false;

        if ("vi".equals(targetCode) || "en".equals(targetCode)) {
            isOfflineModelReady = true;
            return;
        }

        offlineTranslatorService = new OfflineTranslatorService("en", targetCode);
        offlineTranslatorService.downloadModel(new OfflineTranslatorService.DownloadCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    isOfflineModelReady = true;
                    refreshVisibleTranslations();
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> isOfflineModelReady = false);
            }
        });
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

                CameraSelector preferredSelector = USE_FRONT_CAMERA
                        ? CameraSelector.DEFAULT_FRONT_CAMERA
                        : CameraSelector.DEFAULT_BACK_CAMERA;

                CameraSelector fallbackSelector = USE_FRONT_CAMERA
                        ? CameraSelector.DEFAULT_BACK_CAMERA
                        : CameraSelector.DEFAULT_FRONT_CAMERA;

                CameraSelector selector;

                if (provider.hasCamera(preferredSelector)) {
                    selector = preferredSelector;
                    isUsingFrontCamera = USE_FRONT_CAMERA;
                } else if (provider.hasCamera(fallbackSelector)) {
                    selector = fallbackSelector;
                    isUsingFrontCamera = !USE_FRONT_CAMERA;
                } else {
                    throw new IllegalStateException("Thiết bị hoặc emulator không có camera phù hợp");
                }

                provider.unbindAll();
                provider.bindToLifecycle(
                        this,
                        selector,
                        preview,
                        analysis
                );

            } catch (Exception e) {
                Log.e(TAG, "startCamera failed", e);
                mainHandler.post(() ->
                        Toast.makeText(
                                requireContext(),
                                "Không thể mở camera: " + buildErrorMessage(e),
                                Toast.LENGTH_LONG
                        ).show()
                );
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
            results = YoloV8Classifier.getInstance(requireContext()).detect(bitmap);

            if (isUsingFrontCamera) {
                results = mirrorResultsHorizontally(results, imgW);
            }

            results = filterNearestTop3(results, imgW, imgH);

        } catch (Exception e) {
            Log.e(TAG, "YOLO detect failed", e);
            final String errorText = buildErrorMessage(e);

            mainHandler.post(() -> {
                overlayView.clear();
                trackedDetections.clear();
                currentDetections.clear();
                txtStreamingHint.setText("Detect lỗi: " + errorText);
                btnSaveAll.setEnabled(false);
            });
            return;
        }

        final List<YoloV8Classifier.Result> finalResults = results;
        final int finalImgW = imgW;
        final int finalImgH = imgH;

        mainHandler.post(() -> updateTrackedDetections(finalResults, finalImgW, finalImgH));
    }

    private List<YoloV8Classifier.Result> mirrorResultsHorizontally(
            List<YoloV8Classifier.Result> input,
            int imageWidth
    ) {
        List<YoloV8Classifier.Result> mirrored = new ArrayList<>();
        for (YoloV8Classifier.Result r : input) {
            float newLeft = imageWidth - r.right;
            float newRight = imageWidth - r.left;

            mirrored.add(new YoloV8Classifier.Result(
                    r.label,
                    r.conf,
                    newLeft,
                    r.top,
                    newRight,
                    r.bottom
            ));
        }
        return mirrored;
    }

    private List<YoloV8Classifier.Result> filterNearestTop3(
            List<YoloV8Classifier.Result> input,
            int imageWidth,
            int imageHeight
    ) {
        if (input == null || input.isEmpty()) return new ArrayList<>();

        float imageArea = imageWidth * imageHeight;
        List<YoloV8Classifier.Result> filtered = new ArrayList<>();

        for (YoloV8Classifier.Result r : input) {
            float w = Math.max(0f, r.right - r.left);
            float h = Math.max(0f, r.bottom - r.top);
            float area = w * h;
            float areaRatio = area / imageArea;

            if (areaRatio >= MIN_NEAR_BOX_AREA_RATIO) {
                filtered.add(r);
            }
        }

        if (filtered.isEmpty()) {
            filtered.addAll(input);
        }

        Collections.sort(filtered, (a, b) -> {
            float areaA = Math.max(0f, a.right - a.left) * Math.max(0f, a.bottom - a.top);
            float areaB = Math.max(0f, b.right - b.left) * Math.max(0f, b.bottom - b.top);

            int cmpArea = Float.compare(areaB, areaA);
            if (cmpArea != 0) return cmpArea;

            return Float.compare(b.conf, a.conf);
        });

        if (filtered.size() > MAX_NEAR_RESULTS) {
            return new ArrayList<>(filtered.subList(0, MAX_NEAR_RESULTS));
        }

        return filtered;
    }

    private void updateTrackedDetections(List<YoloV8Classifier.Result> results, int imgW, int imgH) {
        lastImageW = imgW;
        lastImageH = imgH;

        long now = System.currentTimeMillis();
        Set<String> matchedIds = new HashSet<>();

        for (YoloV8Classifier.Result result : results) {
            String existingId = findBestMatch(result, matchedIds);
            TrackedDetection tracked;

            if (existingId != null) {
                tracked = trackedDetections.get(existingId);
            } else {
                tracked = new TrackedDetection();
                tracked.id = UUID.randomUUID().toString();
            }

            tracked.result = result;
            tracked.labelVi = VocabMap.getVI(result.label);
            tracked.lastSeenAt = now;
            tracked.translatedText = resolveDisplayTranslation(result.label);

            trackedDetections.put(tracked.id, tracked);
            matchedIds.add(tracked.id);

            requestTranslationIfNeeded(result.label);
        }

        List<String> expiredIds = new ArrayList<>();
        for (Map.Entry<String, TrackedDetection> entry : trackedDetections.entrySet()) {
            if (now - entry.getValue().lastSeenAt > BOX_HOLD_MS) {
                expiredIds.add(entry.getKey());
            }
        }
        for (String id : expiredIds) {
            trackedDetections.remove(id);
        }

        publishTrackedDetections();
    }

    @Nullable
    private String findBestMatch(YoloV8Classifier.Result incoming, Set<String> matchedIds) {
        float bestIou = 0f;
        String bestId = null;

        for (Map.Entry<String, TrackedDetection> entry : trackedDetections.entrySet()) {
            if (matchedIds.contains(entry.getKey())) continue;

            TrackedDetection tracked = entry.getValue();
            if (!tracked.result.label.equals(incoming.label)) continue;

            float iou = computeIou(tracked.result, incoming);
            if (iou > bestIou) {
                bestIou = iou;
                bestId = entry.getKey();
            }
        }

        return bestIou >= TRACK_MATCH_IOU_THRESHOLD ? bestId : null;
    }

    private float computeIou(YoloV8Classifier.Result a, YoloV8Classifier.Result b) {
        float left = Math.max(a.left, b.left);
        float top = Math.max(a.top, b.top);
        float right = Math.min(a.right, b.right);
        float bottom = Math.min(a.bottom, b.bottom);

        float interW = Math.max(0f, right - left);
        float interH = Math.max(0f, bottom - top);
        float interArea = interW * interH;

        float areaA = Math.max(0f, a.right - a.left) * Math.max(0f, a.bottom - a.top);
        float areaB = Math.max(0f, b.right - b.left) * Math.max(0f, b.bottom - b.top);
        float union = areaA + areaB - interArea;

        return union <= 0f ? 0f : interArea / union;
    }

    private void publishTrackedDetections() {
        currentDetections.clear();

        for (TrackedDetection tracked : trackedDetections.values()) {
            currentDetections.add(new BoundingBoxOverlayView.DetectionItem(
                    tracked.id,
                    tracked.result,
                    tracked.labelVi,
                    tracked.translatedText,
                    currentTargetCode
            ));
        }

        overlayView.setDetections(currentDetections, lastImageW, lastImageH);
        btnSaveAll.setEnabled(!currentDetections.isEmpty());

        if (currentDetections.isEmpty()) {
            txtStreamingHint.setText("Hướng camera vào vật thể...");
            return;
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (BoundingBoxOverlayView.DetectionItem item : currentDetections) {
            if (count == 3) break;
            if (count > 0) sb.append("   •   ");
            sb.append(item.result.label)
                    .append(" → ")
                    .append(item.translatedLabel);
            count++;
        }
        txtStreamingHint.setText(sb.toString());
    }

    private String resolveDisplayTranslation(String labelEn) {
        if ("en".equals(currentTargetCode)) {
            return labelEn;
        }
        if ("vi".equals(currentTargetCode)) {
            return VocabMap.getVI(labelEn);
        }

        String key = buildTranslationKey(labelEn, currentTargetCode);
        String cached = translationCache.get(key);
        if (cached != null && !cached.trim().isEmpty()) {
            return cached;
        }

        return VocabMap.getVI(labelEn);
    }

    private void requestTranslationIfNeeded(String labelEn) {
        if ("vi".equals(currentTargetCode) || "en".equals(currentTargetCode)) {
            return;
        }

        String targetCode = currentTargetCode;
        String cacheKey = buildTranslationKey(labelEn, targetCode);

        if (translationCache.containsKey(cacheKey) || pendingTranslationKeys.contains(cacheKey)) {
            return;
        }

        pendingTranslationKeys.add(cacheKey);

        if (NetworkUtils.isInternetAvailable(requireContext())) {
            translatorService.translate(labelEn, targetCode, new AzureTranslatorService.TranslationCallback() {
                @Override
                public void onSuccess(String translatedText) {
                    mainHandler.post(() -> {
                        pendingTranslationKeys.remove(cacheKey);
                        translationCache.put(cacheKey, translatedText);
                        applyTranslatedValue(labelEn, targetCode, translatedText);
                    });
                }

                @Override
                public void onError(String error) {
                    translateOfflineLabel(labelEn, targetCode, cacheKey);
                }
            });
        } else {
            translateOfflineLabel(labelEn, targetCode, cacheKey);
        }
    }

    private void translateOfflineLabel(String labelEn, String targetCode, String cacheKey) {
        if (offlineTranslatorService == null || !isOfflineModelReady) {
            mainHandler.post(() -> pendingTranslationKeys.remove(cacheKey));
            return;
        }

        offlineTranslatorService.translate(labelEn, new OfflineTranslatorService.TranslationCallback() {
            @Override
            public void onSuccess(String translatedText) {
                mainHandler.post(() -> {
                    pendingTranslationKeys.remove(cacheKey);
                    translationCache.put(cacheKey, translatedText);
                    applyTranslatedValue(labelEn, targetCode, translatedText);
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> pendingTranslationKeys.remove(cacheKey));
            }
        });
    }

    private void applyTranslatedValue(String labelEn, String targetCode, String translatedText) {
        if (!targetCode.equals(currentTargetCode)) return;

        for (TrackedDetection tracked : trackedDetections.values()) {
            if (tracked.result.label.equalsIgnoreCase(labelEn)) {
                tracked.translatedText = translatedText;
            }
        }
        publishTrackedDetections();
    }

    private void refreshVisibleTranslations() {
        for (TrackedDetection tracked : trackedDetections.values()) {
            tracked.translatedText = resolveDisplayTranslation(tracked.result.label);
            requestTranslationIfNeeded(tracked.result.label);
        }
        publishTrackedDetections();
    }

    private String buildTranslationKey(String labelEn, String targetCode) {
        return targetCode + "|" + labelEn.trim().toLowerCase(Locale.US);
    }

    private void saveCurrentDetections() {
        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email == null) return;

        if (currentDetections.isEmpty()) {
            toast("Chưa detect được vật thể nào");
            return;
        }

        int savedCount = 0;
        long now = System.currentTimeMillis();

        for (BoundingBoxOverlayView.DetectionItem item : currentDetections) {
            String label = item.result.label;
            Long lastTime = lastSavedTime.get(label);

            if (lastTime != null && now - lastTime < DEBOUNCE_SAVE_MS) {
                continue;
            }

            lastSavedTime.put(label, now);

            repository.saveLearnedWord(
                    email,
                    label,
                    item.labelVi,
                    item.translatedLabel,
                    currentTargetCode,
                    "streaming"
            );
            savedCount++;
        }

        if (savedCount > 0) {
            toast("Đã lưu " + savedCount + " từ vào My Words!");
        } else {
            toast("Các từ này đã được lưu gần đây");
        }
    }

    private void speakDetection(BoundingBoxOverlayView.DetectionItem item) {
        String text = item.translatedLabel;
        if (text == null || text.trim().isEmpty()) {
            text = item.labelVi;
        }
        speak(text, item.targetLangCode);
    }

    private void speak(String text, String langCode) {
        if (!ttsReady || tts == null) {
            toast("Text-to-Speech unavailable");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            toast("Không có nội dung để phát âm");
            return;
        }

        setTtsLanguage(langCode);
        tts.stop();
        tts.setSpeechRate(1.0f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "STREAMING_TTS");
    }

    private int setTtsLanguage(String code) {
        if (!ttsReady || tts == null) return TextToSpeech.ERROR;

        try {
            Locale locale = Locale.forLanguageTag(code);
            int result = tts.setLanguage(locale);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = tts.setLanguage(Locale.US);
            }
            return result;
        } catch (Exception e) {
            return TextToSpeech.ERROR;
        }
    }

    private Bitmap toBitmap(ImageProxy image) {
        try {
            byte[] nv21 = yuv420888ToNv21(image);

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
                    90,
                    out
            );

            byte[] bytes = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap == null) return null;

            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            return rotateBitmap(bitmap, rotationDegrees);

        } catch (Exception e) {
            Log.e(TAG, "toBitmap failed", e);
            return null;
        }
    }

    private byte[] yuv420888ToNv21(ImageProxy image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 2;

        byte[] nv21 = new byte[ySize + uvSize];

        ImageProxy.PlaneProxy[] planes = image.getPlanes();

        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int yPixelStride = planes[0].getPixelStride();

        int uRowStride = planes[1].getRowStride();
        int uPixelStride = planes[1].getPixelStride();

        int vRowStride = planes[2].getRowStride();
        int vPixelStride = planes[2].getPixelStride();

        int offset = 0;

        byte[] yRow = new byte[yRowStride];
        for (int row = 0; row < height; row++) {
            int rowLength = Math.min(yRowStride, yBuffer.remaining());
            yBuffer.get(yRow, 0, rowLength);

            if (yPixelStride == 1) {
                System.arraycopy(yRow, 0, nv21, offset, width);
                offset += width;
            } else {
                for (int col = 0; col < width; col++) {
                    nv21[offset++] = yRow[col * yPixelStride];
                }
            }
        }

        int chromaHeight = height / 2;
        int chromaWidth = width / 2;

        byte[] uRow = new byte[uRowStride];
        byte[] vRow = new byte[vRowStride];

        for (int row = 0; row < chromaHeight; row++) {
            int uLength = Math.min(uRowStride, uBuffer.remaining());
            int vLength = Math.min(vRowStride, vBuffer.remaining());

            uBuffer.get(uRow, 0, uLength);
            vBuffer.get(vRow, 0, vLength);

            for (int col = 0; col < chromaWidth; col++) {
                int uIndex = Math.min(col * uPixelStride, uLength - 1);
                int vIndex = Math.min(col * vPixelStride, vLength - 1);

                nv21[offset++] = vRow[vIndex];
                nv21[offset++] = uRow[uIndex];
            }
        }

        return nv21;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (bitmap == null) return null;
        if (rotationDegrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);

        Bitmap rotated = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
        );

        if (rotated != bitmap) {
            bitmap.recycle();
        }

        return rotated;
    }

    private String buildErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + msg;
    }

    private void toast(String s) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                toast("Bạn cần cấp quyền camera để dùng chế độ streaming");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        if (offlineTranslatorService != null) {
            try {
                offlineTranslatorService.close();
            } catch (Exception ignored) {
            }
        }

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }
        }

        overlayView.clear();
        trackedDetections.clear();
        currentDetections.clear();
    }
}