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
        
        // Hardware Acceleration
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        webView = new WebView(this);
        setContentView(webView);

        // 1. Google Native Sign-In Setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                }
        );

        // 2. WebView Settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // Popups aur Multiple Windows
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);

        // Mixed content allow karein taaki ads block na hon
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Chrome Mobile User-Agent
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

        // Cookies enable
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // WebChromeClient (Popups/Ads ke naye tabs handle karne ke liye)
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(MainActivity.this);
                WebSettings newSettings = newWebView.getSettings();
                newSettings.setJavaScriptEnabled(true);

                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                        String targetUrl = request.getUrl().toString();
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                            startActivity(intent);
                        } catch (Exception ignored) {}
                        return true;
                    }
                });

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            // MONETAG ADS SCRIPT YAHAN AUTO-INJECT HOGA JAB PAGE LOAD HOGA
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Monetag Vignette Ad Script Injection
                String monetagScript = 
                    "(function(s){s.dataset.zone='11989369',s.src='https://n6wxm.com/vignette.min.js'})" +
                    "([document.documentElement, document.body].filter(Boolean).pop().appendChild(document.createElement('script')));";

                view.evaluateJavascript(monetagScript, null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // 1. Google Login Handle
                if (url.contains("accounts.google.com") || url.contains("firebaseapp.com/__/auth/handler")) {
                    openGoogleSignIn();
                    return true;
                }

                // 2. Play Store Ads, External Market aur Telegram Intent Links Handle
                if (url.startsWith("market:") || url.startsWith("intent:") || url.startsWith("tg:") || 
                    url.startsWith("whatsapp:") || url.contains("play.google.com/store") || 
                    url.contains("t.me/") || url.contains("telegram.me/")) {
                    try {
                        Intent intent;
                        if (url.startsWith("intent:")) {
                            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        } else {
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        }
                        if (intent != null) {
                            view.getContext().startActivity(intent);
                            return true;
                        }
                    } catch (Exception e) {
                        try {
                            if (url.contains("t.me/") || url.contains("tg:")) {
                                String webFallback = url.replace("tg://resolve?domain=", "https://t.me/");
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webFallback)));
                            }
                        } catch (Exception ignored) {}
                        return true;
                    }
                    return true;
                }

                // Website aur Monetag redirects WebView me chalenge
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    String failingUrl = request.getUrl().toString();
                    if (!failingUrl.startsWith("tg:") && !failingUrl.startsWith("intent:") && 
                        !failingUrl.startsWith("market:") && !failingUrl.contains("t.me/")) {
                        showCustomOfflineScreen(view);
                    }
                }
            }
        });

        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadUrl(APP_URL);

        // AdMob Initialize
        MobileAds.initialize(this, initializationStatus -> {
            loadAd();
        });
    }

    public void openGoogleSignIn() {
        runOnUiThread(() -> {
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String idToken = account.getIdToken();
                String email = account.getEmail();
                String name = account.getDisplayName();
                
                webView.evaluateJavascript(
                        "javascript:if(window.onNativeGoogleLoginSuccess){window.onNativeGoogleLoginSuccess('" 
                                + idToken + "', '" + email + "', '" + name + "');}else{alert('Logged in as " + email + "');}", 
                        null
                );
            }
        } catch (ApiException e) {
            Toast.makeText(this, "Sign-in cancelled or failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showCustomOfflineScreen(WebView view) {
        String offlineHtml = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "<style>body{background:#070a14;color:#eef1fb;font-family:sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;margin:0;text-align:center;padding:20px;box-sizing:border-box}"
            + ".icon{width:70px;height:70px;background:rgba(255,92,122,.15);border-radius:50%;display:flex;align-items:center;justify-content:center;margin-bottom:15px}"
            + ".btn{background:linear-gradient(120deg,#6c8cff,#9b6cff);color:#fff;border:none;border-radius:12px;padding:12px 25px;font-weight:700;margin-top:20px}</style></head><body>"
            + "<div class=\"icon\"><svg width=\"35\" height=\"35\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#ff5c7a\" stroke-width=\"2\"><line x1=\"1\" y1=\"1\" x2=\"23\" y2=\"23\"/><path d=\"M16.72 11.06A10.94 10.94 0 0 1 19 12.55M5 12.55a10.94 10.94 0 0 1 5.17-2.39M10.71 5.05A16 16 0 0 1 22.58 9M1.42 9a15.91 15.91 0 0 1 4.7-2.88M8.53 16.11a6 6 0 0 1 6.95 0M12 20h.01\"/></svg></div>"
            + "<h2>No Internet Connection</h2><p style=\"color:#7f89a8\">Please check your connection.</p>"
            + "<button class=\"btn\" onclick=\"location.href='" + APP_URL + "'\">TRY AGAIN</button></body></html>";

        view.loadDataWithBaseURL(APP_URL, offlineHtml, "text/html", "UTF-8", null);
    }

    private void loadAd() {
        if (isLoadingAd || mRewardedAd != null) return;
        isLoadingAd = true;

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest,
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    mRewardedAd = ad;
                    isLoadingAd = false;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mRewardedAd = null;
                    isLoadingAd = false;
                }
            });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showAd() {
            runOnUiThread(() -> {
                if (mRewardedAd != null) {
                    mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mRewardedAd = null;
                            loadAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            mRewardedAd = null;
                            loadAd();
                        }
                    });

                    mRewardedAd.show(MainActivity.this, rewardItem -> {
                        webView.evaluateJavascript("javascript:window.onAndroidAdFinished(true)", null);
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Ad is loading, please try again in 5 seconds...", Toast.LENGTH_SHORT).show();
                    loadAd();
                }
            });
        }

        @JavascriptInterface
        public void openGoogleLogin() {
            openGoogleSignIn();
        }

        @JavascriptInterface
        public void openBrowser(String url) {
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Cannot open link", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
