package me.raeeindustries.seaglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/**
 * Neon voice waveform: layered glowing sine "lobes" in blue, violet and cyan,
 * like the Neon Glass design. Moves on its own and swells with the mic level.
 */
final class WaveView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private boolean active = true;
    private float level;

    WaveView(Context c) {
        super(c);
        fill.setStyle(Paint.Style.FILL);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
    }

    /** true = moving (listening), false = calm (done). */
    void setActive(boolean on) {
        active = on;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        float t = SystemClock.uptimeMillis() / 1000f;
        float target = active ? Math.max(0.35f, Math.min(1f, (VoiceEngine.level + 2f) / 10f)) : 0.12f;
        level += (target - level) * 0.18f;
        float mid = h / 2f;
        float d = getResources().getDisplayMetrics().density;

        // Thin centre line.
        line.setShader(null);
        line.setColor(0x664FB4FF);
        line.setStrokeWidth(1.2f * d);
        canvas.drawLine(0, mid, w, mid, line);

        int[][] colors = {
                {0x553BE8FF, 0xFF3BE8FF},   // cyan
                {0x552E8BFF, 0xFF4FB4FF},   // blue
                {0x669B6BFF, 0xFFB78CFF},   // violet (front, tallest)
        };
        float[] speed = {1.3f, -0.9f, 0.6f};
        float[] freq = {3.0f, 2.2f, 1.6f};
        float[] amp = {0.55f, 0.75f, 1.0f};
        for (int k = 0; k < 3; k++) {
            path.reset();
            int steps = 64;
            for (int i = 0; i <= steps; i++) {
                float x = w * i / (float) steps;
                float u = i / (float) steps;
                // Envelope: quiet at both ends, full in the middle.
                float env = (float) Math.pow(Math.sin(Math.PI * u), 1.6);
                float y = (float) Math.sin(u * Math.PI * 2 * freq[k] + t * speed[k] * 3) * env;
                float yy = mid - y * (h * 0.46f) * amp[k] * level;
                if (i == 0) path.moveTo(x, yy); else path.lineTo(x, yy);
            }
            // Mirror back along the other side so each lobe is a closed, glowing shape.
            for (int i = steps; i >= 0; i--) {
                float x = w * i / (float) steps;
                float u = i / (float) steps;
                float env = (float) Math.pow(Math.sin(Math.PI * u), 1.6);
                float y = (float) Math.sin(u * Math.PI * 2 * freq[k] + t * speed[k] * 3) * env;
                path.lineTo(x, mid + y * (h * 0.46f) * amp[k] * level);
            }
            path.close();
            fill.setShader(new LinearGradient(0, 0, 0, h, colors[k][0] & 0x00FFFFFF | 0x10000000,
                    colors[k][0], Shader.TileMode.MIRROR));
            canvas.drawPath(path, fill);
            // Glow: wide faint stroke, then a bright thin one.
            line.setShader(null);
            line.setColor(colors[k][0]);
            line.setStrokeWidth(4f * d);
            canvas.drawPath(path, line);
            line.setColor(colors[k][1]);
            line.setStrokeWidth(1.3f * d);
            canvas.drawPath(path, line);
        }
        if (isAttachedToWindow()) postInvalidateOnAnimation();
    }
}
