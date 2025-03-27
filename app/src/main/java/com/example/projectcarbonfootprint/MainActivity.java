package com.example.projectcarbonfootprint;

import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.*;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.content.BroadcastReceiver;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String URL = "http://10.40.0.200:1029/carbonfootprint/answers?room=101";

    private MyPresentation myPresentation;
    private DisplayManager displayManager;
    private WebView mainWebView;
    private View container;

    private final BroadcastReceiver hdmiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.HDMI_PLUG".equals(intent.getAction())) {
                boolean connected = intent.getBooleanExtra("state", false);
                if (connected) {
                    Log.d("HDMI", "HDMI is connected");
                    // You can rotate content here
                } else {
                    Log.d("HDMI", "HDMI is disconnected");
                }
            }
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: Preventing duplicate instances.");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Lock to reverse landscape for better dual-display layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.main_container);
        mainWebView = findViewById(R.id.webview_main);

        setupWebView();
        setupTouchTracking();

        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);

        // Register HDMI plug/unplug events
        IntentFilter filter = new IntentFilter("android.intent.action.HDMI_PLUG");
        registerReceiver(hdmiReceiver, filter);

        showPresentation(); // Attempt to show if already connected

        displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
                Log.d(TAG, "External Display added: " + displayId);
                showPresentation(); // Ensures MyPresentation is launched
            }

            @Override
            public void onDisplayRemoved(int displayId) {
                Log.d(TAG, "External Display removed: " + displayId);
                dismissPresentation(); // Ensures cleanup
            }

            @Override
            public void onDisplayChanged(int displayId) {
                Log.d(TAG, "External Display changed: " + displayId);
                showPresentation(); // Restart presentation if needed
            }
        }, null);
    }

    private void setupTouchTracking() {
        container.setOnTouchListener((v, event) -> {
            float x = event.getRawX();
            float y = event.getRawY();
            Log.d(TAG, "Touch at → X: " + (int) x + " | Y: " + (int) y);
            return false;
        });
    }

    private void setupWebView() {
        WebSettings settings = mainWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        mainWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mainWebView.setBackgroundColor(Color.BLACK);
        mainWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        mainWebView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page loaded completely");
            }

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Log.e(TAG, "SSL Error: " + error.toString());
                handler.cancel();
            }
        });

        mainWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int newProgress) {
                Log.d(TAG, "Loading progress: " + newProgress + "%");
            }
        });

        mainWebView.loadUrl(URL);
    }

//    private void showPresentation() {
//        Display[] displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
//        if (displays.length > 0 && myPresentation == null) {
//            Display hdmiDisplay = displays[0];
//            myPresentation = new MyPresentation(this, hdmiDisplay);
//            myPresentation.show();
//            Log.i(TAG, "Presentation shown on external display.");
//        }
//    }

    private void showPresentation() {
        Display[] displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);

        if (displays.length > 0) {
            Display hdmiDisplay = displays[0];

            // Check if the presentation is already active
            if (myPresentation == null || myPresentation.getDisplay() != hdmiDisplay) {
                dismissPresentation(); // Dismiss old instance if needed
                myPresentation = new MyPresentation(this, hdmiDisplay);
                myPresentation.show();
                Log.i(TAG, "Presentation shown on external display.");
            } else {
                Log.i(TAG, "Presentation already active.");
            }
        }
    }



    private void dismissPresentation() {
        if (myPresentation != null) {
            myPresentation.dismiss();
            myPresentation = null;
            Log.i(TAG, "Presentation dismissed.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(hdmiReceiver);
        if (mainWebView != null) {
            mainWebView.destroy();
        }
        dismissPresentation();
    }

    @Override
    public void onBackPressed() {
        if (mainWebView.canGoBack()) {
            mainWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mainWebView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainWebView.onResume();
        showPresentation();
    }
}
