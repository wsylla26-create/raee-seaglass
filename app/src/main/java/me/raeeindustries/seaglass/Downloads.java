package me.raeeindustries.seaglass;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.text.format.Formatter;
import android.webkit.URLUtil;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

final class Downloads {
    private static final int MAX_ITEMS = 100;
    private final Context context;
    private final SharedPreferences prefs;

    static final class Item {
        boolean done;
        final long id;
        final String name;
        String status = "";
        final long time;

        Item(long id, String name, long time) {
            this.id = id;
            this.name = name;
            this.time = time;
        }
    }

    Downloads(Context c) {
        this.context = c.getApplicationContext();
        this.prefs = c.getSharedPreferences("seaglass_downloads", 0);
    }

    static String fileName(String url, String disposition, String mime) {
        return URLUtil.guessFileName(url, disposition, mime);
    }

    String start(String url, String userAgent, String cookies, String disposition, String mime, boolean keepInList) {
        String str;
        if (url == null) {
            str = null;
        } else {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                String name = fileName(url, disposition, mime);
                try {
                    DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                    if (mime != null && !mime.isEmpty()) {
                        r.setMimeType(mime);
                    }
                    if (cookies != null && !cookies.isEmpty()) {
                        r.addRequestHeader("Cookie", cookies);
                    }
                    if (userAgent != null) {
                        r.addRequestHeader("User-Agent", userAgent);
                    }
                    r.setTitle(name);
                    r.setDescription("RAEE SeaGlass");
                    r.setNotificationVisibility(1);
                    r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                    DownloadManager dm = (DownloadManager) this.context.getSystemService("download");
                    long id = dm.enqueue(r);
                    if (keepInList) {
                        add(new Item(id, name, System.currentTimeMillis()));
                    }
                    return name;
                } catch (Exception e) {
                    Toast.makeText(this.context, "That file couldn't be downloaded.", 1).show();
                    return null;
                }
            }
            str = null;
        }
        Toast.makeText(this.context, "This website's download can't be saved by RAEE SeaGlass.", 1).show();
        return str;
    }

    List<Item> items() {
        List<Item> out = read();
        if (out.isEmpty()) {
            return out;
        }
        DownloadManager dm = (DownloadManager) this.context.getSystemService("download");
        long[] ids = new long[out.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = out.get(i).id;
        }
        try (Cursor c = dm.query(new DownloadManager.Query().setFilterById(ids))) {
            if (c != null) {
                int idCol = c.getColumnIndex("_id");
                int stCol = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int soFarCol = c.getColumnIndex("bytes_so_far");
                int totalCol = c.getColumnIndex("total_size");
                while (c.moveToNext()) {
                    long id = c.getLong(idCol);
                    for (Item it : out) {
                        if (it.id != id) continue;
                        int st = c.getInt(stCol);
                        long got = c.getLong(soFarCol);
                        long size = c.getLong(totalCol);
                        if (st == DownloadManager.STATUS_SUCCESSFUL) {
                            it.done = true;
                            it.status = "Done" + (size > 0 ? " · " + Formatter.formatShortFileSize(this.context, size) : "");
                        } else if (st == DownloadManager.STATUS_FAILED) {
                            it.status = "Failed";
                        } else if (st == DownloadManager.STATUS_PAUSED) {
                            it.status = "Paused, waiting for internet";
                        } else {
                            it.status = size > 0 ? "Downloading " + ((100 * got) / size) + "%" : "Downloading…";
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        for (Item it2 : out) {
            if (it2.status.isEmpty()) {
                it2.status = "Removed from the phone";
            }
        }
        return out;
    }

    void open(Item it) {
        DownloadManager dm = (DownloadManager) this.context.getSystemService("download");
        Uri uri = dm.getUriForDownloadedFile(it.id);
        if (uri == null) {
            Toast.makeText(this.context, "That file isn't on the phone anymore.", 0).show();
            return;
        }
        String mime = dm.getMimeTypeForDownloadedFile(it.id);
        Intent view = new Intent("android.intent.action.VIEW");
        view.setDataAndType(uri, mime != null ? mime : "*/*");
        view.addFlags(268435457);
        try {
            this.context.startActivity(view);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this.context, "No app on this phone can open " + it.name + ".", 1).show();
        }
    }

    void openFolder() {
        try {
            this.context.startActivity(new Intent("android.intent.action.VIEW_DOWNLOADS").addFlags(268435456));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this.context, "Open the Files app and look in Downloads.", 1).show();
        }
    }

    void remove(long id) {
        List<Item> list = read();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id == id) {
                list.remove(i);
            }
        }
        write(list);
    }

    void clearList() {
        this.prefs.edit().remove("items").apply();
    }

    private void add(Item it) {
        List<Item> list = read();
        list.add(0, it);
        while (list.size() > 100) {
            list.remove(list.size() - 1);
        }
        write(list);
    }

    private List<Item> read() {
        List<Item> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(this.prefs.getString("items", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new Item(o.optLong("id"), o.optString("n"), o.optLong("w")));
            }
        } catch (Exception e) {
        }
        return out;
    }

    private void write(List<Item> list) {
        try {
            JSONArray a = new JSONArray();
            for (Item it : list) {
                JSONObject o = new JSONObject();
                o.put("id", it.id);
                o.put("n", it.name);
                o.put("w", it.time);
                a.put(o);
            }
            this.prefs.edit().putString("items", a.toString()).apply();
        } catch (Exception e) {
        }
    }
}
