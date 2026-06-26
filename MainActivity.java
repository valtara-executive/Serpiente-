package com.streambrowser.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.mediarouter.app.MediaRouteButton;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.streambrowser.R;
import com.streambrowser.adblock.AdBlocker;
import com.streambrowser.cast.CastManager;

import java.io.ByteArrayInputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String HOME_URL = "https://pelisfliz.bz";

    private WebView webView;
    private EditText urlBar;
    private ProgressBar progressBar;
    private CastManager castManager;
    private String lastDetectedVideoUrl = null;
    private String lastDetectedVideoTitle = null;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init AdBlocker in background
        new Thread(() -> AdBlocker.getInstance().init(this)).start();

        // Init Cast context
        try {
            CastContext.getSharedInstance(this);
        } catch (Exception e) {
            Log.w(TAG, "Cast not available: " + e.getMessage());
        }

        setupCastManager();
        setupViews();
        setupWebView();

        webView.loadUrl(HOME_URL);
    }

    private void setupCastManager() {
        castManager = new CastManager(this, new CastManager.OnCastStatusListener() {
            @Override public void onConnected() {
                Toast.makeText(MainActivity.this, "📺 Dispositivo Cast conectado", Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
            }
            @Override public void onDisconnected() {
                Toast.makeText(MainActivity.this, "Cast desconectado", Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
            }
            @Override public void onError(String msg) {
                Toast.makeText(MainActivity.this, "⚠️ " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupViews() {
        webView = findViewById(R.id.webView);
        urlBar = findViewById(R.id.urlBar);
        progressBar = findViewById(R.id.progressBar);

        // URL bar action
        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                navigate(urlBar.getText().toString().trim());
                return true;
            }
            return false;
        });

        // Back / forward buttons
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });
        findViewById(R.id.btnForward).setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });
        findViewById(R.id.btnRefresh).setOnClickListener(v -> webView.reload());
        findViewById(R.id.btnHome).setOnClickListener(v -> webView.loadUrl(HOME_URL));
        findViewById(R.id.btnCast).setOnClickListener(v -> onCastButtonClicked());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        );

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Block ads
                if (AdBlocker.getInstance().shouldBlock(url)) {
                    Log.d(TAG, "BLOCKED: " + url);
                    return emptyResponse();
                }

                // Detect video stream URLs
                detectVideoUrl(url);

                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                urlBar.setText(url);
                lastDetectedVideoUrl = null; // Reset on new page
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                urlBar.setText(url);
                // Inject JS to detect video elements after page load
                injectVideoDetectionJS();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                lastDetectedVideoTitle = title;
            }
        });
    }

    /**
     * Detects if a network request URL looks like a video stream.
     */
    private void detectVideoUrl(String url) {
        if (url == null) return;
        String lower = url.toLowerCase();
        if (lower.contains(".m3u8") || lower.contains(".mp4") ||
            lower.contains(".mpd") || lower.contains(".webm") ||
            lower.contains("manifest") || lower.contains("playlist")) {
            lastDetectedVideoUrl = url;
            Log.d(TAG, "Video URL detected: " + url);
            runOnUiThread(() -> {
                View castBtn = findViewById(R.id.btnCast);
                if (castBtn != null) castBtn.setAlpha(1.0f); // Highlight cast button
            });
        }
    }

    /**
     * Inject JS that finds <video> src and posts it back via console.
     */
    private void injectVideoDetectionJS() {
        webView.evaluateJavascript(
            "(function() {" +
            "  var videos = document.querySelectorAll('video');" +
            "  if (videos.length > 0) {" +
            "    var src = videos[0].src || videos[0].currentSrc;" +
            "    if (src) console.log('STREAMBROWSER_VIDEO:' + src);" +
            "  }" +
            "  var iframes = document.querySelectorAll('iframe');" +
            "  iframes.forEach(function(f) {" +
            "    try {" +
            "      var v = f.contentDocument.querySelector('video');" +
            "      if (v) console.log('STREAMBROWSER_VIDEO:' + (v.src || v.currentSrc));" +
            "    } catch(e) {}" +
            "  });" +
            "})();",
            null
        );
    }

    private void onCastButtonClicked() {
        if (lastDetectedVideoUrl == null) {
            new AlertDialog.Builder(this)
                .setTitle("No se detectó video")
                .setMessage("Reproduce el video primero en la página y luego presiona Cast.\n\n" +
                           "Si el video ya está reproduciéndose, el stream será detectado automáticamente.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        if (!castManager.isConnected()) {
            Toast.makeText(this, "Selecciona un dispositivo Cast usando el ícono 📡 en la barra superior", Toast.LENGTH_LONG).show();
            return;
        }

        String mimeType = CastManager.detectMimeType(lastDetectedVideoUrl);
        new AlertDialog.Builder(this)
            .setTitle("📺 Enviar a TV")
            .setMessage("¿Transmitir este video a tu dispositivo Cast?\n\n" + lastDetectedVideoUrl)
            .setPositiveButton("Transmitir", (d, w) ->
                castManager.castVideo(lastDetectedVideoUrl, lastDetectedVideoTitle, mimeType))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void navigate(String input) {
        String url;
        if (input.startsWith("http://") || input.startsWith("https://")) {
            url = input;
        } else if (input.contains(".") && !input.contains(" ")) {
            url = "https://" + input;
        } else {
            url = "https://www.google.com/search?q=" + input.replace(" ", "+");
        }
        webView.loadUrl(url);
    }

    private WebResourceResponse emptyResponse() {
        return new WebResourceResponse("text/plain", "utf-8",
            new ByteArrayInputStream(new byte[0]));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        // Setup Cast button in toolbar
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        if (mediaRouteMenuItem != null) {
            CastButtonFactory.setUpMediaRouteButton(
                getApplicationContext(), menu, R.id.media_route_menu_item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }
}
