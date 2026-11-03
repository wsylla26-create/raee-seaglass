package me.raeeindustries.seaglass;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Line icons for the pull-down menu, drawn in code on a 24 x 24 grid
 * (same style as the app's other icons: 2-unit round strokes).
 */
final class MenuIcon extends Drawable {
    static final int TABS = 1, NEW_TAB = 2, PRIVATE = 3, BOOKMARKS = 4, HISTORY = 5, DOWNLOAD = 6,
            FIND = 7, SHARE = 8, ADD_HOME = 9, DESKTOP = 10, SETTINGS = 11, TRANSLATE = 12;

    private final int type;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    MenuIcon(int type, int color) {
        this.type = type;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(color);
        build();
    }

    private void line(float x1, float y1, float x2, float y2) {
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
    }

    private void box(float l, float t, float r, float b, float radius) {
        path.addRoundRect(new RectF(l, t, r, b), radius, radius, Path.Direction.CW);
    }

    private void build() {
        switch (type) {
            case TABS:
                box(4, 7, 17, 20, 2);
                path.moveTo(8, 4);
                path.lineTo(18, 4);
                path.quadTo(20, 4, 20, 6);
                path.lineTo(20, 16);
                break;
            case NEW_TAB:
                box(4, 4, 20, 20, 3);
                line(12, 8, 12, 16);
                line(8, 12, 16, 12);
                break;
            case PRIVATE:
                path.moveTo(3, 12);
                path.quadTo(12, 3.5f, 21, 12);
                path.quadTo(12, 20.5f, 3, 12);
                path.addCircle(12, 12, 3, Path.Direction.CW);
                line(4, 4, 20, 20);
                break;
            case BOOKMARKS:
                path.moveTo(6, 4);
                path.lineTo(18, 4);
                path.lineTo(18, 20);
                path.lineTo(12, 15.5f);
                path.lineTo(6, 20);
                path.close();
                break;
            case HISTORY:
                path.addCircle(12, 12, 8.5f, Path.Direction.CW);
                path.moveTo(12, 7);
                path.lineTo(12, 12);
                path.lineTo(15.5f, 14);
                break;
            case DOWNLOAD:
                line(12, 4, 12, 15);
                path.moveTo(7, 10);
                path.lineTo(12, 15);
                path.lineTo(17, 10);
                line(5, 20, 19, 20);
                break;
            case FIND:
                path.addCircle(10.5f, 10.5f, 6.5f, Path.Direction.CW);
                line(15.5f, 15.5f, 20, 20);
                break;
            case SHARE:
                path.addCircle(18, 5, 2.5f, Path.Direction.CW);
                path.addCircle(6, 12, 2.5f, Path.Direction.CW);
                path.addCircle(18, 19, 2.5f, Path.Direction.CW);
                line(8.2f, 10.8f, 15.8f, 6.3f);
                line(8.2f, 13.2f, 15.8f, 17.7f);
                break;
            case ADD_HOME:
                box(4, 4, 10, 10, 1.5f);
                box(14, 4, 20, 10, 1.5f);
                box(4, 14, 10, 20, 1.5f);
                line(17, 13.5f, 17, 20.5f);
                line(13.5f, 17, 20.5f, 17);
                break;
            case DESKTOP:
                box(3, 4, 21, 16, 2);
                line(8, 20, 16, 20);
                line(12, 16, 12, 20);
                break;
            case SETTINGS:
                path.addCircle(12, 12, 3, Path.Direction.CW);
                path.addCircle(12, 12, 6.5f, Path.Direction.CW);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    line(12 + (float) Math.cos(a) * 6.5f, 12 + (float) Math.sin(a) * 6.5f,
                            12 + (float) Math.cos(a) * 9.5f, 12 + (float) Math.sin(a) * 9.5f);
                }
                break;
            case TRANSLATE:
                line(4, 6, 12, 6);
                line(8, 4, 8, 6);
                path.moveTo(10.5f, 6);
                path.quadTo(9.5f, 12, 4.5f, 15);
                path.moveTo(6.5f, 9.5f);
                path.quadTo(8.5f, 13, 12, 14.5f);
                path.moveTo(13, 20);
                path.lineTo(16.5f, 11);
                path.lineTo(20, 20);
                line(14.2f, 17, 18.8f, 17);
                break;
            default:
                break;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        android.graphics.Rect b = getBounds();
        int save = canvas.save();
        canvas.translate(b.left, b.top);
        canvas.scale(b.width() / 24f, b.height() / 24f);
        canvas.drawPath(path, paint);
        canvas.restoreToCount(save);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return 72;
    }

    @Override
    public int getIntrinsicHeight() {
        return 72;
    }
}
