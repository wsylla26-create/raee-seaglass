package me.raeeindustries.seaglass;

import android.content.Context;
import android.webkit.WebResourceResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class AdBlocker {
    private static volatile Set<String> hosts;

    private AdBlocker() {
    }

    static void load(Context c) {
        if (hosts != null) {
            return;
        }
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public final void run() {
                AdBlocker.lambda$load$0(app);
            }
        }, "SeaGlassAdList").start();
    }

    static void lambda$load$0(Context app) {
        Set<String> set = new HashSet<>();
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(app.getAssets().open("adblock.txt")));
            while (true) {
                try {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    String line2 = line.trim().toLowerCase(Locale.ROOT);
                    if (!line2.isEmpty() && !line2.startsWith("#")) {
                        set.add(line2);
                    }
                } finally {
                }
            }
            r.close();
        } catch (Exception e) {
        }
        hosts = set;
    }

    static boolean blocks(String host, String pageHost) {
        Set<String> list = hosts;
        if (list == null || host == null) {
            return false;
        }
        String h = host.toLowerCase(Locale.ROOT);
        if (pageHost != null) {
            String p = pageHost.toLowerCase(Locale.ROOT);
            if (h.equals(p) || h.endsWith("." + p) || p.endsWith("." + h)) {
                return false;
            }
        }
        while (!list.contains(h)) {
            int dot = h.indexOf(46);
            if (dot < 0) {
                return false;
            }
            h = h.substring(dot + 1);
            if (h.indexOf(46) < 0) {
                return false;
            }
        }
        return true;
    }

    static WebResourceResponse empty() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
    }
}
