package me.raeeindustries.seaglass;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import org.json.JSONArray;

final class WebSupport {
    static final String APP_HOST = "appassets.androidplatform.net";
    static final String EXTRA_URL = "me.raeeindustries.seaglass.URL";
    static final String HOME_URL = "https://appassets.androidplatform.net/assets/home.html";
    static final String MINI_HOME_URL = "https://appassets.androidplatform.net/assets/home.html?mini=1";
    static final String READ_PAGE_JS = "(function(){var a=document.querySelector('article')||document.querySelector('main')||document.body;var t=a?a.innerText:'';return (document.title||'')+'. '+t.replace(/\\s+/g,' ').slice(0,12000);})()";

    private WebSupport() {
    }

    static void configure(WebView w) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(1);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, false);
    }

    static WebViewAssetLoader assetLoader(Context c) {
        return new WebViewAssetLoader.Builder().addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(c)).build();
    }

    static boolean isAppPage(String url) {
        return url != null && url.startsWith("https://appassets.androidplatform.net/");
    }

    static boolean isWeb(String url) {
        if (url == null) {
            return false;
        }
        String u = url.toLowerCase();
        return u.startsWith("http://") || u.startsWith("https://");
    }

    static void openOutside(Context c, String url, WebView fallbackView) {
        Intent intent;
        String fallback = null;
        try {
            if (url.startsWith("intent:")) {
                intent = Intent.parseUri(url, 1);
                intent.addCategory("android.intent.category.BROWSABLE");
                intent.setComponent(null);
                intent.setSelector(null);
                fallback = intent.getStringExtra("browser_fallback_url");
            } else {
                intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
            }
            intent.addFlags(268435456);
            c.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            if (fallback == null || !isWeb(fallback) || fallbackView == null) {
                Toast.makeText(c, "No app on this phone can open that link.", 0).show();
            } else {
                fallbackView.loadUrl(fallback);
            }
        } catch (Exception e2) {
            Toast.makeText(c, "That link can't be opened.", 0).show();
        }
    }

    static String decodeJsString(String value) {
        if (value == null || "null".equals(value)) {
            return "";
        }
        try {
            return new JSONArray("[" + value + "]").optString(0, "");
        } catch (Exception e) {
            return "";
        }
    }

    static String scrollJs(boolean down) {
        return "window.scrollBy({top:" + (down ? "" : "-") + "Math.round(window.innerHeight*0.8),behavior:'smooth'})";
    }
}
