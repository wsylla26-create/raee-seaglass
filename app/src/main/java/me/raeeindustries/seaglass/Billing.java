package me.raeeindustries.seaglass;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

final class Billing implements PurchasesUpdatedListener {
    private static final long DAY_MS = 86400000;
    static final String PLUS = "seaglass_plus";
    /** SeaGlass Plus (AI answers subscription) is hidden in version 1. Set to true once an AI server is connected. */
    static final boolean PLUS_ENABLED = false;
    static final int TRIAL_DAYS = 7;
    static final String UNLOCK = "seaglass_unlock";
    private static Billing instance;
    private final BillingClient client;
    private boolean connecting;
    private ProductDetails plusDetails;
    private final SharedPreferences prefs;
    private ProductDetails unlockDetails;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList();

    interface Listener {
        void onBillingMessage(String str);

        void onEntitlementsChanged();
    }

    static synchronized Billing get(Context c) {
        Billing billing;
        synchronized (Billing.class) {
            if (instance == null) {
                instance = new Billing(c.getApplicationContext());
            }
            billing = instance;
        }
        return billing;
    }

    private Billing(Context c) {
        this.prefs = c.getSharedPreferences("seaglass_billing", 0);
        if (!this.prefs.contains("firstRun")) {
            this.prefs.edit().putLong("firstRun", System.currentTimeMillis()).apply();
        }
        this.client = BillingClient.newBuilder(c).setListener(this).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).build();
        connect();
    }

    void addListener(Listener l) {
        if (this.listeners.contains(l)) {
            return;
        }
        this.listeners.add(l);
    }

    void removeListener(Listener l) {
        this.listeners.remove(l);
    }

    boolean ownsUnlock() {
        return this.prefs.getBoolean("ownsUnlock", false);
    }

    boolean hasPlus() {
        return this.prefs.getBoolean("hasPlus", false);
    }

    String plusToken() {
        return this.prefs.getString("plusToken", "");
    }

    boolean inTrial() {
        return trialDaysLeft() > 0;
    }

    int trialDaysLeft() {
        long used = System.currentTimeMillis() - this.prefs.getLong("firstRun", System.currentTimeMillis());
        long left = 7 - (used / DAY_MS);
        return (int) Math.max(0L, Math.min(7L, left));
    }

    boolean canUseFull() {
        return ownsUnlock() || hasPlus() || inTrial();
    }

    String planSummary() {
        if (hasPlus()) {
            return "SeaGlass Plus is active";
        }
        if (ownsUnlock()) {
            return PLUS_ENABLED ? "Unlocked · Plus adds AI answers" : "Full app unlocked · Thank you!";
        }
        if (inTrial()) {
            int d = trialDaysLeft();
            return "Free trial: " + d + (d == 1 ? " day left" : " days left");
        }
        return "Free version · Tap to unlock";
    }

    String unlockPrice() {
        if (this.unlockDetails == null || this.unlockDetails.getOneTimePurchaseOfferDetails() == null) {
            return null;
        }
        return this.unlockDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
    }

    String plusPrice() {
        ProductDetails.SubscriptionOfferDetails offer = plusOffer();
        if (offer == null || offer.getPricingPhases().getPricingPhaseList().isEmpty()) {
            return null;
        }
        List<ProductDetails.PricingPhase> phases = offer.getPricingPhases().getPricingPhaseList();
        return phases.get(phases.size() - 1).getFormattedPrice();
    }

    private ProductDetails.SubscriptionOfferDetails plusOffer() {
        if (this.plusDetails == null || this.plusDetails.getSubscriptionOfferDetails() == null || this.plusDetails.getSubscriptionOfferDetails().isEmpty()) {
            return null;
        }
        return this.plusDetails.getSubscriptionOfferDetails().get(0);
    }

    void buyUnlock(Activity activity) {
        if (this.unlockDetails == null) {
            notReady();
        } else {
            BillingFlowParams.ProductDetailsParams p = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(this.unlockDetails).build();
            this.client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(Collections.singletonList(p)).build());
        }
    }

    void buyPlus(Activity activity) {
        ProductDetails.SubscriptionOfferDetails offer = plusOffer();
        if (this.plusDetails == null || offer == null) {
            notReady();
        } else {
            BillingFlowParams.ProductDetailsParams p = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(this.plusDetails).setOfferToken(offer.getOfferToken()).build();
            this.client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(Collections.singletonList(p)).build());
        }
    }

    void restore() {
        if (!this.client.isReady()) {
            connect();
            message("Checking with Google Play… try again in a moment if nothing changes.");
        } else {
            refreshPurchases(true);
        }
    }

    private void notReady() {
        connect();
        message("Google Play isn't ready yet. Check your internet connection and try again.");
    }

    void connect() {
        if (this.client.isReady() || this.connecting) {
            return;
        }
        this.connecting = true;
        this.client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                Billing.this.connecting = false;
                if (result.getResponseCode() == 0) {
                    Billing.this.loadProducts();
                    Billing.this.refreshPurchases(false);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Billing.this.connecting = false;
            }
        });
    }

    void refresh() {
        if (!this.client.isReady()) {
            connect();
        } else {
            refreshPurchases(false);
        }
    }

    public void loadProducts() {
        this.client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(QueryProductDetailsParams.Product.newBuilder().setProductId(UNLOCK).setProductType("inapp").build())).build(), new ProductDetailsResponseListener() {
            @Override
            public final void onProductDetailsResponse(BillingResult billingResult, QueryProductDetailsResult result) {
                Billing.this.lambda$loadProducts$1(billingResult, result == null ? null : result.getProductDetailsList());
            }
        });
        this.client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(QueryProductDetailsParams.Product.newBuilder().setProductId(PLUS).setProductType("subs").build())).build(), new ProductDetailsResponseListener() {
            @Override
            public final void onProductDetailsResponse(BillingResult billingResult, QueryProductDetailsResult result) {
                Billing.this.lambda$loadProducts$3(billingResult, result == null ? null : result.getProductDetailsList());
            }
        });
    }

    public void lambda$loadProducts$1(BillingResult result, final List list) {
        this.main.post(new Runnable() {
            @Override
            public final void run() {
                Billing.this.lambda$loadProducts$0(list);
            }
        });
    }

    public void lambda$loadProducts$0(List list) {
        if (list != null && !list.isEmpty()) {
            this.unlockDetails = (ProductDetails) list.get(0);
        }
        changed();
    }

    public void lambda$loadProducts$3(BillingResult result, final List list) {
        this.main.post(new Runnable() {
            @Override
            public final void run() {
                Billing.this.lambda$loadProducts$2(list);
            }
        });
    }

    public void lambda$loadProducts$2(List list) {
        if (list != null && !list.isEmpty()) {
            this.plusDetails = (ProductDetails) list.get(0);
        }
        changed();
    }

    public void refreshPurchases(final boolean announce) {
        this.client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType("inapp").build(), new PurchasesResponseListener() {
            @Override
            public final void onQueryPurchasesResponse(BillingResult billingResult, List list) {
                Billing.this.lambda$refreshPurchases$5(billingResult, list);
            }
        });
        this.client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType("subs").build(), new PurchasesResponseListener() {
            @Override
            public final void onQueryPurchasesResponse(BillingResult billingResult, List list) {
                Billing.this.lambda$refreshPurchases$7(announce, billingResult, list);
            }
        });
    }

    public void lambda$refreshPurchases$5(final BillingResult result, final List purchases) {
        this.main.post(new Runnable() {
            @Override
            public final void run() {
                Billing.this.lambda$refreshPurchases$4(result, purchases);
            }
        });
    }

    public void lambda$refreshPurchases$4(BillingResult result, List purchases) {
        if (result.getResponseCode() != 0) {
            return;
        }
        boolean owns = false;
        Iterator it = purchases.iterator();
        while (it.hasNext()) {
            Purchase p = (Purchase) it.next();
            if (p.getProducts().contains(UNLOCK) && p.getPurchaseState() == 1) {
                owns = true;
                acknowledge(p);
            }
        }
        if (owns) {
            this.prefs.edit().putBoolean("ownsUnlock", true).apply();
        }
        changed();
    }

    public void lambda$refreshPurchases$7(final boolean announce, final BillingResult result, final List purchases) {
        this.main.post(new Runnable() {
            @Override
            public final void run() {
                Billing.this.lambda$refreshPurchases$6(result, purchases, announce);
            }
        });
    }

    public void lambda$refreshPurchases$6(BillingResult result, List purchases, boolean announce) {
        String str;
        if (result.getResponseCode() != 0) {
            return;
        }
        String token = null;
        Iterator it = purchases.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Purchase p = (Purchase) it.next();
            if (p.getProducts().contains(PLUS) && p.getPurchaseState() == 1) {
                token = p.getPurchaseToken();
                acknowledge(p);
            }
        }
        this.prefs.edit().putBoolean("hasPlus", token != null).putString("plusToken", token == null ? "" : token).apply();
        changed();
        if (announce) {
            if (hasPlus()) {
                str = "SeaGlass Plus is active.";
            } else {
                str = ownsUnlock() ? "Your unlock is restored." : "No purchases found for this Google account.";
            }
            message(str);
        }
    }

    @Override
    public void onPurchasesUpdated(final BillingResult result, final List<Purchase> purchases) {
        this.main.post(new Runnable() {
            @Override
            public final void run() {
                Billing.this.lambda$onPurchasesUpdated$8(result, purchases);
            }
        });
    }

    public void lambda$onPurchasesUpdated$8(BillingResult result, List purchases) {
        int code = result.getResponseCode();
        if (code == 0 && purchases != null) {
            Iterator it = purchases.iterator();
            while (it.hasNext()) {
                Purchase p = (Purchase) it.next();
                handlePurchase(p);
            }
            return;
        }
        if (code == 7) {
            refreshPurchases(true);
        } else if (code != 1) {
            message("The purchase didn't go through. You haven't been charged. Please try again.");
        }
    }

    private void handlePurchase(Purchase p) {
        if (p.getPurchaseState() == 2) {
            message("Your payment is pending. Features unlock as soon as Google Play confirms it.");
            return;
        }
        if (p.getPurchaseState() != 1) {
            return;
        }
        SharedPreferences.Editor e = this.prefs.edit();
        if (p.getProducts().contains(UNLOCK)) {
            e.putBoolean("ownsUnlock", true);
        }
        if (p.getProducts().contains(PLUS)) {
            e.putBoolean("hasPlus", true).putString("plusToken", p.getPurchaseToken());
        }
        e.apply();
        acknowledge(p);
        changed();
        message(p.getProducts().contains(PLUS) ? "Thank you! SeaGlass Plus is active." : "Thank you! RAEE SeaGlass is unlocked.");
    }

    private void acknowledge(Purchase p) {
        if (p.isAcknowledged()) {
            return;
        }
        this.client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(), new AcknowledgePurchaseResponseListener() {
            @Override
            public final void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                Billing.lambda$acknowledge$9(billingResult);
            }
        });
    }

    static void lambda$acknowledge$9(BillingResult r) {
    }

    private void changed() {
        Iterator it = new ArrayList(this.listeners).iterator();
        while (it.hasNext()) {
            Listener l = (Listener) it.next();
            l.onEntitlementsChanged();
        }
    }

    private void message(String text) {
        Iterator it = new ArrayList(this.listeners).iterator();
        while (it.hasNext()) {
            Listener l = (Listener) it.next();
            l.onBillingMessage(text);
        }
    }
}
