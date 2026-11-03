package me.raeeindustries.seaglass;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.core.view.ViewCompat;

final class Glass {
    static int ACCENT = 0;
    static int BUBBLE = 0;
    static int EDGE = 0;
    static int GLASS = 0;
    static int GLASS_FIELD = 0;
    static int GLASS_PANEL = 0;
    static int GLASS_STRONG = 0;
    static int GLOW = 0;
    static int INK = 0;
    static int INK_SOFT = 0;
    static final int LISTENING = -1948580;
    static int LOCK = 0;
    static final String MIDNIGHT = "midnight";
    static int PILL = 0;
    static int PRIVATE = 0;
    static int ROW = 0;
    static int ROW_ON = 0;
    static int SCRIM = 0;
    static int[] SHEEN = null;
    static int[] SHEEN_OVAL = null;
    static int SHEET = 0;
    static final int SPEAKING = -9741338;
    static int SWIPE_BG = 0;
    static final String TEAL = "teal";
    static int TEAL_BG;
    static int WEB_BOX;
    static boolean midnight;

    static {
        apply(false);
    }

    static void load(Context c) {
        apply(MIDNIGHT.equals(c.getSharedPreferences("seaglass", 0).getString("look", TEAL)));
    }

    static void save(Context c, boolean useMidnight) {
        c.getSharedPreferences("seaglass", 0).edit().putString("look", useMidnight ? MIDNIGHT : TEAL).apply();
        apply(useMidnight);
    }

    static void apply(boolean useMidnight) {
        midnight = useMidnight;
        if (useMidnight) {
            TEAL_BG = -16113098;
            INK = -1509889;
            INK_SOFT = -6302731;
            ACCENT = -14771232;
            GLASS = 1293635194;
            GLASS_PANEL = -536139968;
            GLASS_STRONG = -870957210;
            EDGE = -1500720385;
            GLASS_FIELD = -1291445715;
            SHEET = -150397388;
            ROW = -15452326;
            ROW_ON = -14787958;
            PILL = -234349520;
            SCRIM = 1711278100;
            PRIVATE = -1724240244;
            LOCK = -149680333;
            BUBBLE = -2145694086;
            WEB_BOX = 637934125;
            SWIPE_BG = -16046272;
            SHEEN = new int[]{1082970879, 227332863, 9229055};
            SHEEN_OVAL = new int[]{1435292415, 227332863, 9229055};
            GLOW = -11549697;
            return;
        }
        TEAL_BG = -7349791;
        INK = -16371141;
        INK_SOFT = -14396065;
        ACCENT = -16020839;
        GLASS = 315160314;
        GLASS_PANEL = 566818554;
        GLASS_STRONG = -1495073538;
        EDGE = -419430401;
        GLASS_FIELD = -1931281154;
        SHEET = -303891202;
        ROW = -1073741825;
        ROW_ON = 1292602009;
        PILL = -421331714;
        SCRIM = 637940283;
        PRIVATE = 644570086;
        LOCK = -219552005;
        BUBBLE = 533264122;
        WEB_BOX = 184549375;
        SWIPE_BG = -1901314;
        SHEEN = new int[]{1509949439, 352321535, ViewCompat.MEASURED_SIZE_MASK};
        SHEEN_OVAL = new int[]{1728053247, 234881023, ViewCompat.MEASURED_SIZE_MASK};
        GLOW = ViewCompat.MEASURED_STATE_MASK;
    }

    static void glow(View v) {
        if (Build.VERSION.SDK_INT < 28) {
            return;
        }
        v.setOutlineAmbientShadowColor(GLOW);
        v.setOutlineSpotShadowColor(GLOW);
    }

    private Glass() {
    }

    static int dp(Context c, float v) {
        return Math.round(c.getResources().getDisplayMetrics().density * v);
    }

    static GradientDrawable panel(Context c, int fill, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 1.0f)), EDGE);
        return g;
    }

    static Drawable clearGlass(Context c, int tint, float radiusDp) {
        GradientDrawable base = new GradientDrawable();
        base.setColor(tint);
        base.setCornerRadius(dp(c, radiusDp));
        base.setStroke(Math.max(2, dp(c, 1.5f)), EDGE);
        GradientDrawable sheen = new GradientDrawable(GradientDrawable.Orientation.TL_BR, SHEEN);
        sheen.setCornerRadius(dp(c, radiusDp));
        LayerDrawable glass = new LayerDrawable(new Drawable[]{base, sheen});
        int rim = Math.max(2, dp(c, 1.5f));
        glass.setLayerInset(1, rim, rim, rim, rim);
        return glass;
    }

    static Drawable clearGlassOval(Context c, int tint) {
        GradientDrawable base = new GradientDrawable();
        base.setShape(1);
        base.setColor(tint);
        base.setStroke(dp(c, 2.0f), EDGE);
        GradientDrawable sheen = new GradientDrawable(GradientDrawable.Orientation.TL_BR, SHEEN_OVAL);
        sheen.setShape(1);
        LayerDrawable glass = new LayerDrawable(new Drawable[]{base, sheen});
        glass.setLayerInset(1, dp(c, 2.0f), dp(c, 2.0f), dp(c, 2.0f), dp(c, 2.0f));
        return glass;
    }

    static GradientDrawable oval(Context c, int fill, int strokeDp, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(1);
        g.setColor(fill);
        if (strokeDp > 0) {
            g.setStroke(dp(c, strokeDp), strokeColor);
        }
        return g;
    }

    static ImageButton iconButton(Context c, int icon, String label, int sizeDp, int bg, int tint) {
        ImageButton b = new ImageButton(c);
        b.setImageResource(icon);
        b.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        b.setContentDescription(label);
        b.setBackground(bg == 0 ? null : panel(c, bg, 12.0f));
        int pad = dp(c, (sizeDp - 22) / 2.0f);
        b.setPadding(pad, pad, pad, pad);
        b.setScaleType(ImageView.ScaleType.FIT_CENTER);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return b;
    }
}
