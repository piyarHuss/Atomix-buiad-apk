package com.atomix.app;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private final String APP_URL = "https://atomix-one.vercel.app/";
    private final String MONETAG_DIRECT_LINK = "https://uplcm.com/4/12028803";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        // CHECK VPN AT APP LAUNCH
        if (isVpnConnected()) {
            showVpnBlockScreen();
            return;
        }

        webView = new WebView(this);
        setContentView(webView);

        // WEBVIEW SETTINGS FOR SEAMLESS GOOGLE POPUP & MONETAG
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // Crucial for Google Sign In inside WebView
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setUserAgentString(WebSettings.getDefaultUserAgent(this));

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Check VPN on clicks
                if (isVpnConnected()) {
                    showVpnBlockScreen();
                    return true;
                }

                // External Intents
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

    public boolean isVpnConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Network activeNetwork = cm.getActiveNetwork();
                    if (activeNetwork != null) {
                        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                        return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
                    }
                } else {
                    android.net.NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_VPN);
                    return networkInfo != null && networkInfo.isConnected();
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void showVpnBlockScreen() {
        String vpnHtml = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "<style>body{background:#050711;color:#ff5c7a;font-family:sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;margin:0;text-align:center;padding:24px;box-sizing:border-box}"
            + "h1{font-size:24px;margin-bottom:8px}p{color:#a5b1d9;font-size:14px;max-width:320px;line-height:1.5}"
            + ".btn{background:#2fe08e;color:#05140d;border:none;border-radius:12px;padding:12px 25px;font-weight:800;margin-top:20px;cursor:pointer}</style></head><body>"
            + "<h1>⛔ VPN Detected!</h1><p>Using VPN/Proxy is not allowed. Please turn off your VPN and restart the app.</p>"
            + "<button class=\"btn\" onclick=\"location.reload()\">RETRY</button></body></html>";

        if (webView == null) {
            webView = new WebView(this);
            setContentView(webView);
        }
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadDataWithBaseURL(null, vpnHtml, "text/html", "UTF-8", null);
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
        public boolean isVpnActive() {
            return isVpnConnected();
        }

        @JavascriptInterface
        public void showAd() {
            runOnUiThread(() -> {
                try {
                    if (isVpnConnected()) {
                        showVpnBlockScreen();
                        return;
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MONETAG_DIRECT_LINK));
                    startActivity(intent);

                    webView.evaluateJavascript("javascript:if(window.onAndroidAdFinished){window.onAndroidAdFinished(true);}", null);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Failed to open Ad Link", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void openGoogleLogin() {
            // Native dialog ke badle webview ko standard Firebase Google Auth trigger karne dega
            runOnUiThread(() -> {
                webView.evaluateJavascript("javascript:(function(){" +
                    "  if(window.auth && window.GoogleAuthProvider){" +
                    "    var provider = new window.GoogleAuthProvider();" +
                    "    window.signInWithPopup(window.auth, provider).then(function(res){" +
                    "      if(window.handleSuccessfulLogin) window.handleSuccessfulLogin(res.user);" +
                    "    }).catch(function(err){" +
                    "      if(window.toast) window.toast(err.message);" +
                    "    });" +
                    "  } else {" +
                    "    if(window.openAuthSheet) window.openAuthSheet('email');" +
                    "  }" +
                    "})();", null);
            });
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
    protected void onResume() {
        super.onResume();
        if (isVpnConnected()) {
            showVpnBlockScreen();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
