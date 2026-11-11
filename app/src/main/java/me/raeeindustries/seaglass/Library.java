package me.raeeindustries.seaglass;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

final class Library {
    private static final String[][] DEFAULT_SHORTCUTS = {new String[]{"YouTube", "https://www.youtube.com"}, new String[]{"Gmail", "https://mail.google.com"}, new String[]{"Maps", "https://maps.google.com"}, new String[]{"News", "https://news.google.com"}, new String[]{"Weather", "https://weather.com"}, new String[]{"Wikipedia", "https://www.wikipedia.org"}, new String[]{"Amazon", "https://www.amazon.com"}, new String[]{"RAEE Industries", "https://raee-industries.me"}};
    private static final int MAX_HISTORY = 300;
    static final int MAX_SHORTCUTS = 16;
    private final SharedPreferences prefs;

    static final class Entry {
        final long time;
        final String title;
        final String url;

        Entry(String title, String url, long time) {
            this.title = title;
            this.url = url;
            this.time = time;
        }
    }

    Library(Context c) {
        this.prefs = c.getSharedPreferences("seaglass_library", 0);
    }

    List<Entry> bookmarks() {
        return read("bookmarks");
    }

    boolean isBookmarked(String url) {
        if (url == null) {
            return false;
        }
        for (Entry e : bookmarks()) {
            if (e.url.equals(url)) {
                return true;
            }
        }
        return false;
    }

    boolean toggleBookmark(String title, String url) {
        List<Entry> list = bookmarks();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).url.equals(url)) {
                list.remove(i);
                write("bookmarks", list);
                return false;
            }
        }
        list.add(0, new Entry(clean(title, url), url, System.currentTimeMillis()));
        write("bookmarks", list);
        return true;
    }

    void removeBookmark(String url) {
        List<Entry> list = bookmarks();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).url.equals(url)) {
                list.remove(i);
            }
        }
        write("bookmarks", list);
    }

    List<Entry> history() {
        return read("history");
    }

    void addHistory(String title, String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        List<Entry> list = history();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).url.equals(url)) {
                list.remove(i);
            }
        }
        list.add(0, new Entry(clean(title, url), url, System.currentTimeMillis()));
        while (list.size() > MAX_HISTORY) {
            list.remove(list.size() - 1);
        }
        write("history", list);
    }

    void clearHistory() {
        this.prefs.edit().remove("history").apply();
    }

    List<Entry> shortcuts() {
        if (!this.prefs.contains("shortcuts")) {
            List<Entry> out = new ArrayList<>();
            for (String[] s : DEFAULT_SHORTCUTS) {
                out.add(new Entry(s[0], s[1], 0L));
            }
            return out;
        }
        return read("shortcuts");
    }

    boolean isShortcut(String url) {
        if (url == null) {
            return false;
        }
        for (Entry e : shortcuts()) {
            if (sameSite(e.url, url)) {
                return true;
            }
        }
        return false;
    }

    Boolean toggleShortcut(String title, String url) {
        List<Entry> list = shortcuts();
        for (int i = 0; i < list.size(); i++) {
            if (sameSite(list.get(i).url, url)) {
                list.remove(i);
                write("shortcuts", list);
                return false;
            }
        }
        int i2 = list.size();
        if (i2 >= 16) {
            return null;
        }
        String name = clean(title, url);
        if (name.length() > 24) {
            name = name.substring(0, 24).trim();
        }
        list.add(new Entry(name, url, System.currentTimeMillis()));
        write("shortcuts", list);
        return true;
    }

    void removeShortcut(String url) {
        List<Entry> list = shortcuts();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).url.equals(url)) {
                list.remove(i);
            }
        }
        write("shortcuts", list);
    }

    String shortcutsJson() {
        JSONArray a = new JSONArray();
        for (Entry e : shortcuts()) {
            JSONArray item = new JSONArray();
            item.put(e.title);
            item.put(e.url);
            a.put(item);
        }
        return a.toString();
    }

    private static boolean sameSite(String a, String b) {
        return trimUrl(a).equals(trimUrl(b));
    }

    private static String trimUrl(String u) {
        return u == null ? "" : u.replaceAll("^https?://(www\\.)?", "").replaceAll("/+$", "");
    }

    // Numbered saved sites: the user says "save this site as number 1", later "open number 1".
    Entry slot(int number) {
        try {
            JSONObject o = new JSONObject(this.prefs.getString("slots", "{}")).optJSONObject(String.valueOf(number));
            if (o == null || o.optString("u").isEmpty()) {
                return null;
            }
            return new Entry(o.optString("t"), o.optString("u"), o.optLong("w"));
        } catch (Exception e) {
            return null;
        }
    }

    void setSlot(int number, String title, String url) {
        try {
            JSONObject all = new JSONObject(this.prefs.getString("slots", "{}"));
            JSONObject o = new JSONObject();
            o.put("t", clean(title, url));
            o.put("u", url);
            o.put("w", System.currentTimeMillis());
            all.put(String.valueOf(number), o);
            this.prefs.edit().putString("slots", all.toString()).apply();
        } catch (Exception e) {
        }
    }

    boolean removeSlot(int number) {
        try {
            JSONObject all = new JSONObject(this.prefs.getString("slots", "{}"));
            if (all.remove(String.valueOf(number)) == null) {
                return false;
            }
            this.prefs.edit().putString("slots", all.toString()).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Saved sites in number order.
    java.util.TreeMap<Integer, Entry> slots() {
        java.util.TreeMap<Integer, Entry> out = new java.util.TreeMap<>();
        for (int i = 1; i <= 99; i++) {
            Entry e = slot(i);
            if (e != null) {
                out.put(i, e);
            }
        }
        return out;
    }

    static String slotName(Entry e) {
        String host = trimUrl(e.url).replaceAll("/.*$", "");
        return (e.title == null || e.title.isEmpty() || e.title.equals(e.url)) ? host : e.title;
    }

    void saveTabs(List<String> urls, int current) {
        try {
            JSONObject o = new JSONObject();
            o.put("urls", new JSONArray((Collection) urls));
            o.put("current", current);
            this.prefs.edit().putString("tabs", o.toString()).apply();
        } catch (Exception e) {
        }
    }

    List<String> savedTabUrls() {
        List<String> out = new ArrayList<>();
        try {
            JSONArray a = new JSONObject(this.prefs.getString("tabs", "{}")).optJSONArray("urls");
            if (a != null) {
                for (int i = 0; i < a.length(); i++) {
                    out.add(a.optString(i, ""));
                }
            }
        } catch (Exception e) {
        }
        return out;
    }

    int savedCurrentTab() {
        try {
            return new JSONObject(this.prefs.getString("tabs", "{}")).optInt("current", 0);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String clean(String title, String url) {
        return (title == null || title.trim().isEmpty()) ? url : title.trim();
    }

    private List<Entry> read(String key) {
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(this.prefs.getString(key, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new Entry(o.optString("t"), o.optString("u"), o.optLong("w")));
            }
        } catch (Exception e) {
        }
        return out;
    }

    private void write(String key, List<Entry> list) {
        try {
            JSONArray a = new JSONArray();
            for (Entry e : list) {
                JSONObject o = new JSONObject();
                o.put("t", e.title);
                o.put("u", e.url);
                o.put("w", e.time);
                a.put(o);
            }
            this.prefs.edit().putString(key, a.toString()).apply();
        } catch (Exception e2) {
        }
    }
}
