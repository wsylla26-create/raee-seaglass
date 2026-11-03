package me.raeeindustries.seaglass;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.webkit.WebViewAssetLoader;

public class FloatingService extends Service implements VoiceEngine.Listener, CommandRouter.Browser {
    static final String ACTION_STOP = "me.raeeindustries.seaglass.STOP_BUBBLE";
    private static final String CHANNEL = "floating_bubble";
    private static final int MATCH = -1;
    private static final int NOTIFICATION_ID = 7;
    private static final int WRAP = -2;
    private EditText address;
    private WebViewAssetLoader assets;
    private FrameLayout bubble;
    private WindowManager.LayoutParams bubbleLp;
    private boolean bubbleShown;
    private boolean expanded;
    private boolean lastFromVoice;
    private Library library;
    private ImageButton mic;
    private LinearLayout panel;
    private WindowManager.LayoutParams panelLp;
    private boolean speaking;
    private TextView status;
    private VoiceEngine voice;
    private WebView web;
    private WindowManager wm;
    private String currentUrl = "https://appassets.androidplatform.net/assets/home.html?mini=1";
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Glass.load(this);
        goForeground();
        this.wm = (WindowManager) getSystemService("window");
        this.assets = WebSupport.assetLoader(this);
        this.library = new Library(this);
        this.voice = new VoiceEngine(this, this);
        this.voice.setMuted(getSharedPreferences("seaglass", 0).getBoolean("muted", false));
        buildBubble();
        buildPanel();
        try {
            this.wm.addView(this.bubble, this.bubbleLp);
            this.bubbleShown = true;
        } catch (RuntimeException e) {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return 2;
        }
        if (intent != null) {
            String url = intent.getStringExtra("me.raeeindustries.seaglass.URL");
            if (WebSupport.isWeb(url)) {
                this.web.loadUrl(url);
            }
        }
        return 2;
    }

    private int dp(float v) {
        return Glass.dp(this, v);
    }

    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Floating bubble", 2);
        channel.setDescription("Shown while the RAEE SeaGlass bubble is on screen.");
        nm.createNotificationChannel(channel);
        Intent openIntent = new Intent(this, (Class<?>) MainActivity.class).addFlags(268435456);
        boolean micAllowed = false;
        PendingIntent open = PendingIntent.getActivity(this, 0, openIntent, 201326592);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, (Class<?>) FloatingService.class).setAction(ACTION_STOP), 201326592);
        Notification n = new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_mic).setContentTitle("RAEE SeaGlass bubble is on").setContentText("Tap to open RAEE SeaGlass. Turn off to remove the bubble.").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_close), "Turn off", stop).build()).build();
        if (checkSelfPermission("android.permission.RECORD_AUDIO") == 0) {
            micAllowed = true;
        }
        if (Build.VERSION.SDK_INT >= 34) {
            int type = micAllowed ? 1073741824 | 128 : 1073741824;
            startForeground(7, n, type);
        } else if (Build.VERSION.SDK_INT >= 30 && micAllowed) {
            startForeground(7, n, 128);
        } else {
            startForeground(7, n);
        }
    }

    private Point screen() {
        DisplayMetrics m = getResources().getDisplayMetrics();
        return new Point(m.widthPixels, m.heightPixels);
    }

    private void buildBubble() {
        int size = dp(64.0f);
        this.bubble = new FrameLayout(this);
        this.bubble.setBackground(Glass.clearGlassOval(this, Glass.BUBBLE));
        this.bubble.setContentDescription("Open RAEE SeaGlass");
        ImageView inner = new ImageView(this);
        inner.setImageResource(R.drawable.ic_mic);
        inner.setColorFilter(-1, PorterDuff.Mode.SRC_IN);
        inner.setBackground(Glass.oval(this, Glass.ACCENT, 0, 0));
        inner.setPadding(dp(12.0f), dp(12.0f), dp(12.0f), dp(12.0f));
        this.bubble.addView(inner, new FrameLayout.LayoutParams(dp(46.0f), dp(46.0f), 17));
        this.bubbleLp = new WindowManager.LayoutParams(size, size, 2038, 8, -3);
        this.bubbleLp.gravity = 8388659;
        Point s = screen();
        this.bubbleLp.x = (s.x - size) - dp(8.0f);
        this.bubbleLp.y = (int) (s.y * 0.35f);
        this.bubble.setOnTouchListener(new DragToMove());
    }

    private class DragToMove implements View.OnTouchListener {
        private float downX;
        private float downY;
        private boolean moved;
        private final int slop;
        private int startX;
        private int startY;

        private DragToMove() {
            this.slop = ViewConfiguration.get(FloatingService.this).getScaledTouchSlop();
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case 0:
                    this.startX = FloatingService.this.bubbleLp.x;
                    this.startY = FloatingService.this.bubbleLp.y;
                    this.downX = e.getRawX();
                    this.downY = e.getRawY();
                    this.moved = false;
                    return true;
                case 1:
                    if (!this.moved) {
                        v.performClick();
                        FloatingService.this.expand();
                    } else {
                        FloatingService.this.snapBubble();
                    }
                    return true;
                case 2:
                    int dx = (int) (e.getRawX() - this.downX);
                    int dy = (int) (e.getRawY() - this.downY);
                    if (Math.abs(dx) > this.slop || Math.abs(dy) > this.slop) {
                        this.moved = true;
                    }
                    if (this.moved) {
                        FloatingService.this.bubbleLp.x = this.startX + dx;
                        FloatingService.this.bubbleLp.y = this.startY + dy;
                        FloatingService.this.wm.updateViewLayout(FloatingService.this.bubble, FloatingService.this.bubbleLp);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    public void snapBubble() {
        Point s = screen();
        int size = this.bubbleLp.width;
        this.bubbleLp.x = this.bubbleLp.x + (size / 2) < s.x / 2 ? dp(8.0f) : (s.x - size) - dp(8.0f);
        this.bubbleLp.y = Math.max(dp(24.0f), Math.min(this.bubbleLp.y, (s.y - size) - dp(48.0f)));
        if (this.bubbleShown) {
            this.wm.updateViewLayout(this.bubble, this.bubbleLp);
        }
    }

    private void buildPanel() {
        this.panel = new LinearLayout(this) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getKeyCode() == 4) {
                    if (event.getAction() == 1) {
                        FloatingService.this.collapse();
                    }
                    return true;
                }
                return super.dispatchKeyEvent(event);
            }
        };
        this.panel.setOrientation(1);
        this.panel.setBackground(Glass.clearGlass(this, Glass.GLASS_PANEL, 28.0f));
        this.panel.setPadding(dp(12.0f), dp(10.0f), dp(12.0f), dp(12.0f));
        this.panel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                boolean lambda$buildPanel$0;
                lambda$buildPanel$0 = FloatingService.this.lambda$buildPanel$0(view, motionEvent);
                return lambda$buildPanel$0;
            }
        });
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(0);
        header.setGravity(16);
        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(Glass.INK);
        title.setTextSize(17.0f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        TextView maker = new TextView(this);
        maker.setText("raee-industries.me");
        maker.setTextColor(Glass.INK_SOFT);
        maker.setTextSize(12.0f);
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(1);
        names.addView(title);
        names.addView(maker);
        header.addView(names, new LinearLayout.LayoutParams(0, -2, 1.0f));
        ImageButton full = Glass.iconButton(this, R.drawable.ic_open, "Open full screen", 40, Glass.GLASS_STRONG, Glass.INK);
        ImageButton shrink = Glass.iconButton(this, R.drawable.ic_minimize, "Shrink to bubble", 40, Glass.GLASS_STRONG, Glass.INK);
        ImageButton close = Glass.iconButton(this, R.drawable.ic_close, "Turn off the bubble", 40, Glass.GLASS_STRONG, Glass.INK);
        full.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                FloatingService.this.lambda$buildPanel$1(view);
            }
        });
        shrink.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                FloatingService.this.lambda$buildPanel$2(view);
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                FloatingService.this.lambda$buildPanel$3(view);
            }
        });
        header.addView(full, gap(40));
        header.addView(shrink, gap(40));
        header.addView(close, gap(40));
        this.panel.addView(header);
        LinearLayout addressRow = new LinearLayout(this);
        addressRow.setOrientation(0);
        addressRow.setGravity(16);
        this.address = new EditText(this);
        this.address.setSingleLine(true);
        this.address.setHint("Search or type a website");
        this.address.setHintTextColor(Glass.INK_SOFT);
        this.address.setTextColor(Glass.INK);
        this.address.setTextSize(15.0f);
        this.address.setInputType(17);
        this.address.setImeOptions(2);
        this.address.setSelectAllOnFocus(true);
        this.address.setBackground(Glass.panel(this, Glass.GLASS_FIELD, 14.0f));
        this.address.setPadding(dp(12.0f), 0, dp(10.0f), 0);
        this.address.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                boolean lambda$buildPanel$4;
                lambda$buildPanel$4 = FloatingService.this.lambda$buildPanel$4(textView, i, keyEvent);
                return lambda$buildPanel$4;
            }
        });
        addressRow.addView(this.address, new LinearLayout.LayoutParams(0, dp(44.0f), 1.0f));
        ImageButton go = Glass.iconButton(this, R.drawable.ic_go, "Go", 44, Glass.ACCENT, -1);
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                FloatingService.this.lambda$buildPanel$5(view);
            }
        });
        addressRow.addView(go, gap(44));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.topMargin = dp(8.0f);
        this.panel.addView(addressRow, rowLp);
        FrameLayout webBox = new FrameLayout(this);
        webBox.setBackground(Glass.panel(this, Glass.WEB_BOX, 18.0f));
        webBox.setClipToOutline(true);
        this.web = new WebView(this);
        this.web.setBackgroundColor(0);
        WebSupport.configure(this.web);
        this.web.setWebViewClient(new MiniClient());
        this.web.setWebChromeClient(new MiniChrome());
        this.web.addJavascriptInterface(new HomeBridge(), "SeaGlass");
        this.web.setDownloadListener(new DownloadListener() {
            @Override
            public final void onDownloadStart(String str, String str2, String str3, String str4, long j) {
                FloatingService.this.lambda$buildPanel$6(str, str2, str3, str4, j);
            }
        });
        webBox.addView(this.web, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        boxLp.topMargin = dp(10.0f);
        this.panel.addView(webBox, boxLp);
        this.web.loadUrl("https://appassets.androidplatform.net/assets/home.html?mini=1");
        this.status = new TextView(this);
        this.status.setTextColor(Glass.INK);
        this.status.setTextSize(14.0f);
        this.status.setTypeface(Typeface.DEFAULT_BOLD);
        this.status.setGravity(17);
        this.status.setMaxLines(5);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.topMargin = dp(8.0f);
        this.panel.addView(this.status, statusLp);
        this.mic = new ImageButton(this);
        this.mic.setImageResource(R.drawable.ic_mic);
        this.mic.setColorFilter(-1, PorterDuff.Mode.SRC_IN);
        this.mic.setBackground(Glass.oval(this, Glass.ACCENT, 4, Glass.GLASS_STRONG));
        this.mic.setPadding(dp(17.0f), dp(17.0f), dp(17.0f), dp(17.0f));
        this.mic.setScaleType(ImageView.ScaleType.FIT_CENTER);
        this.mic.setContentDescription("Tap and talk");
        this.mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                FloatingService.this.lambda$buildPanel$7(view);
            }
        });
        LinearLayout.LayoutParams micLp = new LinearLayout.LayoutParams(dp(60.0f), dp(60.0f));
        micLp.gravity = 1;
        micLp.topMargin = dp(6.0f);
        this.panel.addView(this.mic, micLp);
        this.panelLp = new WindowManager.LayoutParams(-2, -2, 2038, 262176, -3);
        this.panelLp.gravity = 17;
        this.panelLp.softInputMode = 32;
        sizePanel();
    }

    public boolean lambda$buildPanel$0(View v, MotionEvent e) {
        if (e.getAction() == 4) {
            collapse();
            return true;
        }
        return false;
    }

    public void lambda$buildPanel$1(View v) {
        openFull();
    }

    public void lambda$buildPanel$2(View v) {
        collapse();
    }

    public void lambda$buildPanel$3(View v) {
        stopSelf();
    }

    public boolean lambda$buildPanel$4(TextView v, int actionId, KeyEvent event) {
        if (actionId == 2 || (event != null && event.getKeyCode() == 66 && event.getAction() == 0)) {
            submitTyped();
            return true;
        }
        return false;
    }

    public void lambda$buildPanel$5(View v) {
        submitTyped();
    }

    public void lambda$buildPanel$6(String url, String ua, String disposition, String mime, long length) {
        WebSupport.openOutside(this, url, null);
    }

    public void lambda$buildPanel$7(View v) {
        onMicTapped();
    }

    private LinearLayout.LayoutParams gap(int sizeDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        lp.leftMargin = dp(6.0f);
        return lp;
    }

    private void sizePanel() {
        Point s = screen();
        this.panelLp.width = Math.min(s.x - dp(24.0f), dp(460.0f));
        this.panelLp.height = (int) (s.y * (s.y > s.x ? 0.66f : 0.86f));
    }

    public void expand() {
        if (this.expanded) {
            return;
        }
        try {
            if (this.bubbleShown) {
                this.wm.removeView(this.bubble);
            }
            this.bubbleShown = false;
            sizePanel();
            this.wm.addView(this.panel, this.panelLp);
            this.expanded = true;
            this.web.onResume();
        } catch (RuntimeException e) {
            stopSelf();
        }
    }

    public void collapse() {
        if (this.expanded) {
            this.voice.stopListening();
            InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
            if (imm != null) {
                imm.hideSoftInputFromWindow(this.address.getWindowToken(), 0);
            }
            try {
                this.wm.removeView(this.panel);
                this.expanded = false;
                this.wm.addView(this.bubble, this.bubbleLp);
                this.bubbleShown = true;
            } catch (RuntimeException e) {
                stopSelf();
            }
        }
    }

    private void openFull() {
        Intent i = new Intent(this, (Class<?>) MainActivity.class).addFlags(268435456);
        if (WebSupport.isWeb(this.currentUrl) && !WebSupport.isAppPage(this.currentUrl)) {
            i.putExtra("me.raeeindustries.seaglass.URL", this.currentUrl);
        }
        startActivity(i);
        collapse();
    }

    private void openFullOn(String screen) {
        Intent i = new Intent(this, (Class<?>) MainActivity.class).addFlags(268435456);
        i.putExtra("me.raeeindustries.seaglass.SHOW", screen);
        startActivity(i);
        collapse();
    }

    public void showStatus(String text) {
        this.status.setText(text == null ? "" : text);
    }

    private void updateMic() {
        int color = this.voice.isListening() ? -1948580 : this.speaking ? -9741338 : Glass.ACCENT;
        ((GradientDrawable) this.mic.getBackground()).setColor(color);
        this.mic.setContentDescription(this.voice.isListening() ? "Stop listening" : "Tap and talk");
    }

    private void submitTyped() {
        String text = this.address.getText().toString().trim();
        InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        if (imm != null) {
            imm.hideSoftInputFromWindow(this.address.getWindowToken(), 0);
        }
        this.address.clearFocus();
        if (text.isEmpty()) {
            return;
        }
        handleRequest(text, false);
    }

    public void handleRequest(String text, boolean speak) {
        this.lastFromVoice = speak;
        String reply = CommandRouter.run(text, this);
        if (reply != null) {
            showStatus(reply);
            if (speak) {
                this.voice.say(reply);
            }
        }
        this.lastFromVoice = false;
    }

    private void tell(String message) {
        showStatus(message);
        if (this.lastFromVoice) {
            this.voice.say(message);
        }
    }

    public void onMicTapped() {
        if (this.voice.isListening()) {
            this.voice.stopListening();
            return;
        }
        if (!Billing.get(this).canUseFull()) {
            showStatus("Voice commands are part of the full app. Tap the open button to unlock RAEE SeaGlass.");
        } else if (checkSelfPermission("android.permission.RECORD_AUDIO") != 0) {
            showStatus("Open RAEE SeaGlass and tap its mic once to allow the microphone.");
        } else {
            this.voice.listen();
        }
    }

    @Override
    public void openUrl(String url) {
        this.web.loadUrl(url);
    }

    @Override
    public void goBack() {
        if (this.web.canGoBack()) {
            this.web.goBack();
        }
    }

    @Override
    public void goForward() {
        if (this.web.canGoForward()) {
            this.web.goForward();
        }
    }

    @Override
    public void reload() {
        this.web.reload();
    }

    @Override
    public void goHome() {
        this.web.loadUrl("https://appassets.androidplatform.net/assets/home.html?mini=1");
    }

    @Override
    public void scroll(boolean down) {
        this.web.evaluateJavascript(WebSupport.scrollJs(down), null);
    }

    @Override
    public void stopSpeaking() {
        this.voice.stopSpeaking();
    }

    @Override
    public void newTab(boolean privateTab) {
        openFullOn(privateTab ? "private" : "newtab");
    }

    @Override
    public void closeTab() {
        tell("Tabs are in the full app. Tap the open button to see them.");
    }

    @Override
    public void nextTab() {
        openFullOn("tabs");
    }

    @Override
    public void showTabs() {
        openFullOn("tabs");
    }

    @Override
    public void showBookmarks() {
        openFullOn("bookmarks");
    }

    @Override
    public void showHistory() {
        openFullOn("history");
    }

    @Override
    public void clearHistory() {
        this.library.clearHistory();
    }

    @Override
    public void bookmarkPage() {
        if (WebSupport.isAppPage(this.currentUrl) || !WebSupport.isWeb(this.currentUrl)) {
            tell("Open a website first, then bookmark it.");
        } else {
            boolean added = this.library.toggleBookmark(this.web.getTitle(), this.currentUrl);
            tell(added ? "Bookmarked." : "Bookmark removed.");
        }
    }

    @Override
    public void ask(final String question) {
        if (!AiClient.isReady() || !Billing.PLUS_ENABLED) {
            openUrl(CommandRouter.searchUrl(question));
            tell("Here are results for " + question + ".");
            return;
        }
        Billing billing = Billing.get(this);
        if (!billing.hasPlus()) {
            openUrl(CommandRouter.searchUrl(question));
            tell("Spoken AI answers come with SeaGlass Plus. Here are search results.");
        } else {
            showStatus("Thinking…");
            final boolean speak = this.lastFromVoice;
            AiClient.ask(question, billing.plusToken(), new AiClient.Callback() {
                @Override
                public void onAnswer(String answer) {
                    FloatingService.this.showStatus(answer);
                    if (speak) {
                        FloatingService.this.voice.say(answer);
                    }
                }

                @Override
                public void onError(String message) {
                    FloatingService.this.openUrl(CommandRouter.searchUrl(question));
                    FloatingService.this.showStatus(message);
                }
            });
        }
    }

    @Override
    public void readPage() {
        if (!Billing.get(this).canUseFull()) {
            showStatus("Reading aloud is part of the full app. Tap the open button to unlock RAEE SeaGlass.");
        } else if (WebSupport.isAppPage(this.currentUrl)) {
            showStatus("Open a website first, then ask me to read the page.");
            this.voice.say("Open a website first, then ask me to read the page.");
        } else {
            this.web.evaluateJavascript("(function(){var a=document.querySelector('article')||document.querySelector('main')||document.body;var t=a?a.innerText:'';return (document.title||'')+'. '+t.replace(/\\s+/g,' ').slice(0,12000);})()", new ValueCallback() {
                @Override
                public final void onReceiveValue(Object obj) {
                    FloatingService.this.lambda$readPage$8((String) obj);
                }
            });
        }
    }

    public void lambda$readPage$8(String value) {
        String text = WebSupport.decodeJsString(value);
        if (text.trim().length() < 3) {
            showStatus("I couldn't find text to read on this page.");
        } else {
            showStatus("Reading this page. Say “stop” or tap the mic to stop.");
            this.voice.read(text);
        }
    }

    @Override
    public void onListeningChanged(boolean listening) {
        updateMic();
        if (listening) {
            showStatus("Listening…");
        }
    }

    @Override
    public void onPartial(String text) {
        showStatus(text);
    }

    @Override
    public void onHeard(String text) {
        showStatus("“" + text + "”");
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

    private class MiniClient extends WebViewClient {
        private MiniClient() {
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return FloatingService.this.assets.shouldInterceptRequest(request.getUrl());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (WebSupport.isWeb(url)) {
                return false;
            }
            WebSupport.openOutside(FloatingService.this, url, view);
            FloatingService.this.collapse();
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            FloatingService.this.currentUrl = url;
            if (!FloatingService.this.address.hasFocus()) {
                FloatingService.this.address.setText(WebSupport.isAppPage(url) ? "" : url);
            }
            view.setBackgroundColor(WebSupport.isAppPage(url) ? 0 : -1);
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            FloatingService.this.currentUrl = url;
            if (!FloatingService.this.address.hasFocus()) {
                FloatingService.this.address.setText(WebSupport.isAppPage(url) ? "" : url);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!WebSupport.isWeb(url) || WebSupport.isAppPage(url)) {
                return;
            }
            FloatingService.this.library.addHistory(view.getTitle(), url);
        }
    }

    private static class MiniChrome extends WebChromeClient {
        private MiniChrome() {
        }

        @Override
        public boolean onJsAlert(WebView v, String url, String message, JsResult result) {
            result.cancel();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView v, String url, String message, JsResult result) {
            result.cancel();
            return true;
        }

        @Override
        public boolean onJsPrompt(WebView v, String url, String message, String def, JsPromptResult result) {
            result.cancel();
            return true;
        }
    }

    class HomeBridge {
        private HomeBridge() {
        }

        @JavascriptInterface
        public void run(final String text) {
            FloatingService.this.handler.post(new Runnable() {
                @Override
                public final void run() {
                    FloatingService.HomeBridge.this.lambda$run$0(text);
                }
            });
        }

        public void lambda$run$0(String text) {
            if (WebSupport.isAppPage(FloatingService.this.currentUrl)) {
                FloatingService.this.handleRequest(text, true);
            }
        }

        @JavascriptInterface
        public void listen() {
            FloatingService.this.handler.post(new Runnable() {
                @Override
                public final void run() {
                    FloatingService.HomeBridge.this.lambda$listen$1();
                }
            });
        }

        public void lambda$listen$1() {
            if (WebSupport.isAppPage(FloatingService.this.currentUrl)) {
                FloatingService.this.onMicTapped();
            }
        }

        @JavascriptInterface
        public String look() {
            return Glass.midnight ? "midnight" : "teal";
        }

        @JavascriptInterface
        public String shortcuts() {
            return WebSupport.isAppPage(FloatingService.this.currentUrl) ? new Library(FloatingService.this).shortcutsJson() : "[]";
        }

        @JavascriptInterface
        public void removeShortcut(final String url) {
            FloatingService.this.handler.post(new Runnable() {
                @Override
                public final void run() {
                    FloatingService.HomeBridge.this.lambda$removeShortcut$2(url);
                }
            });
        }

        public void lambda$removeShortcut$2(String url) {
            if (WebSupport.isAppPage(FloatingService.this.currentUrl)) {
                new Library(FloatingService.this).removeShortcut(url);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        sizePanel();
        if (this.expanded) {
            this.wm.updateViewLayout(this.panel, this.panelLp);
        }
        snapBubble();
    }

    @Override
    public void onDestroy() {
        this.handler.removeCallbacksAndMessages(null);
        this.voice.destroy();
        try {
            if (this.expanded) {
                this.wm.removeView(this.panel);
            }
        } catch (RuntimeException e) {
        }
        try {
            if (this.bubbleShown) {
                this.wm.removeView(this.bubble);
            }
        } catch (RuntimeException e2) {
        }
        this.expanded = false;
        this.bubbleShown = false;
        this.web.destroy();
        super.onDestroy();
    }
}
