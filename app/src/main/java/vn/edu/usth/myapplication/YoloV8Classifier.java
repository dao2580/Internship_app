package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class YoloV8Classifier {

    private static final String TAG = "YoloV8Classifier";
    private static final String LABEL_FILE = "labels_35.txt";

    // Ưu tiên float16 trước để chạy mobile nhẹ hơn
    private static final String[] MODEL_CANDIDATES = {
            "best_35_float16.tflite",
            "best_35_float32.tflite",
            "best_float16.tflite",
            "best_float32.tflite",
            "best.tflite",
            "yolo26n_float16.tflite",
            "yolo26n_float32.tflite",
            "yolo26n.tflite"
    };

    private static final int EXPECTED_LABEL_COUNT = 35;
    private static final float SCORE_THRESHOLD = 0.20f;
    private static final float NMS_IOU_THRESHOLD = 0.45f;

    // Output model hiện tại của bạn đang parse theo xyxy
    private static final BoxFormat END2END_BOX_FORMAT = BoxFormat.XYXY;

    private enum OutputMode {
        END2END_HWC, // [1, N, 6]
        END2END_CHW  // [1, 6, N]
    }

    private enum BoxFormat {
        XYWH,
        XYXY,
        YXYX
    }

    private static YoloV8Classifier instance;

    private final Interpreter interpreter;
    private final String modelFileUsed;
    private final int inputWidth;
    private final int inputHeight;
    private final int outDim1;
    private final int outDim2;
    private final OutputMode outputMode;
    private final List<String> labels = new ArrayList<>();

    // Reuse buffers để giảm lag
    private final ByteBuffer inputBuffer;
    private final int[] pixelBuffer;
    private final float[][][] outputBuffer;

    private static final class Candidate {
        final int classId;
        final float score;
        final float left;
        final float top;
        final float right;
        final float bottom;

        Candidate(int classId, float score, float left, float top, float right, float bottom) {
            this.classId = classId;
            this.score = score;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    private static final class LetterboxInfo {
        final Bitmap bitmap;
        final float scale;
        final float dx;
        final float dy;

        LetterboxInfo(Bitmap bitmap, float scale, float dx, float dy) {
            this.bitmap = bitmap;
            this.scale = scale;
            this.dx = dx;
            this.dy = dy;
        }
    }

    private YoloV8Classifier(Context context) {
        try {
            loadLabels(context, LABEL_FILE);

            if (labels.size() != EXPECTED_LABEL_COUNT) {
                throw new IllegalStateException(
                        "labels_35.txt phai co " + EXPECTED_LABEL_COUNT + " dong, hien tai = " + labels.size()
                );
            }

            modelFileUsed = chooseModelFile(context);

            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

            interpreter = new Interpreter(loadModelFile(context, modelFileUsed), options);
            interpreter.allocateTensors();

            int[] inputShape = interpreter.getInputTensor(0).shape();
            DataType inputType = interpreter.getInputTensor(0).dataType();

            if (inputShape.length != 4) {
                throw new IllegalStateException("Unexpected input tensor rank: " + Arrays.toString(inputShape));
            }

            inputHeight = inputShape[1];
            inputWidth = inputShape[2];

            if (inputType != DataType.FLOAT32) {
                throw new IllegalStateException("Model input is not FLOAT32: " + inputType);
            }

            if (interpreter.getOutputTensorCount() < 1) {
                throw new IllegalStateException("Model has no output tensor");
            }

            int[] outputShape = interpreter.getOutputTensor(0).shape();
            if (outputShape.length != 3) {
                throw new IllegalStateException("Unexpected output tensor rank: " + Arrays.toString(outputShape));
            }

            outDim1 = outputShape[1];
            outDim2 = outputShape[2];
            outputMode = resolveOutputMode(outDim1, outDim2);

            inputBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
                    .order(ByteOrder.nativeOrder());
            pixelBuffer = new int[inputWidth * inputHeight];
            outputBuffer = new float[1][outDim1][outDim2];

            Log.d(TAG, "Loaded model: " + modelFileUsed);
            Log.d(TAG, "labels size = " + labels.size());
            Log.d(TAG, "input shape = " + Arrays.toString(inputShape));
            Log.d(TAG, "output[0] shape = " + Arrays.toString(outputShape));
            Log.d(TAG, "output mode = " + outputMode);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load YOLO model", e);
        }
    }

    public static synchronized YoloV8Classifier getInstance(Context context) {
        if (instance == null) {
            instance = new YoloV8Classifier(context.getApplicationContext());
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = null;
    }

    public List<Result> detect(Bitmap bitmap) {
        return runDetection(bitmap, 10);
    }

    public List<Result> detectTop3(Bitmap bitmap) {
        return runDetection(bitmap, 3);
    }

    private List<Result> runDetection(Bitmap bitmap, int maxResults) {
        if (bitmap == null) return Collections.emptyList();

        LetterboxInfo lb = letterbox(bitmap, inputWidth, inputHeight);
        preprocessBitmap(lb.bitmap);

        interpreter.run(inputBuffer, outputBuffer);

        return postprocess(outputBuffer, bitmap.getWidth(), bitmap.getHeight(), lb.scale, lb.dx, lb.dy, maxResults);
    }

    private OutputMode resolveOutputMode(int d1, int d2) {
        if (d2 == 6) return OutputMode.END2END_HWC;
        if (d1 == 6) return OutputMode.END2END_CHW;

        throw new IllegalStateException(
                "Unsupported output shape: [1, " + d1 + ", " + d2 + "]. Expected [1, N, 6] or [1, 6, N]."
        );
    }

    private LetterboxInfo letterbox(Bitmap src, int targetW, int targetH) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();

        float scale = Math.min((float) targetW / srcW, (float) targetH / srcH);
        int newW = Math.round(srcW * scale);
        int newH = Math.round(srcH * scale);

        float dx = (targetW - newW) / 2f;
        float dy = (targetH - newH) / 2f;

        Bitmap out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.rgb(114, 114, 114));

        Bitmap resized = Bitmap.createScaledBitmap(src, newW, newH, true);
        canvas.drawBitmap(resized, dx, dy, null);

        if (resized != src && !resized.isRecycled()) {
            resized.recycle();
        }

        return new LetterboxInfo(out, scale, dx, dy);
    }

    private void preprocessBitmap(Bitmap bitmap) {
        inputBuffer.rewind();
        bitmap.getPixels(pixelBuffer, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        for (int val : pixelBuffer) {
            inputBuffer.putFloat(((val >> 16) & 0xFF) / 255f); // R
            inputBuffer.putFloat(((val >> 8) & 0xFF) / 255f);  // G
            inputBuffer.putFloat((val & 0xFF) / 255f);         // B
        }

        inputBuffer.rewind();
    }

    private List<Result> postprocess(
            float[][][] output,
            int origW,
            int origH,
            float scale,
            float dx,
            float dy,
            int maxResults
    ) {
        List<Candidate> decoded = new ArrayList<>();

        if (outputMode == OutputMode.END2END_HWC) {
            int numBoxes = outDim1;
            for (int i = 0; i < numBoxes; i++) {
                float[] row = output[0][i];
                addCandidate(decoded, row[0], row[1], row[2], row[3], row[4], row[5], origW, origH, scale, dx, dy);
            }
        } else {
            int numBoxes = outDim2;
            for (int i = 0; i < numBoxes; i++) {
                addCandidate(
                        decoded,
                        output[0][0][i],
                        output[0][1][i],
                        output[0][2][i],
                        output[0][3][i],
                        output[0][4][i],
                        output[0][5][i],
                        origW,
                        origH,
                        scale,
                        dx,
                        dy
                );
            }
        }

        // Giữ top ứng viên trước NMS để giảm chi phí
        Collections.sort(decoded, (a, b) -> Float.compare(b.score, a.score));
        if (decoded.size() > 60) {
            decoded = new ArrayList<>(decoded.subList(0, 60));
        }

        List<Candidate> nms = applyClassWiseNms(decoded, NMS_IOU_THRESHOLD);
        Collections.sort(nms, (a, b) -> Float.compare(b.score, a.score));

        if (nms.size() > maxResults) {
            nms = new ArrayList<>(nms.subList(0, maxResults));
        }

        List<Result> results = new ArrayList<>();
        for (Candidate c : nms) {
            results.add(new Result(labels.get(c.classId), c.score, c.left, c.top, c.right, c.bottom));
        }
        return results;
    }

    private void addCandidate(
            List<Candidate> out,
            float v0,
            float v1,
            float v2,
            float v3,
            float score,
            float classValue,
            int origW,
            int origH,
            float scale,
            float dx,
            float dy
    ) {
        if (score < SCORE_THRESHOLD) return;

        int classId = Math.round(classValue);
        if (classId < 0 || classId >= labels.size()) return;

        float left;
        float top;
        float right;
        float bottom;

        switch (END2END_BOX_FORMAT) {
            case XYWH: {
                float cx = v0;
                float cy = v1;
                float w = v2;
                float h = v3;
                left = cx - (w / 2f);
                top = cy - (h / 2f);
                right = cx + (w / 2f);
                bottom = cy + (h / 2f);
                break;
            }
            case YXYX: {
                top = v0;
                left = v1;
                bottom = v2;
                right = v3;
                break;
            }
            case XYXY:
            default: {
                left = v0;
                top = v1;
                right = v2;
                bottom = v3;
                break;
            }
        }

        // Nếu output đang normalize 0..1 thì đổi về pixel trên ảnh input
        if (left <= 1.5f && top <= 1.5f && right <= 1.5f && bottom <= 1.5f) {
            left *= inputWidth;
            top *= inputHeight;
            right *= inputWidth;
            bottom *= inputHeight;
        }

        // Bỏ padding, map ngược về ảnh gốc
        left = (left - dx) / scale;
        right = (right - dx) / scale;
        top = (top - dy) / scale;
        bottom = (bottom - dy) / scale;

        left = clamp(left, 0f, origW);
        top = clamp(top, 0f, origH);
        right = clamp(right, 0f, origW);
        bottom = clamp(bottom, 0f, origH);

        if (right <= left || bottom <= top) return;

        out.add(new Candidate(classId, score, left, top, right, bottom));
    }

    private List<Candidate> applyClassWiseNms(List<Candidate> input, float iouThreshold) {
        List<Candidate> sorted = new ArrayList<>(input);
        Collections.sort(sorted, (a, b) -> Float.compare(b.score, a.score));

        List<Candidate> kept = new ArrayList<>();
        boolean[] removed = new boolean[sorted.size()];

        for (int i = 0; i < sorted.size(); i++) {
            if (removed[i]) continue;

            Candidate a = sorted.get(i);
            kept.add(a);

            for (int j = i + 1; j < sorted.size(); j++) {
                if (removed[j]) continue;

                Candidate b = sorted.get(j);
                if (a.classId != b.classId) continue;

                float iou = computeIou(a, b);
                if (iou >= iouThreshold) {
                    removed[j] = true;
                }
            }
        }

        return kept;
    }

    private float computeIou(Candidate a, Candidate b) {
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

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private String chooseModelFile(Context context) throws IOException {
        for (String candidate : MODEL_CANDIDATES) {
            if (assetExists(context, candidate)) {
                return candidate;
            }
        }
        throw new IOException("No YOLO model found in assets. Expected one of: " + Arrays.toString(MODEL_CANDIDATES));
    }

    private boolean assetExists(Context context, String assetName) {
        AssetFileDescriptor fd = null;
        try {
            fd = context.getAssets().openFd(assetName);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd(modelName);
        try (FileInputStream is = new FileInputStream(fd.getFileDescriptor())) {
            return is.getChannel().map(
                    FileChannel.MapMode.READ_ONLY,
                    fd.getStartOffset(),
                    fd.getDeclaredLength()
            );
        }
    }

    private void loadLabels(Context context, String fileName) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    labels.add(line);
                }
            }
        }
    }

    public static class Result {
        public final String label;
        public final float conf;
        public final float left;
        public final float top;
        public final float right;
        public final float bottom;

        public Result(String label, float conf, float left, float top, float right, float bottom) {
            this.label = label;
            this.conf = conf;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}