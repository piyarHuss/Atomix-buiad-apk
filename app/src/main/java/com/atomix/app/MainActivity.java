package com.atomix.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private RewardedAd mRewardedAd;
    private final String APP_URL = "https://atomix-one.vercel.app/";
    private final String WEB_CLIENT_ID = "268858397066-0t6ak4r1eeqgr53lkdu0r0ge3ru2k99j.apps.googleusercontent.com";
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private boolean isLoadingAd = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        webView = new WebView(this);
        setContentView(webView);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID).requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                }
        );

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setUserAgentString(WebSettings.getDefaultUserAgent(this));

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(MainActivity.this);
                newWebView.getSettings().setJavaScriptEnabled(true);
                newWebView.getSettings().setDomStorageEnabled(true);

                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                        String targetUrl = request.getUrl().toString();
                        if (targetUrl.startsWith("intent:") || targetUrl.startsWith("market:") || targetUrl.startsWith("tg:")) {
                            try {
                                Intent intent = targetUrl.startsWith("intent:") ? Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME) : new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                                startActivity(intent);
                            } catch (Exception ignored) {}
                            return true;
                        }
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)));
                        } catch (Exception ignored) {}
                        return true;
                    }
                });
                ((WebView.WebViewTransport) resultMsg.obj).setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String monetagScript = "var meta=document.createElement('meta');meta.name='monetag';meta.content='6c2cce59c8d9495f201a172a9df79dc2';document.head.appendChild(meta);" +
                    "(function(s){s.dataset.zone='11984536';s.src='https://al5sm.com/tag.min.js';document.body.appendChild(s)})(document.createElement('script'));" +
                    "(function(s){s.dataset.zone='11989369';s.src='https://n6wxm.com/vignette.min.js';document.body.appendChild(s)})(document.createElement('script'));";
                view.evaluateJavascript(monetagScript, null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("accounts.google.com") || url.contains("firebaseapp.com/__/auth/handler")) {
                    openGoogleSignIn();
                    return true;
                }
                if (url.startsWith("market:") || url.startsWith("intent:") || url.startsWith("tg:") || 
                    url.startsWith("whatsapp:") || url.contains("play.google.com/store") || 
                    url.contains("t.me/") || url.contains("telegram.me/")) {
                    try {
                        Intent intent = url.startsWith("intent:") ? Intent.parseUri(url, Intent.URI_INTENT_SCHEME) : new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        if (intent != null) { view.getContext().startActivity(intent); return true; }
                    } catch (Exception e) {
                        try {
                            if (url.contains("t.me/") || url.contains("tg:")) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("tg://resolve?domain=", "https://t.me/"))));
                            }
                        } catch (Exception ignored) {}
                        return true;
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    String url = request.getUrl().toString();
                    if (!url.startsWith("tg:") && !url.startsWith("intent:") && !url.startsWith("market:") && !url.contains("t.me/")) {
                        showCustomOfflineScreen(view);
                    }
                }
            }
        });

        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadUrl(APP_URL);
        MobileAds.initialize(this, initializationStatus -> loadAd());
    }

    public void openGoogleSignIn() {
        runOnUiThread(() -> {
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
            });
        });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                webView.evaluateJavascript("javascript:if(window.onNativeGoogleLoginSuccess){window.onNativeGoogleLoginSuccess('" + account.getIdToken() + "', '" + account.getEmail() + "', '" + account.getDisplayName() + "');}", null);
            }
        } catch (ApiException e) {
            Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCustomOfflineScreen(WebView view) {
        String html = "<!DOCTYPE html><html><body style='background:#070a14;color:#fff;display:flex;justify-content:center;align-items:center;height:100vh;flex-direction:column;font-family:sans-serif;'><h2>No Internet Connection</h2><button onclick=\"location.reload()\" style='padding:10px 20px;background:#6c8cff;border:none;border-radius:10px;color:#fff;'>RETRY</button></body></html>";
        view.loadDataWithBaseURL(APP_URL, html, "text/html", "UTF-8", null);
    }

    private void loadAd() {
        if (isLoadingAd || mRewardedAd != null) return;
        isLoadingAd = true;
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) { mRewardedAd = ad; isLoadingAd = false; }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) { mRewardedAd = null; isLoadingAd = false; }
            });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showAd() {
            runOnUiThread(() -> {
                if (mRewardedAd != null) {
                    mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() { mRewardedAd = null; loadAd(); }
                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError err) { mRewardedAd = null; loadAd(); }
                    });
                    mRewardedAd.show(MainActivity.this, rewardItem -> {
                        webView.evaluateJavascript("javascript:window.onAndroidAdFinished(true)", null);
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Ad is loading, please wait...", Toast.LENGTH_SHORT).show();
                    loadAd();
                }
            });
        }
        @JavascriptInterface
        public void openGoogleLogin() { openGoogleSignIn(); }
        @JavascriptInterface
        public void openBrowser(String url) {
            runOnUiThread(() -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) {}
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
