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

    // Spoken tab numbers: "1", "one", "first", and what speech often hears ("to", "too", "for", "won").
    private static final String NUM = "(\\d{1,2}|one|won|first|two|to|too|second|three|third|four|for|fourth|five|fifth|six|sixth|seven|seventh|eight|eighth|nine|ninth|ten|tenth)";
    private static final String TAB_NO = "(?:(?:the )?(?:tab|site|page|website|number|slot)(?: number)? |number )" + NUM;
    private static final Pattern CLOSE_N = Pattern.compile("^(?:close|shut|remove|delete|get rid of) (?:" + TAB_NO + "|(?:the )?" + NUM + " tab)$");
    private static final Pattern SAVE_N = Pattern.compile("^(?:save|bookmark|keep|star)(?: the)?(?: site| page| website| tab)?(?: (?:at|on|in|from|of|to))? (?:" + TAB_NO + "|(?:the )?" + NUM + " tab)$");
    private static final Pattern SWITCH_N = Pattern.compile("^(?:(?:go|switch|change|move|jump|take me) (?:to|back to) |open |show(?: me)? |)(?:" + TAB_NO + "|(?:the )?" + NUM + " tab)$");
    private static final Pattern NEW_TAB = Pattern.compile("^(?:open|start|add|make|create|give me|get|i need|i want)?(?: me)? ?(?:a |another |one more |an extra )?(?:new |fresh |blank |empty |another |second |extra )?tab(?: for me)?$");
    private static final Pattern NEW_TAB_THEN = Pattern.compile("^(?:open |start |add )?(?:a |another )?(?:new |another |second )tab (?:and |then |to |with |for |on |at )(?:(?:go to|open|visit|load|show me|take me to) )?(.+)$");
    private static final Pattern IN_NEW_TAB = Pattern.compile("^(?:open |go to |visit |load |show me |take me to )?(.+?) (?:in|on|into) (?:a |another |the )?(?:new|another|separate|second|different) tab$");
    private static final Pattern POLITE = Pattern.compile("^(?:(?:hey|hi|ok|okay) )?(?:(?:nfurian|en furian|sea ?glass)[ ,]+)?(?:(?:can|could|would|will) you (?:please )?|please |i(?:'d| would) like (?:you )?to |i want (?:you )?to |let's |go ahead and )+");

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

        default int tabCount() {
            return 0;
        }

        default String switchToTab(int number) {
            showTabs();
            return null;
        }

        default String closeTabNumber(int number) {
            return "Tabs are in the full app. Tap the open button to see them.";
        }

        default String saveTabNumber(int number) {
            return "Tabs are in the full app. Tap the open button to see them.";
        }

        default String closeAllTabs() {
            return "Tabs are in the full app. Tap the open button to see them.";
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
        String polite = POLITE.matcher(t).replaceFirst("").replaceAll("[ ,]+(?:please|for me|now|thanks|thank you)$", "").trim();
        if (!polite.isEmpty() && !polite.equals(t) && !QUESTION.matcher(t).matches()) {
            t = polite;
        } else if (!polite.isEmpty() && polite.matches(".*\\btabs?\\b.*")) {
            t = polite;
        }
        String tabReply = tabCommand(t, b);
        if (tabReply != null) {
            return tabReply.isEmpty() ? null : tabReply;
        }
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
        if (NEW_TAB.matcher(t).matches() && t.contains("tab")) {
            return openNewTab(b);
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

    // Tab numbers count from the left: tab 1 is the first tab, tab 2 the next one, and so on.
    // Returns null when the words aren't a tab command, "" when handled with nothing to say.
    private static String tabCommand(String t, Browser b) {
        if (!t.matches(".*\\b(tabs?|site|page|website|number|slot)\\b.*")) {
            return null;
        }
        if (NEW_TAB.matcher(t).matches()) {
            return openNewTab(b);
        }
        if (t.matches("close (all|every|all the|all my|all of my|all of the) tabs?|close everything")) {
            return b.closeAllTabs();
        }
        Matcher m = NEW_TAB_THEN.matcher(t);
        if (m.matches()) {
            return newTabThen(m.group(1), b);
        }
        Matcher m2 = IN_NEW_TAB.matcher(t);
        if (m2.matches() && !m2.group(1).matches("(a |the )?(new |private )?tab")) {
            return newTabThen(m2.group(1), b);
        }
        Matcher m3 = CLOSE_N.matcher(t);
        if (m3.matches()) {
            int n = tabNumber(m3.group(1) != null ? m3.group(1) : m3.group(2));
            return n > 0 ? orEmpty(b.closeTabNumber(n)) : null;
        }
        Matcher m4 = SAVE_N.matcher(t);
        if (m4.matches()) {
            int n = tabNumber(m4.group(1) != null ? m4.group(1) : m4.group(2));
            return n > 0 ? orEmpty(b.saveTabNumber(n)) : null;
        }
        Matcher m5 = SWITCH_N.matcher(t);
        if (m5.matches()) {
            int n = tabNumber(m5.group(1) != null ? m5.group(1) : m5.group(2));
            return n > 0 ? orEmpty(b.switchToTab(n)) : null;
        }
        return null;
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String openNewTab(Browser b) {
        b.newTab(false);
        int n = b.tabCount();
        return n > 0 ? "New tab. This is tab " + n + "." : "New tab.";
    }

    private static String newTabThen(String what, Browser b) {
        String w = what.trim().replaceAll("^(the|my) ", "");
        b.newTab(false);
        String cmd = w.matches("^(search|google|look up|find|play|watch|directions to|navigate to) .+") ? w : "open " + w;
        String reply = run(cmd, b);
        int n = b.tabCount();
        if (n <= 0) {
            return reply;
        }
        return (reply == null ? "" : reply + " ") + "That's tab " + n + ".";
    }

    static int tabNumber(String w) {
        if (w == null) {
            return -1;
        }
        String s = w.trim();
        if (s.matches("\\d{1,2}")) {
            return Integer.parseInt(s);
        }
        switch (s) {
            case "one": case "won": case "first": return 1;
            case "two": case "to": case "too": case "second": return 2;
            case "three": case "third": return 3;
            case "four": case "for": case "fourth": return 4;
            case "five": case "fifth": return 5;
            case "six": case "sixth": return 6;
            case "seven": case "seventh": return 7;
            case "eight": case "eighth": return 8;
            case "nine": case "ninth": return 9;
            case "ten": case "tenth": return 10;
            default: return -1;
        }
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
