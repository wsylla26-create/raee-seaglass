package me.raeeindustries.seaglass;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebSettings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

final class PageTools {
    static final Map<String, String> LANGUAGES = new LinkedHashMap();
    static final String READER_JS = "(function(){function bad(e){for(;e&&e!==document.body;e=e.parentElement){var n=e.tagName;if(n=='NAV'||n=='FOOTER'||n=='ASIDE'||n=='FORM'||n=='BUTTON')return true;var r=e.getAttribute('role');if(r=='navigation'||r=='complementary'||r=='contentinfo'||r=='banner')return true;var c=(e.className&&e.className.baseVal===undefined?e.className:'')+' '+(e.id||'');if(/\\b(comment|share|social|related|promo|advert|sidebar|newsletter|cookie|footer|menu)/i.test(c))return true;}return false;}var best=null,top=0,m=new Map();document.querySelectorAll('p').forEach(function(p){var l=(p.innerText||'').trim().length;if(l<40||bad(p))return;var par=p.parentElement;if(!par)return;var s=(m.get(par)||0)+l;m.set(par,s);if(s>top){top=s;best=par;}});var root=document.querySelector('article');if(!root||(root.innerText||'').length<500)root=best||document.querySelector('main')||document.body;if(best&&root!==best&&!root.contains(best))root=best;var out=[],total=0,seen=new Set();root.querySelectorAll('h1,h2,h3,h4,p,li,blockquote,pre').forEach(function(e){if(total>60000||bad(e))return;var n=e.tagName;if((n=='LI'||n=='BLOCKQUOTE')&&e.querySelector('p'))return;for(var a=e.parentElement;a&&a!==root;a=a.parentElement){if(seen.has(a))return;}var t=(e.innerText||'').replace(/\\s+/g,' ').trim();if(t.length<2)return;seen.add(e);total+=t.length;out.push([n.charAt(0)=='H'?'h':n=='LI'?'li':n=='BLOCKQUOTE'?'q':'p',t]);});var h=document.querySelector('h1');return JSON.stringify({t:(h&&h.innerText.trim())||document.title||'',b:out});})()";
    private static String desktopUa;

    private PageTools() {
    }

    static final class Article {
        String title = "";
        final List<String[]> blocks = new ArrayList();

        Article() {
        }

        int length() {
            int n = 0;
            for (String[] b : this.blocks) {
                n += b[1].length();
            }
            return n;
        }

        String plainText() {
            StringBuilder s = new StringBuilder(this.title).append(". ");
            for (String[] b : this.blocks) {
                s.append(b[1]).append(b[0].equals("h") ? ". " : " ");
            }
            return s.toString();
        }
    }

    static Article parseArticle(String jsResult) {
        Article a = new Article();
        try {
            JSONObject o = new JSONObject(WebSupport.decodeJsString(jsResult));
            a.title = o.optString("t", "");
            JSONArray b = o.optJSONArray("b");
            if (b != null) {
                for (int i = 0; i < b.length(); i++) {
                    JSONArray item = b.optJSONArray(i);
                    if (item != null && item.length() == 2 && (!a.blocks.isEmpty() || !"h".equals(item.optString(0)) || !item.optString(1).equals(a.title))) {
                        a.blocks.add(new String[]{item.optString(0), item.optString(1)});
                    }
                }
            }
        } catch (Exception e) {
        }
        return a;
    }

    static String darkJs(boolean on) {
        return "(function(on){var id='__seaglass_dark';var s=document.getElementById(id);if(!on){if(s)s.remove();return;}if(s)return;function dark(el){if(!el)return false;var c=getComputedStyle(el).backgroundColor;var m=c&&c.match(/[\\d.]+/g);if(!m||m.length<3||(m.length>3&&+m[3]===0))return null;return (0.299*m[0]+0.587*m[1]+0.114*m[2])<110;}var d=dark(document.body);if(d===null)d=dark(document.documentElement);if(d)return;s=document.createElement('style');s.id=id;s.textContent='html{filter:invert(0.92) hue-rotate(180deg)!important;background:#fff!important}img,video,picture,canvas,iframe,embed,object,[style*=\"background-image\"]{filter:invert(1) hue-rotate(180deg)!important}';(document.head||document.documentElement).appendChild(s);})(" + on + ")";
    }

    static String desktopUserAgent(Context c) {
        if (desktopUa == null) {
            String ua = WebSettings.getDefaultUserAgent(c);
            desktopUa = ua.replaceAll("\\(Linux; Android[^)]*\\)", "(X11; Linux x86_64)").replace(" Mobile", "").replace("; wv", "").replaceAll("Version/\\d+(\\.\\d+)* ", "");
        }
        String ua2 = desktopUa;
        return ua2;
    }

    static {
        LANGUAGES.put("english", "en");
        LANGUAGES.put("spanish", "es");
        LANGUAGES.put("french", "fr");
        LANGUAGES.put("haitian creole", "ht");
        LANGUAGES.put("portuguese", "pt");
        LANGUAGES.put("arabic", "ar");
        LANGUAGES.put("chinese", "zh-CN");
        LANGUAGES.put("hindi", "hi");
        LANGUAGES.put("bengali", "bn");
        LANGUAGES.put("russian", "ru");
        LANGUAGES.put("german", "de");
        LANGUAGES.put("italian", "it");
        LANGUAGES.put("japanese", "ja");
        LANGUAGES.put("korean", "ko");
        LANGUAGES.put("vietnamese", "vi");
        LANGUAGES.put("tagalog", "tl");
        LANGUAGES.put("urdu", "ur");
        LANGUAGES.put("swahili", "sw");
        LANGUAGES.put("yoruba", "yo");
        LANGUAGES.put("igbo", "ig");
        LANGUAGES.put("hausa", "ha");
        LANGUAGES.put("wolof", "wo");
        LANGUAGES.put("amharic", "am");
        LANGUAGES.put("somali", "so");
        LANGUAGES.put("turkish", "tr");
        LANGUAGES.put("polish", "pl");
        LANGUAGES.put("ukrainian", "uk");
        LANGUAGES.put("dutch", "nl");
        LANGUAGES.put("greek", "el");
        LANGUAGES.put("hebrew", "iw");
    }

    static String languageCode(String spoken) {
        if (spoken == null) {
            return null;
        }
        String s = spoken.toLowerCase(Locale.ROOT).trim().replaceAll("^the ", "");
        if (s.equals("creole")) {
            s = "haitian creole";
        }
        if (s.equals("mandarin")) {
            s = "chinese";
        }
        if (s.equals("filipino")) {
            s = "tagalog";
        }
        return LANGUAGES.get(s);
    }

    static String phoneLanguageName() {
        return Locale.getDefault().getDisplayLanguage(Locale.getDefault());
    }

    static String phoneLanguageCode() {
        String lang = Locale.getDefault().getLanguage();
        return lang.equals("zh") ? "zh-CN" : lang.equals("he") ? "iw" : lang;
    }

    static boolean isTranslated(String url) {
        return url != null && (url.contains(".translate.goog") || url.startsWith("https://translate.google."));
    }

    static String translateUrl(String pageUrl, String languageCode) {
        return "https://translate.google.com/translate?sl=auto&tl=" + Uri.encode(languageCode) + "&u=" + Uri.encode(pageUrl);
    }
}
