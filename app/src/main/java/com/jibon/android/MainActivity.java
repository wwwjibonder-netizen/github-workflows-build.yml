package com.jibon.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintManager;
import android.print.PrintAttributes;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.Manifest;

public class MainActivity extends Activity {

    private WebView webView;

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int FILE_REQUEST = 101;

    private PermissionRequest pendingPermissionRequest;
    private ValueCallback<Uri[]> fileCallback;

    private final String HOME_URL =
            "https://jibonder.pythonanywhere.com";

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
        settings.setAllowFileAccess(false);
        settings.setSupportMultipleWindows(false);

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        webView.addJavascriptInterface(
                new PrintBridge(), "AndroidPrint"
        );

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request) {

                Uri uri = request.getUrl();

                if ("https".equalsIgnoreCase(uri.getScheme())
                        || "http".equalsIgnoreCase(uri.getScheme())) {
                    return false;
                }

                return true;
            }

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

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(
                    PermissionRequest request) {

                runOnUiThread(() -> {
                    boolean wantsCamera = false;

                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE
                                .equals(resource)) {
                            wantsCamera = true;
                            break;
                        }
                    }

                    if (!wantsCamera) {
                        request.deny();
                        return;
                    }

                    if (checkSelfPermission(Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {

                        request.grant(new String[] {
                                PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        });

                    } else {
                        pendingPermissionRequest = request;

                        requestPermissions(
                                new String[] {
                                        Manifest.permission.CAMERA
                                },
                                CAMERA_PERMISSION_REQUEST
                        );
                    }
                });
            }

            @Override
            public void onPermissionRequestCanceled(
                    PermissionRequest request) {

                if (pendingPermissionRequest == request) {
                    pendingPermissionRequest = null;
                }
            }

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, FILE_REQUEST);
                    return true;

                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }
        });

        webView.setOnTouchListener(new View.OnTouchListener() {
    private float startY;
    private boolean refreshing = false;

    @Override
    public boolean onTouch(View view,
                           android.view.MotionEvent event) {
        switch (event.getAction()) {
            case android.view.MotionEvent.ACTION_DOWN:
                startY = event.getY();
                refreshing = false;
                break;

            case android.view.MotionEvent.ACTION_UP:
                float distance = event.getY() - startY;

                if (distance > 180
                        && webView.getScrollY() == 0
                        && !refreshing) {
                    refreshing = true;
                    webView.reload();
                }
                break;
        }

        return false;
    }
});
        setContentView(webView);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(HOME_URL);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults
        );

        if (requestCode == CAMERA_PERMISSION_REQUEST) {

            PermissionRequest request = pendingPermissionRequest;
            pendingPermissionRequest = null;

            if (request == null) {
                return;
            }

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                request.grant(new String[] {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE
                });

            } else {
                request.deny();
            }
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_REQUEST && fileCallback != null) {

            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {
                Uri result = data.getData();

                if (result != null) {
                    results = new Uri[] { result };
                }
            }

            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    
private class PrintBridge {

    @JavascriptInterface
    public void printPage() {
        runOnUiThread(() -> {
            if (webView == null) return;

            webView.evaluateJavascript(
                "(function() {" +
                "var style = document.getElementById('android-print-style');" +
                "if (!style) {" +
                "style = document.createElement('style');" +
                "style.id = 'android-print-style';" +
                "document.head.appendChild(style);" +
                "}" +
                "style.textContent = '@media print {' +" +
                "'@page { size: A4; margin: 12mm; }' +" +
                "'html, body { height: auto !important; overflow: visible !important; }' +" +
                "'table { width: 100% !important; border-collapse: collapse; }' +" +
                "'tr { break-inside: avoid; page-break-inside: avoid; }' +" +
                "'thead { display: table-header-group; }' +" +
                "'tfoot { display: table-footer-group; }' +" +
                "'img { max-width: 100% !important; }' +" +
                "'}' ;" +
                "window.AndroidPrintReady = true;" +
                "})();",
                result -> {
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
                }
            );
        });
    }
}


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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (webView != null) {
            webView.saveState(outState);
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

    @Override
    protected void onDestroy() {

        if (pendingPermissionRequest != null) {
            pendingPermissionRequest.deny();
            pendingPermissionRequest = null;
        }

        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }

        super.onDestroy();
    }
}
