package me.raeeindustries.seaglass;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Insets;
import android.graphics.PorterDuff;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewDatabase;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.Profile;
import androidx.webkit.ProfileStore;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;

public class MainActivity extends Activity implements VoiceEngine.Listener, CommandRouter.Browser, Billing.Listener {
    static final String EXTRA_SHOW = "me.raeeindustries.seaglass.SHOW";
    private static final int MATCH = -1;
    private static final String PRIVATE_PROFILE = "seaglass_private";
    private static final int REQ_FILE = 43;
    private static final int REQ_LOCATION = 45;
    private static final int REQ_MIC = 41;
    private static final int REQ_NOTIFY = 42;
    private static final int REQ_SITE = 44;
    private static final int REQ_STORAGE = 47;
    private static final int REQ_WEATHER = 48;
    private static final int WRAP = -2;
    private volatile boolean adBlockOn;
    private EditText address;
    private WebViewAssetLoader assets;
    private boolean authInProgress;
    private Billing billing;
    private Tab current;
    private WebChromeClient.CustomViewCallback customCallback;
    private View customView;
    private LinearLayout dock;
    private Downloads downloads;
    private ValueCallback<Uri[]> fileCallback;
    private LinearLayout findBar;
    private TextView findCount;
    private EditText findField;
    private int insetBottom;
    private int insetLeft;
    private int insetRight;
    private int insetTop;
    private boolean lastFromVoice;
    private Library library;
    private GeolocationPermissions.Callback locationCallback;
    private String locationOrigin;
    private FrameLayout lockCover;
    private boolean lockPromptDismissed;
    private ImageButton micButton;
    private ImageButton muteButton;
    private String[] pendingDownload;
    private boolean pendingOverlay;
    private SharedPreferences prefs;
    private boolean privateLocked;
    private ProgressBar progress;
    private String readerText;
    private FrameLayout readerView;
    private FrameLayout root;
    private FrameLayout sheet;
    private PermissionRequest sitePermission;
    private String[] siteResources;
    private boolean speaking;
    private ImageButton starButton;
    private TextView statusPill;
    private SwipeRefreshLayout swipe;
    private TextView tabCount;
    private LinearLayout topBar;
    private String upgradeFeature;
    private boolean upgradeOpen;
    private VoiceEngine voice;
    // Slim bar + pull-down menu + auto-hide + teal fade
    private ImageButton menuButton;
    private TextView pullTab;
    private boolean barHidden;
    private boolean pullMenuOpen;
    private long ignoreScrollUntil;
    private boolean tealFadeOn;
    // Same three shades and 12-second loop as the start page (assets/home.html).
    private static final int[] TEAL_SHADES = {0xFF8FD9E1, 0xFF86CBEC, 0xFF96E6C8};
    private static final long TEAL_LOOP_MS = 12000L;
    private final Runnable tealTick = new Runnable() {
        @Override
        public void run() {
            if (!MainActivity.this.tealFadeOn || Glass.midnight || MainActivity.this.root == null) {
                return;
            }
            MainActivity.this.root.setBackgroundColor(tealColorNow());
            MainActivity.this.handler.postDelayed(this, 50L);
        }
    };
    private FrameLayout webHolder;
    private static final AtomicInteger blockedSinceSave = new AtomicInteger();
    private static final int[] TEXT_SIZES = {80, 90, 100, 115, 130, 150, 175, 200};
    private final List<Tab> tabs = new ArrayList();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hidePill = new Runnable() {
        @Override
        public final void run() {
            MainActivity.this.lambda$new$0();
        }
    };

    static final class Tab {
        volatile int blocked;
        boolean desktop;
        volatile String host;
        boolean isPrivate;
        String title;
        String url;
        WebView web;

        private Tab() {
            this.url = "https://appassets.androidplatform.net/assets/home.html";
            this.title = "New tab";
        }
    }

    static final class Row {
        final boolean fullText;
        final boolean highlight;
        final Runnable onRemove;
        final Runnable onTap;
        final String subtitle;
        final String title;

        Row(String title, String subtitle, Runnable onTap, Runnable onRemove, boolean highlight, boolean fullText) {
            this.title = title;
            this.subtitle = subtitle;
            this.onTap = onTap;
            this.onRemove = onRemove;
            this.highlight = highlight;
            this.fullText = fullText;
        }
    }

    public void lambda$new$0() {
        this.statusPill.setVisibility(8);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.prefs = getSharedPreferences("seaglass", 0);
        this.library = new Library(this);
        this.assets = WebSupport.assetLoader(this);
        this.voice = new VoiceEngine(this, this);
        this.voice.setMuted(this.prefs.getBoolean("muted", false));
        this.billing = Billing.get(this);
        this.billing.addListener(this);
        this.downloads = new Downloads(this);
        AdBlocker.load(this);
        updateAdBlock();
        Glass.load(this);
        drawBehindSystemBars();
        buildUi();
        updateSystemBars();
        updateMuteButton();
        wipePrivateData();
        List<String> saved = this.library.savedTabUrls();
        Iterator<String> it = saved.iterator();
        while (true) {
            String str = null;
            if (!it.hasNext()) {
                break;
            }
            String url = it.next();
            if (WebSupport.isWeb(url)) {
                str = url;
            }
            createTab(false, str, false);
        }
        if (this.tabs.isEmpty()) {
            createTab(false, null, false);
        }
        switchTo(this.tabs.get(Math.max(0, Math.min(this.library.savedCurrentTab(), this.tabs.size() - 1))));
        handleLaunch(getIntent());
        if (savedInstanceState != null || !isLauncherOpen(getIntent())) {
            return;
        }
        welcome();
    }

    private static boolean isLauncherOpen(Intent intent) {
        return intent != null && "android.intent.action.MAIN".equals(intent.getAction()) && intent.hasCategory("android.intent.category.LAUNCHER");
    }

    private void welcome() {
        int hour = Calendar.getInstance().get(11);
        String part = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        showStatus("Welcome to RAEE SeaGlass");
        this.voice.say(part + ". Welcome to Ray Sea Glass. Tap the mic and tell me where to go.");
    }

    private int dp(float v) {
        return Glass.dp(this, v);
    }

    private void drawBehindSystemBars() {
        Window w = getWindow();
        w.setStatusBarColor(0);
        w.setNavigationBarColor(0);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(10000);
        }
    }

    private void buildUi() {
        this.tealFadeOn = false;
        this.handler.removeCallbacks(this.tealTick);
        this.root = new FrameLayout(this);
        this.root.setBackgroundColor(Glass.TEAL_BG);
        this.webHolder = new FrameLayout(this);
        this.swipe = new SwipeRefreshLayout(this);
        this.swipe.setColorSchemeColors(Glass.ACCENT);
        this.swipe.setProgressBackgroundColorSchemeColor(Glass.SWIPE_BG);
        this.swipe.addView(this.webHolder, new ViewGroup.LayoutParams(-1, -1));
        this.swipe.setOnChildScrollUpCallback(new SwipeRefreshLayout.OnChildScrollUpCallback() {
            @Override
            public final boolean canChildScrollUp(SwipeRefreshLayout swipeRefreshLayout, View view) {
                boolean lambda$buildUi$1;
                lambda$buildUi$1 = MainActivity.this.lambda$buildUi$1(swipeRefreshLayout, view);
                return lambda$buildUi$1;
            }
        });
        this.swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public final void onRefresh() {
                MainActivity.this.lambda$buildUi$3();
            }
        });
        this.root.addView(this.swipe, new FrameLayout.LayoutParams(-1, -1));
        this.topBar = new LinearLayout(this);
        this.topBar.setOrientation(1);
        this.topBar.setBackground(Glass.clearGlass(this, Glass.GLASS, 22.0f));
        this.topBar.setElevation(dp(3.0f));
        this.topBar.setPadding(dp(4.0f), dp(4.0f), dp(4.0f), dp(2.0f));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(0);
        row.setGravity(16);
        this.address = new EditText(this);
        this.address.setSingleLine(true);
        this.address.setHintTextColor(Glass.INK_SOFT);
        this.address.setTextColor(Glass.INK);
        this.address.setTextSize(15.0f);
        this.address.setInputType(17);
        this.address.setImeOptions(2);
        this.address.setSelectAllOnFocus(true);
        this.address.setBackground(Glass.panel(this, Glass.GLASS_FIELD, 16.0f));
        this.address.setPadding(dp(14.0f), 0, dp(10.0f), 0);
        this.address.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                boolean lambda$buildUi$5;
                lambda$buildUi$5 = MainActivity.this.lambda$buildUi$5(textView, i, keyEvent);
                return lambda$buildUi$5;
            }
        });
        LinearLayout.LayoutParams addressLp = new LinearLayout.LayoutParams(0, dp(44.0f), 1.0f);
        addressLp.leftMargin = dp(4.0f);
        addressLp.rightMargin = dp(2.0f);
        row.addView(this.address, addressLp);
        // Mic now lives in the top bar (it used to be in the bottom dock).
        this.micButton = new ImageButton(this);
        this.micButton.setImageResource(R.drawable.ic_mic);
        this.micButton.setColorFilter(-1, PorterDuff.Mode.SRC_IN);
        this.micButton.setBackground(Glass.oval(this, Glass.ACCENT, 2, Glass.GLASS_STRONG));
        this.micButton.setPadding(dp(11.0f), dp(11.0f), dp(11.0f), dp(11.0f));
        this.micButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        this.micButton.setContentDescription("Tap and talk");
        this.micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onMicTapped();
            }
        });
        Glass.glow(this.micButton);
        row.addView(this.micButton, spaced(44, 3));
        this.menuButton = Glass.iconButton(this, R.drawable.ic_menu, "Menu", 44, 0, Glass.INK);
        this.menuButton.setBackground(Glass.oval(this, Glass.GLASS_STRONG, 1, Glass.EDGE));
        this.menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePullMenu();
            }
        });
        row.addView(this.menuButton, spaced(44, 3));
        // Star and tab count moved into the pull-down menu; kept as hidden views
        // so the existing update code keeps working.
        this.starButton = Glass.iconButton(this, R.drawable.ic_star, "Bookmark this page", 42, 0, Glass.INK);
        this.tabCount = new TextView(this);
        this.topBar.addView(row);
        buildFindBar();
        this.progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        this.progress.setMax(100);
        this.progress.setProgressTintList(ColorStateList.valueOf(Glass.ACCENT));
        this.progress.setProgressBackgroundTintList(ColorStateList.valueOf(0));
        this.progress.setVisibility(4);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(-1, dp(3.0f));
        progressLp.topMargin = dp(3.0f);
        progressLp.leftMargin = dp(10.0f);
        progressLp.rightMargin = dp(10.0f);
        this.topBar.addView(this.progress, progressLp);
        Glass.glow(this.topBar);
        this.root.addView(this.topBar, new FrameLayout.LayoutParams(-1, -2, REQ_WEATHER));
        this.statusPill = new TextView(this);
        this.statusPill.setBackground(Glass.panel(this, Glass.PILL, 18.0f));
        this.statusPill.setElevation(dp(2.0f));
        this.statusPill.setTextColor(Glass.INK);
        this.statusPill.setTextSize(14.0f);
        this.statusPill.setTypeface(Typeface.DEFAULT_BOLD);
        this.statusPill.setGravity(17);
        this.statusPill.setMaxLines(3);
        this.statusPill.setPadding(dp(16.0f), dp(9.0f), dp(16.0f), dp(9.0f));
        this.statusPill.setVisibility(8);
        this.root.addView(this.statusPill, new FrameLayout.LayoutParams(-2, -2, 81));
        // The bottom dock is gone: its buttons are in the pull-down menu now.
        this.dock = new LinearLayout(this);
        this.dock.setVisibility(8);
        this.muteButton = Glass.iconButton(this, R.drawable.ic_volume, "Mute spoken replies", 44, 0, Glass.INK);
        Glass.glow(this.statusPill);
        buildPullTab();
        setContentView(this.root);
        this.root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public final WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                WindowInsets lambda$buildUi$14;
                lambda$buildUi$14 = MainActivity.this.lambda$buildUi$14(view, windowInsets);
                return lambda$buildUi$14;
            }
        });
        this.topBar.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                MainActivity.this.lambda$buildUi$15(view, i, i2, i3, i4, i5, i6, i7, i8);
            }
        });
        this.root.requestApplyInsets();
        this.barHidden = false;
        startTealFade();
    }

    public boolean lambda$buildUi$1(SwipeRefreshLayout parent, View child) {
        return this.current == null || this.readerView != null || WebSupport.isAppPage(this.current.url) || this.current.web.getScrollY() > 0 || this.current.web.canScrollVertically(-1);
    }

    public void lambda$buildUi$3() {
        if (this.current != null) {
            this.current.web.reload();
        }
        this.handler.postDelayed(new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$buildUi$2();
            }
        }, 8000L);
    }

    public void lambda$buildUi$2() {
        this.swipe.setRefreshing(false);
    }

    public void lambda$buildUi$4(View v) {
        goBack();
    }

    public boolean lambda$buildUi$5(TextView v, int actionId, KeyEvent event) {
        boolean enter = event != null && event.getKeyCode() == 66 && event.getAction() == 0;
        if (actionId != 2 && !enter) {
            return false;
        }
        submitTyped();
        return true;
    }

    public void lambda$buildUi$6(View v) {
        bookmarkPage();
    }

    public void lambda$buildUi$7(View v) {
        showTabs();
    }

    public void lambda$buildUi$8(View v) {
        showMenu();
    }

    public void lambda$buildUi$9(View v) {
        goHome();
    }

    public void lambda$buildUi$10(View v) {
        readPage();
    }

    public void lambda$buildUi$11(View v) {
        startFloating();
    }

    public void lambda$buildUi$12(View v) {
        toggleMute();
    }

    public void lambda$buildUi$13(View v) {
        onMicTapped();
    }

    public WindowInsets lambda$buildUi$14(View v, WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 30) {
            Insets s = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            this.insetTop = s.top;
            this.insetBottom = s.bottom;
            this.insetLeft = s.left;
            this.insetRight = s.right;
        } else {
            this.insetTop = insets.getSystemWindowInsetTop();
            this.insetBottom = insets.getSystemWindowInsetBottom();
            this.insetLeft = insets.getSystemWindowInsetLeft();
            this.insetRight = insets.getSystemWindowInsetRight();
        }
        placeGlass();
        return insets;
    }

    public void lambda$buildUi$15(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
        placeContent();
    }

    public void lambda$buildUi$16(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
        placeContent();
    }

    private LinearLayout.LayoutParams spaced(int sizeDp, int marginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        lp.leftMargin = dp(marginDp);
        lp.rightMargin = dp(marginDp);
        return lp;
    }

    private void placeGlass() {
        FrameLayout.LayoutParams top = (FrameLayout.LayoutParams) this.topBar.getLayoutParams();
        top.setMargins(this.insetLeft + dp(8.0f), this.insetTop + dp(6.0f), this.insetRight + dp(8.0f), 0);
        this.topBar.setLayoutParams(top);
        if (this.pullTab != null) {
            FrameLayout.LayoutParams tab = (FrameLayout.LayoutParams) this.pullTab.getLayoutParams();
            tab.topMargin = this.insetTop + dp(4.0f);
            this.pullTab.setLayoutParams(tab);
        }
    }

    /** Where page content starts: under the bar, or right under the status bar when the bar is hidden. */
    private int contentTop() {
        return this.barHidden ? this.insetTop : this.topBar.getBottom() + dp(6.0f);
    }

    private void placeContent() {
        if (this.customView != null) {
            return;
        }
        final FrameLayout.LayoutParams w = (FrameLayout.LayoutParams) this.swipe.getLayoutParams();
        int wantTop = contentTop();
        if (w.topMargin != wantTop || w.leftMargin != this.insetLeft || w.rightMargin != this.insetRight) {
            final int shift = w.topMargin == 0 ? 0 : wantTop - w.topMargin;
            w.setMargins(this.insetLeft, wantTop, this.insetRight, 0);
            this.swipe.post(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.swipe.setLayoutParams(w);
                    // Keep the page visually still while the bar slides away or back.
                    if (MainActivity.this.current != null && shift != 0) {
                        MainActivity.this.ignoreScrollUntil = SystemClock.uptimeMillis() + 350;
                        MainActivity.this.current.web.scrollBy(0, -shift);
                    }
                }
            });
        }
        placeOverlay(this.readerView);
        placeOverlay(this.lockCover);
        final FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) this.statusPill.getLayoutParams();
        int wantBottom = this.insetBottom + dp(18.0f);
        if (p.bottomMargin != wantBottom) {
            p.bottomMargin = wantBottom;
            p.leftMargin = dp(24.0f);
            p.rightMargin = dp(24.0f);
            this.statusPill.post(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.statusPill.setLayoutParams(p);
                }
            });
        }
    }

    public void lambda$placeContent$17(FrameLayout.LayoutParams w) {
        this.swipe.setLayoutParams(w);
    }

    public void lambda$placeContent$18(FrameLayout.LayoutParams p) {
        this.statusPill.setLayoutParams(p);
    }

    private static boolean privateProfileSupported() {
        return WebViewFeature.isFeatureSupported("MULTI_PROFILE");
    }

    private Tab createTab(boolean isPrivate, String url, boolean show) {
        Tab tab = buildTab(isPrivate);
        this.tabs.add(tab);
        tab.web.loadUrl(url != null ? url : "https://appassets.androidplatform.net/assets/home.html");
        if (show) {
            switchTo(tab);
        } else {
            refreshChrome();
        }
        return tab;
    }

    public Tab buildTab(boolean isPrivate) {
        final Tab tab = new Tab();
        tab.isPrivate = isPrivate;
        WebView w = new WebView(this);
        if (isPrivate && privateProfileSupported()) {
            try {
                ProfileStore.getInstance().getOrCreateProfile(PRIVATE_PROFILE);
                WebViewCompat.setProfile(w, PRIVATE_PROFILE);
            } catch (RuntimeException e) {
            }
        }
        WebSupport.configure(w);
        WebSettings s = w.getSettings();
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setGeolocationEnabled(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, !isPrivate);
        if (isPrivate) {
            s.setCacheMode(2);
        }
        s.setTextZoom(this.prefs.getInt("textZoom", 100));
        w.setBackgroundColor(Glass.TEAL_BG);
        w.setWebViewClient(new BrowserClient(tab));
        w.setWebChromeClient(new ChromeClient(tab));
        w.addJavascriptInterface(new HomeBridge(tab), "SeaGlass");
        w.setDownloadListener(new DownloadListener() {
            @Override
            public final void onDownloadStart(String str, String str2, String str3, String str4, long j) {
                MainActivity.this.lambda$buildTab$19(tab, str, str2, str3, str4, j);
            }
        });
        w.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public final boolean onLongClick(View view) {
                boolean lambda$buildTab$20;
                lambda$buildTab$20 = MainActivity.this.lambda$buildTab$20(tab, view);
                return lambda$buildTab$20;
            }
        });
        w.setFindListener(new WebView.FindListener() {
            @Override
            public final void onFindResultReceived(int i, int i2, boolean z) {
                MainActivity.this.lambda$buildTab$21(tab, i, i2, z);
            }
        });
        w.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int x, int y, int oldX, int oldY) {
                onPageScrolled(tab, y, y - oldY);
            }
        });
        tab.web = w;
        return tab;
    }

    public void lambda$buildTab$19(Tab tab, String u, String ua, String disposition, String mime, long length) {
        startDownload(tab, u, ua, disposition, mime);
    }

    public boolean lambda$buildTab$20(Tab tab, View v) {
        return onLongPress(tab);
    }

    public void lambda$buildTab$21(Tab tab, int active, int total, boolean done) {
        if (tab != this.current || this.findBar.getVisibility() != 0) {
            return;
        }
        if (this.findField.getText().length() != 0) {
            this.findCount.setText(total == 0 ? "0/0" : (active + 1) + "/" + total);
        } else {
            this.findCount.setText("");
        }
    }

    public void switchTo(Tab tab) {
        if (tab == null) {
            return;
        }
        if (this.current != tab) {
            exitFullscreen();
            closeFind();
            closeReader();
            if (this.current != null) {
                this.webHolder.removeView(this.current.web);
                this.current.web.onPause();
            }
            this.current = tab;
            this.webHolder.addView(tab.web, new FrameLayout.LayoutParams(-1, -1));
            tab.web.onResume();
            this.voice.stopSpeaking();
        }
        refreshChrome();
        checkLock();
    }

    public void closeTabNow(Tab tab) {
        int i = this.tabs.indexOf(tab);
        if (i < 0) {
            return;
        }
        this.tabs.remove(i);
        if (this.current == tab) {
            exitFullscreen();
            this.webHolder.removeView(tab.web);
            this.current = null;
        }
        tab.web.destroy();
        if (this.tabs.isEmpty()) {
            createTab(false, null, true);
        } else if (this.current == null) {
            switchTo(this.tabs.get(Math.min(i, this.tabs.size() - 1)));
        }
        if (tab.isPrivate && !hasPrivateTabs()) {
            this.handler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.wipePrivateData();
                }
            }, 500L);
        }
        refreshChrome();
        saveTabs();
    }

    private boolean hasPrivateTabs() {
        for (Tab t : this.tabs) {
            if (t.isPrivate) {
                return true;
            }
        }
        return false;
    }

    public void wipePrivateData() {
        if (hasPrivateTabs() || !privateProfileSupported()) {
            return;
        }
        try {
            ProfileStore store = ProfileStore.getInstance();
            if (store.getProfile(PRIVATE_PROFILE) != null) {
                store.deleteProfile(PRIVATE_PROFILE);
            }
        } catch (RuntimeException e) {
        }
    }

    private void saveTabs() {
        List<String> urls = new ArrayList<>();
        int currentIndex = 0;
        for (Tab t : this.tabs) {
            if (!t.isPrivate) {
                if (t == this.current) {
                    currentIndex = urls.size();
                }
                urls.add(WebSupport.isAppPage(t.url) ? "" : t.url);
            }
        }
        this.library.saveTabs(urls, currentIndex);
    }

    private void refreshChrome() {
        if (this.current == null) {
            return;
        }
        this.topBar.setBackground(Glass.clearGlass(this, this.current.isPrivate ? Glass.PRIVATE : Glass.GLASS, 22.0f));
        this.address.setHint(this.current.isPrivate ? "Private tab: search or type a website" : "Search, or say “open YouTube”");
        showAddress(this.current.url);
        int n = this.tabs.size();
        this.tabCount.setText(n > 99 ? ":)" : String.valueOf(n));
        this.tabCount.setContentDescription(n + (n == 1 ? " tab open" : " tabs open") + ". Show tabs");
        updateStar();
        this.progress.setVisibility(4);
        showBar();
    }

    public void showAddress(String url) {
        if (this.address.hasFocus()) {
            return;
        }
        boolean hide = WebSupport.isAppPage(url) || (this.current != null && this.current.isPrivate && this.privateLocked);
        this.address.setText(hide ? "" : url);
    }

    public void updateStar() {
        boolean saved = this.current != null && this.library.isBookmarked(this.current.url);
        this.starButton.setImageResource(saved ? R.drawable.ic_star_filled : R.drawable.ic_star);
        this.starButton.setColorFilter(Glass.INK, PorterDuff.Mode.SRC_IN);
        this.starButton.setContentDescription(saved ? "Remove bookmark" : "Bookmark this page");
        boolean canSave = (this.current == null || WebSupport.isAppPage(this.current.url)) ? false : true;
        this.starButton.setAlpha(canSave ? 1.0f : 0.35f);
    }

    private String label(Tab t) {
        return (t.isPrivate && this.privateLocked) ? "Private tab (locked)" : WebSupport.isAppPage(t.url) ? t.isPrivate ? "New private tab" : "New tab" : TextUtils.isEmpty(t.title) ? t.url : t.title;
    }

    private void showSheet(String title, List<Row> rows, String emptyText, String[] actions, Runnable[] actionRuns) {
        closeSheet();
        this.sheet = new FrameLayout(this);
        this.sheet.setBackgroundColor(Glass.SCRIM);
        this.sheet.setClickable(true);
        this.sheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.lambda$showSheet$22(view);
            }
        });
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(Glass.panel(this, Glass.SHEET, 26.0f));
        linearLayout.setPadding(dp(16.0f), dp(12.0f), dp(16.0f), dp(16.0f));
        linearLayout.setClickable(true);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(16);
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(Glass.INK);
        heading.setTextSize(19.0f);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1.0f));
        ImageButton close = Glass.iconButton(this, R.drawable.ic_close, "Close", 40, Glass.GLASS_STRONG, Glass.INK);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.lambda$showSheet$23(view);
            }
        });
        header.addView(close);
        linearLayout.addView(header);
        ScrollView scroll = new ScrollView(this) {
            @Override
            protected void onMeasure(int widthSpec, int heightSpec) {
                int cap = (int) (MainActivity.this.root.getHeight() * 0.55f);
                if (cap > 0) {
                    heightSpec = View.MeasureSpec.makeMeasureSpec(cap, Integer.MIN_VALUE);
                }
                super.onMeasure(widthSpec, heightSpec);
            }
        };
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(1);
        scroll.addView(list);
        if (rows.isEmpty() && emptyText != null) {
            TextView empty = new TextView(this);
            empty.setText(emptyText);
            empty.setTextColor(Glass.INK_SOFT);
            empty.setTextSize(15.0f);
            empty.setPadding(dp(4.0f), dp(14.0f), dp(4.0f), dp(8.0f));
            list.addView(empty);
        }
        for (Row r : rows) {
            list.addView(rowView(r));
        }
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, -2);
        scrollLp.topMargin = dp(4.0f);
        linearLayout.addView(scroll, scrollLp);
        if (actions != null && actions.length > 0) {
            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(0);
            int i = 0;
            while (i < actions.length) {
                final Runnable run = actionRuns[i];
                TextView b = new TextView(this);
                b.setText(actions[i]);
                b.setGravity(17);
                b.setTextSize(15.0f);
                b.setTypeface(Typeface.DEFAULT_BOLD);
                b.setTextColor(i == 0 ? -1 : Glass.INK);
                ImageButton close2 = close;
                b.setBackground(Glass.panel(this, i == 0 ? Glass.ACCENT : Glass.GLASS_STRONG, 14.0f));
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        run.run();
                    }
                });
                ScrollView scroll2 = scroll;
                LinearLayout list2 = list;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48.0f), 1.0f);
                if (i > 0) {
                    lp.leftMargin = dp(8.0f);
                }
                buttons.addView(b, lp);
                i++;
                list = list2;
                close = close2;
                scroll = scroll2;
            }
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
            blp.topMargin = dp(12.0f);
            linearLayout.addView(buttons, blp);
        }
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(-1, -2, 80);
        cardLp.setMargins(this.insetLeft + dp(10.0f), 0, this.insetRight + dp(10.0f), this.insetBottom + dp(10.0f));
        this.sheet.addView(linearLayout, cardLp);
        this.root.addView(this.sheet, new FrameLayout.LayoutParams(-1, -1));
    }

    public void lambda$showSheet$22(View v) {
        closeSheet();
    }

    public void lambda$showSheet$23(View v) {
        closeSheet();
    }

    private View rowView(final Row r) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setBackground(Glass.panel(this, r.highlight ? Glass.ROW_ON : Glass.ROW, 14.0f));
        linearLayout.setPadding(dp(14.0f), dp(10.0f), dp(6.0f), dp(10.0f));
        linearLayout.setMinimumHeight(dp(52.0f));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(1);
        TextView t = new TextView(this);
        t.setText(r.title);
        t.setTextColor(Glass.INK);
        t.setTextSize(15.0f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        if (!r.fullText) {
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
        }
        texts.addView(t);
        if (r.subtitle != null && !r.subtitle.isEmpty()) {
            TextView s = new TextView(this);
            s.setText(r.subtitle);
            s.setTextColor(r.fullText ? Glass.INK : Glass.INK_SOFT);
            s.setTextSize(r.fullText ? 16.0f : 13.0f);
            if (r.fullText) {
                s.setLineSpacing(0.0f, 1.2f);
                s.setPadding(0, dp(6.0f), 0, 0);
                s.setTextIsSelectable(true);
            } else {
                s.setSingleLine(true);
                s.setEllipsize(TextUtils.TruncateAt.END);
            }
            texts.addView(s);
        }
        linearLayout.addView(texts, new LinearLayout.LayoutParams(0, -2, 1.0f));
        if (r.onRemove != null) {
            ImageButton remove = Glass.iconButton(this, R.drawable.ic_close, "Remove " + r.title, 44, 0, Glass.INK_SOFT);
            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    r.onRemove.run();
                }
            });
            linearLayout.addView(remove);
        }
        if (r.onTap != null) {
            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    MainActivity.this.lambda$rowView$26(r, view);
                }
            });
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8.0f);
        linearLayout.setLayoutParams(lp);
        return linearLayout;
    }

    public void lambda$rowView$26(Row r, View v) {
        closeSheet();
        r.onTap.run();
    }

    private void closeSheet() {
        this.upgradeOpen = false;
        if (this.pullMenuOpen) {
            this.pullMenuOpen = false;
            if (Build.VERSION.SDK_INT >= 31) {
                this.swipe.setRenderEffect(null);
            }
            this.menuButton.setImageResource(R.drawable.ic_menu);
            this.menuButton.setColorFilter(Glass.INK, PorterDuff.Mode.SRC_IN);
            this.menuButton.setContentDescription("Menu");
        }
        if (this.sheet != null) {
            this.root.removeView(this.sheet);
            this.sheet = null;
        }
    }

    private void showMenu() {
        String adSub;
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(Billing.PLUS_ENABLED ? "RAEE SeaGlass Plus" : "Your plan", this.billing.planSummary(), new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showMenu$27();
            }
        }, null, true, false));
        rows.add(new Row("New tab", null, new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showMenu$28();
            }
        }, null, false, false));
        rows.add(new Row("New private tab", "Doesn't save history", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showMenu$29();
            }
        }, null, false, false));
        rows.add(new Row("Tabs", this.tabs.size() + " open", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.showTabs();
            }
        }, null, false, false));
        if (this.current != null && !WebSupport.isAppPage(this.current.url)) {
            boolean saved = this.library.isBookmarked(this.current.url);
            rows.add(new Row(saved ? "Remove bookmark" : "Bookmark this page", null, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.bookmarkPage();
                }
            }, null, false, false));
        }
        boolean onWeb = (this.current == null || WebSupport.isAppPage(this.current.url) || !WebSupport.isWeb(this.current.url)) ? false : true;
        if (onWeb) {
            boolean pinned = this.library.isShortcut(this.current.url);
            rows.add(new Row(pinned ? "Remove from home shortcuts" : "Add to home shortcuts", null, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showMenu$30();
                }
            }, null, false, false));
        }
        rows.add(new Row("Bookmarks", null, new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.showBookmarks();
            }
        }, null, false, false));
        rows.add(new Row("History", null, new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.showHistory();
            }
        }, null, false, false));
        rows.add(new Row("Downloads", null, new Runnable() { @Override public void run() { showDownloads(); } }, null, false, false));
        if (onWeb) {
            rows.add(new Row("Find on page", "Or say “find battery on this page”", new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showMenu$31();
                }
            }, null, false, false));
            rows.add(new Row("Share page", null, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.sharePage();
                }
            }, null, false, false));
            rows.add(new Row("Reader mode", "Just the article, easy to read", new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.openReader();
                }
            }, null, false, false));
            rows.add(new Row("Translate page", null, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.showTranslate();
                }
            }, null, false, false));
            rows.add(new Row(this.current.desktop ? "Mobile site" : "Desktop site", this.current.desktop ? "Back to the phone version" : "Load the full computer version", new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showMenu$32();
                }
            }, null, false, false));
        }
        rows.add(new Row("Look: " + (Glass.midnight ? "Midnight Glass" : "Teal Glass"), Glass.midnight ? "Switch to Teal Glass" : "Switch to Midnight Glass", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showMenu$33();
            }
        }, null, false, false));
        rows.add(new Row("Text size", this.prefs.getInt("textZoom", 100) + "%", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.showTextSize();
            }
        }, null, false, false));
        final boolean dark = this.prefs.getBoolean("darkSites", false);
        rows.add(new Row("Dark websites: " + (dark ? "On" : "Off"), "Turns bright pages dark", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showMenu$34(dark);
            }
        }, null, false, false));
        if (!this.billing.canUseFull()) {
            adSub = "Part of the full app · Tap to unlock";
        } else if (!this.adBlockOn) {
            adSub = "Ads and trackers are allowed";
        } else if (this.current != null && this.current.blocked > 0) {
            adSub = this.current.blocked + " ads and trackers blocked on this page";
        } else {
            adSub = "Blocks ads and trackers";
        }
        rows.add(new Row("Ad blocker: " + (this.adBlockOn ? "On" : "Off"), adSub, new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showMenu$35();
            }
        }, null, false, false));
        final boolean locked = this.prefs.getBoolean("lockPrivate", false);
        rows.add(new Row("Lock private tabs: " + (locked ? "On" : "Off"), "Ask for your fingerprint or screen lock", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showMenu$36(locked);
            }
        }, null, false, false));
        rows.add(new Row("Clear browsing data", "History, cookies, cache", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.showClearData();
            }
        }, null, false, false));
        rows.add(new Row("Forward", null, new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.goForward();
            }
        }, null, false, false));
        rows.add(new Row("Reload", null, new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.reload();
            }
        }, null, false, false));
        rows.add(new Row("Read this page aloud", null, new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.readPage();
            }
        }, null, false, false));
        rows.add(new Row("About RAEE SeaGlass", "Made by RAEE Industries · raee-industries.me", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showMenu$37();
            }
        }, null, false, false));
        showSheet("RAEE SeaGlass", rows, null, null, null);
    }

    public void lambda$showMenu$27() {
        showUpgrade(null);
    }

    public void lambda$showMenu$28() {
        newTab(false);
    }

    public void lambda$showMenu$29() {
        newTab(true);
    }

    public void lambda$showMenu$30() {
        tool("shortcut", null);
    }

    public void lambda$showMenu$31() {
        openFind(null);
    }

    public void lambda$showMenu$32() {
        setDesktop(!this.current.desktop);
    }

    public void lambda$showMenu$33() {
        setLook(!Glass.midnight);
    }

    public void lambda$showMenu$34(boolean dark) {
        setDark(!dark);
    }

    public void lambda$showMenu$35() {
        setAdBlock(!this.adBlockOn);
    }

    public void lambda$showMenu$36(boolean locked) {
        setPrivateLock(!locked);
    }

    public void lambda$showMenu$37() {
        openUrl("https://raee-industries.me");
    }

    public void showStatus(String message) {
        showStatus(message, true);
    }

    private void showStatus(String message, boolean autoHide) {
        this.handler.removeCallbacks(this.hidePill);
        if (message == null || message.isEmpty()) {
            this.statusPill.setVisibility(8);
            return;
        }
        this.statusPill.setText(message);
        this.statusPill.setVisibility(this.customView == null ? 0 : 8);
        if (autoHide) {
            this.handler.postDelayed(this.hidePill, 4500L);
        }
    }

    private void tell(String message) {
        showStatus(message);
        if (this.lastFromVoice) {
            this.voice.say(message);
        }
    }

    private void updateMic() {
        int color = this.voice.isListening() ? -1948580 : this.speaking ? -9741338 : Glass.ACCENT;
        ((GradientDrawable) this.micButton.getBackground()).setColor(color);
        this.micButton.setContentDescription(this.voice.isListening() ? "Stop listening" : "Tap and talk");
    }

    private void submitTyped() {
        String text = this.address.getText().toString().trim();
        hideKeyboard();
        this.address.clearFocus();
        if (text.isEmpty()) {
            showAddress(this.current.url);
        } else {
            handleRequest(text, false);
        }
    }

    public void handleRequest(String text, boolean fromVoice) {
        this.lastFromVoice = fromVoice;
        String reply = CommandRouter.run(text, this);
        if (reply != null) {
            showStatus(reply);
            if (fromVoice) {
                this.voice.say(reply);
            }
        }
        this.lastFromVoice = false;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        if (imm != null) {
            imm.hideSoftInputFromWindow(this.address.getWindowToken(), 0);
        }
    }

    public void onMicTapped() {
        if (this.voice.isListening()) {
            this.voice.stopListening();
            return;
        }
        if (!this.billing.canUseFull()) {
            showUpgrade("Voice commands");
        } else if (checkSelfPermission("android.permission.RECORD_AUDIO") != 0) {
            requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 41);
        } else {
            this.voice.listen();
        }
    }

    private void toggleMute() {
        this.voice.setMuted(!this.voice.isMuted());
        this.prefs.edit().putBoolean("muted", this.voice.isMuted()).apply();
        updateMuteButton();
        showStatus(this.voice.isMuted() ? "Spoken replies are off." : "Spoken replies are on.");
    }

    private void updateMuteButton() {
        boolean muted = this.voice.isMuted();
        this.muteButton.setImageResource(muted ? R.drawable.ic_volume_off : R.drawable.ic_volume);
        this.muteButton.setColorFilter(Glass.INK, PorterDuff.Mode.SRC_IN);
        this.muteButton.setContentDescription(muted ? "Turn spoken replies on" : "Mute spoken replies");
    }

    private void startFloating() {
        if (!this.billing.canUseFull()) {
            showUpgrade("The floating bubble");
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            this.pendingOverlay = true;
            showStatus("Turn on “Display over other apps” for RAEE SeaGlass, then come back.", false);
            try {
                startActivity(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName())));
                return;
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION"));
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0 && !this.prefs.getBoolean("askedNotify", false)) {
            this.prefs.edit().putBoolean("askedNotify", true).apply();
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 42);
            return;
        }
        Intent i = new Intent(this, (Class<?>) FloatingService.class);
        if (this.current != null && !this.current.isPrivate && WebSupport.isWeb(this.current.url) && !WebSupport.isAppPage(this.current.url)) {
            i.putExtra("me.raeeindustries.seaglass.URL", this.current.url);
        }
        startForegroundService(i);
        this.voice.stopSpeaking();
        moveTaskToBack(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        boolean granted = results.length > 0 && results[0] == 0;
        if (requestCode == 41) {
            if (!granted) {
                showStatus("The microphone is off. Allow it in Settings to talk to RAEE SeaGlass, or type instead.");
                return;
            } else {
                this.voice.listen();
                return;
            }
        }
        if (requestCode == 42) {
            startFloating();
            return;
        }
        if (requestCode == 47) {
            String[] d = this.pendingDownload;
            this.pendingDownload = null;
            if (!granted || d == null) {
                if (d != null) {
                    showStatus("RAEE SeaGlass needs storage access to save downloads on this phone.");
                    return;
                }
                return;
            }
            startDownloadNow(d[0], d[1], d[2], d[3], d[4], "1".equals(d[5]));
            return;
        }
        if (requestCode == REQ_WEATHER) {
            boolean any = false;
            int length = results.length;
            for (int i = 0; i < length; i++) {
                int r = results[i];
                any |= r == 0;
            }
            if (any) {
                Weather.setEnabled(this, true);
                pushWeather();
                Weather.refresh(this, new Runnable() { @Override public void run() { pushWeather(); } });
                return;
            }
            showStatus("Weather needs your location. You can allow it later in Settings.");
            return;
        }
        if (requestCode == 44) {
            finishSitePermission();
            return;
        }
        if (requestCode == 45) {
            boolean any2 = false;
            int length2 = results.length;
            for (int i2 = 0; i2 < length2; i2++) {
                int r2 = results[i2];
                any2 |= r2 == 0;
            }
            GeolocationPermissions.Callback cb = this.locationCallback;
            String origin = this.locationOrigin;
            this.locationCallback = null;
            this.locationOrigin = null;
            if (cb != null) {
                cb.invoke(origin, any2, false);
            }
            if (!any2) {
                showStatus("Location is off for RAEE SeaGlass. You can turn it on in Settings.");
            }
        }
    }

    @Override
    public void openUrl(String url) {
        this.current.web.loadUrl(url);
    }

    @Override
    public void goBack() {
        if (!this.current.web.canGoBack()) {
            showStatus("There's no earlier page.");
        } else {
            this.current.web.goBack();
        }
    }

    @Override
    public void goForward() {
        if (!this.current.web.canGoForward()) {
            showStatus("There's no next page.");
        } else {
            this.current.web.goForward();
        }
    }

    @Override
    public void reload() {
        this.current.web.reload();
    }

    @Override
    public void goHome() {
        this.current.web.loadUrl("https://appassets.androidplatform.net/assets/home.html");
    }

    @Override
    public void scroll(boolean down) {
        this.current.web.evaluateJavascript(WebSupport.scrollJs(down), null);
    }

    @Override
    public void stopSpeaking() {
        this.voice.stopSpeaking();
    }

    @Override
    public void readPage() {
        if (!this.billing.canUseFull()) {
            showUpgrade("Reading pages aloud");
        } else if (WebSupport.isAppPage(this.current.url)) {
            showStatus("Open a website first, then ask me to read the page.");
            this.voice.say("Open a website first, then ask me to read the page.");
        } else {
            this.current.web.evaluateJavascript("(function(){var a=document.querySelector('article')||document.querySelector('main')||document.body;var t=a?a.innerText:'';return (document.title||'')+'. '+t.replace(/\\s+/g,' ').slice(0,12000);})()", new ValueCallback() {
                @Override
                public final void onReceiveValue(Object obj) {
                    MainActivity.this.lambda$readPage$38((String) obj);
                }
            });
        }
    }

    public void lambda$readPage$38(String value) {
        String text = WebSupport.decodeJsString(value);
        if (text.trim().length() < 3) {
            showStatus("I couldn't find text to read on this page.");
        } else {
            showStatus("Reading this page. Say “stop” or tap the mic to stop.");
            this.voice.read(text);
        }
    }

    @Override
    public void newTab(boolean isPrivate) {
        String str;
        if (isPrivate && !this.billing.canUseFull()) {
            showUpgrade("Private tabs");
            return;
        }
        createTab(isPrivate, null, true);
        saveTabs();
        if (isPrivate) {
            if (privateProfileSupported()) {
                str = "Private tab: no history is saved, and its cookies are erased when you close it.";
            } else {
                str = "Private tab: no history is saved. On this phone, sites may still remember your sign-ins.";
            }
            showStatus(str);
        }
    }

    @Override
    public void closeTab() {
        closeTabNow(this.current);
    }

    @Override
    public void nextTab() {
        if (this.tabs.size() < 2) {
            tell("This is your only tab.");
        } else {
            switchTo(this.tabs.get((this.tabs.indexOf(this.current) + 1) % this.tabs.size()));
            tell("Tab " + (this.tabs.indexOf(this.current) + 1) + " of " + this.tabs.size() + ": " + label(this.current) + ".");
        }
    }

    @Override
    public void showTabs() {
        String sub;
        List<Row> rows = new ArrayList<>();
        Iterator it = new ArrayList(this.tabs).iterator();
        while (it.hasNext()) {
            final Tab t = (Tab) it.next();
            if (t == this.current) {
                sub = t.isPrivate ? "Private · Open now" : "Open now";
            } else if (t.isPrivate) {
                sub = "Private";
            } else {
                String sub2 = t.url;
                sub = WebSupport.isAppPage(sub2) ? "Start page" : t.url;
            }
            rows.add(new Row(label(t), sub, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.switchTo(t);
                }
            }, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showTabs$40(t);
                }
            }, t == this.current, false));
        }
        showSheet("Tabs (" + this.tabs.size() + ")", rows, null, new String[]{"New tab", "Private tab"}, new Runnable[]{new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showTabs$41();
            }
        }, new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showTabs$42();
            }
        }});
    }

    public void lambda$showTabs$40(Tab t) {
        closeTabNow(t);
        showTabs();
    }

    public void lambda$showTabs$41() {
        closeSheet();
        newTab(false);
    }

    public void lambda$showTabs$42() {
        closeSheet();
        newTab(true);
    }

    @Override
    public void bookmarkPage() {
        if (this.current == null || WebSupport.isAppPage(this.current.url) || !WebSupport.isWeb(this.current.url)) {
            tell("Open a website first, then bookmark it.");
            return;
        }
        boolean added = this.library.toggleBookmark(this.current.web.getTitle(), this.current.url);
        updateStar();
        tell(added ? "Bookmarked." : "Bookmark removed.");
    }

    @Override
    public void showBookmarks() {
        List<Row> rows = new ArrayList<>();
        for (final Library.Entry e : this.library.bookmarks()) {
            rows.add(new Row(e.title, e.url, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showBookmarks$43(e);
                }
            }, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showBookmarks$44(e);
                }
            }, false, false));
        }
        showSheet("Bookmarks", rows, "No bookmarks yet. Tap the star on any page, or say “bookmark this page.”", null, null);
    }

    public void lambda$showBookmarks$43(Library.Entry e) {
        openUrl(e.url);
    }

    public void lambda$showBookmarks$44(Library.Entry e) {
        this.library.removeBookmark(e.url);
        updateStar();
        showBookmarks();
    }

    @Override
    public void showHistory() {
        List<Row> rows = new ArrayList<>();
        for (final Library.Entry e : this.library.history()) {
            String when = DateUtils.getRelativeTimeSpanString(e.time, System.currentTimeMillis(), 60000L).toString();
            rows.add(new Row(e.title, when + " · " + e.url, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showHistory$45(e);
                }
            }, null, false, false));
        }
        showSheet("History", rows, "No history yet. Private tabs never add to it.", new String[]{"Clear history"}, new Runnable[]{new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showHistory$46();
            }
        }});
    }

    public void lambda$showHistory$45(Library.Entry e) {
        openUrl(e.url);
    }

    public void lambda$showHistory$46() {
        closeSheet();
        clearHistory();
        showStatus("History cleared.");
    }

    @Override
    public void clearHistory() {
        this.library.clearHistory();
    }

    @Override
    public void ask(final String question) {
        if (!AiClient.isReady() || !Billing.PLUS_ENABLED) {
            openUrl(CommandRouter.searchUrl(question));
            tell("Here are results for " + question + ".");
        } else if (!this.billing.hasPlus()) {
            openUrl(CommandRouter.searchUrl(question));
            showUpgrade("Spoken AI answers");
        } else {
            showStatus("Thinking…", false);
            final boolean speak = this.lastFromVoice;
            AiClient.ask(question, this.billing.plusToken(), new AiClient.Callback() {
                @Override
                public void onAnswer(String answer) {
                    MainActivity.this.showStatus(null);
                    MainActivity.this.showAnswer(question, answer);
                    if (speak) {
                        MainActivity.this.voice.say(answer);
                    }
                }

                @Override
                public void onError(String message) {
                    MainActivity.this.openUrl(CommandRouter.searchUrl(question));
                    MainActivity.this.showStatus(message);
                }
            });
        }
    }

    public void showAnswer(final String question, final String answer) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(question, answer, null, null, false, true));
        showSheet("Answer", rows, null, new String[]{"Search the web", "Read again"}, new Runnable[]{new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showAnswer$47(question);
            }
        }, new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showAnswer$48(answer);
            }
        }});
    }

    public void lambda$showAnswer$47(String question) {
        closeSheet();
        openUrl(CommandRouter.searchUrl(question));
    }

    public void lambda$showAnswer$48(String answer) {
        this.voice.read(answer);
    }

    private void showUpgrade(String feature) {
        String intro;
        String unlockPrice = this.billing.unlockPrice();
        String plusPrice = this.billing.plusPrice();
        List<Row> rows = new ArrayList<>();
        if (feature == null) {
            intro = this.billing.planSummary() + ".";
        } else {
            intro = feature.equals("Spoken AI answers") ? "Spoken AI answers come with SeaGlass Plus. Here are web results for now." : "Your free trial has ended. Unlock the full app to keep using: " + feature + ".";
        }
        rows.add(new Row(feature == null ? "Your plan" : "Upgrade to keep going", intro + "\nBrowsing, tabs, bookmarks and history are always free.", null, null, false, true));
        if (this.billing.ownsUnlock() || this.billing.hasPlus()) {
            rows.add(new Row("Full app unlocked ✓", "Voice commands, read aloud, ad blocker, reader mode, translate, floating bubble and private tabs.", null, null, false, true));
        } else {
            rows.add(new Row("Unlock RAEE SeaGlass" + (unlockPrice != null ? " · " + unlockPrice + " once" : ""), "Voice commands, read aloud, the ad blocker, reader mode, translate, the floating bubble and private tabs. Pay once, keep it forever.", new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showUpgrade$49();
                }
            }, null, true, true));
        }
        if (!Billing.PLUS_ENABLED && !this.billing.hasPlus()) {
            // Plus is hidden in version 1: only the one-time unlock is offered.
        } else if (this.billing.hasPlus()) {
            rows.add(new Row("SeaGlass Plus is active ✓", "Tap to manage or cancel your subscription in Google Play.", new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showUpgrade$50();
                }
            }, null, false, true));
        } else {
            rows.add(new Row("SeaGlass Plus" + (plusPrice != null ? " · " + plusPrice + " a month" : ""), "Everything in the unlock, plus spoken AI answers to your questions. Cancel anytime in Google Play.", new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showUpgrade$51();
                }
            }, null, true, true));
        }
        rows.add(new Row("Restore purchases", "Bought on this Google account before? Tap to check again.", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showUpgrade$52();
            }
        }, null, false, true));
        showSheet("RAEE SeaGlass", rows, null, null, null);
        this.upgradeOpen = true;
        this.upgradeFeature = feature;
    }

    public void lambda$showUpgrade$49() {
        this.billing.buyUnlock(this);
    }

    public void lambda$showUpgrade$50() {
        WebSupport.openOutside(this, "https://play.google.com/store/account/subscriptions?sku=seaglass_plus&package=" + getPackageName(), null);
    }

    public void lambda$showUpgrade$51() {
        this.billing.buyPlus(this);
    }

    public void lambda$showUpgrade$52() {
        this.billing.restore();
    }

    @Override
    public void onEntitlementsChanged() {
        updateAdBlock();
        if (this.upgradeOpen) {
            showUpgrade(this.upgradeFeature);
        }
    }

    @Override
    public void onBillingMessage(String message) {
        if (this.upgradeOpen && (this.billing.ownsUnlock() || this.billing.hasPlus())) {
            closeSheet();
        }
        showStatus(message);
    }

    @Override
    public void onListeningChanged(boolean listening) {
        updateMic();
        if (listening) {
            showStatus("Listening…", false);
        }
    }

    @Override
    public void onPartial(String text) {
        showStatus(text, false);
    }

    @Override
    public void onHeard(String text) {
        showStatus("“" + text + "”");
        closeSheet();
        handleRequest(text, true);
    }

    @Override
    public void onVoiceError(String message) {
        showStatus(message);
    }

    @Override
    public void onSpeakingChanged(boolean isSpeaking) {
        this.speaking = isSpeaking;
        updateMic();
    }

    class BrowserClient extends WebViewClient {
        private final Tab tab;

        BrowserClient(Tab tab) {
            this.tab = tab;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            WebResourceResponse local = MainActivity.this.assets.shouldInterceptRequest(request.getUrl());
            if (local != null) {
                return local;
            }
            if (MainActivity.this.adBlockOn && !request.isForMainFrame() && AdBlocker.blocks(request.getUrl().getHost(), this.tab.host)) {
                this.tab.blocked++;
                MainActivity.blockedSinceSave.incrementAndGet();
                return AdBlocker.empty();
            }
            return null;
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
            MainActivity.this.applyDark(this.tab);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (WebSupport.isWeb(url)) {
                return false;
            }
            WebSupport.openOutside(MainActivity.this, url, view);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            this.tab.url = url;
            this.tab.host = Uri.parse(url).getHost();
            this.tab.blocked = 0;
            if (this.tab == MainActivity.this.current) {
                MainActivity.this.showAddress(url);
                MainActivity.this.updateStar();
                MainActivity.this.showBar();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            this.tab.url = url;
            String title = view.getTitle();
            if (!TextUtils.isEmpty(title)) {
                this.tab.title = title;
            }
            if (!this.tab.isPrivate && WebSupport.isWeb(url) && !WebSupport.isAppPage(url)) {
                MainActivity.this.library.addHistory(this.tab.title, url);
            }
            MainActivity.this.applyDark(this.tab);
            if (WebSupport.isAppPage(url)) {
                MainActivity mainActivity = MainActivity.this;
                final MainActivity mainActivity2 = MainActivity.this;
                Weather.refreshIfStale(mainActivity, new Runnable() {
                    @Override
                    public final void run() {
                        MainActivity.this.pushWeather();
                    }
                });
            }
            if (this.tab == MainActivity.this.current) {
                MainActivity.this.updateStar();
                MainActivity.this.swipe.setRefreshing(false);
            }
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            this.tab.url = url;
            if (this.tab == MainActivity.this.current) {
                MainActivity.this.showAddress(url);
                MainActivity.this.updateStar();
            }
        }
    }

    class ChromeClient extends WebChromeClient {
        private final Tab tab;

        ChromeClient(Tab tab) {
            this.tab = tab;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (this.tab != MainActivity.this.current) {
                return;
            }
            MainActivity.this.progress.setProgress(newProgress);
            MainActivity.this.progress.setVisibility(newProgress >= 100 ? 4 : 0);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (!TextUtils.isEmpty(title)) {
                this.tab.title = title;
            }
        }

        @Override
        public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
            if (MainActivity.this.customView != null || this.tab != MainActivity.this.current) {
                callback.onCustomViewHidden();
                return;
            }
            MainActivity.this.customView = view;
            MainActivity.this.customCallback = callback;
            view.setBackgroundColor(ViewCompat.MEASURED_STATE_MASK);
            MainActivity.this.root.addView(view, new FrameLayout.LayoutParams(-1, -1));
            MainActivity.this.topBar.setVisibility(8);
            MainActivity.this.pullTab.setVisibility(8);
            MainActivity.this.statusPill.setVisibility(8);
        }

        @Override
        public void onHideCustomView() {
            MainActivity.this.exitFullscreen();
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            if (!isUserGesture) {
                MainActivity.this.showStatus("Blocked a pop-up.");
                return false;
            }
            Tab popup = MainActivity.this.buildTab(this.tab.isPrivate);
            popup.url = "";
            popup.title = "Sign-in window";
            MainActivity.this.tabs.add(MainActivity.this.tabs.indexOf(this.tab) + 1, popup);
            MainActivity.this.switchTo(popup);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popup.web);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            if (!MainActivity.this.tabs.contains(this.tab) || this.tab.web != window) {
                return;
            }
            MainActivity.this.closeTabNow(this.tab);
        }

        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
            if (MainActivity.this.fileCallback != null) {
                MainActivity.this.fileCallback.onReceiveValue(null);
            }
            MainActivity.this.fileCallback = callback;
            try {
                Intent pick = params.createIntent();
                pick.addCategory("android.intent.category.OPENABLE");
                if (params.getMode() == 1) {
                    pick.putExtra("android.intent.extra.ALLOW_MULTIPLE", true);
                }
                MainActivity.this.startActivityForResult(Intent.createChooser(pick, "Choose a file"), 43);
                return true;
            } catch (ActivityNotFoundException e) {
                MainActivity.this.fileCallback = null;
                MainActivity.this.showStatus("No app on this phone can pick files.");
                return false;
            }
        }

        public void lambda$onPermissionRequest$0(PermissionRequest request) {
            MainActivity.this.askSitePermission(this.tab, request);
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public final void run() {
                    MainActivity.ChromeClient.this.lambda$onPermissionRequest$0(request);
                }
            });
        }

        @Override
        public void onPermissionRequestCanceled(PermissionRequest request) {
            if (MainActivity.this.sitePermission == request) {
                MainActivity.this.sitePermission = null;
                MainActivity.this.siteResources = null;
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            MainActivity.this.askLocation(origin, callback);
        }

        @Override
        public void onGeolocationPermissionsHidePrompt() {
            MainActivity.this.locationCallback = null;
            MainActivity.this.locationOrigin = null;
        }
    }

    @Override
    public String tool(String name, String arg) {
        char c;
        switch (name.hashCode()) {
            case -1270433929:
                if (name.equals("cleardata")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -1149708726:
                if (name.equals("adblock")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -934979389:
                if (name.equals("reader")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -342500282:
                if (name.equals("shortcut")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 3075958:
                if (name.equals("dark")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 3143097:
                if (name.equals("find")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 3327647:
                if (name.equals("look")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 3556653:
                if (name.equals("text")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 109400031:
                if (name.equals("share")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1052832078:
                if (name.equals("translate")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 1312704747:
                if (name.equals("downloads")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1557106716:
                if (name.equals("desktop")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                openFind(arg);
                return null;
            case 1:
                showDownloads();
                return null;
            case 2:
                sharePage();
                return null;
            case 3:
                openReader();
                return null;
            case 4:
                if (arg == null) {
                    lambda$showTranslate$75(PageTools.phoneLanguageCode(), PageTools.phoneLanguageName());
                    return null;
                }
                String code = PageTools.languageCode(arg);
                if (code == null) {
                    return "I don't know the language " + arg + " yet. Try another, like Spanish or French.";
                }
                lambda$showTranslate$75(code, arg);
                return null;
            case 5:
                setDesktop("on".equals(arg));
                return null;
            case 6:
                stepTextSize("bigger".equals(arg) ? 1 : "smaller".equals(arg) ? -1 : 0);
                return "Text size " + this.prefs.getInt("textZoom", 100) + " percent.";
            case 7:
                setDark("on".equals(arg));
                return null;
            case '\b':
                setAdBlock("on".equals(arg));
                return null;
            case '\t':
                showClearData();
                return null;
            case '\n':
                setLook("midnight".equals(arg));
                return null;
            case 11:
                if (this.current == null || WebSupport.isAppPage(this.current.url) || !WebSupport.isWeb(this.current.url)) {
                    return "Open a website first, then add it to your home shortcuts.";
                }
                Boolean added = this.library.toggleShortcut(this.current.web.getTitle(), this.current.url);
                return added == null ? "Your home screen is full. Remove a shortcut first: tap Edit under Shortcuts." : added.booleanValue() ? "Added to your home screen shortcuts." : "Removed from your home screen shortcuts.";
            default:
                return null;
        }
    }

    private boolean needsWebPage(String what) {
        if (this.current != null && !WebSupport.isAppPage(this.current.url) && WebSupport.isWeb(this.current.url)) {
            return false;
        }
        tell("Open a website first, then " + what + ".");
        return true;
    }

    private void buildFindBar() {
        this.findBar = new LinearLayout(this);
        this.findBar.setOrientation(0);
        this.findBar.setGravity(16);
        this.findBar.setVisibility(8);
        this.findField = new EditText(this);
        this.findField.setSingleLine(true);
        this.findField.setHint("Find on page");
        this.findField.setHintTextColor(Glass.INK_SOFT);
        this.findField.setTextColor(Glass.INK);
        this.findField.setTextSize(15.0f);
        this.findField.setImeOptions(3);
        this.findField.setBackground(Glass.panel(this, Glass.GLASS_FIELD, 16.0f));
        this.findField.setPadding(dp(14.0f), 0, dp(10.0f), 0);
        this.findField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                if (MainActivity.this.current == null) {
                    return;
                }
                if (e.length() == 0) {
                    MainActivity.this.current.web.clearMatches();
                    MainActivity.this.findCount.setText("");
                } else {
                    MainActivity.this.current.web.findAllAsync(e.toString());
                }
            }
        });
        this.findField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                boolean lambda$buildFindBar$53;
                lambda$buildFindBar$53 = MainActivity.this.lambda$buildFindBar$53(textView, i, keyEvent);
                return lambda$buildFindBar$53;
            }
        });
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, dp(44.0f), 1.0f);
        flp.leftMargin = dp(6.0f);
        this.findBar.addView(this.findField, flp);
        this.findCount = new TextView(this);
        this.findCount.setTextColor(Glass.INK_SOFT);
        this.findCount.setTextSize(13.0f);
        this.findCount.setGravity(17);
        this.findBar.addView(this.findCount, new LinearLayout.LayoutParams(dp(52.0f), -2));
        ImageButton up = Glass.iconButton(this, R.drawable.ic_back, "Previous match", 42, 0, Glass.INK);
        up.setRotation(90.0f);
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.lambda$buildFindBar$54(view);
            }
        });
        this.findBar.addView(up);
        ImageButton down = Glass.iconButton(this, R.drawable.ic_back, "Next match", 42, 0, Glass.INK);
        down.setRotation(-90.0f);
        down.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.lambda$buildFindBar$55(view);
            }
        });
        this.findBar.addView(down);
        ImageButton close = Glass.iconButton(this, R.drawable.ic_close, "Close find", 42, 0, Glass.INK);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.lambda$buildFindBar$56(view);
            }
        });
        this.findBar.addView(close);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4.0f);
        this.topBar.addView(this.findBar, lp);
    }

    public boolean lambda$buildFindBar$53(TextView v, int id, KeyEvent ev) {
        if (this.current != null) {
            this.current.web.findNext(true);
        }
        return true;
    }

    public void lambda$buildFindBar$54(View v) {
        if (this.current != null) {
            this.current.web.findNext(false);
        }
    }

    public void lambda$buildFindBar$55(View v) {
        if (this.current != null) {
            this.current.web.findNext(true);
        }
    }

    public void lambda$buildFindBar$56(View v) {
        closeFind();
    }

    private void openFind(String query) {
        if (needsWebPage("ask me to find words on it")) {
            return;
        }
        closeReader();
        this.findBar.setVisibility(0);
        if (query != null && !query.trim().isEmpty()) {
            this.findField.setText(query.trim());
            this.findField.setSelection(this.findField.getText().length());
            hideKeyboard();
            if (this.lastFromVoice) {
                final String q = query.trim();
                this.handler.postDelayed(new Runnable() {
                    @Override
                    public final void run() {
                        MainActivity.this.lambda$openFind$57(q);
                    }
                }, 900L);
                return;
            }
            return;
        }
        this.findField.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        if (imm != null) {
            imm.showSoftInput(this.findField, 1);
        }
    }

    public void lambda$openFind$57(String q) {
        String c = this.findCount.getText().toString();
        if (c.isEmpty() || c.startsWith("0/")) {
            this.voice.say("I couldn't find " + q + " on this page.");
        } else {
            this.voice.say("Found " + q + " " + c.substring(c.indexOf(47) + 1) + " times.");
        }
    }

    private void closeFind() {
        if (this.findBar == null || this.findBar.getVisibility() != 0) {
            return;
        }
        this.findBar.setVisibility(8);
        this.findField.setText("");
        this.findCount.setText("");
        if (this.current != null) {
            this.current.web.clearMatches();
        }
        InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        if (imm != null) {
            imm.hideSoftInputFromWindow(this.findField.getWindowToken(), 0);
        }
    }

    public void sharePage() {
        if (needsWebPage("ask me to share it")) {
            return;
        }
        shareText(this.current.web.getTitle(), this.current.url);
    }

    private void shareText(String title, String url) {
        Intent send = new Intent("android.intent.action.SEND");
        send.setType("text/plain");
        if (!TextUtils.isEmpty(title)) {
            send.putExtra("android.intent.extra.SUBJECT", title);
        }
        send.putExtra("android.intent.extra.TEXT", url);
        try {
            startActivity(Intent.createChooser(send, "Share with"));
        } catch (ActivityNotFoundException e) {
            showStatus("No app on this phone can share links.");
        }
    }

    private boolean onLongPress(final Tab tab) {
        if (tab != this.current || WebSupport.isAppPage(tab.url)) {
            return false;
        }
        WebView.HitTestResult hit = tab.web.getHitTestResult();
        int type = hit.getType();
        final String extra = hit.getExtra();
        if (extra == null) {
            return false;
        }
        if (type == 7) {
            showLinkMenu(tab, extra, null);
            return true;
        }
        if (type == 5) {
            showLinkMenu(tab, null, extra);
            return true;
        }
        if (type != 8) {
            return false;
        }
        Handler h = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public final boolean handleMessage(Message message) {
                boolean lambda$onLongPress$58;
                lambda$onLongPress$58 = MainActivity.this.lambda$onLongPress$58(tab, extra, message);
                return lambda$onLongPress$58;
            }
        });
        tab.web.requestFocusNodeHref(h.obtainMessage());
        return true;
    }

    public boolean lambda$onLongPress$58(Tab tab, String extra, Message msg) {
        String link = msg.getData().getString("url");
        showLinkMenu(tab, WebSupport.isWeb(link) ? link : null, extra);
        return true;
    }

    private void showLinkMenu(final Tab tab, final String link, final String image) {
        List<Row> rows = new ArrayList<>();
        if (WebSupport.isWeb(link)) {
            rows.add(new Row("Open in new tab", link, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showLinkMenu$59(tab, link);
                }
            }, null, false, false));
            if (!tab.isPrivate) {
                rows.add(new Row("Open in private tab", null, new Runnable() {
                    @Override
                    public final void run() {
                        MainActivity.this.lambda$showLinkMenu$60(link);
                    }
                }, null, false, false));
            }
            rows.add(new Row("Copy link", null, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showLinkMenu$61(link);
                }
            }, null, false, false));
            rows.add(new Row("Share link", null, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showLinkMenu$62(link);
                }
            }, null, false, false));
            rows.add(new Row("Download link", null, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showLinkMenu$63(tab, link);
                }
            }, null, false, false));
        }
        if (WebSupport.isWeb(image)) {
            rows.add(new Row("Open image in new tab", null, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showLinkMenu$64(tab, image);
                }
            }, null, false, false));
            rows.add(new Row("Save image", "Saves to your Downloads folder", new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showLinkMenu$65(tab, image);
                }
            }, null, false, false));
            rows.add(new Row("Share image", null, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showLinkMenu$66(image);
                }
            }, null, false, false));
            rows.add(new Row("Copy image address", null, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showLinkMenu$67(image);
                }
            }, null, false, false));
        }
        if (rows.isEmpty()) {
            return;
        }
        showSheet(WebSupport.isWeb(link) ? "Link" : "Image", rows, null, null, null);
    }

    public void lambda$showLinkMenu$59(Tab tab, String link) {
        createTab(tab.isPrivate, link, true);
        saveTabs();
    }

    public void lambda$showLinkMenu$60(String link) {
        if (this.billing.canUseFull()) {
            createTab(true, link, true);
        } else {
            showUpgrade("Private tabs");
        }
    }

    public void lambda$showLinkMenu$61(String link) {
        copy("Link", link);
    }

    public void lambda$showLinkMenu$62(String link) {
        shareText(null, link);
    }

    public void lambda$showLinkMenu$63(Tab tab, String link) {
        startDownload(tab, link, tab.web.getSettings().getUserAgentString(), null, null);
    }

    public void lambda$showLinkMenu$64(Tab tab, String image) {
        createTab(tab.isPrivate, image, true);
    }

    public void lambda$showLinkMenu$65(Tab tab, String image) {
        startDownload(tab, image, tab.web.getSettings().getUserAgentString(), null, null);
    }

    public void lambda$showLinkMenu$66(String image) {
        shareText(null, image);
    }

    public void lambda$showLinkMenu$67(String image) {
        copy("Image address", image);
    }

    private void copy(String what, String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService("clipboard");
        if (cm == null) {
            return;
        }
        cm.setPrimaryClip(ClipData.newPlainText(what, text));
        if (Build.VERSION.SDK_INT < 33) {
            showStatus(what + " copied.");
        }
    }

    private void startDownload(Tab tab, String url, String userAgent, String disposition, String mime) {
        if (url != null && url.startsWith("blob:")) {
            showStatus("This website makes its file inside the page, so it can't be saved here. Try its own download button again, or open the site in Chrome.");
            return;
        }
        String cookies = null;
        try {
            if (tab.isPrivate && privateProfileSupported()) {
                Profile p = ProfileStore.getInstance().getProfile(PRIVATE_PROFILE);
                cookies = p != null ? p.getCookieManager().getCookie(url) : null;
            } else {
                cookies = CookieManager.getInstance().getCookie(url);
            }
        } catch (RuntimeException e) {
        }
        String keep = tab.isPrivate ? "0" : "1";
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            this.pendingDownload = new String[]{url, userAgent, cookies, disposition, mime, keep};
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 47);
        } else {
            startDownloadNow(url, userAgent, cookies, disposition, mime, !tab.isPrivate);
        }
    }

    private void startDownloadNow(String url, String ua, String cookies, String disposition, String mime, boolean keep) {
        String name = this.downloads.start(url, ua, cookies, disposition, mime, keep);
        if (name != null) {
            showStatus("Downloading " + name + ". Find it in Downloads.");
        }
    }

    public void showDownloads() {
        List<Row> rows = new ArrayList<>();
        for (final Downloads.Item it : this.downloads.items()) {
            String when = DateUtils.getRelativeTimeSpanString(it.time, System.currentTimeMillis(), 60000L).toString();
            rows.add(new Row(it.name, it.status + " · " + when, it.done ? new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showDownloads$68(it);
                }
            } : null, new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.lambda$showDownloads$69(it);
                }
            }, false, false));
        }
        showSheet("Downloads", rows, "Nothing downloaded yet. Files you download are saved to your phone's Downloads folder.", new String[]{"Open Downloads folder", "Refresh"}, new Runnable[]{new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showDownloads$70();
            }
        }, new Runnable() { @Override public void run() { showDownloads(); } }});
    }

    public void lambda$showDownloads$68(Downloads.Item it) {
        this.downloads.open(it);
    }

    public void lambda$showDownloads$69(Downloads.Item it) {
        this.downloads.remove(it.id);
        showDownloads();
    }

    public void lambda$showDownloads$70() {
        closeSheet();
        this.downloads.openFolder();
    }

    public void openReader() {
        if (needsWebPage("ask for reader mode")) {
            return;
        }
        if (!this.billing.canUseFull()) {
            showUpgrade("Reader mode");
        } else {
            closeFind();
            this.current.web.evaluateJavascript("(function(){function bad(e){for(;e&&e!==document.body;e=e.parentElement){var n=e.tagName;if(n=='NAV'||n=='FOOTER'||n=='ASIDE'||n=='FORM'||n=='BUTTON')return true;var r=e.getAttribute('role');if(r=='navigation'||r=='complementary'||r=='contentinfo'||r=='banner')return true;var c=(e.className&&e.className.baseVal===undefined?e.className:'')+' '+(e.id||'');if(/\\b(comment|share|social|related|promo|advert|sidebar|newsletter|cookie|footer|menu)/i.test(c))return true;}return false;}var best=null,top=0,m=new Map();document.querySelectorAll('p').forEach(function(p){var l=(p.innerText||'').trim().length;if(l<40||bad(p))return;var par=p.parentElement;if(!par)return;var s=(m.get(par)||0)+l;m.set(par,s);if(s>top){top=s;best=par;}});var root=document.querySelector('article');if(!root||(root.innerText||'').length<500)root=best||document.querySelector('main')||document.body;if(best&&root!==best&&!root.contains(best))root=best;var out=[],total=0,seen=new Set();root.querySelectorAll('h1,h2,h3,h4,p,li,blockquote,pre').forEach(function(e){if(total>60000||bad(e))return;var n=e.tagName;if((n=='LI'||n=='BLOCKQUOTE')&&e.querySelector('p'))return;for(var a=e.parentElement;a&&a!==root;a=a.parentElement){if(seen.has(a))return;}var t=(e.innerText||'').replace(/\\s+/g,' ').trim();if(t.length<2)return;seen.add(e);total+=t.length;out.push([n.charAt(0)=='H'?'h':n=='LI'?'li':n=='BLOCKQUOTE'?'q':'p',t]);});var h=document.querySelector('h1');return JSON.stringify({t:(h&&h.innerText.trim())||document.title||'',b:out});})()", new ValueCallback() {
                @Override
                public final void onReceiveValue(Object obj) {
                    MainActivity.this.lambda$openReader$71((String) obj);
                }
            });
        }
    }

    public void lambda$openReader$71(String value) {
        PageTools.Article a = PageTools.parseArticle(value);
        if (a.length() < 200) {
            tell("This page doesn't have an article to show in reader mode.");
            return;
        }
        showReader(a);
        if (this.lastFromVoice) {
            this.voice.say("Reader mode. Say “read this page” to hear it.");
        }
    }

    private void showReader(PageTools.Article a) {
        TextView title;
        Iterator<String[]> it;
        String str;
        closeReader();
        boolean dark = this.prefs.getBoolean("darkSites", false) || Glass.midnight;
        int bg = dark ? -233954520 : -168493570;
        int ink = dark ? -1640712 : Glass.INK;
        int soft = dark ? -6306614 : Glass.INK_SOFT;
        float zoom = this.prefs.getInt("textZoom", 100) / 100.0f;
        this.readerView = new FrameLayout(this);
        this.readerView.setBackground(Glass.panel(this, bg, 22.0f));
        this.readerView.setClickable(true);
        this.readerView.setElevation(dp(3.0f));
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(20.0f), dp(14.0f), dp(20.0f), dp(120.0f));
        scrollView.addView(linearLayout);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(16);
        TextView label = new TextView(this);
        label.setText("READER");
        label.setTextColor(soft);
        label.setTextSize(12.0f);
        label.setLetterSpacing(0.12f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(label, new LinearLayout.LayoutParams(0, -2, 1.0f));
        ImageButton speak = Glass.iconButton(this, R.drawable.ic_read, "Read aloud", 40, Glass.GLASS_STRONG, Glass.INK);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.lambda$showReader$72(view);
            }
        });
        header.addView(speak);
        ImageButton close = Glass.iconButton(this, R.drawable.ic_close, "Close reader", 40, Glass.GLASS_STRONG, Glass.INK);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(40.0f), dp(40.0f));
        clp.leftMargin = dp(8.0f);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.lambda$showReader$73(view);
            }
        });
        header.addView(close, clp);
        linearLayout.addView(header);
        TextView title2 = new TextView(this);
        title2.setText(a.title);
        title2.setTextColor(ink);
        title2.setTextSize(25.0f * zoom);
        title2.setTypeface(Typeface.DEFAULT_BOLD);
        title2.setLineSpacing(0.0f, 1.15f);
        title2.setPadding(0, dp(10.0f), 0, dp(6.0f));
        title2.setTextIsSelectable(true);
        linearLayout.addView(title2);
        Iterator<String[]> it2 = a.blocks.iterator();
        while (it2.hasNext()) {
            String[] b = it2.next();
            TextView t = new TextView(this);
            ImageButton close2 = close;
            String kind = b[0];
            LinearLayout.LayoutParams clp2 = clp;
            if (kind.equals("li")) {
                title = title2;
                it = it2;
                str = "•  " + b[1];
            } else {
                title = title2;
                it = it2;
                str = b[1];
            }
            t.setText(str);
            t.setTextColor(kind.equals("q") ? soft : ink);
            t.setTextIsSelectable(true);
            if (kind.equals("h")) {
                t.setTextSize(zoom * 20.0f);
                t.setTypeface(Typeface.DEFAULT_BOLD);
                t.setPadding(0, dp(16.0f), 0, dp(4.0f));
            } else {
                t.setTextSize(18.0f * zoom);
                t.setLineSpacing(0.0f, 1.45f);
                t.setPadding((kind.equals("q") || kind.equals("li")) ? dp(14.0f) : 0, dp(7.0f), 0, dp(7.0f));
                if (kind.equals("q")) {
                    t.setTypeface(Typeface.create(Typeface.SERIF, 2));
                } else {
                    t.setTypeface(Typeface.SERIF);
                }
            }
            linearLayout.addView(t);
            close = close2;
            clp = clp2;
            title2 = title;
            it2 = it;
        }
        this.readerView.addView(scrollView, new FrameLayout.LayoutParams(-1, -1));
        this.readerText = a.plainText();
        this.root.addView(this.readerView, this.root.indexOfChild(this.swipe) + 1, new FrameLayout.LayoutParams(-1, -1));
        placeOverlay(this.readerView);
    }

    public void lambda$showReader$72(View v) {
        if (!this.billing.canUseFull()) {
            showUpgrade("Reading pages aloud");
        } else {
            this.voice.read(this.readerText);
            showStatus("Reading. Say “stop” or tap the mic to stop.");
        }
    }

    public void lambda$showReader$73(View v) {
        closeReader();
    }

    private void closeReader() {
        if (this.readerView == null) {
            return;
        }
        this.root.removeView(this.readerView);
        this.readerView = null;
        this.readerText = null;
    }

    private void placeOverlay(final View v) {
        if (v == null || this.topBar.getBottom() == 0) {
            return;
        }
        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
        int top = contentTop();
        if (lp.topMargin == top && lp.leftMargin == this.insetLeft + dp(6.0f)) {
            return;
        }
        lp.setMargins(this.insetLeft + dp(6.0f), top, this.insetRight + dp(6.0f), this.insetBottom + dp(6.0f));
        v.post(new Runnable() {
            @Override
            public final void run() {
                v.setLayoutParams(lp);
            }
        });
    }

    public void showTranslate() {
        if (needsWebPage("ask me to translate it")) {
            return;
        }
        List<Row> rows = new ArrayList<>();
        final String phone = PageTools.phoneLanguageName();
        final String phoneCode = PageTools.phoneLanguageCode();
        rows.add(new Row(phone, "Your phone's language", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showTranslate$75(phoneCode, phone);
            }
        }, null, true, false));
        for (final Map.Entry<String, String> e : PageTools.LANGUAGES.entrySet()) {
            if (!e.getValue().equalsIgnoreCase(phoneCode)) {
                final String name = Character.toUpperCase(e.getKey().charAt(0)) + e.getKey().substring(1);
                rows.add(new Row(name, null, new Runnable() {
                    @Override
                    public final void run() {
                        MainActivity.this.lambda$showTranslate$76(e, name);
                    }
                }, null, false, false));
            }
        }
        showSheet("Translate this page to", rows, null, null, null);
    }

    public void lambda$showTranslate$76(Map.Entry e, String name) {
        lambda$showTranslate$75((String) e.getValue(), name);
    }

    public void lambda$showTranslate$75(String code, String languageName) {
        if (needsWebPage("ask me to translate it")) {
            return;
        }
        if (!this.billing.canUseFull()) {
            showUpgrade("Translate page");
        } else {
            if (PageTools.isTranslated(this.current.url)) {
                tell("This page is already translated. Go back to see the original.");
                return;
            }
            this.current.web.loadUrl(PageTools.translateUrl(this.current.url, code));
            String lang = languageName.substring(0, 1).toUpperCase() + languageName.substring(1);
            tell("Translating to " + lang + ". Go back for the original.");
        }
    }

    private void setDesktop(boolean on) {
        if (needsWebPage("ask for the desktop site")) {
            return;
        }
        if (this.current.desktop == on) {
            tell(on ? "This is already the desktop site." : "This is already the mobile site.");
            return;
        }
        this.current.desktop = on;
        this.current.web.getSettings().setUserAgentString(on ? PageTools.desktopUserAgent(this) : null);
        this.current.web.reload();
        tell(on ? "Loading the desktop site." : "Loading the mobile site.");
    }

    public void showTextSize() {
        int zoom = this.prefs.getInt("textZoom", 100);
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("Text size: " + zoom + "%", "Changes the size of words on websites and in reader mode.", null, null, false, true));
        showSheet("Text size", rows, null, new String[]{"Bigger", "Smaller", "Reset"}, new Runnable[]{new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showTextSize$77();
            }
        }, new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showTextSize$78();
            }
        }, new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showTextSize$79();
            }
        }});
    }

    public void lambda$showTextSize$77() {
        stepTextSize(1);
        showTextSize();
    }

    public void lambda$showTextSize$78() {
        stepTextSize(-1);
        showTextSize();
    }

    public void lambda$showTextSize$79() {
        stepTextSize(0);
        showTextSize();
    }

    private void stepTextSize(int direction) {
        int zoom = this.prefs.getInt("textZoom", 100);
        int i = 2;
        for (int k = 0; k < TEXT_SIZES.length; k++) {
            if (TEXT_SIZES[k] <= zoom) {
                i = k;
            }
        }
        int next = direction != 0 ? TEXT_SIZES[Math.max(0, Math.min(TEXT_SIZES.length - 1, i + direction))] : 100;
        this.prefs.edit().putInt("textZoom", next).apply();
        for (Tab t : this.tabs) {
            t.web.getSettings().setTextZoom(next);
        }
    }

    private void setDark(boolean on) {
        this.prefs.edit().putBoolean("darkSites", on).apply();
        for (Tab t : this.tabs) {
            applyDark(t);
        }
        tell(on ? "Dark websites on." : "Dark websites off.");
    }

    public void applyDark(Tab tab) {
        if (tab.web == null || WebSupport.isAppPage(tab.url) || !WebSupport.isWeb(tab.url)) {
            return;
        }
        tab.web.evaluateJavascript(PageTools.darkJs(this.prefs.getBoolean("darkSites", false)), null);
    }

    private void updateAdBlock() {
        this.adBlockOn = this.prefs.getBoolean("adBlock", true) && this.billing.canUseFull();
    }

    private void setAdBlock(boolean on) {
        if (on && !this.billing.canUseFull()) {
            showUpgrade("The ad blocker");
            return;
        }
        this.prefs.edit().putBoolean("adBlock", on).apply();
        updateAdBlock();
        if (this.current != null && !WebSupport.isAppPage(this.current.url)) {
            this.current.web.reload();
        }
        tell(on ? "Ad blocker on." : "Ad blocker off. Ads will show again.");
    }

    public void showClearData() {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("Browsing history", "Pages you've visited", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showClearData$80();
            }
        }, null, false, false));
        rows.add(new Row("Cookies and site data", "Signs you out of most websites", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showClearData$81();
            }
        }, null, false, false));
        rows.add(new Row("Cached images and files", "Pages may load a little slower at first", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showClearData$82();
            }
        }, null, false, false));
        rows.add(new Row("Downloads list", "Your files stay in the Downloads folder", new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showClearData$83();
            }
        }, null, false, false));
        showSheet("Clear browsing data", rows, null, new String[]{"Clear everything"}, new Runnable[]{new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$showClearData$84();
            }
        }});
    }

    public void lambda$showClearData$80() {
        this.library.clearHistory();
        showStatus("History cleared.");
    }

    public void lambda$showClearData$81() {
        clearCookies();
        showStatus("Cookies and site data cleared.");
    }

    public void lambda$showClearData$82() {
        clearCache();
        showStatus("Cache cleared.");
    }

    public void lambda$showClearData$83() {
        this.downloads.clearList();
        showStatus("Downloads list cleared.");
    }

    public void lambda$showClearData$84() {
        closeSheet();
        this.library.clearHistory();
        clearCookies();
        clearCache();
        this.downloads.clearList();
        tell("All browsing data cleared.");
    }

    private void clearCookies() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
        WebViewDatabase.getInstance(this).clearHttpAuthUsernamePassword();
    }

    private void clearCache() {
        if (this.current != null) {
            this.current.web.clearCache(true);
        }
        for (Tab t : this.tabs) {
            t.web.clearFormData();
        }
    }

    private void setPrivateLock(boolean on) {
        if (on && !PrivateLock.phoneHasLock(this)) {
            tell("Set up a screen lock on your phone first (Settings, then Lock screen), then try again.");
            return;
        }
        this.prefs.edit().putBoolean("lockPrivate", on).apply();
        if (!on) {
            unlocked();
        }
        tell(on ? "Private tabs will lock when you leave RAEE SeaGlass." : "Private tabs won't be locked.");
    }

    private void checkLock() {
        if (this.current == null) {
            return;
        }
        boolean z = false;
        if (this.current.isPrivate && this.privateLocked && this.prefs.getBoolean("lockPrivate", false)) {
            z = true;
        }
        boolean cover = z;
        if (!cover) {
            if (this.lockCover != null) {
                this.root.removeView(this.lockCover);
                this.lockCover = null;
                return;
            }
            return;
        }
        closeReader();
        closeFind();
        showAddress(this.current.url);
        if (this.lockCover == null) {
            this.lockCover = new FrameLayout(this);
            this.lockCover.setBackground(Glass.panel(this, Glass.LOCK, 22.0f));
            this.lockCover.setClickable(true);
            this.lockCover.setElevation(dp(3.0f));
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(1);
            box.setGravity(1);
            TextView t = new TextView(this);
            t.setText("Private tabs are locked");
            t.setTextColor(Glass.INK);
            t.setTextSize(20.0f);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setGravity(17);
            box.addView(t);
            TextView unlock = new TextView(this);
            unlock.setText("Unlock");
            unlock.setTextColor(-1);
            unlock.setTextSize(16.0f);
            unlock.setTypeface(Typeface.DEFAULT_BOLD);
            unlock.setGravity(17);
            unlock.setBackground(Glass.panel(this, -9741338, 16.0f));
            unlock.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    MainActivity.this.lambda$checkLock$85(view);
                }
            });
            LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(dp(180.0f), dp(52.0f));
            ulp.topMargin = dp(18.0f);
            box.addView(unlock, ulp);
            this.lockCover.addView(box, new FrameLayout.LayoutParams(-2, -2, 17));
            this.root.addView(this.lockCover, this.root.indexOfChild(this.swipe) + 1, new FrameLayout.LayoutParams(-1, -1));
            placeOverlay(this.lockCover);
        }
        if (this.authInProgress || this.lockPromptDismissed) {
            return;
        }
        this.handler.postDelayed(new Runnable() {
            @Override
            public final void run() {
                MainActivity.this.lambda$checkLock$86();
            }
        }, 300L);
    }

    public void lambda$checkLock$85(View v) {
        askUnlock();
    }

    public void lambda$checkLock$86() {
        if (this.lockCover == null || this.authInProgress || this.lockPromptDismissed) {
            return;
        }
        askUnlock();
    }

    private void askUnlock() {
        if (this.authInProgress) {
            return;
        }
        this.authInProgress = true;
        PrivateLock.ask(this, new PrivateLock.Result() {
            @Override
            public void onUnlocked() {
                MainActivity.this.authInProgress = false;
                MainActivity.this.unlocked();
            }

            @Override
            public void onCancelled() {
                MainActivity.this.authInProgress = false;
                MainActivity.this.lockPromptDismissed = true;
            }
        });
    }

    public void unlocked() {
        this.privateLocked = false;
        checkLock();
        refreshChrome();
    }

    private void setLook(boolean useMidnight) {
        if (Glass.midnight == useMidnight) {
            tell(useMidnight ? "Midnight Glass is already on." : "Teal Glass is already on.");
            return;
        }
        Glass.save(this, useMidnight);
        closeSheet();
        closeReader();
        closeFind();
        exitFullscreen();
        if (this.lockCover != null) {
            this.lockCover = null;
        }
        if (this.current != null) {
            this.webHolder.removeView(this.current.web);
        }
        buildUi();
        updateSystemBars();
        updateMuteButton();
        updateMic();
        for (Tab t : this.tabs) {
            t.web.setBackgroundColor(Glass.TEAL_BG);
            if (WebSupport.isAppPage(t.url)) {
                t.web.reload();
            }
        }
        if (this.current != null) {
            this.webHolder.addView(this.current.web, new FrameLayout.LayoutParams(-1, -1));
        }
        refreshChrome();
        checkLock();
        if (this.current != null && this.current.isPrivate) {
            refreshChrome();
        }
        tell(useMidnight ? "Midnight Glass." : "Teal Glass.");
    }

    private void updateSystemBars() {
        boolean darkIcons = !Glass.midnight;
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.setSystemBarsAppearance(darkIcons ? 24 : 0, 24);
            }
        } else {
            int flags = darkIcons ? 1792 | 8208 : 1792;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
        getWindow().getDecorView().setBackgroundColor(Glass.TEAL_BG);
    }

    private static long weekNumber() {
        return ((System.currentTimeMillis() / 86400000) + 3) / 7;
    }

    public int adsThisWeekCount() {
        int saved = this.prefs.getLong("adsWeek", -1L) == weekNumber() ? this.prefs.getInt("adsCount", 0) : 0;
        return blockedSinceSave.get() + saved;
    }

    private void saveAdCount() {
        int add = blockedSinceSave.getAndSet(0);
        long week = weekNumber();
        int saved = this.prefs.getLong("adsWeek", -1L) == week ? this.prefs.getInt("adsCount", 0) : 0;
        this.prefs.edit().putLong("adsWeek", week).putInt("adsCount", saved + add).apply();
    }

    public void turnOnWeather() {
        if (Weather.hasPermission(this)) {
            Weather.setEnabled(this, true);
            pushWeather();
            Weather.refresh(this, new Runnable() { @Override public void run() { pushWeather(); } });
            showStatus("Getting your local weather…");
            return;
        }
        requestPermissions(new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, REQ_WEATHER);
    }

    public void pushWeather() {
        for (Tab t : this.tabs) {
            if (WebSupport.isAppPage(t.url)) {
                t.web.evaluateJavascript("window.seaglassWeather&&seaglassWeather()", null);
            }
        }
    }

    private static String siteName(String origin) {
        if (origin == null) {
            return "This website";
        }
        Uri u = Uri.parse(origin);
        return u.getHost() != null ? u.getHost() : origin;
    }

    public void askSitePermission(Tab tab, final PermissionRequest request) {
        String str;
        if (this.sitePermission != null) {
            this.sitePermission.deny();
        }
        boolean wantsCamera = false;
        boolean wantsMic = false;
        final List<String> allowed = new ArrayList<>();
        for (String r : request.getResources()) {
            if ("android.webkit.resource.VIDEO_CAPTURE".equals(r)) {
                wantsCamera = true;
                allowed.add(r);
            } else if ("android.webkit.resource.AUDIO_CAPTURE".equals(r)) {
                wantsMic = true;
                allowed.add(r);
            } else if ("android.webkit.resource.PROTECTED_MEDIA_ID".equals(r)) {
                allowed.add(r);
            }
        }
        if (allowed.isEmpty() || WebSupport.isAppPage(request.getOrigin().toString())) {
            request.deny();
            return;
        }
        if (!wantsCamera && !wantsMic) {
            request.grant((String[]) allowed.toArray(new String[0]));
            return;
        }
        if (wantsCamera && wantsMic) {
            str = "your camera and microphone";
        } else {
            str = wantsCamera ? "your camera" : "your microphone";
        }
        String what = str;
        String site = siteName(request.getOrigin().toString());
        final boolean cam = wantsCamera;
        final boolean mic = wantsMic;
        new AlertDialog.Builder(this).setTitle("Allow " + site + "?").setMessage(site + " wants to use " + what + ". Only allow sites you trust.").setPositiveButton("Allow", new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.lambda$askSitePermission$87(request, allowed, cam, mic, dialogInterface, i);
            }
        }).setNegativeButton("Block", new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                request.deny();
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public final void onCancel(DialogInterface dialogInterface) {
                request.deny();
            }
        }).show();
    }

    public void lambda$askSitePermission$87(PermissionRequest request, List allowed, boolean cam, boolean mic, DialogInterface d, int which) {
        this.sitePermission = request;
        this.siteResources = (String[]) allowed.toArray(new String[0]);
        List<String> needed = new ArrayList<>();
        if (cam && checkSelfPermission("android.permission.CAMERA") != 0) {
            needed.add("android.permission.CAMERA");
        }
        if (mic && checkSelfPermission("android.permission.RECORD_AUDIO") != 0) {
            needed.add("android.permission.RECORD_AUDIO");
        }
        if (!needed.isEmpty()) {
            requestPermissions((String[]) needed.toArray(new String[0]), 44);
        } else {
            finishSitePermission();
        }
    }

    private void finishSitePermission() {
        PermissionRequest request = this.sitePermission;
        String[] wanted = this.siteResources;
        this.sitePermission = null;
        this.siteResources = null;
        if (request == null || wanted == null) {
            return;
        }
        List<String> ok = new ArrayList<>();
        for (String r : wanted) {
            if ((!"android.webkit.resource.VIDEO_CAPTURE".equals(r) || checkSelfPermission("android.permission.CAMERA") == 0) && (!"android.webkit.resource.AUDIO_CAPTURE".equals(r) || checkSelfPermission("android.permission.RECORD_AUDIO") == 0)) {
                ok.add(r);
            }
        }
        if (ok.isEmpty()) {
            request.deny();
            showStatus("The site can't use that until you allow it in Settings.");
        } else {
            request.grant((String[]) ok.toArray(new String[0]));
        }
    }

    public void askLocation(final String origin, final GeolocationPermissions.Callback callback) {
        if (this.locationCallback != null) {
            this.locationCallback.invoke(this.locationOrigin, false, false);
        }
        String site = siteName(origin);
        new AlertDialog.Builder(this).setTitle("Allow " + site + "?").setMessage(site + " wants to know your location, for example to show places near you.").setPositiveButton("Allow", new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.lambda$askLocation$90(callback, origin, dialogInterface, i);
            }
        }).setNegativeButton("Block", new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                callback.invoke(origin, false, false);
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public final void onCancel(DialogInterface dialogInterface) {
                callback.invoke(origin, false, false);
            }
        }).show();
    }

    public void lambda$askLocation$90(GeolocationPermissions.Callback callback, String origin, DialogInterface d, int which) {
        if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0 || checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == 0) {
            callback.invoke(origin, true, false);
            return;
        }
        this.locationOrigin = origin;
        this.locationCallback = callback;
        requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"}, 45);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 46) {
            this.authInProgress = false;
            if (resultCode != -1) {
                this.lockPromptDismissed = true;
                return;
            } else {
                unlocked();
                return;
            }
        }
        if (requestCode != 43) {
            return;
        }
        ValueCallback<Uri[]> cb = this.fileCallback;
        this.fileCallback = null;
        if (cb == null) {
            return;
        }
        Uri[] result = null;
        if (resultCode == -1 && data != null) {
            if (data.getClipData() != null && data.getClipData().getItemCount() > 0) {
                int n = data.getClipData().getItemCount();
                result = new Uri[n];
                for (int i = 0; i < n; i++) {
                    result[i] = data.getClipData().getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
        }
        cb.onReceiveValue(result);
    }

    public void exitFullscreen() {
        if (this.customView == null) {
            return;
        }
        View v = this.customView;
        WebChromeClient.CustomViewCallback cb = this.customCallback;
        this.customView = null;
        this.customCallback = null;
        this.root.removeView(v);
        this.topBar.setVisibility(0);
        this.pullTab.setVisibility(this.barHidden ? 0 : 8);
        if (cb != null) {
            cb.onCustomViewHidden();
        }
    }

    class HomeBridge {
        private final Tab tab;

        HomeBridge(Tab tab) {
            this.tab = tab;
        }

        @JavascriptInterface
        public void run(final String text) {
            MainActivity.this.handler.post(new Runnable() {
                @Override
                public final void run() {
                    MainActivity.HomeBridge.this.lambda$run$0(text);
                }
            });
        }

        public void lambda$run$0(String text) {
            if (this.tab != MainActivity.this.current || !WebSupport.isAppPage(this.tab.url)) {
                return;
            }
            MainActivity.this.handleRequest(text, true);
        }

        @JavascriptInterface
        public String look() {
            return Glass.midnight ? "midnight" : "teal";
        }

        @JavascriptInterface
        public String recent() {
            if (!WebSupport.isAppPage(this.tab.url) || this.tab.isPrivate) {
                return "[]";
            }
            JSONArray a = new JSONArray();
            for (Library.Entry e : MainActivity.this.library.history()) {
                if (a.length() >= 4) {
                    break;
                }
                JSONArray item = new JSONArray();
                item.put(e.title);
                item.put(e.url);
                a.put(item);
            }
            return a.toString();
        }

        @JavascriptInterface
        public String adsThisWeek() {
            return !WebSupport.isAppPage(this.tab.url) ? "0" : MainActivity.this.adBlockOn ? String.valueOf(MainActivity.this.adsThisWeekCount()) : "-1";
        }

        @JavascriptInterface
        public String weather() {
            return WebSupport.isAppPage(this.tab.url) ? Weather.forPage(MainActivity.this) : "";
        }

        @JavascriptInterface
        public void enableWeather() {
            MainActivity.this.handler.post(new Runnable() {
                @Override
                public final void run() {
                    MainActivity.HomeBridge.this.lambda$enableWeather$1();
                }
            });
        }

        public void lambda$enableWeather$1() {
            if (this.tab != MainActivity.this.current || !WebSupport.isAppPage(this.tab.url)) {
                return;
            }
            MainActivity.this.turnOnWeather();
        }

        @JavascriptInterface
        public String shortcuts() {
            return WebSupport.isAppPage(this.tab.url) ? MainActivity.this.library.shortcutsJson() : "[]";
        }

        @JavascriptInterface
        public void removeShortcut(final String url) {
            MainActivity.this.handler.post(new Runnable() {
                @Override
                public final void run() {
                    MainActivity.HomeBridge.this.lambda$removeShortcut$2(url);
                }
            });
        }

        public void lambda$removeShortcut$2(String url) {
            if (WebSupport.isAppPage(this.tab.url)) {
                MainActivity.this.library.removeShortcut(url);
            }
        }

        @JavascriptInterface
        public void listen() {
            MainActivity.this.handler.post(new Runnable() {
                @Override
                public final void run() {
                    MainActivity.HomeBridge.this.lambda$listen$3();
                }
            });
        }

        public void lambda$listen$3() {
            if (this.tab != MainActivity.this.current || !WebSupport.isAppPage(this.tab.url)) {
                return;
            }
            MainActivity.this.onMicTapped();
        }
    }

    private void handleLaunch(Intent intent) {
        char c;
        if (intent == null) {
            return;
        }
        String url = intent.getStringExtra("me.raeeindustries.seaglass.URL");
        if (!WebSupport.isWeb(url) && "android.intent.action.VIEW".equals(intent.getAction()) && intent.getData() != null) {
            url = intent.getData().toString();
        }
        if (WebSupport.isWeb(url)) {
            if (WebSupport.isAppPage(this.current.url)) {
                this.current.web.loadUrl(url);
            } else {
                createTab(false, url, true);
            }
            saveTabs();
        }
        String show = intent.getStringExtra(EXTRA_SHOW);
        if (show != null) {
            switch (show.hashCode()) {
                case -1048825355:
                    if (show.equals("newtab")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -314497661:
                    if (show.equals("private")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 3552126:
                    if (show.equals("tabs")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 926934164:
                    if (show.equals("history")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 2037187069:
                    if (show.equals("bookmarks")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    showTabs();
                    break;
                case 1:
                    showBookmarks();
                    break;
                case 2:
                    showHistory();
                    break;
                case 3:
                    newTab(false);
                    break;
                case 4:
                    newTab(true);
                    break;
            }
        }
        intent.removeExtra("me.raeeindustries.seaglass.URL");
        intent.removeExtra(EXTRA_SHOW);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLaunch(intent);
        if (isLauncherOpen(intent)) {
            welcome();
        }
    }

    @Override
    public void onBackPressed() {
        if (this.sheet != null) {
            closeSheet();
            return;
        }
        if (this.readerView != null) {
            closeReader();
            return;
        }
        if (this.findBar.getVisibility() == 0) {
            closeFind();
            return;
        }
        if (this.customView != null) {
            exitFullscreen();
            return;
        }
        if (this.current.web.canGoBack()) {
            this.current.web.goBack();
        } else if (this.tabs.size() > 1 && this.current.isPrivate) {
            closeTabNow(this.current);
        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.tealFadeOn) {
            this.handler.removeCallbacks(this.tealTick);
            this.handler.post(this.tealTick);
        }
        this.billing.refresh();
        if (this.current != null) {
            this.current.web.onResume();
        }
        if (this.pendingOverlay && Settings.canDrawOverlays(this)) {
            this.pendingOverlay = false;
            startFloating();
        }
        updateAdBlock();
        checkLock();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!this.authInProgress && this.prefs.getBoolean("lockPrivate", false) && hasPrivateTabs()) {
            this.privateLocked = true;
            this.lockPromptDismissed = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.handler.removeCallbacks(this.tealTick);
        this.voice.stopListening();
        if (this.current != null) {
            this.current.web.onPause();
        }
        saveTabs();
        saveAdCount();
    }

    @Override
    protected void onDestroy() {
        this.tealFadeOn = false;
        this.handler.removeCallbacksAndMessages(null);
        this.billing.removeListener(this);
        this.voice.destroy();
        this.webHolder.removeAllViews();
        for (Tab t : this.tabs) {
            t.web.destroy();
        }
        boolean hadPrivate = hasPrivateTabs();
        this.tabs.clear();
        if (hadPrivate) {
            wipePrivateData();
        }
        super.onDestroy();
    }

    /* ================= Slim bar: pull-down menu, auto-hide, teal fade ================= */

    /** Small "Pull down for menu" tab shown at the top while the bar is hidden. */
    private void buildPullTab() {
        this.pullTab = new TextView(this);
        this.pullTab.setText("Pull down for menu  ▾");
        this.pullTab.setTextColor(Glass.INK);
        this.pullTab.setTextSize(13.0f);
        this.pullTab.setTypeface(Typeface.DEFAULT_BOLD);
        this.pullTab.setGravity(17);
        this.pullTab.setPadding(dp(16.0f), dp(7.0f), dp(16.0f), dp(7.0f));
        this.pullTab.setBackground(Glass.panel(this, Glass.PILL, 18.0f));
        this.pullTab.setElevation(dp(3.0f));
        this.pullTab.setContentDescription("Show the address bar and menu");
        this.pullTab.setVisibility(8);
        Glass.glow(this.pullTab);
        this.pullTab.setOnTouchListener(new View.OnTouchListener() {
            private float downY;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    this.downY = e.getRawY();
                    return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                    boolean pulled = e.getRawY() - this.downY > dp(24.0f);
                    showBar();
                    if (pulled) {
                        MainActivity.this.handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showPullMenu();
                            }
                        }, 220L);
                    }
                    v.performClick();
                    return true;
                }
                return e.getActionMasked() == MotionEvent.ACTION_MOVE;
            }
        });
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, 49);
        lp.topMargin = this.insetTop + dp(4.0f);
        this.root.addView(this.pullTab, lp);
    }

    /** Hides the bar while reading down a page; brings it back near the top or when scrolling up. */
    private void onPageScrolled(Tab tab, int y, int dy) {
        if (tab != this.current || SystemClock.uptimeMillis() < this.ignoreScrollUntil) {
            return;
        }
        if (WebSupport.isAppPage(tab.url) || y < dp(48.0f)) {
            showBar();
        } else if (dy > dp(6.0f)) {
            hideBar();
        } else if (dy < -dp(12.0f)) {
            showBar();
        }
    }

    private void hideBar() {
        if (this.barHidden || this.customView != null || this.sheet != null || this.readerView != null
                || this.findBar.getVisibility() == 0 || this.address.hasFocus() || this.topBar.getBottom() == 0) {
            return;
        }
        this.barHidden = true;
        this.topBar.animate().translationY(-(this.topBar.getBottom() + dp(8.0f))).setDuration(180L).start();
        this.pullTab.setAlpha(0.0f);
        this.pullTab.setVisibility(0);
        this.pullTab.animate().alpha(1.0f).setDuration(180L).start();
        placeContent();
    }

    public void showBar() {
        if (!this.barHidden) {
            return;
        }
        this.barHidden = false;
        this.topBar.animate().translationY(0.0f).setDuration(180L).start();
        this.pullTab.setVisibility(8);
        placeContent();
    }

    private void togglePullMenu() {
        if (this.pullMenuOpen) {
            closeSheet();
        } else {
            showPullMenu();
        }
    }

    private boolean onWebPage() {
        return this.current != null && !WebSupport.isAppPage(this.current.url) && WebSupport.isWeb(this.current.url);
    }

    /** The glass menu that drops down under the slim bar. */
    private void showPullMenu() {
        hideKeyboard();
        closeSheet();
        showBar();
        final boolean onWeb = onWebPage();
        this.sheet = new FrameLayout(this);
        // Dim the page more than other sheets so the menu stands out.
        this.sheet.setBackgroundColor(Glass.midnight ? 0x66000000 : 0x4006323B);
        this.sheet.setClickable(true);
        this.sheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSheet();
            }
        });
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(1);
        // Frosted glass: nearly solid, with the usual glass edge and sheen.
        int frost = Glass.midnight ? 0xF20D2A40 : 0xF5EEFBFC;
        card.setBackground(Glass.clearGlass(this, frost, 26.0f));
        card.setElevation(dp(8.0f));
        card.setPadding(dp(12.0f), dp(12.0f), dp(12.0f), dp(10.0f));
        card.setClickable(true);
        Glass.glow(card);

        // Back / Forward / Reload / Home
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(0);
        nav.addView(navButton(R.drawable.ic_back, "Back", new Runnable() {
            @Override
            public void run() {
                goBack();
            }
        }), navLp(false));
        nav.addView(navButton(R.drawable.ic_forward, "Forward", new Runnable() {
            @Override
            public void run() {
                goForward();
            }
        }), navLp(true));
        nav.addView(navButton(R.drawable.ic_reload, "Reload", new Runnable() {
            @Override
            public void run() {
                reload();
            }
        }), navLp(true));
        nav.addView(navButton(R.drawable.ic_home, "Home", new Runnable() {
            @Override
            public void run() {
                goHome();
            }
        }), navLp(true));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(1);
        body.addView(nav, new LinearLayout.LayoutParams(-1, -2));

        // ---- This page (only on websites) ----
        if (onWeb) {
            List<View> page = new ArrayList<>();
            boolean saved = this.library.isBookmarked(this.current.url);
            page.add(tile(getDrawable(saved ? R.drawable.ic_star_filled : R.drawable.ic_star),
                    saved ? "Bookmarked" : "Bookmark", false, true, new Runnable() {
                @Override
                public void run() {
                    bookmarkPage();
                }
            }));
            page.add(tile(new MenuIcon(MenuIcon.READ_ALOUD, Glass.INK), "Read aloud", false, true, new Runnable() {
                @Override
                public void run() {
                    readPage();
                }
            }));
            page.add(tile(getDrawable(R.drawable.ic_read), "Reader", false, true, new Runnable() {
                @Override
                public void run() {
                    openReader();
                }
            }));
            page.add(tile(new MenuIcon(MenuIcon.FIND, Glass.INK), "Find", false, true, new Runnable() {
                @Override
                public void run() {
                    openFind(null);
                }
            }));
            page.add(tile(new MenuIcon(MenuIcon.SHARE, Glass.INK), "Share", false, true, new Runnable() {
                @Override
                public void run() {
                    sharePage();
                }
            }));
            page.add(tile(new MenuIcon(MenuIcon.TRANSLATE, Glass.INK), "Translate", false, true, new Runnable() {
                @Override
                public void run() {
                    showTranslate();
                }
            }));
            boolean pinned = this.library.isShortcut(this.current.url);
            page.add(tile(new MenuIcon(MenuIcon.ADD_HOME, Glass.INK), pinned ? "On home" : "Add to home", false, true, new Runnable() {
                @Override
                public void run() {
                    tool("shortcut", null);
                }
            }));
            boolean desktop = this.current.desktop;
            page.add(tile(new MenuIcon(MenuIcon.DESKTOP, Glass.INK), desktop ? "Mobile site" : "Desktop site", false, true, new Runnable() {
                @Override
                public void run() {
                    setDesktop(!MainActivity.this.current.desktop);
                }
            }));
            body.addView(sectionTitle("This page"));
            body.addView(grid(page));
        }

        // ---- SeaGlass ----
        List<View> app = new ArrayList<>();
        app.add(tile(new MenuIcon(MenuIcon.TABS, Glass.INK), "Tabs · " + this.tabs.size(), false, true, new Runnable() {
            @Override
            public void run() {
                showTabs();
            }
        }));
        app.add(tile(new MenuIcon(MenuIcon.NEW_TAB, Glass.INK), "New tab", false, true, new Runnable() {
            @Override
            public void run() {
                newTab(false);
            }
        }));
        app.add(tile(new MenuIcon(MenuIcon.PRIVATE, Glass.INK), "Private tab", false, true, new Runnable() {
            @Override
            public void run() {
                newTab(true);
            }
        }));
        app.add(tile(getDrawable(R.drawable.ic_float), "Bubble", true, true, new Runnable() {
            @Override
            public void run() {
                startFloating();
            }
        }));
        app.add(tile(new MenuIcon(MenuIcon.BOOKMARKS, Glass.INK), "Bookmarks", false, true, new Runnable() {
            @Override
            public void run() {
                showBookmarks();
            }
        }));
        app.add(tile(new MenuIcon(MenuIcon.HISTORY, Glass.INK), "History", false, true, new Runnable() {
            @Override
            public void run() {
                showHistory();
            }
        }));
        app.add(tile(new MenuIcon(MenuIcon.DOWNLOAD, Glass.INK), "Downloads", false, true, new Runnable() {
            @Override
            public void run() {
                showDownloads();
            }
        }));
        boolean muted = this.voice.isMuted();
        app.add(tile(getDrawable(muted ? R.drawable.ic_volume_off : R.drawable.ic_volume),
                muted ? "Voice off" : "Voice on", false, true, new Runnable() {
            @Override
            public void run() {
                toggleMute();
            }
        }));
        body.addView(sectionTitle("SeaGlass"));
        body.addView(grid(app));
        if (!onWeb) {
            TextView hint = new TextView(this);
            hint.setText("Open a website to bookmark, share, translate or read it aloud.");
            hint.setTextColor(Glass.INK_SOFT);
            hint.setTextSize(13.0f);
            hint.setGravity(17);
            hint.setPadding(dp(8.0f), dp(12.0f), dp(8.0f), 0);
            body.addView(hint);
        }

        // ---- More & settings (full width) ----
        LinearLayout more = new LinearLayout(this);
        more.setOrientation(0);
        more.setGravity(16);
        more.setBackground(Glass.panel(this, Glass.ROW, 16.0f));
        more.setPadding(dp(14.0f), 0, dp(14.0f), 0);
        more.setContentDescription("More and settings");
        ImageView gear = new ImageView(this);
        gear.setImageDrawable(new MenuIcon(MenuIcon.SETTINGS, Glass.INK));
        more.addView(gear, new LinearLayout.LayoutParams(dp(22.0f), dp(22.0f)));
        TextView moreText = new TextView(this);
        moreText.setText("More & settings");
        moreText.setTextColor(Glass.INK);
        moreText.setTextSize(15.0f);
        moreText.setTypeface(Typeface.DEFAULT_BOLD);
        moreText.setPadding(dp(12.0f), 0, 0, 0);
        more.addView(moreText, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView chevron = new TextView(this);
        chevron.setText("›");
        chevron.setTextColor(Glass.INK_SOFT);
        chevron.setTextSize(24.0f);
        more.addView(chevron);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSheet();
                showMenu();
            }
        });
        LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(-1, dp(50.0f));
        moreLp.topMargin = dp(14.0f);
        body.addView(more, moreLp);

        ScrollView scroll = new ScrollView(this) {
            @Override
            protected void onMeasure(int widthSpec, int heightSpec) {
                int cap = (int) (MainActivity.this.root.getHeight() * 0.72f);
                if (cap > 0) {
                    heightSpec = View.MeasureSpec.makeMeasureSpec(cap, Integer.MIN_VALUE);
                }
                super.onMeasure(widthSpec, heightSpec);
            }
        };
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(body);
        card.addView(scroll, new LinearLayout.LayoutParams(-1, -2));

        // Grab handle: swipe up on the menu to close it
        View handle = new View(this);
        GradientDrawable h = new GradientDrawable();
        h.setColor(Glass.INK_SOFT);
        h.setAlpha(110);
        h.setCornerRadius(dp(3.0f));
        handle.setBackground(h);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(44.0f), dp(5.0f));
        hlp.gravity = 1;
        hlp.topMargin = dp(12.0f);
        card.addView(handle, hlp);
        card.setOnTouchListener(new View.OnTouchListener() {
            private float downY;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    this.downY = e.getRawY();
                } else if (e.getActionMasked() == MotionEvent.ACTION_UP && this.downY - e.getRawY() > dp(40.0f)) {
                    closeSheet();
                }
                return true;
            }
        });

        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(-1, -2, 48);
        cardLp.setMargins(this.insetLeft + dp(8.0f), dp(6.0f), this.insetRight + dp(8.0f), 0);
        this.sheet.addView(card, cardLp);
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(-1, -1);
        sheetLp.topMargin = this.topBar.getBottom();
        this.root.addView(this.sheet, sheetLp);
        card.setTranslationY(-dp(24.0f));
        card.setAlpha(0.0f);
        card.animate().translationY(0.0f).alpha(1.0f).setDuration(170L).start();
        // Frost the page behind the menu (Android 12 and newer).
        if (Build.VERSION.SDK_INT >= 31) {
            this.swipe.setRenderEffect(RenderEffect.createBlurEffect(dp(14.0f), dp(14.0f), Shader.TileMode.CLAMP));
        }

        this.pullMenuOpen = true;
        this.menuButton.setImageResource(R.drawable.ic_close);
        this.menuButton.setColorFilter(Glass.INK, PorterDuff.Mode.SRC_IN);
        this.menuButton.setContentDescription("Close menu");
    }

    private TextView sectionTitle(String text) {
        TextView t = new TextView(this);
        t.setText(text.toUpperCase(java.util.Locale.ROOT));
        t.setTextColor(Glass.INK_SOFT);
        t.setTextSize(12.0f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.12f);
        t.setPadding(dp(4.0f), dp(16.0f), 0, dp(2.0f));
        return t;
    }

    private LinearLayout grid(List<View> tiles) {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(1);
        LinearLayout line = null;
        for (int i = 0; i < tiles.size(); i++) {
            if (i % 4 == 0) {
                line = new LinearLayout(this);
                line.setOrientation(0);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-1, -2);
                llp.topMargin = dp(8.0f);
                grid.addView(line, llp);
            }
            line.addView(tiles.get(i), new LinearLayout.LayoutParams(0, -2, 1.0f));
        }
        while (line != null && line.getChildCount() < 4) {
            line.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1.0f));
        }
        return grid;
    }

    private LinearLayout.LayoutParams navLp(boolean gap) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44.0f), 1.0f);
        if (gap) {
            lp.leftMargin = dp(6.0f);
        }
        return lp;
    }

    private View navButton(int icon, String label, final Runnable action) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(0);
        b.setGravity(17);
        b.setBackground(Glass.panel(this, Glass.ROW, 14.0f));
        b.setContentDescription(label);
        ImageView i = new ImageView(this);
        i.setImageResource(icon);
        i.setColorFilter(Glass.INK, PorterDuff.Mode.SRC_IN);
        b.addView(i, new LinearLayout.LayoutParams(dp(18.0f), dp(18.0f)));
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Glass.INK);
        t.setTextSize(12.5f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setSingleLine(true);
        t.setPadding(dp(4.0f), 0, 0, 0);
        b.addView(t);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSheet();
                action.run();
            }
        });
        return b;
    }

    private View tile(Drawable icon, String label, boolean accent, final boolean enabled, final Runnable action) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(1);
        t.setGravity(1);
        t.setPadding(0, dp(2.0f), 0, dp(2.0f));
        t.setContentDescription(label);
        ImageView box = new ImageView(this);
        box.setImageDrawable(icon.mutate());
        box.setColorFilter(accent ? -1 : Glass.INK, PorterDuff.Mode.SRC_IN);
        box.setScaleType(ImageView.ScaleType.FIT_CENTER);
        box.setPadding(dp(13.0f), dp(13.0f), dp(13.0f), dp(13.0f));
        box.setBackground(Glass.panel(this, accent ? Glass.ACCENT : Glass.ROW, 16.0f));
        t.addView(box, new LinearLayout.LayoutParams(dp(50.0f), dp(50.0f)));
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(Glass.INK);
        name.setTextSize(11.5f);
        name.setGravity(1);
        name.setMaxLines(2);
        name.setPadding(dp(2.0f), dp(5.0f), dp(2.0f), 0);
        t.addView(name, new LinearLayout.LayoutParams(-1, -2));
        if (!enabled) {
            t.setAlpha(0.4f);
        }
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!enabled) {
                    showStatus("Open a website first.");
                    return;
                }
                closeSheet();
                action.run();
            }
        });
        return t;
    }

    /**
     * Fades the glass background through three teal shades on a 12-second loop (Teal Glass look only).
     * Driven by a simple timer, so it keeps running even when the phone's animations are turned off.
     */
    private void startTealFade() {
        this.handler.removeCallbacks(this.tealTick);
        if (Glass.midnight) {
            this.tealFadeOn = false;
            this.root.setBackgroundColor(Glass.TEAL_BG);
            return;
        }
        this.tealFadeOn = true;
        this.handler.post(this.tealTick);
    }

    /** Smooth (ease-in-out) blend between the three shades for the current moment. */
    private static int tealColorNow() {
        float pos = (SystemClock.uptimeMillis() % TEAL_LOOP_MS) / (float) TEAL_LOOP_MS * TEAL_SHADES.length;
        int i = (int) pos;
        float f = pos - i;
        f = f * f * (3.0f - 2.0f * f);
        int a = TEAL_SHADES[i % TEAL_SHADES.length];
        int b = TEAL_SHADES[(i + 1) % TEAL_SHADES.length];
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * f);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * f);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
