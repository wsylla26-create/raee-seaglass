package me.raeeindustries.seaglass;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.ArrayList;
import java.util.Locale;

final class VoiceEngine {
    private final Context context;
    private final Listener listener;
    private boolean listening;
    private boolean muted;
    private boolean pendingForce;
    private String pendingText;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ttsReady;
    private int utteranceCount;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile String lastUtterance = "";

    interface Listener {
        void onHeard(String str);

        void onListeningChanged(boolean z);

        void onPartial(String str);

        void onSpeakingChanged(boolean z);

        void onVoiceError(String str);
    }

    VoiceEngine(Context context, final Listener listener) {
        this.context = context;
        this.listener = listener;
        this.tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public final void onInit(int i) {
                VoiceEngine.this.lambda$new$1(listener, i);
            }
        });
    }

    public void lambda$new$1(Listener listener, int status) {
        if (status != 0 || this.tts == null) {
            return;
        }
        this.tts.setLanguage(Locale.getDefault());
        this.tts.setOnUtteranceProgressListener(new AnonymousClass1(listener));
        this.ttsReady = true;
        if (this.pendingText != null) {
            final String text = this.pendingText;
            final boolean force = this.pendingForce;
            this.pendingText = null;
            this.main.post(new Runnable() {
                @Override
                public final void run() {
                    VoiceEngine.this.lambda$new$0(text, force);
                }
            });
        }
    }

    class AnonymousClass1 extends UtteranceProgressListener {
        final Listener val$listener;

        AnonymousClass1(Listener listener) {
            this.val$listener = listener;
        }

        @Override
        public void onStart(String id) {
            Handler handler = VoiceEngine.this.main;
            final Listener listener = this.val$listener;
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    listener.onSpeakingChanged(true);
                }
            });
        }

        @Override
        public void onDone(String id) {
            if (id.equals(VoiceEngine.this.lastUtterance)) {
                Handler handler = VoiceEngine.this.main;
                final Listener listener = this.val$listener;
                handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        listener.onSpeakingChanged(false);
                    }
                });
            }
        }

        @Override
        public void onError(String id) {
            Handler handler = VoiceEngine.this.main;
            final Listener listener = this.val$listener;
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    listener.onSpeakingChanged(false);
                }
            });
        }

        @Override
        public void onStop(String id, boolean interrupted) {
            Handler handler = VoiceEngine.this.main;
            final Listener listener = this.val$listener;
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    listener.onSpeakingChanged(false);
                }
            });
        }
    }

    boolean isListening() {
        return this.listening;
    }

    void listen() {
        stopSpeaking();
        if (!SpeechRecognizer.isRecognitionAvailable(this.context)) {
            this.listener.onVoiceError("Voice input isn't available on this phone. You can type instead.");
            return;
        }
        if (this.recognizer != null) {
            this.recognizer.destroy();
        }
        this.recognizer = SpeechRecognizer.createSpeechRecognizer(this.context);
        this.recognizer.setRecognitionListener(new Recognition());
        Intent intent = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        intent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
        intent.putExtra("android.speech.extra.PARTIAL_RESULTS", true);
        intent.putExtra("calling_package", this.context.getPackageName());
        intent.putExtra("android.speech.extra.LANGUAGE", Locale.getDefault().toLanguageTag());
        setListening(true);
        this.recognizer.startListening(intent);
    }

    void stopListening() {
        if (this.recognizer == null || !this.listening) {
            return;
        }
        this.recognizer.stopListening();
    }

    void setMuted(boolean m) {
        this.muted = m;
        if (m) {
            stopSpeaking();
        }
    }

    boolean isMuted() {
        return this.muted;
    }

    void say(String text) {
        lambda$new$0(text, false);
    }

    void read(String text) {
        lambda$new$0(text, true);
    }

    public void lambda$new$0(String text, boolean force) {
        int cut;
        if (text == null || this.tts == null) {
            return;
        }
        if (!this.muted || force) {
            if (!this.ttsReady) {
                this.pendingText = text;
                this.pendingForce = force;
                return;
            }
            String rest = text.trim();
            if (rest.isEmpty()) {
                return;
            }
            int max = Math.max(200, Math.min(3500, TextToSpeech.getMaxSpeechInputLength() - 100));
            int mode = 0;
            while (!rest.isEmpty()) {
                int end = Math.min(max, rest.length());
                if (end < rest.length() && (cut = rest.lastIndexOf(". ", end)) > max / 2) {
                    end = cut + 1;
                }
                String part = rest.substring(0, end);
                rest = rest.substring(end).trim();
                StringBuilder append = new StringBuilder().append("x");
                int i = this.utteranceCount + 1;
                this.utteranceCount = i;
                this.lastUtterance = append.append(i).toString();
                this.tts.speak(part, mode, null, this.lastUtterance);
                mode = 1;
            }
        }
    }

    void stopSpeaking() {
        this.pendingText = null;
        if (this.tts != null) {
            this.tts.stop();
        }
    }

    void destroy() {
        if (this.recognizer != null) {
            this.recognizer.destroy();
        }
        this.recognizer = null;
        if (this.tts != null) {
            this.tts.shutdown();
        }
        this.tts = null;
    }

    public void setListening(boolean on) {
        if (this.listening == on) {
            return;
        }
        this.listening = on;
        this.listener.onListeningChanged(on);
    }

    public static String first(Bundle b) {
        ArrayList<String> list;
        String t;
        if (b == null || (list = b.getStringArrayList("results_recognition")) == null || list.isEmpty() || (t = list.get(0)) == null || t.trim().isEmpty()) {
            return null;
        }
        return t.trim();
    }

    private class Recognition implements RecognitionListener {
        private Recognition() {
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onPartialResults(Bundle partial) {
            String t = VoiceEngine.first(partial);
            if (t != null) {
                VoiceEngine.this.listener.onPartial(t);
            }
        }

        @Override
        public void onResults(Bundle results) {
            VoiceEngine.this.setListening(false);
            String t = VoiceEngine.first(results);
            if (t == null) {
                VoiceEngine.this.listener.onVoiceError("I didn't catch that. Tap the mic and try again.");
            } else {
                VoiceEngine.this.listener.onHeard(t);
            }
        }

        @Override
        public void onError(int error) {
            String message;
            VoiceEngine.this.setListening(false);
            switch (error) {
                case 1:
                case 2:
                    message = "Voice needs an internet connection. You can type instead.";
                    break;
                case 3:
                case 4:
                case 5:
                default:
                    message = "Voice input stopped. You can type instead.";
                    break;
                case 6:
                case 7:
                    message = "I didn't catch that. Tap the mic and try again.";
                    break;
                case 8:
                    message = "The microphone is busy. Try again in a moment.";
                    break;
                case 9:
                    message = "The microphone is off for RAEE SeaGlass. Allow it in Settings to talk.";
                    break;
            }
            VoiceEngine.this.listener.onVoiceError(message);
        }
    }
}
