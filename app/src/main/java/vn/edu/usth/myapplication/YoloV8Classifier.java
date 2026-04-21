package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
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
    private static final String TAG = "Yolo26";
    private static final String LABEL_FILE = "labels.txt";

    // Ưu tiên model mới của bạn trước
    private static final String[] MODEL_CANDIDATES = {
            "best_float32.tflite",
            "best_float16.tflite",
            "best.tflite",
            "yolo26n_float32.tflite",
            "yolo26n_float16.tflite",
            "yolo26n.tflite"
    };

    private static final int EXPECTED_LABEL_COUNT = 45;
    private static final float SCORE_THRESHOLD = 0.25f;

    private enum OutputMode {
        END2END_HWC, // [1, N, 6]
        END2END_CHW  // [1, 6, N]
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

    private YoloV8Classifier(Context context) {
        try {
            loadLabels(context, LABEL_FILE);

            if (labels.size() != EXPECTED_LABEL_COUNT) {
                throw new IllegalStateException(
                        "labels.txt phai co " + EXPECTED_LABEL_COUNT + " dong, hien tai = " + labels.size()
                );
            }

            modelFileUsed = chooseModelFile(context);

            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);

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

            Log.d(TAG, "Loaded model: " + modelFileUsed);
            Log.d(TAG, "labels size = " + labels.size());
            Log.d(TAG, "input shape = " + Arrays.toString(inputShape));
            Log.d(TAG, "input type = " + inputType);
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

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
        ByteBuffer input = preprocessBitmap(resized);

        float[][][] output = new float[1][outDim1][outDim2];

        interpreter.run(input, output);

        return postprocess(output, bitmap.getWidth(), bitmap.getHeight(), maxResults);
    }

    private OutputMode resolveOutputMode(int d1, int d2) {
        if (d2 == 6) return OutputMode.END2END_HWC; // [1, N, 6]
        if (d1 == 6) return OutputMode.END2END_CHW; // [1, 6, N]

        throw new IllegalStateException(
                "Unsupported output shape: [1, " + d1 + ", " + d2 + "]. Expected [1, N, 6] or [1, 6, N]."
        );
    }

    private ByteBuffer preprocessBitmap(Bitmap bitmap) {
        ByteBuffer buf = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
                .order(ByteOrder.nativeOrder());

        int[] pixels = new int[inputWidth * inputHeight];
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        for (int val : pixels) {
            buf.putFloat(((val >> 16) & 0xFF) / 255f); // R
            buf.putFloat(((val >> 8) & 0xFF) / 255f);  // G
            buf.putFloat((val & 0xFF) / 255f);         // B
        }

        buf.rewind();
        return buf;
    }

    private List<Result> postprocess(float[][][] output, int origW, int origH, int maxResults) {
        List<Result> results = new ArrayList<>();

        if (outputMode == OutputMode.END2END_HWC) {
            int numBoxes = outDim1;
            for (int i = 0; i < numBoxes; i++) {
                float[] row = output[0][i];
                addEnd2EndResult(results, row[0], row[1], row[2], row[3], row[4], row[5], origW, origH);
            }
        } else {
            int numBoxes = outDim2;
            for (int i = 0; i < numBoxes; i++) {
                float left = output[0][0][i];
                float top = output[0][1][i];
                float right = output[0][2][i];
                float bottom = output[0][3][i];
                float score = output[0][4][i];
                float cls = output[0][5][i];
                addEnd2EndResult(results, left, top, right, bottom, score, cls, origW, origH);
            }
        }

        Collections.sort(results, (a, b) -> Float.compare(b.conf, a.conf));

        if (results.size() > maxResults) {
            return new ArrayList<>(results.subList(0, maxResults));
        }
        return results;
    }

    private void addEnd2EndResult(
            List<Result> results,
            float left,
            float top,
            float right,
            float bottom,
            float score,
            float classValue,
            int origW,
            int origH
    ) {
        if (score < SCORE_THRESHOLD) return;

        int classId = Math.round(classValue);
        if (classId < 0 || classId >= labels.size()) return;

        float l = clamp(toImageX(left, origW), 0f, origW);
        float t = clamp(toImageY(top, origH), 0f, origH);
        float r = clamp(toImageX(right, origW), 0f, origW);
        float b = clamp(toImageY(bottom, origH), 0f, origH);

        if (r <= l || b <= t) return;

        results.add(new Result(labels.get(classId), score, l, t, r, b));
    }

    private float toImageX(float x, int origW) {
        if (x >= 0f && x <= 1.5f) return x * origW;
        return (x / inputWidth) * origW;
    }

    private float toImageY(float y, int origH) {
        if (y >= 0f && y <= 1.5f) return y * origH;
        return (y / inputHeight) * origH;
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
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(context.getAssets().open(fileName)))) {
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