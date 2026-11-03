package me.raeeindustries.seaglass;

import android.net.Uri;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CommandRouter {
    private static final Pattern OPEN;
    private static final Pattern PLACE;
    private static final Pattern PLAY;
    private static final Pattern QUESTION;
    private static final Pattern SEARCH;
    private static final Pattern URLISH;
    private static final Pattern FIND = Pattern.compile("^(?:find|search for|look for|highlight) (.+) (?:on|in) (?:this|the) page$|^(?:find|search) (?:on|in) (?:this |the )?page(?: for)? (.+)$");
    private static final Pattern TRANSLATE = Pattern.compile("^translate(?: this| the)?(?: page| site| website| article)?(?: (?:to|into|in) (.+))?$");
    private static final Map<String, String> SITES = new HashMap();

    interface Browser {
        void ask(String str);

        void bookmarkPage();

        void clearHistory();

        void closeTab();

        void goBack();

        void goForward();

        void goHome();

        void newTab(boolean z);

        void nextTab();

        void openUrl(String str);

        void readPage();

        void reload();

        void scroll(boolean z);

        void showBookmarks();

        void showHistory();

        void showTabs();

        void stopSpeaking();

        default String tool(String name, String arg) {
            return "Open the full browser to do that.";
        }
    }

    static {
        SITES.put("youtube", "www.youtube.com");
        SITES.put("facebook", "www.facebook.com");
        SITES.put("google", "www.google.com");
        SITES.put("gmail", "mail.google.com");
        SITES.put("email", "mail.google.com");
        SITES.put("maps", "maps.google.com");
        SITES.put("google maps", "maps.google.com");
        SITES.put("amazon", "www.amazon.com");
        SITES.put("ebay", "www.ebay.com");
        SITES.put("weather", "weather.com");
        SITES.put("news", "news.google.com");
        SITES.put("instagram", "www.instagram.com");
        SITES.put("tiktok", "www.tiktok.com");
        SITES.put("whatsapp", "web.whatsapp.com");
        SITES.put("wikipedia", "www.wikipedia.org");
        SITES.put("netflix", "www.netflix.com");
        SITES.put("reddit", "www.reddit.com");
        SITES.put("x", "x.com");
        SITES.put("twitter", "x.com");
        SITES.put("linkedin", "www.linkedin.com");
        SITES.put("shopify", "www.shopify.com");
        SITES.put("raee", "raee-industries.me");
        SITES.put("raee industries", "raee-industries.me");
        SITES.put("ray", "raee-industries.me");
        SITES.put("ray industries", "raee-industries.me");
        SITES.put("rae industries", "raee-industries.me");
        OPEN = Pattern.compile("^(?:open|go to|visit|launch|take me to|show me) (.+)$");
        PLAY = Pattern.compile("^(?:play|watch) (.+)$");
        PLACE = Pattern.compile("^(?:directions to|navigate to|where is|find) (.+)$");
        SEARCH = Pattern.compile("^(?:search(?: the web)?(?: for)?|google|look up) (.+)$");
        QUESTION = Pattern.compile("^(what|what's|who|who's|whom|whose|why|how|how's|when|which|is|are|was|were|can|could|does|do|did|should|will|would|tell me|explain|define|describe)\\b.*");
        URLISH = Pattern.compile("^(https?://)?[\\w-]+(\\.[\\w-]+)+(:\\d+)?(/\\S*)?$", 2);
    }

    private CommandRouter() {
    }

    static String run(String raw, Browser b) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        String t = text.toLowerCase(Locale.ROOT).replaceAll("[.!?,]+$", "").trim();
        if (t.matches("stop|quiet|be quiet|cancel|shush|stop talking|stop reading|pause")) {
            b.stopSpeaking();
            return null;
        }
        if (t.matches("(go )?back|previous page")) {
            b.goBack();
            return "Going back.";
        }
        if (t.matches("(go )?forward|next page")) {
            b.goForward();
            return "Going forward.";
        }
        if (t.matches("(reload|refresh)( the)?( page)?")) {
            b.reload();
            return "Reloading.";
        }
        if (t.matches("(go )?home|home page|start page")) {
            b.goHome();
            return "Home.";
        }
        if (t.matches("(open |new |start )?(a )?(new )?private tab|(open )?(an )?incognito( tab| mode)?|go private|private mode")) {
            b.newTab(true);
            return "Private tab opened.";
        }
        if (t.matches("(open |start )?(a )?new tab")) {
            b.newTab(false);
            return "New tab.";
        }
        if (t.matches("close (this |the |my )?tab")) {
            b.closeTab();
            return "Tab closed.";
        }
        if (t.matches("(next|switch|change) tabs?|switch to (the )?next tab|go to (the )?next tab")) {
            b.nextTab();
            return null;
        }
        if (t.matches("(show |open |list |see )?(me )?(my |all )?(open )?tabs")) {
            b.showTabs();
            return null;
        }
        if (t.matches("bookmark( this)?( page| site)?|save (this )?(page|site)|add (a )?bookmark|star (this )?(page)?")) {
            b.bookmarkPage();
            return null;
        }
        if (t.matches("(show |open |see )?(me )?(my )?(bookmarks|saved pages)")) {
            b.showBookmarks();
            return null;
        }
        if (t.matches("clear (my )?(browsing )?history|delete (my )?(browsing )?history|erase (my )?history")) {
            b.clearHistory();
            return "History cleared.";
        }
        if (t.matches("(show |open |see )?(me )?(my )?(browsing )?history")) {
            b.showHistory();
            return null;
        }
        if (t.startsWith("read") && (t.equals("read") || t.equals("read it") || t.contains("page") || t.contains("this") || t.contains("article") || t.contains("aloud") || t.contains("out loud"))) {
            b.readPage();
            return null;
        }
        Matcher m = FIND.matcher(t);
        if (m.matches()) {
            return b.tool("find", m.group(1) != null ? m.group(1) : m.group(2));
        }
        if (t.matches("find (on|in) (this |the )?page|find in page|search (this|the) page")) {
            return b.tool("find", null);
        }
        if (t.matches("(show |open |see )?(me )?(my )?downloads|(show |open )?(my )?downloaded files")) {
            return b.tool("downloads", null);
        }
        if (t.matches("share( this| the)?( page| link| site| website| article)?|send (this |the )?(page|link) to .*")) {
            return b.tool("share", null);
        }
        if (t.matches("(open |turn on |switch to |use |show )?(the )?reader( mode| view)?|simplify( this)?( page)?|clean (this )?page")) {
            return b.tool("reader", null);
        }
        Matcher m2 = TRANSLATE.matcher(t);
        if (m2.matches()) {
            return b.tool("translate", m2.group(1));
        }
        if (t.matches("(show |open |use |switch to |request |load )?(the )?(desktop|computer|full) (site|version|mode|website)")) {
            return b.tool("desktop", "on");
        }
        if (t.matches("(show |use |switch to |go back to |back to |load )?(the )?(mobile|phone) (site|version|mode|website)")) {
            return b.tool("desktop", "off");
        }
        if (t.matches("(make )?(the )?(text|words|font) (bigger|larger)|(bigger|larger) (text|words|font)|zoom in|increase (the )?text( size)?")) {
            return b.tool("text", "bigger");
        }
        if (t.matches("(make )?(the )?(text|words|font) smaller|smaller (text|words|font)|zoom out|decrease (the )?text( size)?")) {
            return b.tool("text", "smaller");
        }
        if (t.matches("(normal|reset|default|regular) (text|font)( size)?|reset zoom")) {
            return b.tool("text", "normal");
        }
        if (t.matches("(turn off|disable|stop) dark (mode|websites|pages|theme)|(turn on |use |switch to )?light (mode|websites|pages|theme)|dark (mode )?off")) {
            return b.tool("dark", "off");
        }
        if (t.matches("(turn on |use |switch to |enable )?dark (mode|websites|pages|theme)( on)?|night mode|make (it|this|the page|websites) dark")) {
            return b.tool("dark", "on");
        }
        if (t.matches("(turn off|disable|stop|pause) (the )?ad ?block(er|ing)?|(allow|show) ads|ad ?block(er)? off")) {
            return b.tool("adblock", "off");
        }
        if (t.matches("(turn on |enable |start |use )?(the )?ad ?block(er|ing)?( on)?|block (the )?ads|stop (the )?ads|no ads")) {
            return b.tool("adblock", "on");
        }
        if (t.matches("(switch to |use |turn on |change to )?(the )?midnight( glass)?( look| theme| mode| style)?")) {
            return b.tool("look", "midnight");
        }
        if (t.matches("(switch to |use |turn on |change to |back to )?(the )?teal( glass)?( look| theme| mode| style)?")) {
            return b.tool("look", "teal");
        }
        if (t.matches("clear (my )?(browsing )?data|clear (my |the )?cookies|clear (the )?cache|delete (my )?cookies")) {
            return b.tool("cleardata", null);
        }
        if (t.matches("(add|pin|save) (this )?(page |site )?to (my )?(home|start)( screen| page)?( shortcuts)?|add (a )?shortcut")) {
            return b.tool("shortcut", null);
        }
        if (t.matches("scroll( down)?|page down|go down|down")) {
            b.scroll(true);
            return null;
        }
        if (t.matches("scroll up|page up|go up|up|top")) {
            b.scroll(false);
            return null;
        }
        if (t.matches(".*\\b(what time is it|what's the time|what is the time|current time)\\b.*")) {
            return "It's " + DateFormat.getTimeInstance(3).format(new Date()) + ".";
        }
        if (t.matches(".*\\b(what's the date|what is the date|what day is it|what is today|what's today|today's date)\\b.*")) {
            return "Today is " + DateFormat.getDateInstance(0).format(new Date()) + ".";
        }
        Matcher m3 = OPEN.matcher(t);
        if (m3.matches()) {
            String target = m3.group(1).replaceAll("^(the|my) ", "");
            if (SITES.containsKey(target) || looksLikeUrl(target) || target.split("\\s+").length <= 2) {
                String host = hostFor(target);
                b.openUrl("https://" + host);
                return "Opening " + host.replace("www.", "") + ".";
            }
            return search(target, b);
        }
        if (looksLikeUrl(text)) {
            b.openUrl(normalizeUrl(text));
            return null;
        }
        Matcher m4 = PLAY.matcher(t);
        if (m4.matches()) {
            b.openUrl("https://www.youtube.com/results?search_query=" + Uri.encode(m4.group(1)));
            return "Here are videos for " + m4.group(1) + ".";
        }
        Matcher m5 = PLACE.matcher(t);
        if (m5.matches()) {
            b.openUrl("https://www.google.com/maps/search/" + Uri.encode(m5.group(1)));
            return "Here's " + m5.group(1) + " on the map.";
        }
        Matcher m6 = SEARCH.matcher(t);
        if (m6.matches()) {
            return search(m6.group(1), b);
        }
        if (text.endsWith("?") || QUESTION.matcher(t).matches()) {
            b.ask(text);
            return null;
        }
        return search(text, b);
    }

    static boolean looksLikeUrl(String s) {
        return (s == null || s.trim().contains(" ") || !URLISH.matcher(s.trim()).matches()) ? false : true;
    }

    static String normalizeUrl(String s) {
        String t = s.trim();
        String lower = t.toLowerCase(Locale.ROOT);
        return (lower.startsWith("http://") || lower.startsWith("https://")) ? t : "https://" + t;
    }

    static String searchUrl(String q) {
        return "https://www.google.com/search?q=" + Uri.encode(q);
    }

    private static String search(String q, Browser b) {
        b.openUrl(searchUrl(q));
        return "Here are results for " + q + ".";
    }

    private static String hostFor(String target) {
        String t = target.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+(website|site|page|app)$", "").trim();
        if (SITES.containsKey(t)) {
            return SITES.get(t);
        }
        String t2 = t.replaceAll("\\s+dot\\s+", ".").replaceAll("\\s+", "");
        if (!t2.contains(".")) {
            t2 = t2 + ".com";
        }
        return t2.replaceAll("^https?://", "").replaceAll("/$", "");
    }
}
