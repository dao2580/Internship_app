package vn.edu.usth.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxOverlayView extends View {

    // Mỗi detection result kèm nhãn VI để vẽ

    public static class DetectionItem {
        public final YoloV8Classifier.Result result;
        public final String labelVi;

        public DetectionItem(YoloV8Classifier.Result result, String labelVi) {
            this.result = result;
            this.labelVi = labelVi;

        }
    }

    private final Paint boxPaint = new Paint();
    private final Paint textBg = new Paint();
    private final Paint textPaint = new Paint();

    private List<DetectionItem> items = new ArrayList<>();

    // Tỷ lệ scale từ ảnh gốc ra màn hình
    private float scaleX = 1f, scaleY = 1f;

    public BoundingBoxOverlayView(Context context) {
        super(context);
        init();
    }

    public BoundingBoxOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3f);
        boxPaint.setColor(Color.parseColor("#00C896")); // teal

        textBg.setStyle(Paint.Style.FILL);
        textBg.setColor(Color.parseColor("#CC00C896"));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setFakeBoldText(true);
        textPaint.setAntiAlias(true);
    }

    /**
     * Gọi từ StreamingFragment sau khi detect xong.
     *
     * @param items  danh sách detection + nhãn VI
     * @param imgW   chiều rộng frame gốc (dùng để tính scale)
     * @param imgH   chiều cao frame gốc
     */
    private List<DetectionItem> detections = new ArrayList<>();
    private int imageW = 1;
    private int imageH = 1;

    public void setDetections(List<DetectionItem> items, int imageW, int imageH) {
        this.detections = items != null ? items : new ArrayList<>();
        this.imageW = imageW;
        this.imageH = imageH;
        postInvalidate();
    }

    public void clear() {
        detections = new ArrayList<>();
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (detections == null || detections.isEmpty()) return;
        if (imageW <= 0 || imageH <= 0) return;

        float scaleX = (float) getWidth() / imageW;
        float scaleY = (float) getHeight() / imageH;

        for (DetectionItem item : detections) {
            YoloV8Classifier.Result r = item.result;

            float left = r.left * scaleX;
            float top = r.top * scaleY;
            float right = r.right * scaleX;
            float bottom = r.bottom * scaleY;

            canvas.drawRect(left, top, right, bottom, boxPaint);

            String text = r.label + " / " + item.labelVi + " " + String.format("%.2f", r.conf);
            canvas.drawText(text, left, Math.max(top - 10, 40), textPaint);
        }
    }
}

