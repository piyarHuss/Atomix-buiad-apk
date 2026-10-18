package com.atomix.app;

import android.app.Activity;
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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private final String APP_URL = "https://atomix-one.vercel.app/";
    private final String WEB_CLIENT_ID = "268858397066-0t6ak4r1eeqgr53lkdu0r0ge3ru2k99j.apps.googleusercontent.com";
    
    // Monetag Direct Link (Backup task rewards ke liye)
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

        // ⭐ AD-MAVEN IN-APP OVERLAY AD INITIALIZE (App Open Hote Hi) ⭐
        new InAppAdsOverlay(this).show();

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

        // Popups and Windows Settings
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setUserAgentString(WebSettings.getDefaultUserAgent(this));

        // Cookies enable
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Popups and Redirects Handler
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
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                if (isVpnConnected()) {
                    showVpnBlockScreen();
                    return true;
                }

                if (url.contains("accounts.google.com") || url.contains("firebaseapp.com/__/auth/handler")) {
                    openGoogleSignIn();
                    return true;
                }

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

        // ⭐ WATCH AD BUTTON DABANE PAR (Ad-Maven overlay trigger hoga) ⭐
        @JavascriptInterface
        public void showAd() {
            runOnUiThread(() -> {
                try {
                    if (isVpnConnected()) {
                        showVpnBlockScreen();
                        return;
                    }
                    
                    // Ad-Maven Overlay Call
                    new InAppAdsOverlay(MainActivity.this).show();

                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Failed to load Ad", Toast.LENGTH_SHORT).show();
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

    // ==============================================================
    // ⭐ AD-MAVEN IN-APP ADS OVERLAY CLASS (Built-in) ⭐
    // ==============================================================
    public static class InAppAdsOverlay {
        private static final String RT_DOMAIN = "boostapp.me";
        private static final String TID = "1727994";
        private static final String BUTTON_TEXT = "Continue";
        private static final boolean IS_DARK_MODE = true; // Dark mode enabled for Atomix

        private FrameLayout overlayContainer;
        private final Activity activity;
        private String ptrUrl;

        public InAppAdsOverlay(Activity activity) {
            this.activity = activity;
        }

        public void show() {
            new Thread(() -> {
                try {
                    URL url = new URL("https://" + RT_DOMAIN + "/inapp?tid=" + TID);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 204) {
                        return; // No ad available
                    }

                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        JSONObject json = new JSONObject(sb.toString());
                        ptrUrl = json.getString("ptr");

                        activity.runOnUiThread(this::showOverlay);
                    }
                } catch (Exception ignored) {}
            }).start();
        }

        private void showOverlay() {
            try {
                overlayContainer = new FrameLayout(activity);
                overlayContainer.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                overlayContainer.setBackgroundColor(Color.argb(170, 0, 0, 0));
                overlayContainer.setClickable(true);

                int[] gradientColors = IS_DARK_MODE
                        ? new int[]{ Color.parseColor("#7C5CF3"), Color.parseColor("#4A3BD9") }   // Violet / Indigo
                        : new int[]{ Color.parseColor("#3B82F6"), Color.parseColor("#17B5D6") };  // Blue / Cyan

                GradientDrawable buttonBackground = new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR, gradientColors);
                buttonBackground.setCornerRadius(dp(14));

                Button continueButton = new Button(activity);
                continueButton.setText(BUTTON_TEXT);
                continueButton.setTextColor(Color.WHITE);
                continueButton.setTextSize(18);
                continueButton.setAllCaps(false);
                continueButton.setTypeface(continueButton.getTypeface(), android.graphics.Typeface.BOLD);
                continueButton.setBackground(buttonBackground);
                continueButton.setPadding(dp(48), dp(16), dp(48), dp(16));
                continueButton.setElevation(dp(10));
                continueButton.setStateListAnimator(null);

                FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                buttonParams.gravity = Gravity.CENTER;
                continueButton.setLayoutParams(buttonParams);

                continueButton.setOnClickListener(v -> {
                    if (ptrUrl != null) {
                        String fullUrl = ptrUrl.startsWith("//") ? "https:" + ptrUrl : ptrUrl;
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl));
                        activity.startActivity(browserIntent);
                    }

                    // Web App me ad complete ka signal bhejna taaki mining count ho
                    if (activity instanceof MainActivity) {
                        MainActivity ma = (MainActivity) activity;
                        if (ma.webView != null) {
                            ma.webView.evaluateJavascript("javascript:if(window.onAndroidAdFinished){window.onAndroidAdFinished(true);}", null);
                        }
                    }

                    dismiss();
                });

                overlayContainer.addView(continueButton);

                ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();
                rootView.addView(overlayContainer);
            } catch (Exception ignored) {}
        }

        private int dp(int value) {
            float density = activity.getResources().getDisplayMetrics().density;
            return (int) (value * density);
        }

        private void dismiss() {
            if (overlayContainer != null && overlayContainer.getParent() != null) {
                ((ViewGroup) overlayContainer.getParent()).removeView(overlayContainer);
                overlayContainer = null;
            }
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
