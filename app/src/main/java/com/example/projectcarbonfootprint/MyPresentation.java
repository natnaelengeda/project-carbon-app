package com.example.projectcarbonfootprint;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MyPresentation extends Presentation {

    private static final String TAG = "MyPresentation";
    private static final String URL = "http://10.40.0.200:1029/carbonfootprint/questions?room=101";

    private FrameLayout rotatedContainer;
    private WebView presentationWebView;
    private TextView positionIndicator;

    public MyPresentation(Context context, Display display) {
        super(context, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.presentation_layout);

        rotatedContainer = findViewById(R.id.rotated_container);
        presentationWebView = findViewById(R.id.webview_presentation);
        positionIndicator = findViewById(R.id.position_indicator);

        setupWebView();
        setupTouchTracking();
        setupInitialLayout();
    }

    private void setupWebView() {
        WebSettings settings = presentationWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        presentationWebView.setInitialScale(100);

        presentationWebView.setWebViewClient(new WebViewClient());
        presentationWebView.setBackgroundColor(Color.BLACK);
        presentationWebView.loadUrl(URL);
    }

    private void setupTouchTracking() {
        rotatedContainer.setOnTouchListener((v, event) -> {
            float x = event.getRawX();
            float y = event.getRawY();

            positionIndicator.setText("X: " + (int) x + ", Y: " + (int) y);
            positionIndicator.setX(x + 20);
            positionIndicator.setY(y - 60);
            positionIndicator.setVisibility(View.VISIBLE);

            if (event.getAction() == MotionEvent.ACTION_UP) {
                positionIndicator.setVisibility(View.GONE);
            }

            return false;
        });
    }

    private void setupInitialLayout() {
        rotatedContainer.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Point screenSize = new Point();
            if (wm != null) {
                wm.getDefaultDisplay().getRealSize(screenSize);
            }

            int screenWidth = screenSize.x;
            int screenHeight = screenSize.y;

            applyRotation(screenWidth, screenHeight);
        });
    }

    private void applyRotation(int screenWidth, int screenHeight) {
        // Set layout size
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(screenHeight, screenWidth);
        rotatedContainer.setLayoutParams(layoutParams);
        presentationWebView.setLayoutParams(layoutParams);

        // Apply 270-degree rotation
        rotatedContainer.setRotation(270f);

        // Translate to center
        float offsetX = (screenWidth - screenHeight) / 2f;
        float offsetY = (screenHeight - screenWidth) / 2f;

        rotatedContainer.setTranslationX(offsetX);
        rotatedContainer.setTranslationY(offsetY);

        Log.d(TAG, "applyRotation: Screen W=" + screenWidth + ", H=" + screenHeight);
        Log.d(TAG, "applyRotation: OffsetX=" + offsetX + ", OffsetY=" + offsetY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (presentationWebView != null) {
            presentationWebView.onPause();
        }
    }

//    @Override void dismiss(){
//
//    }

    @Override
    public void dismiss() {
        if (presentationWebView != null) {
            presentationWebView.onPause(); // Move from onStop()
            presentationWebView.destroy();
        }
        super.dismiss();
    }
}
