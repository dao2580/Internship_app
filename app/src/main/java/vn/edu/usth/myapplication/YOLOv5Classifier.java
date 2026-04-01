package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import org.tensorflow.lite.Interpreter;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.*;

public final class YOLOv5Classifier {
    private static YOLOv5Classifier instance;
    private final Interpreter interpreter;
    private final int inputSize = 640;
    private int numClasses = 80; // đổi số này khi thay model mới
    private final List<String> labels = new ArrayList<>();

    private YOLOv5Classifier(Context context) {
        try {
            interpreter = new Interpreter(loadModelFile(context, "yolov5s-fp16.tflite"));
            loadLabels(context, "labels.txt");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YOLO model", e);
        }
    }

    public static synchronized YOLOv5Classifier getInstance(Context context) {
        if (instance == null)
            instance = new YOLOv5Classifier(context.getApplicationContext());
        return instance;
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd(modelName);
        try (FileInputStream is = new FileInputStream(fd.getFileDescriptor())) {
            return is.getChannel().map(FileChannel.MapMode.READ_ONLY,
                    fd.getStartOffset(), fd.getDeclaredLength());
        }
    }

    private void loadLabels(Context context, String fileName) throws IOException {
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(context.getAssets().open(fileName)))) {
            String line;
            while ((line = r.readLine()) != null) labels.add(line);
        }
    }

    /** One-Shot: trả đúng 1 object confidence cao nhất */
    public List<Result> detect(Bitmap bitmap) {
        return runDetection(bitmap, 1);
    }

    /** Streaming: trả tối đa 3 objects với NMS */
    public List<Result> detectTop3(Bitmap bitmap) {
        return runDetection(bitmap, 3);
    }

    private List<Result> runDetection(Bitmap bitmap, int maxResults) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);
        ByteBuffer input = preprocessBitmap(resized);
        float[][][] output = new float[1][25200][5 + numClasses];
        interpreter.run(input, output);
        return postprocess(output, bitmap.getWidth(), bitmap.getHeight(), maxResults);
    }

    private ByteBuffer preprocessBitmap(Bitmap bitmap) {
        ByteBuffer buf = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
                .order(ByteOrder.nativeOrder());
        int[] pixels = new int[inputSize * inputSize];
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);
        for (int val : pixels) {
            buf.putFloat(((val >> 16) & 0xFF) / 255f);
            buf.putFloat(((val >> 8)  & 0xFF) / 255f);
            buf.putFloat((val         & 0xFF) / 255f);
        }
        return buf;
    }

    private List<Result> postprocess(float[][][] output, int origW, int origH, int maxResults) {
        List<Result> candidates = new ArrayList<>();
        for (float[] row : output[0]) {
            float conf = row[4];
            if (conf < 0.4f) continue;
            int classId = -1;
            float maxProb = 0;
            for (int c = 5; c < 5 + numClasses; c++) {
                if (row[c] > maxProb) { maxProb = row[c]; classId = c - 5; }
            }
            if (classId < 0 || classId >= labels.size()) continue;
            float cx = row[0], cy = row[1], w = row[2], h = row[3];
            candidates.add(new Result(
                    labels.get(classId), conf,
                    (cx - w/2) / inputSize * origW,
                    (cy - h/2) / inputSize * origH,
                    (cx + w/2) / inputSize * origW,
                    (cy + h/2) / inputSize * origH));
        }

        Collections.sort(candidates, (a, b) -> Float.compare(b.conf, a.conf));

        // NMS
        List<Result> results = new ArrayList<>();
        for (Result c : candidates) {
            if (results.size() >= maxResults) break;
            boolean skip = false;
            for (Result kept : results) {
                if (iou(c, kept) > 0.5f) { skip = true; break; }
            }
            if (!skip) results.add(c);
        }
        return results;
    }

    private float iou(Result a, Result b) {
        float iL = Math.max(a.left, b.left), iT = Math.max(a.top, b.top);
        float iR = Math.min(a.right, b.right), iB = Math.min(a.bottom, b.bottom);
        if (iR <= iL || iB <= iT) return 0f;
        float inter = (iR - iL) * (iB - iT);
        return inter / ((a.right-a.left)*(a.bottom-a.top)
                + (b.right-b.left)*(b.bottom-b.top) - inter);
    }

    public static class Result {
        public final String label;
        public final float conf;
        public final float left, top, right, bottom;

        public Result(String label, float conf,
                      float left, float top, float right, float bottom) {
            this.label = label; this.conf = conf;
            this.left = left; this.top = top;
            this.right = right; this.bottom = bottom;
        }
    }
}