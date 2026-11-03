package me.raeeindustries.seaglass;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;

final class PrivateLock {
    static final int REQ_UNLOCK = 46;

    interface Result {
        void onCancelled();

        void onUnlocked();
    }

    private PrivateLock() {
    }

    static boolean phoneHasLock(Context c) {
        KeyguardManager km = (KeyguardManager) c.getSystemService("keyguard");
        return km != null && km.isDeviceSecure();
    }

    static void ask(Activity a, final Result result) {
        BiometricManager bm;
        if (!phoneHasLock(a)) {
            result.onUnlocked();
            return;
        }
        if (Build.VERSION.SDK_INT >= 30 && (bm = (BiometricManager) a.getSystemService(BiometricManager.class)) != null && bm.canAuthenticate(33023) == 0) {
            BiometricPrompt prompt = new BiometricPrompt.Builder(a).setTitle("Unlock private tabs").setSubtitle("RAEE SeaGlass").setAllowedAuthenticators(33023).build();
            prompt.authenticate(new CancellationSignal(), a.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                    result.onUnlocked();
                }

                @Override
                public void onAuthenticationError(int code, CharSequence message) {
                    result.onCancelled();
                }
            });
            return;
        }
        KeyguardManager km = (KeyguardManager) a.getSystemService("keyguard");
        Intent i = km.createConfirmDeviceCredentialIntent("Unlock private tabs", "RAEE SeaGlass");
        if (i == null) {
            result.onUnlocked();
        } else {
            a.startActivityForResult(i, 46);
        }
    }
}
