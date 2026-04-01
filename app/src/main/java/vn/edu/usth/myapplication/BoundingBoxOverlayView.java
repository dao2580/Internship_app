package vn.edu.usth.myapplication;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class BoundingBoxOverlayView extends View {

    public interface OnObjectSelectedListener {
        void onObjectSelected(String labelEn, String labelVi);
    }

    public static class DetectedBox {
        public final RectF box;
        public final String labelEn, labelVi;
        public final float confidence;
        public DetectedBox(RectF box, String en, String vi, float conf) {
            this.box = box; labelEn = en; labelVi = vi; confidence = conf;
        }
    }

    private static class BtnArea {
        final RectF rect; final DetectedBox item;
        BtnArea(RectF r, DetectedBox d) { rect = r; item = d; }
    }

    private List<DetectedBox> boxes = new ArrayList<>();
    private final List<BtnArea> btnAreas = new ArrayList<>();
    private OnObjectSelectedListener listener;

    private final Paint pBox   = new Paint(), pLbBg  = new Paint(),
            pEn    = new Paint(), pVi    = new Paint(),
            pConf  = new Paint(), pBtn   = new Paint(),
            pBtnTx = new Paint();

    public BoundingBoxOverlayView(Context c) { super(c); init(); }
    public BoundingBoxOverlayView(Context c, AttributeSet a) { super(c, a); init(); }
    public BoundingBoxOverlayView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        pBox.setStyle(Paint.Style.STROKE);
        pBox.setColor(Color.parseColor("#00E5FF")); pBox.setStrokeWidth(4f); pBox.setAntiAlias(true);

        pLbBg.setStyle(Paint.Style.FILL); pLbBg.setColor(Color.parseColor("#CC000000"));

        pEn.setColor(Color.WHITE); pEn.setTextSize(42f); pEn.setFakeBoldText(true); pEn.setAntiAlias(true);
        pVi.setColor(Color.parseColor("#FFD600")); pVi.setTextSize(34f); pVi.setAntiAlias(true);
        pConf.setColor(Color.parseColor("#99FFFFFF")); pConf.setTextSize(28f); pConf.setAntiAlias(true);

        pBtn.setStyle(Paint.Style.FILL); pBtn.setColor(Color.parseColor("#FF5722")); pBtn.setAntiAlias(true);
        pBtnTx.setColor(Color.WHITE); pBtnTx.setTextSize(30f); pBtnTx.setFakeBoldText(true); pBtnTx.setAntiAlias(true);
    }

    public void setOnObjectSelectedListener(OnObjectSelectedListener l) { listener = l; }

    public void setBoxes(List<DetectedBox> list) {
        boxes = list != null ? list : new ArrayList<>();
        postInvalidate();
    }

    public void clear() { boxes = new ArrayList<>(); postInvalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        btnAreas.clear();
        int vW = getWidth(), vH = getHeight();

        for (DetectedBox d : boxes) {
            float l = Math.max(2f, d.box.left),    t = Math.max(2f, d.box.top);
            float r = Math.min(vW-2f, d.box.right), b = Math.min(vH-2f, d.box.bottom);
            if (r <= l || b <= t) continue;

            // Bounding box
            canvas.drawRect(l, t, r, b, pBox);

            // Label nền + tên EN trắng + tên VI vàng
            float lbW = Math.max(pEn.measureText(d.labelEn), pVi.measureText(d.labelVi)) + 24f;
            float lbTop = (t - 90f >= 0) ? t - 90f : b;
            canvas.drawRect(l, lbTop, l + lbW, lbTop + 90f, pLbBg);
            canvas.drawText(d.labelEn, l + 10f, lbTop + 38f, pEn);
            canvas.drawText(d.labelVi, l + 10f, lbTop + 78f, pVi);

            // Confidence
            String cs = String.format("%.0f%%", d.confidence * 100);
            canvas.drawText(cs, r - pConf.measureText(cs) - 6f, t + 30f, pConf);

            // Nút "Học từ này"
            String btnStr = " Học từ này ";
            float btnW = pBtnTx.measureText(btnStr) + 8f, btnH = 54f;
            float btnL = l, btnT = b + 8f;
            if (btnT + btnH > vH) btnT = b - btnH - 8f;
            if (btnL + btnW > vW) btnL = vW - btnW - 4f;
            RectF btnRect = new RectF(btnL, btnT, btnL + btnW, btnT + btnH);
            canvas.drawRoundRect(btnRect, 14f, 14f, pBtn);
            canvas.drawText(btnStr, btnL + 4f, btnT + 36f, pBtnTx);
            btnAreas.add(new BtnArea(btnRect, d));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            for (BtnArea ba : btnAreas) {
                if (ba.rect.contains(e.getX(), e.getY()) && listener != null) {
                    listener.onObjectSelected(ba.item.labelEn, ba.item.labelVi);
                    return true;
                }
            }
        }
        return super.onTouchEvent(e);
    }
}