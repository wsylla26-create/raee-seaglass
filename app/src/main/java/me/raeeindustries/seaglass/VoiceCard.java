package me.raeeindustries.seaglass;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Neon Glass voice card: a glowing glass card with a live waveform that appears
 * while SeaGlass is listening, then shows "Completed ✓" in green.
 */
final class VoiceCard extends LinearLayout {
    private final WaveView wave;
    private final TextView heard;
    private final TextView state;
    private final TextView done;

    VoiceCard(Context c) {
        super(c);
        setOrientation(VERTICAL);
        int pad = Glass.dp(c, 16);
        setPadding(pad, Glass.dp(c, 12), pad, Glass.dp(c, 12));
        setBackground(neonCard(c, 22));
        setElevation(Glass.dp(c, 10));
        Glass.glow(this);
        setClickable(true);

        TextView title = new TextView(c);
        title.setText("VOICE CONTROLS");
        title.setTextColor(Glass.INK);
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(0.08f);
        title.setShadowLayer(Glass.dp(c, 6), 0, 0, Glass.NEON);
        addView(title);

        wave = new WaveView(c);
        LayoutParams wl = new LayoutParams(-1, Glass.dp(c, 70));
        wl.topMargin = Glass.dp(c, 6);
        addView(wave, wl);

        heard = new TextView(c);
        heard.setTextColor(Glass.INK);
        heard.setTextSize(15);
        heard.setMaxLines(2);
        heard.setEllipsize(TextUtils.TruncateAt.END);
        heard.setGravity(Gravity.CENTER_HORIZONTAL);
        heard.setVisibility(GONE);
        addView(heard, new LayoutParams(-1, -2));

        LinearLayout foot = new LinearLayout(c);
        foot.setOrientation(HORIZONTAL);
        foot.setGravity(Gravity.CENTER_VERTICAL);
        state = new TextView(c);
        state.setTextColor(Glass.INK_SOFT);
        state.setTextSize(14);
        state.setTypeface(Typeface.DEFAULT_BOLD);
        foot.addView(state, new LayoutParams(0, -2, 1));
        done = new TextView(c);
        done.setText("Completed ✓");
        done.setTextColor(Glass.NEON_GREEN);
        done.setTextSize(14);
        done.setTypeface(Typeface.DEFAULT_BOLD);
        done.setShadowLayer(Glass.dp(c, 6), 0, 0, 0x994DF59A);
        done.setVisibility(INVISIBLE);
        foot.addView(done);
        LayoutParams fl = new LayoutParams(-1, -2);
        fl.topMargin = Glass.dp(c, 6);
        addView(foot, fl);
        listening();
    }

    void listening() {
        wave.setActive(true);
        state.setText("◉  Listening");
        state.setTextColor(Glass.INK_SOFT);
        done.setVisibility(INVISIBLE);
        heard.setVisibility(GONE);
    }

    void partial(String text) {
        if (text == null || text.isEmpty()) return;
        heard.setText(text);
        heard.setVisibility(VISIBLE);
    }

    void completed(String text) {
        wave.setActive(false);
        partial("“" + text + "”");
        state.setText("◉  Heard you");
        done.setText("Completed ✓");
        done.setTextColor(Glass.NEON_GREEN);
        done.setVisibility(VISIBLE);
    }

    void failed(String message) {
        wave.setActive(false);
        state.setText("◉  Stopped");
        partial(message);
        done.setVisibility(INVISIBLE);
    }

    /** Dark glass card with a bright electric-blue rim and a soft blue sheen. */
    static Drawable neonCard(Context c, float radiusDp) {
        float r = Glass.dp(c, radiusDp);
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xF20B1C47, 0xF2071233, 0xF2050B22});
        base.setCornerRadius(r);
        base.setStroke(Math.max(2, Glass.dp(c, 1.6f)), Glass.NEON);
        GradientDrawable halo = new GradientDrawable();
        halo.setCornerRadius(r);
        halo.setColor(0);
        halo.setStroke(Glass.dp(c, 5), 0x332E8BFF);
        GradientDrawable sheen = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x224FB4FF, 0x004FB4FF});
        sheen.setCornerRadius(r);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{halo, base, sheen});
        int in = Glass.dp(c, 2);
        ld.setLayerInset(1, in, in, in, in);
        ld.setLayerInset(2, in * 2, in * 2, in * 2, in * 2);
        return ld;
    }
}
