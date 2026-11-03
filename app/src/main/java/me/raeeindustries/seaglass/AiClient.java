package me.raeeindustries.seaglass;

import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

final class AiClient {
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    interface Callback {
        void onAnswer(String str);

        void onError(String str);
    }

    private AiClient() {
    }

    static boolean isReady() {
        return BuildConfig.AI_ENDPOINT.startsWith("https://");
    }

    static void ask(final String question, final String purchaseToken, final Callback callback) {
        POOL.execute(new Runnable() {
            @Override
            public void run() {
                askNow(question, purchaseToken, callback);
            }
        });
    }

    private static void fail(final Callback callback, final String message) {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message);
            }
        });
    }

    private static void askNow(String question, String purchaseToken, final Callback callback) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(BuildConfig.AI_ENDPOINT).openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(10000);
            c.setReadTimeout(30000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("X-App-Key", BuildConfig.AI_APP_KEY);
            JSONObject body = new JSONObject();
            body.put("question", question);
            body.put("language", Locale.getDefault().toLanguageTag());
            body.put("purchaseToken", purchaseToken == null ? "" : purchaseToken);
            body.put("packageName", BuildConfig.APPLICATION_ID);
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = c.getResponseCode();
            String text = readAll(code < 400 ? c.getInputStream() : c.getErrorStream());
            if (code == 402) {
                fail(callback, "Your SeaGlass Plus subscription couldn't be confirmed, so here are search results.");
                return;
            }
            if (code >= 400) {
                throw new IOException("HTTP " + code);
            }
            final String answer = new JSONObject(text).optString("answer", "").trim();
            if (answer.isEmpty()) {
                throw new IOException("Empty answer");
            }
            MAIN.post(new Runnable() {
                @Override
                public void run() {
                    callback.onAnswer(answer);
                }
            });
        } catch (Exception e) {
            fail(callback, "I couldn't get an answer right now, so here are search results.");
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try (InputStream src = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = src.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toString("UTF-8");
        }
    }
}
