package com.atomix.app;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private RewardedAd mRewardedAd;
    private final String APP_URL = "https://atomix-one.vercel.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        MobileAds.initialize(this, initializationStatus -> {});
        loadAd();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        
        webView.setWebViewClient(new WebViewClient() {
            // Internet na hone par custom screen dikhana
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showCustomOfflineScreen(view);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                showCustomOfflineScreen(view);
            }
        });

        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadUrl(APP_URL);
    }

    // Professional Dark Theme No-Internet Screen
    private void showCustomOfflineScreen(WebView view) {
        String offlineHtml = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "<style>"
            + "body { background: #070a14; color: #eef1fb; font-family: -apple-system, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; padding: 20px; box-sizing: border-box; }"
            + ".icon-box { width: 76px; height: 76px; background: rgba(255, 92, 122, 0.12); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-bottom: 18px; border: 1px solid rgba(255, 92, 122, 0.3); }"
            + "h2 { font-size: 20px; margin: 0 0 8px; font-weight: 700; letter-spacing: 0.5px; }"
            + "p { color: #7f89a8; font-size: 13px; margin: 0 0 26px; line-height: 1.5; }"
            + ".btn { background: linear-gradient(120deg, #6c8cff, #9b6cff); color: #fff; border: none; border-radius: 14px; padding: 14px 30px; font-size: 14px; font-weight: 700; cursor: pointer; box-shadow: 0 8px 20px rgba(108, 140, 255, 0.35); outline: none; }"
            + ".btn:active { transform: scale(0.96); }"
            + "</style></head><body>"
            + "<div class=\"icon-box\"><svg width=\"36\" height=\"36\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#ff5c7a\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><line x1=\"1\" y1=\"1\" x2=\"23\" y2=\"23\"></line><path d=\"M16.72 11.06A10.94 10.94 0 0 1 19 12.55\"></path><path d=\"M5 12.55a10.94 10.94 0 0 1 5.17-2.39\"></path><path d=\"M10.71 5.05A16 16 0 0 1 22.58 9\"></path><path d=\"M1.42 9a15.91 15.91 0 0 1 4.7-2.88\"></path><path d=\"M8.53 16.11a6 6 0 0 1 6.95 0\"></path><line x1=\"12\" y1=\"20\" x2=\"12.01\" y2=\"20\"></line></svg></div>"
            + "<h2>No Internet Connection</h2>"
            + "<p>Please check your mobile data or Wi-Fi network and try again.</p>"
            + "<button class=\"btn\" onclick=\"location.href='" + APP_URL + "'\">TRY AGAIN</button>"
            + "</body></html>";

        view.loadDataWithBaseURL(APP_URL, offlineHtml, "text/html", "UTF-8", null);
    }

    private void loadAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest,
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    mRewardedAd = ad;
                }
            });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showAd() {
            runOnUiThread(() -> {
                if (mRewardedAd != null) {
                    mRewardedAd.show(MainActivity.this, rewardItem -> {
                        webView.evaluateJavascript("javascript:window.onAndroidAdFinished(true)", null);
                        loadAd();
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Ad loading, please wait...", Toast.LENGTH_SHORT).show();
                    loadAd();
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
