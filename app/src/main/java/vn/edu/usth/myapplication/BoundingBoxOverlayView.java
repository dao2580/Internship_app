package vn.edu.usth.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoundingBoxOverlayView extends View {

    public interface OnSpeakClickListener {
        void onSpeakClick(DetectionItem item);
    }

    public static class DetectionItem {
        public final String id;
        public final YoloV8Classifier.Result result;
        public final String labelVi;
        public final String translatedLabel;
        public final String targetLangCode;

        public DetectionItem(String id,
                             YoloV8Classifier.Result result,
                             String labelVi,
                             String translatedLabel,
                             String targetLangCode) {
            this.id = id;
            this.result = result;
            this.labelVi = labelVi;
            this.translatedLabel = translatedLabel;
            this.targetLangCode = targetLangCode;
        }
    }

    private final float density;

    private final Paint boxStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boxFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final TextPaint topTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint bottomTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private final Map<String, RectF> speakButtonRects = new HashMap<>();
    private final Map<String, DetectionItem> detectionById = new HashMap<>();

    private List<DetectionItem> detections = new ArrayList<>();
    private int imageW = 1;
    private int imageH = 1;

    @Nullable
    private OnSpeakClickListener onSpeakClickListener;

    public BoundingBoxOverlayView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        init();
    }

    public BoundingBoxOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        boxStrokePaint.setStyle(Paint.Style.STROKE);
        boxStrokePaint.setStrokeWidth(2.6f * density);

        boxFillPaint.setStyle(Paint.Style.FILL);

        chipPaint.setStyle(Paint.Style.FILL);
        cardPaint.setStyle(Paint.Style.FILL);
        playButtonPaint.setStyle(Paint.Style.FILL);

        playIconPaint.setStyle(Paint.Style.FILL);
        playIconPaint.setColor(Color.WHITE);

        topTextPaint.setColor(Color.WHITE);
        topTextPaint.setFakeBoldText(true);
        topTextPaint.setTextSize(13f * density);

        bottomTextPaint.setColor(Color.WHITE);
        bottomTextPaint.setTextSize(14f * density);
        bottomTextPaint.setFakeBoldText(true);
    }

    public void setOnSpeakClickListener(@Nullable OnSpeakClickListener listener) {
        this.onSpeakClickListener = listener;
    }

    public void setDetections(@Nullable List<DetectionItem> items, int imageW, int imageH) {
        this.detections = items != null ? items : new ArrayList<>();
        this.imageW = Math.max(1, imageW);
        this.imageH = Math.max(1, imageH);

        detectionById.clear();
        for (DetectionItem item : this.detections) {
            detectionById.put(item.id, item);
        }
        postInvalidateOnAnimation();
    }

    public void clear() {
        detections = new ArrayList<>();
        detectionById.clear();
        speakButtonRects.clear();
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (detections == null || detections.isEmpty()) return;
        if (imageW <= 0 || imageH <= 0) return;

        speakButtonRects.clear();

        float viewW = getWidth();
        float viewH = getHeight();

        float scale = Math.max(viewW / imageW, viewH / imageH);
        float drawW = imageW * scale;
        float drawH = imageH * scale;
        float offsetX = (viewW - drawW) / 2f;
        float offsetY = (viewH - drawH) / 2f;

        for (DetectionItem item : detections) {
            drawDetection(canvas, item, scale, offsetX, offsetY);
        }
    }

    private void drawDetection(Canvas canvas,
                               DetectionItem item,
                               float scale,
                               float offsetX,
                               float offsetY) {
        YoloV8Classifier.Result r = item.result;
        int accent = colorForLabel(r.label);

        float left = offsetX + r.left * scale;
        float top = offsetY + r.top * scale;
        float right = offsetX + r.right * scale;
        float bottom = offsetY + r.bottom * scale;

        RectF box = new RectF(left, top, right, bottom);

        boxStrokePaint.setColor(accent);
        boxFillPaint.setColor(withAlpha(accent, 36));
        chipPaint.setColor(withAlpha(accent, 210));
        cardPaint.setColor(withAlpha(Color.BLACK, 205));
        playButtonPaint.setColor(accent);

        float radius = 16f * density;
        canvas.drawRoundRect(box, radius, radius, boxFillPaint);
        canvas.drawRoundRect(box, radius, radius, boxStrokePaint);

        String topText = r.label + "  " + Math.round(r.conf * 100f) + "%";
        drawTopChip(canvas, box, topText);

        String translated = item.translatedLabel;
        if (translated == null || translated.trim().isEmpty()) {
            translated = item.labelVi;
        }

        RectF buttonRect = drawBottomCard(canvas, box, translated);
        if (buttonRect != null) {
            speakButtonRects.put(item.id, buttonRect);
        }
    }

    private void drawTopChip(Canvas canvas, RectF box, String rawText) {
        float padH = 10f * density;
        float padV = 7f * density;
        float chipH = 30f * density;
        float maxWidth = getWidth() - 16f * density;

        String text = TextUtils.ellipsize(
                rawText,
                topTextPaint,
                maxWidth - padH * 2f,
                TextUtils.TruncateAt.END
        ).toString();

        float textW = topTextPaint.measureText(text);
        float chipW = textW + padH * 2f;

        float left = clamp(box.left, 8f * density, getWidth() - chipW - 8f * density);
        float top = box.top - chipH - 8f * density;
        if (top < 8f * density) {
            top = box.top + 8f * density;
        }

        RectF chip = new RectF(left, top, left + chipW, top + chipH);
        canvas.drawRoundRect(chip, 14f * density, 14f * density, chipPaint);

        float baseline = chip.centerY() - (topTextPaint.descent() + topTextPaint.ascent()) / 2f;
        canvas.drawText(text, chip.left + padH, baseline, topTextPaint);
    }

    @Nullable
    private RectF drawBottomCard(Canvas canvas, RectF box, String rawText) {
        float reservedBottom = 94f * density;
        float outerPad = 8f * density;
        float innerPad = 12f * density;
        float cardH = 42f * density;
        float btnSize = 28f * density;
        float gap = 10f * density;

        float maxCardW = getWidth() - 16f * density;
        float textMaxW = maxCardW - innerPad * 3f - btnSize;

        String text = TextUtils.ellipsize(
                rawText,
                bottomTextPaint,
                textMaxW,
                TextUtils.TruncateAt.END
        ).toString();

        float textW = bottomTextPaint.measureText(text);
        float cardW = Math.min(maxCardW, textW + innerPad * 3f + btnSize);
        cardW = Math.max(cardW, 140f * density);

        float left = clamp(box.left, 8f * density, getWidth() - cardW - 8f * density);

        float top = box.bottom + outerPad;
        float safeBottom = getHeight() - reservedBottom;
        if (top + cardH > safeBottom) {
            top = box.top - outerPad - cardH;
        }
        top = clamp(top, 8f * density, getHeight() - cardH - 8f * density);

        RectF card = new RectF(left, top, left + cardW, top + cardH);
        canvas.drawRoundRect(card, 16f * density, 16f * density, cardPaint);

        float textBaseline = card.centerY() - (bottomTextPaint.descent() + bottomTextPaint.ascent()) / 2f;
        canvas.drawText(text, card.left + innerPad, textBaseline, bottomTextPaint);

        RectF btn = new RectF(
                card.right - innerPad - btnSize,
                card.centerY() - btnSize / 2f,
                card.right - innerPad,
                card.centerY() + btnSize / 2f
        );
        canvas.drawRoundRect(btn, 14f * density, 14f * density, playButtonPaint);

        Path triangle = new Path();
        float cx = btn.centerX();
        float cy = btn.centerY();
        float s = 6.5f * density;
        triangle.moveTo(cx - s * 0.6f, cy - s);
        triangle.lineTo(cx - s * 0.6f, cy + s);
        triangle.lineTo(cx + s, cy);
        triangle.close();
        canvas.drawPath(triangle, playIconPaint);

        return btn;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) return super.onTouchEvent(null);

        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            for (Map.Entry<String, RectF> entry : speakButtonRects.entrySet()) {
                if (entry.getValue().contains(x, y)) {
                    DetectionItem item = detectionById.get(entry.getKey());
                    if (item != null && onSpeakClickListener != null) {
                        onSpeakClickListener.onSpeakClick(item);
                        performClick();
                        return true;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int colorForLabel(String label) {
        int hash = Math.abs(label.hashCode());
        float hue = hash % 360;
        float[] hsv = new float[]{hue, 0.70f, 1.0f};
        return Color.HSVToColor(hsv);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(
                alpha,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}