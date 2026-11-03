package me.raeeindustries.seaglass;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.json.JSONObject;

final class Weather {
    private static final long FRESH_MS = 1800000;
    private static boolean busy;

    private Weather() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("seaglass_weather", 0);
    }

    static boolean enabled(Context c) {
        return prefs(c).getBoolean("on", false);
    }

    static void setEnabled(Context c, boolean on) {
        SharedPreferences.Editor e = prefs(c).edit().putBoolean("on", on);
        if (!on) {
            e.remove("data");
        }
        e.apply();
    }

    static boolean hasPermission(Context c) {
        return c.checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == 0 || c.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0;
    }

    static String forPage(Context c) {
        if (!enabled(c) || !hasPermission(c)) {
            return "";
        }
        return prefs(c).getString("data", "{}");
    }

    static boolean stale(Context c) {
        return System.currentTimeMillis() - prefs(c).getLong("time", 0L) > FRESH_MS;
    }

    static void refreshIfStale(Context c, Runnable onDone) {
        if (!enabled(c) || !hasPermission(c) || !stale(c)) {
            return;
        }
        refresh(c, onDone);
    }

    static void refresh(Context context, final Runnable onDone) {
        if (busy || !hasPermission(context)) {
            return;
        }
        final Context c = context.getApplicationContext();
        LocationManager lm = (LocationManager) c.getSystemService("location");
        if (lm == null) {
            return;
        }
        Location best = null;
        try {
            for (String p : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) {
                    best = l;
                }
            }
        } catch (SecurityException e) {
        }
        if (best != null) {
            fetch(c, best, onDone);
            return;
        }
        if (Build.VERSION.SDK_INT >= 30 && lm.isProviderEnabled("network")) {
            busy = true;
            try {
                lm.getCurrentLocation("network", null, c.getMainExecutor(), new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        Weather.lambda$refresh$0(c, onDone, (Location) obj);
                    }
                });
            } catch (SecurityException e2) {
                busy = false;
            }
        }
    }

    static void lambda$refresh$0(Context c, Runnable onDone, Location l) {
        busy = false;
        if (l != null) {
            fetch(c, l, onDone);
        }
    }

    private static void fetch(final Context c, Location loc, final Runnable onDone) {
        busy = true;
        final Handler main = new Handler(Looper.getMainLooper());
        final double lat = Math.round(loc.getLatitude() * 100.0d) / 100.0d;
        final double lon = Math.round(loc.getLongitude() * 100.0d) / 100.0d;
        new Thread(new Runnable() {
            @Override
            public final void run() {
                Weather.lambda$fetch$3(lat, lon, c, main, onDone);
            }
        }, "SeaGlassWeather").start();
    }

    static void lambda$fetch$3(double lat, double lon, Context c, Handler main, final Runnable onDone) {
        String country = "";
        try {
            country = Locale.getDefault().getCountry();
        } catch (Exception e) {
        }
        boolean fahrenheit = country.equals("US") || country.equals("LR") || country.equals("MM");
        HttpURLConnection http = null;
        try {
            URL url = new URL(String.format(Locale.ROOT,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code&temperature_unit=%s",
                    lat, lon, fahrenheit ? "fahrenheit" : "celsius"));
            http = (HttpURLConnection) url.openConnection();
            http.setConnectTimeout(10000);
            http.setReadTimeout(10000);
            StringBuilder body = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(http.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    body.append(line);
                }
            }
            JSONObject current = new JSONObject(body.toString()).getJSONObject("current");
            JSONObject out = new JSONObject();
            out.put("t", current.getDouble("temperature_2m"));
            out.put("u", fahrenheit ? "F" : "C");
            out.put("d", describe(current.optInt("weather_code", -1)));
            out.put("c", city(c, lat, lon));
            prefs(c).edit().putString("data", out.toString()).putLong("time", System.currentTimeMillis()).apply();
            main.post(new Runnable() {
                @Override
                public void run() {
                    Weather.lambda$fetch$1(onDone);
                }
            });
        } catch (Exception e) {
            main.post(new Runnable() {
                @Override
                public void run() {
                    Weather.busy = false;
                }
            });
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    static void lambda$fetch$1(Runnable onDone) {
        busy = false;
        if (onDone != null) {
            onDone.run();
        }
    }

    private static String city(Context c, double lat, double lon) {
        if (!Geocoder.isPresent()) {
            return "";
        }
        try {
            List<Address> list = new Geocoder(c, Locale.getDefault()).getFromLocation(lat, lon, 1);
            if (list != null && !list.isEmpty()) {
                Address a = list.get(0);
                return a.getSubLocality() != null ? a.getSubLocality() : a.getLocality() != null ? a.getLocality() : a.getSubAdminArea() != null ? a.getSubAdminArea() : "";
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    static String describe(int code) {
        if (code == 0) {
            return "Clear";
        }
        if (code == 1 || code == 2) {
            return "Partly cloudy";
        }
        if (code == 3) {
            return "Cloudy";
        }
        if (code == 45 || code == 48) {
            return "Fog";
        }
        if (code >= 51 && code <= 57) {
            return "Drizzle";
        }
        if (code >= 61 && code <= 67) {
            return "Rain";
        }
        if (code >= 71 && code <= 77) {
            return "Snow";
        }
        if (code >= 80 && code <= 82) {
            return "Showers";
        }
        if (code == 85 || code == 86) {
            return "Snow showers";
        }
        return code >= 95 ? "Thunderstorms" : "";
    }
}
