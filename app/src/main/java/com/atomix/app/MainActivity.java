package com.atomix.app;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private final String APP_URL = "https://atomix-one.vercel.app/";
    private final String WEB_CLIENT_ID = "268858397066-0t6ak4r1eeqgr53lkdu0r0ge3ru2k99j.apps.googleusercontent.com";
    
    // Monetag Direct Link (Task Rewards ke liye)
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

        // CHECK VPN AT APP LAUNCH
        if (isVpnConnected()) {
            showVpnBlockScreen();
            return;
        }

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

        // Popups and Windows Settings (Adsterra & Ads ke liye zaroori)
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setUserAgentString(WebSettings.getDefaultUserAgent(this));

        // Cookies enable
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // â­ POPUPS & ADSTERRA POPUNDER HANDLER â­
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(MainActivity.this);
                WebSettings newSettings = newWebView.getSettings();
                newSettings.setJavaScriptEnabled(true);
                newSettings.setDomStorageEnabled(true);

                newWebView.setWebViewClient(new WebViewClient() {
                    private boolean handleUrl(WebView v, String targetUrl) {
                        // Empty ya blank URL ko filter karein
                        if (targetUrl == null || targetUrl.isEmpty() || targetUrl.startsWith("about:blank")) {
                            return false;
                        }

                        try {
                            if (targetUrl.startsWith("intent:") || targetUrl.startsWith("market:") || targetUrl.startsWith("tg:")) {
                                Intent intent;
                                if (targetUrl.startsWith("intent:")) {
                                    intent = Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME);
                                } else {
                                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                                }
                                startActivity(intent);
                            } else {
                                // Adsterra Popunder link ko external Chrome/browser me open karein
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                                startActivity(intent);
                            }
                            v.destroy(); // Memory leak se bachane ke liye
                        } catch (Exception ignored) {}
                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                        return handleUrl(v, request.getUrl().toString());
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, String url) {
                        return handleUrl(v, url);
                    }
                });

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Check VPN on link clicks too
                if (isVpnConnected()) {
                    showVpnBlockScreen();
                    return true;
                }

                // 1. Google Login Handle
                if (url.contains("accounts.google.com") || url.contains("firebaseapp.com/__/auth/handler")) {
                    openGoogleSignIn();
                    return true;
                }

                // 2. Play Store, Telegram, WhatsApp, External Intents
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

    // NATIVE ANDROID SYSTEM VPN DETECTION
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

    // Full-screen native VPN blocker
    private void showVpnBlockScreen() {
        String vpnHtml = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "<style>body{background:#050711;color:#ff5c7a;font-family:sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;margin:0;text-align:center;padding:24px;box-sizing:border-box}"
            + "h1{font-size:24px;margin-bottom:8px}p{color:#a5b1d9;font-size:14px;max-width:320px;line-height:1.5}"
            + ".btn{background:#2fe08e;color:#05140d;border:none;border-radius:12px;padding:12px 25px;font-weight:800;margin-top:20px;cursor:pointer}</style></head><body>"
            + "<h1>â›” VPN Detected!</h1><p>Using VPN/Proxy is not allowed. Please turn off your VPN and restart the app.</p>"
            + "<button class=\"btn\" onclick=\"location.reload()\">RETRY</button></body></html>";

        if (webView == null) {
            webView = new WebView(this);
            setContentView(webView);
        }
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadDataWithBaseURL(null, vpnHtml, "text/html", "UTF-8", null);
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
                String accountId = account.getId() != null ? account.getId() : "";

                if (idToken != null && !idToken.isEmpty()) {
                    JSONObject userObj = new JSONObject();
                    userObj.put("idToken", idToken);
                    userObj.put("email", email);
                    userObj.put("name", name);
                    userObj.put("uid", accountId);

                    String jsCode = String.format(
                        "javascript:(function(){" +
                        "  var data = %s;" +
                        "  if (typeof window.onNativeGoogleLoginSuccess === 'function') {" +
                        "    window.onNativeGoogleLoginSuccess(data.idToken, data.email, data.name);" +
                        "  } else if (typeof window.handleSuccessfulLogin === 'function') {" +
                        "    window.handleSuccessfulLogin({uid: data.uid, displayName: data.name, email: data.email, idToken: data.idToken}, data.name, data.email);" +
                        "  }" +
                        "})();",
                        userObj.toString()
                    );

                    webView.evaluateJavascript(jsCode, null);
                } else {
                    Toast.makeText(this, "ID Token null mila!", Toast.LENGTH_LONG).show();
                    resetWebLoadingState();
                }
            }
        } catch (ApiException e) {
            int code = e.getStatusCode();
            resetWebLoadingState();

            if (code == 10) {
                String actualSha1 = getInstalledAppSHA1();
                showSha1Dialog(actualSha1);
            } else if (code == 12500) {
                Toast.makeText(this, "Error 12500: Phone restart karein.", Toast.LENGTH_LONG).show();
            } else if (code == 12501) {
                Toast.makeText(this, "Sign-in cancelled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Google Error: " + code, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            resetWebLoadingState();
        }
    }

    private void showSha1Dialog(String actualSha1) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Google Error 10 Diagnostic");
        builder.setCancelable(false);

        String message = "Aapke is phone par chal rahe APK ka Asli SHA-1 ye hai:\n\n"
                + actualSha1 + "\n\n"
                + "Ise COPY karein aur Firebase Console mein jakar 'Add fingerprint' mein daal dein!";

        builder.setMessage(message);
        builder.setPositiveButton("COPY SHA-1", (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("SHA1", actualSha1);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "SHA-1 Copied! Firebase me add karein.", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("OK", null);
        builder.show();
    }

    private String getInstalledAppSHA1() {
        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                if (info.signingInfo != null) {
                    Signature[] signatures = info.signingInfo.hasMultipleSigners() 
                            ? info.signingInfo.getApkContentsSigners() 
                            : info.signingInfo.getSigningCertificateHistory();
                    if (signatures != null && signatures.length > 0) {
                        return hashSignature(signatures[0]);
                    }
                }
            } else {
                info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
                if (info.signatures != null && info.signatures.length > 0) {
                    return hashSignature(info.signatures[0]);
                }
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
        return "UNKNOWN";
    }

    private String hashSignature(Signature sig) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(sig.toByteArray());
            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                String hex = Integer.toHexString(0xFF & digest[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex.toUpperCase());
                if (i < digest.length - 1) hexString.append(":");
            }
            return hexString.toString();
        } catch (Exception e) {
            return "Error hashing";
        }
    }

    private void resetWebLoadingState() {
        runOnUiThread(() -> {
            webView.evaluateJavascript(
                "javascript:(function(){" +
                "  if (typeof window.resetGoogleLoginState === 'function') { window.resetGoogleLoginState(); }" +
                "  var btn = document.getElementById('obGoogle');" +
                "  var txt = document.getElementById('obGoogleTxt');" +
                "  if (btn) btn.disabled = false;" +
                "  if (txt) txt.textContent = 'Continue with Google';" +
                "})();",
                null
            );
        });
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

        // â­ IN-APP APK DOWNLOAD & AUTO-INSTALLATION METHOD â­
        @JavascriptInterface
        public void downloadAndInstallApk(String apkUrl) {
            runOnUiThread(() -> {
                try {
                    Toast.makeText(MainActivity.this, "Downloading latest update...", Toast.LENGTH_SHORT).show();

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
                    request.setTitle("Atomix Miner Update");
                    request.setDescription("Downloading latest version APK...");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    
                    String fileName = "Atomix_Update_" + System.currentTimeMillis() + ".apk";
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    request.setMimeType("application/vnd.android.package-archive");

                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);

                        BroadcastReceiver onComplete = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                try {
                                    context.unregisterReceiver(this);
                                } catch (Exception ignored) {}

                                try {
                                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                                    if (file.exists()) {
                                        Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                        installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                        Uri apkUri;
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            apkUri = FileProvider.getUriForFile(
                                                MainActivity.this,
                                                getPackageName() + ".provider",
                                                file
                                            );
                                            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        } else {
                                            apkUri = Uri.fromFile(file);
                                        }

                                        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                                        startActivity(installIntent);
                                    }
                                } catch (Exception e) {
                                    try {
                                        Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                                        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(fallbackIntent);
                                    } catch (Exception ignored) {}
                                }
                            }
                        };

                        ContextCompat.registerReceiver(
                            MainActivity.this,
                            onComplete,
                            new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                            ContextCompat.RECEIVER_EXPORTED
                        );
                    } else {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(browserIntent);
                    }
                } catch (Exception e) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(browserIntent);
                    } catch (Exception ignored) {}
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
