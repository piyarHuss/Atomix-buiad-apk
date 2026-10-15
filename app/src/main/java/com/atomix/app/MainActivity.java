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
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private final String APP_URL = "https://atomix-one.vercel.app/";
    private final String WEB_CLIENT_ID = "268858397066-0t6ak4r1eeqgr53lkdu0r0ge3ru2k99j.apps.googleusercontent.com";
    
    // Monetag Direct Link
    private final String MONETAG_DIRECT_LINK = "https://uplcm.com/4/12028803";
    
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

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

        // 1. Google Native Sign-In Setup (Smart Configuration)
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(WEB_CLIENT_ID)
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        }

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

        // *** MONETAG ADS ENABLE KARNE KE LIYE CRITICAL SETTINGS ***
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // Monetag verification ke liye standard User-Agent bohot zaroori hai
        webSettings.setUserAgentString(WebSettings.getDefaultUserAgent(this));

        // Cookies enable (Monetag tracking ke liye required)
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // *** MONETAG VIGNETTE/POPUP ADS HANDLE KARNE KE LIYE ***
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(MainActivity.this);
                WebSettings newSettings = newWebView.getSettings();
                newSettings.setJavaScriptEnabled(true);
                newSettings.setDomStorageEnabled(true);

                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                        String targetUrl = request.getUrl().toString();
                        
                        // Handle intent and market links properly in popups
                        if (targetUrl.startsWith("intent:") || targetUrl.startsWith("market:") || targetUrl.startsWith("tg:")) {
                            try {
                                Intent intent;
                                if (targetUrl.startsWith("intent:")) {
                                    intent = Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME);
                                } else {
                                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                                }
                                startActivity(intent);
                            } catch (Exception ignored) {}
                            return true;
                        }
                        
                        // Ad URLs open in normal browser
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

                // Monetag Tag Injection
                String monetagScript = 
                    "var meta = document.createElement('meta');" +
                    "meta.name = 'monetag';" +
                    "meta.content = '6c2cce59c8d9495f201a172a9df79dc2';" +
                    "document.getElementsByTagName('head')[0].appendChild(meta);" +
                    
                    "(function(s){s.dataset.zone='11984536',s.src='https://al5sm.com/tag.min.js'})" +
                    "([document.documentElement, document.body].filter(Boolean).pop().appendChild(document.createElement('script')));" +
                    
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
                String email = account.getEmail() != null ? account.getEmail() : "";
                String name = account.getDisplayName() != null ? account.getDisplayName() : "Google User";
                String accountId = account.getId() != null ? account.getId() : "G_" + System.currentTimeMillis();

                // 1. Agar ID Token mila to normal Firebase credential login
                if (idToken != null && !idToken.isEmpty()) {
                    webView.evaluateJavascript(
                        "javascript:(function(){" +
                        "  if(window.onNativeGoogleLoginSuccess){" +
                        "    window.onNativeGoogleLoginSuccess('" + idToken + "', '" + email + "', '" + name + "');" +
                        "  } else if(window.handleSuccessfulLogin){" +
                        "    window.handleSuccessfulLogin({uid:'" + accountId + "', displayName:'" + name + "', email:'" + email + "'}, '" + name + "', '" + email + "');" +
                        "  }" +
                        "})();", 
                        null
                    );
                } else {
                    // 2. Agar token nahi mila to Direct Account Login (Zero Error Fallback)
                    webView.evaluateJavascript(
                        "javascript:(function(){" +
                        "  if(window.handleSuccessfulLogin){" +
                        "    window.handleSuccessfulLogin({uid:'" + accountId + "', displayName:'" + name + "', email:'" + email + "'}, '" + name + "', '" + email + "');" +
                        "  } else {" +
                        "    location.reload();" +
                        "  }" +
                        "})();", 
                        null
                    );
                }
            }
        } catch (ApiException e) {
            int code = e.getStatusCode();
            // Agar Error 10 ya koi issue aaye to basic sign-in fallback karega
            if (code == 10 || code == 12500) {
                GoogleSignInOptions simpleGso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();
                mGoogleSignInClient = GoogleSignIn.getClient(this, simpleGso);
                
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            } else {
                Toast.makeText(this, "Sign-in cancelled or failed: " + code, Toast.LENGTH_SHORT).show();
            }
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

    public class WebAppInterface {
        @JavascriptInterface
        public void showAd() {
            runOnUiThread(() -> {
                try {
                    // Monetag Direct Ad Link open karega
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MONETAG_DIRECT_LINK));
                    startActivity(intent);

                    // Website ko callback bhejna (Ad dekha gaya)
                    webView.evaluateJavascript("javascript:if(window.onAndroidAdFinished){window.onAndroidAdFinished(true);}", null);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Failed to open Ad Link", Toast.LENGTH_SHORT).show();
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
