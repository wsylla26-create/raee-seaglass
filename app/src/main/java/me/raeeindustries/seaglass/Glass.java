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
        apply(MIDNIGHT.equals(c.getSharedPreferences("seaglass", 0).getString("look", MIDNIGHT)));
    }

    static void save(Context c, boolean useMidnight) {
        c.getSharedPreferences("seaglass", 0).edit().putString("look", useMidnight ? MIDNIGHT : TEAL).apply();
        apply(useMidnight);
    }

    static void apply(boolean useMidnight) {
        midnight = useMidnight;
        if (useMidnight) {
            // Neon Glass: deep-space navy with glowing electric-blue glass edges.
            TEAL_BG = 0xFF040A1C;
            INK = 0xFFEAF6FF;
            INK_SOFT = 0xFF8EC5F5;
            ACCENT = 0xFF2E8BFF;
            GLASS = 0x590A1A40;
            GLASS_PANEL = 0xCC071433;
            GLASS_STRONG = 0x33349CFF;
            EDGE = 0xE64FB4FF;
            GLASS_FIELD = 0xB30A1838;
            SHEET = 0xF2060E26;
            ROW = 0xCC0A1A3D;
            ROW_ON = 0xD912357A;
            PILL = 0xF0081633;
            SCRIM = 0x99000510;
            PRIVATE = 0x99602A9E;
            LOCK = 0xF7060E26;
            BUBBLE = 0x80122B66;
            WEB_BOX = 637934125;
            SWIPE_BG = 0xFF0A1838;
            SHEEN = new int[]{0x334FB4FF, 0x0D4FB4FF, 0x004FB4FF};
            SHEEN_OVAL = new int[]{0x554FB4FF, 0x0D4FB4FF, 0x004FB4FF};
            GLOW = 0xFF2E8BFF;
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

    static final int NEON = 0xFF4FB4FF;
    static final int NEON_CYAN = 0xFF3BE8FF;
    static final int NEON_VIOLET = 0xFF9B6BFF;
    static final int NEON_GREEN = 0xFF4DF59A;

    /** Deep-space backdrop used behind the Neon Glass chrome. */
    static Drawable space() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0B1A4A, 0xFF050C24, 0xFF02050F});
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
