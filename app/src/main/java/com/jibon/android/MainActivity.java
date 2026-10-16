package com.jibon.android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.print.PrintManager;
import android.print.PrintAttributes;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private WebView webView;
    private static final int CAMERA_PERMISSION_REQUEST = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        webView.addJavascriptInterface(
            new PrintBridge(), "AndroidPrint"
        );

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                view.evaluateJavascript(
                    "(function() {" +
                    "window.print = function() {" +
                    "AndroidPrint.printPage();" +
                    "};" +
                    "})();",
                    null
                );
            }
        });

        

        setContentView(webView);
        webView.loadUrl("https://jibonder.pythonanywhere.com");
    }

    private class PrintBridge {
        @JavascriptInterface
        public void printPage() {
            runOnUiThread(() -> {
                PrintManager printManager =
                    (PrintManager) getSystemService(
                        Context.PRINT_SERVICE
                    );

                if (printManager != null && webView != null) {
                    PrintAttributes attributes =
                        new PrintAttributes.Builder()
                            .setMediaSize(
                                PrintAttributes.MediaSize.ISO_A4
                            )
                            .setResolution(
                                new PrintAttributes.Resolution(
                                    "pdf", "PDF", 300, 300
                                )
                            )
                            .setMinMargins(
                                PrintAttributes.Margins.NO_MARGINS
                            )
                            .build();

                    printManager.print(
                        "Jibon Report",
                        webView.createPrintDocumentAdapter(
                            "Jibon Report"
                        ),
                        attributes
                    );
                }
            });
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
