package com.streambrowser.ui;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Página de inicio personalizada
    private static final String HOME_HTML =
        "<!DOCTYPE html><html><head>" +
        "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
        "<style>" +
        "*{margin:0;padding:0;box-sizing:border-box;}" +
        "body{background:#0A0A0F;font-family:'Segoe UI',sans-serif;color:#E0E0FF;" +
        "display:flex;flex-direction:column;align-items:center;justify-content:center;" +
        "min-height:100vh;overflow:hidden;}" +
        ".particles{position:fixed;top:0;left:0;width:100%;height:100%;z-index:0;pointer-events:none;}" +
        ".content{position:relative;z-index:1;text-align:center;padding:20px;width:100%;max-width:600px;}" +
        ".logo{font-size:13px;color:#7B68EE;letter-spacing:3px;text-transform:uppercase;" +
        "margin-bottom:8px;opacity:0.8;}" +
        ".brand{font-size:36px;font-weight:700;background:linear-gradient(135deg,#7B68EE,#9B8FFF,#C9B8FF);" +
        "-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:4px;}" +
        ".tagline{font-size:12px;color:#5A5A8A;margin-bottom:40px;letter-spacing:1px;}" +
        ".search-box{background:rgba(255,255,255,0.05);border:1px solid rgba(123,104,238,0.3);" +
        "border-radius:50px;padding:16px 24px;width:100%;font-size:16px;color:#E0E0FF;" +
        "outline:none;backdrop-filter:blur(10px);transition:all 0.3s;}" +
        ".search-box:focus{border-color:#7B68EE;background:rgba(123,104,238,0.1);" +
        "box-shadow:0 0 20px rgba(123,104,238,0.3);}" +
        ".search-box::placeholder{color:#44445A;}" +
        ".quick-links{display:flex;gap:12px;margin-top:24px;flex-wrap:wrap;justify-content:center;}" +
        ".link{background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.08);" +
        "border-radius:12px;padding:10px 16px;font-size:12px;color:#8888AA;text-decoration:none;" +
        "transition:all 0.2s;display:flex;align-items:center;gap:6px;}" +
        ".link:hover{background:rgba(123,104,238,0.15);border-color:#7B68EE;color:#C9B8FF;}" +
        ".footer{position:fixed;bottom:20px;font-size:10px;color:#2A2A4A;letter-spacing:2px;" +
        "text-transform:uppercase;}" +
        ".shield{font-size:11px;color:#3A3A6A;margin-top:16px;}" +
        "</style></head><body>" +
        "<canvas class='particles' id='c'></canvas>" +
        "<div class='content'>" +
        "<div class='logo'>StreamBrowser</div>" +
        "<div class='brand'>Grupo Gevizz</div>" +
        "<div class='tagline'>Líder en Desarrollo Web &amp; Soluciones Digitales</div>" +
        "<input class='search-box' id='s' type='text' placeholder='Busca en la web o ingresa una URL...' " +
        "onkeydown=\"if(event.key==='Enter'){var v=this.value.trim();" +
        "window.location.href=v.includes('.')?'https://'+v:'https://www.google.com/search?q='+encodeURIComponent(v);}\">" +
        "<div class='quick-links'>" +
        "<a class='link' href='https://valtaraexecutive.com'>🌐 Valtara Executive</a>" +
        "<a class='link' href='https://google.com'>🔍 Google</a>" +
        "<a class='link' href='https://youtube.com'>▶ YouTube</a>" +
        "<a class='link' href='https://duckduckgo.com'>🦆 DuckDuckGo</a>" +
        "</div>" +
        "<div class='shield'>🛡️ Navegación protegida · AdBlock activo · Sin rastreadores</div>" +
        "</div>" +
        "<div class='footer'>Grupo Gevizz · Líder en Desarrollo Web</div>" +
        "<script>" +
        "document.getElementById('s').focus();" +
        "var c=document.getElementById('c'),ctx=c.getContext('2d');" +
        "c.width=window.innerWidth;c.height=window.innerHeight;" +
        "var pts=[];for(var i=0;i<60;i++)pts.push({x:Math.random()*c.width," +
        "y:Math.random()*c.height,r:Math.random()*1.5+0.5," +
        "dx:(Math.random()-0.5)*0.3,dy:(Math.random()-0.5)*0.3," +
        "o:Math.random()*0.5+0.1});" +
        "function draw(){ctx.clearRect(0,0,c.width,c.height);" +
        "pts.forEach(function(p){ctx.beginPath();ctx.arc(p.x,p.y,p.r,0,Math.PI*2);" +
        "ctx.fillStyle='rgba(123,104,238,'+p.o+')';ctx.fill();" +
        "p.x+=p.dx;p.y+=p.dy;" +
        "if(p.x<0||p.x>c.width)p.dx*=-1;" +
        "if(p.y<0||p.y>c.height)p.dy*=-1;});" +
        "requestAnimationFrame(draw);}draw();" +
        "</script></body></html>";

    private WebView webView;
    private EditText urlBar;
    private ProgressBar progressBar;
    private ImageButton btnCast;
    private CastManager castManager;
    private String lastDetectedVideoUrl = null;
    private String lastDetectedVideoTitle = null;
    private final List<String> detectedVideos = new ArrayList<>();
    private int blockedAdsCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pantalla siempre encendida durante streaming
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        // AdBlocker en background
        new Thread(() -> AdBlocker.getInstance().init(this)).start();

        // Cast context
        try { CastContext.getSharedInstance(this); }
        catch (Exception e) { Log.w(TAG, "Cast no disponible: " + e.getMessage()); }

        setupCastManager();
        setupViews();
        setupWebView();

        // Cargar página de inicio
        webView.loadDataWithBaseURL("https://home.streambrowser/",
            HOME_HTML, "text/html", "UTF-8", null);
    }

    private void setupCastManager() {
        castManager = new CastManager(this, new CastManager.OnCastStatusListener() {
            @Override public void onConnected() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                        "📺 TV conectada", Toast.LENGTH_SHORT).show();
                    if (btnCast != null) btnCast.setColorFilter(Color.parseColor("#7B68EE"));
                });
            }
            @Override public void onDisconnected() {
                runOnUiThread(() ->
                    Toast.makeText(MainActivity.this,
                        "TV desconectada", Toast.LENGTH_SHORT).show());
            }
            @Override public void onError(String msg) {
                runOnUiThread(() ->
                    Toast.makeText(MainActivity.this,
                        "⚠️ " + msg, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void setupViews() {
        webView    = findViewById(R.id.webView);
        urlBar     = findViewById(R.id.urlBar);
        progressBar= findViewById(R.id.progressBar);
        btnCast    = findViewById(R.id.btnCast);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });
        findViewById(R.id.btnForward).setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });
        findViewById(R.id.btnRefresh).setOnClickListener(v -> webView.reload());
        findViewById(R.id.btnHome).setOnClickListener(v ->
            webView.loadDataWithBaseURL("https://home.streambrowser/",
                HOME_HTML, "text/html", "UTF-8", null));

        btnCast.setOnClickListener(v -> onCastButtonClicked());

        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                navigate(urlBar.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });

        // Setup MediaRoute button para Cast
        MediaRouteButton mrBtn = findViewById(R.id.mediaRouteButton);
        if (mrBtn != null) {
            try { CastButtonFactory.setUpMediaRouteButton(this, mrBtn); }
            catch (Exception e) { Log.w(TAG, "MediaRoute setup failed"); }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();

        // === FUNCIONALIDAD ===
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        // === SEGURIDAD MILITAR ===
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSaveFormData(false);
        s.setSavePassword(false);
        s.setGeolocationEnabled(false);

        // === MODO OSCURO — compatible con todas las versiones ===
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // API 33+ : usar CSS vía JS (setForceDark eliminado)
            // El modo oscuro se aplica por injectDarkModeJS() en onPageFinished
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // API 29-32
            webView.getSettings().setForceDark(WebSettings.FORCE_DARK_ON);
        }

        // User Agent moderno
        s.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

        // === BLOQUEO DE DESCARGAS (TOTAL) ===
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            new AlertDialog.Builder(this)
                .setTitle("🛡️ Descarga bloqueada")
                .setMessage("StreamBrowser bloqueó una descarga automática por seguridad.\n\n" +
                    "Tipo: " + mimeType)
                .setPositiveButton("OK", null)
                .show();
            Log.w(TAG, "DOWNLOAD BLOCKED: " + url);
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                if (url.startsWith("http://") && !url.startsWith("http://localhost")) {
                    Log.d(TAG, "HTTP blocked: " + url);
                    return emptyResponse();
                }

                if (AdBlocker.getInstance().shouldBlock(url)) {
                    blockedAdsCount++;
                    return emptyResponse();
                }

                detectVideoUrl(url);

                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                if (!url.startsWith("data:")) urlBar.setText(url);
                lastDetectedVideoUrl = null;
                detectedVideos.clear();
                if (btnCast != null) btnCast.setAlpha(0.5f);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                if (!url.startsWith("data:")) urlBar.setText(url);
                injectSecurityJS();
                injectCosmeticCSS();
                injectVideoDetectionJS();
                injectDarkModeJS();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (url.startsWith("javascript:") || url.startsWith("vbscript:") ||
                    url.startsWith("file:") || url.startsWith("intent:") ||
                    url.startsWith("android-app:")) {
                    Log.w(TAG, "DANGEROUS URL blocked: " + url);
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) progressBar.setVisibility(View.GONE);
                else progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                lastDetectedVideoTitle = title;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.deny();
                Log.w(TAG, "Permission DENIED: " +
                    java.util.Arrays.toString(request.getResources()));
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, false, false);
            }

            @Override
            public boolean onJsAlert(WebView view, String url,
                    String message, JsResult result) {
                result.cancel();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url,
                    String message, JsResult result) {
                result.cancel();
                return true;
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage msg) {
                String m = msg.message();
                if (m != null && m.startsWith("STREAMBROWSER_VIDEO:")) {
                    String videoUrl = m.substring(20);
                    if (!videoUrl.isEmpty() && !detectedVideos.contains(videoUrl)) {
                        detectedVideos.add(videoUrl);
                        lastDetectedVideoUrl = videoUrl;
                        runOnUiThread(() -> {
                            if (btnCast != null) {
                                btnCast.setAlpha(1.0f);
                                btnCast.setColorFilter(Color.parseColor("#7B68EE"));
                            }
                        });
                    }
                }
                return true;
            }
        });
    }

    private void injectSecurityJS() {
        webView.evaluateJavascript(
            "(function(){" +
            "if(window.Notification)window.Notification=function(){return{}};" +
            "if(navigator.serviceWorker)navigator.serviceWorker.register=function(){" +
            "  return Promise.reject('blocked');};" +
            "if(window.RTCPeerConnection)window.RTCPeerConnection=function(){return null;};" +
            "if(window.webkitRTCPeerConnection)window.webkitRTCPeerConnection=function(){return null;};" +
            "Object.defineProperty(navigator,'plugins',{get:function(){return[];}});" +
            "Object.defineProperty(navigator,'languages',{get:function(){return['es-MX','es'];}});" +
            "window.open=function(){return null;};" +
            "window.alert=function(){};" +
            "window.confirm=function(){return false;};" +
            "window.prompt=function(){return null;};" +
            "})();", null);
    }

    private void injectCosmeticCSS() {
        String css = AdBlocker.getCosmeticCSS()
            .replace("'", "\\'").replace("\n", "");
        webView.evaluateJavascript(
            "(function(){" +
            "var s=document.createElement('style');" +
            "s.innerHTML='" + css + "';" +
            "document.head&&document.head.appendChild(s);" +
            "})();", null);
    }

    private void injectDarkModeJS() {
        webView.evaluateJavascript(
            "(function(){" +
            "var s=document.createElement('style');" +
            "s.innerHTML='html{filter:invert(0.9) hue-rotate(180deg)!important;}" +
            "img,video,canvas,iframe{filter:invert(1) hue-rotate(180deg)!important;}';"+
            "document.head&&document.head.appendChild(s);" +
            "})();", null);
    }

    private void injectVideoDetectionJS() {
        webView.evaluateJavascript(
            "(function(){" +
            "function report(u){if(u&&u.length>5)console.log('STREAMBROWSER_VIDEO:'+u);}" +
            "document.querySelectorAll('video').forEach(function(v){" +
            "  report(v.src||v.currentSrc);" +
            "  v.addEventListener('loadeddata',function(){report(v.currentSrc);});" +
            "});" +
            "document.querySelectorAll('video source').forEach(function(s){report(s.src);});" +
            "document.querySelectorAll('iframe').forEach(function(f){" +
            "  try{var v=f.contentDocument.querySelector('video');" +
            "  if(v)report(v.src||v.currentSrc);}catch(e){}" +
            "});" +
            "var origFetch=window.fetch;" +
            "window.fetch=function(u,o){" +
            "  if(typeof u==='string'&&(u.includes('.m3u8')||u.includes('.mpd')||u.includes('.mp4')))" +
            "    console.log('STREAMBROWSER_VIDEO:'+u);" +
            "  return origFetch.apply(this,arguments);};" +
            "var oXHR=XMLHttpRequest.prototype.open;" +
            "XMLHttpRequest.prototype.open=function(m,u){" +
            "  if(typeof u==='string'&&(u.includes('.m3u8')||u.includes('.mpd')||u.includes('.mp4')))" +
            "    console.log('STREAMBROWSER_VIDEO:'+u);" +
            "  return oXHR.apply(this,arguments);};" +
            "})();", null);
    }

    private void detectVideoUrl(String url) {
        if (url == null) return;
        String lower = url.toLowerCase();
        boolean isVideo =
            lower.contains(".m3u8") || lower.contains(".mp4") ||
            lower.contains(".mpd")  || lower.contains(".webm") ||
            lower.contains(".mkv")  || lower.contains(".avi") ||
            lower.contains(".mov")  || lower.contains(".flv") ||
            lower.contains("manifest") || lower.contains("playlist") ||
            lower.contains("/video/") || lower.contains("videoplayback") ||
            lower.contains("stream") || lower.contains("media") &&
            (lower.contains("token") || lower.contains("cdn"));

        if (isVideo && !detectedVideos.contains(url)) {
            detectedVideos.add(url);
            lastDetectedVideoUrl = url;
            runOnUiThread(() -> {
                if (btnCast != null) btnCast.setAlpha(1.0f);
            });
        }
    }

    private void onCastButtonClicked() {
        if (detectedVideos.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("🎬 No se detectó video")
                .setMessage("Reproduce el video primero en la página.\n\n" +
                    "El botón Cast se iluminará automáticamente cuando detecte un stream.")
                .setPositiveButton("OK", null).show();
            return;
        }

        if (!castManager.isConnected()) {
            Toast.makeText(this,
                "📡 Selecciona tu TV usando el botón de Cast en la barra superior",
                Toast.LENGTH_LONG).show();
            return;
        }

        if (detectedVideos.size() == 1) {
            confirmCast(detectedVideos.get(0));
        } else {
            String[] items = detectedVideos.toArray(new String[0]);
            new AlertDialog.Builder(this)
                .setTitle("🎬 Selecciona el video")
                .setItems(items, (d, which) -> confirmCast(items[which]))
                .show();
        }
    }

    private void confirmCast(String videoUrl) {
        String mime = CastManager.detectMimeType(videoUrl);
        new AlertDialog.Builder(this)
            .setTitle("📺 Enviar a TV")
            .setMessage("¿Transmitir a tu dispositivo?\n\nFormato: " + mime)
            .setPositiveButton("Transmitir", (d, w) ->
                castManager.castVideo(videoUrl, lastDetectedVideoTitle, mime))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void navigate(String input) {
        if (input == null || input.isEmpty()) return;
        String url;
        if (input.startsWith("http://") || input.startsWith("https://")) url = input;
        else if (input.contains(".") && !input.contains(" ")) url = "https://" + input;
        else url = "https://www.google.com/search?q=" + Uri.encode(input);
        webView.loadUrl(url);
    }

    private WebResourceResponse emptyResponse() {
        return new WebResourceResponse("text/plain", "utf-8",
            new ByteArrayInputStream(new byte[0]));
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
            getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onResume() { super.onResume(); webView.onResume(); }

    @Override
    protected void onPause() { super.onPause(); webView.onPause(); }

    @Override
    protected void onDestroy() {
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
        super.onDestroy();
    }
                    }
