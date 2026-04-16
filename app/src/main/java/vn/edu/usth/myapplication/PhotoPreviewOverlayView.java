package vn.edu.usth.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

public class PhotoPreviewOverlayView extends View {

    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Matrix drawMatrix = new Matrix();
    private YoloV8Classifier.Result detection;

    public PhotoPreviewOverlayView(Context context) {
        super(context);
        init();
    }

    public PhotoPreviewOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5f);
        boxPaint.setColor(Color.parseColor("#00E5A8"));

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(35, 0, 229, 168));

        labelBgPaint.setStyle(Paint.Style.FILL);
        labelBgPaint.setColor(Color.parseColor("#CC00E5A8"));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(34f);
        textPaint.setFakeBoldText(true);
    }

    public void setDetection(@Nullable YoloV8Classifier.Result result, @Nullable ImageView imageView) {
        detection = result;

        drawMatrix.reset();
        if (imageView != null) {
            drawMatrix.set(imageView.getImageMatrix());
        }

        postInvalidateOnAnimation();
    }

    public void clear() {
        detection = null;
        drawMatrix.reset();
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (detection == null) return;

        RectF rect = new RectF(detection.left, detection.top, detection.right, detection.bottom);
        drawMatrix.mapRect(rect);

        rect.left = clamp(rect.left, 0, getWidth());
        rect.top = clamp(rect.top, 0, getHeight());
        rect.right = clamp(rect.right, 0, getWidth());
        rect.bottom = clamp(rect.bottom, 0, getHeight());

        canvas.drawRoundRect(rect, 14f, 14f, fillPaint);
        canvas.drawRoundRect(rect, 14f, 14f, boxPaint);

        String label = detection.label + " " + Math.round(detection.conf * 100f) + "%";

        float textPaddingX = 18f;
        float textPaddingY = 12f;
        float textWidth = textPaint.measureText(label);
        float textHeight = textPaint.getTextSize() + textPaddingY * 2f;

        float bgLeft = rect.left;
        float bgTop = rect.top - textHeight - 8f;
        if (bgTop < 0) bgTop = rect.top + 8f;

        float bgRight = Math.min(bgLeft + textWidth + textPaddingX * 2f, getWidth() - 8f);
        float bgBottom = bgTop + textHeight;

        RectF labelBg = new RectF(bgLeft, bgTop, bgRight, bgBottom);
        canvas.drawRoundRect(labelBg, 12f, 12f, labelBgPaint);

        float textX = labelBg.left + textPaddingX;
        float textY = labelBg.bottom - textPaddingY;
        canvas.drawText(label, textX, textY, textPaint);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}