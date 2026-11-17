package me.raeeindustries.seaglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.SystemClock;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Neon Glass "energy flow" list: each card is linked to the next one by a glowing
 * energy line that curves out on the right, with a spark that travels along it.
 */
final class EnergyList extends LinearLayout {
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final PathMeasure measure = new PathMeasure();
    private final float[] pos = new float[2];
    private final float d;

    EnergyList(Context c) {
        super(c);
        setOrientation(VERTICAL);
        d = c.getResources().getDisplayMetrics().density;
        setPadding(0, 0, Math.round(22 * d), 0);
        setClipToPadding(false);
        setWillNotDraw(false);
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeCap(Paint.Cap.ROUND);
        dot.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int cards = 0;
        View prev = null;
        float t = (SystemClock.uptimeMillis() % 2400L) / 2400f;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v.getVisibility() != VISIBLE || v.getHeight() < 20 * d) continue;
            cards++;
            if (prev != null) {
                float x = prev.getRight();
                float y1 = prev.getTop() + prev.getHeight() / 2f;
                float y2 = v.getTop() + v.getHeight() / 2f;
                float out = x + 18 * d;
                path.reset();
                path.moveTo(x, y1);
                path.cubicTo(out, y1, out, y2, x, y2);
                // Glow: three passes from wide and faint to thin and bright.
                stroke(canvas, 0x262E8BFF, 7f);
                stroke(canvas, 0x593BE8FF, 3.5f);
                stroke(canvas, 0xFF7FE6FF, 1.3f);
                // Travelling spark.
                measure.setPath(path, false);
                measure.getPosTan(measure.getLength() * t, pos, null);
                dot.setColor(0x553BE8FF);
                canvas.drawCircle(pos[0], pos[1], 5 * d, dot);
                dot.setColor(0xFFE9FBFF);
                canvas.drawCircle(pos[0], pos[1], 2 * d, dot);
                // Nodes where the line meets each card.
                node(canvas, x, y1);
                node(canvas, x, y2);
            }
            prev = v;
        }
        super.dispatchDraw(canvas);
        if (cards > 1 && isAttachedToWindow()) postInvalidateDelayed(33);
    }

    private void stroke(Canvas c, int color, float widthDp) {
        glow.setColor(color);
        glow.setStrokeWidth(widthDp * d);
        c.drawPath(path, glow);
    }

    private void node(Canvas c, float x, float y) {
        dot.setColor(0x664FB4FF);
        c.drawCircle(x, y, 4.5f * d, dot);
        dot.setColor(0xFFBFF2FF);
        c.drawCircle(x, y, 2f * d, dot);
    }
}
