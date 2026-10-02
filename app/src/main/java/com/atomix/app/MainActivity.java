package com.atomix.app;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
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
        
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadUrl("https://atomix-one.vercel.app/"); 
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
