package com.pilamogar.compas;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;

/**
 * Compás flamenco para Android: la misma app web, servida desde los archivos
 * incluidos en el APK (funciona sin conexión).
 *
 * Las páginas se cargan desde https://appassets.androidplatform.net/ y cada
 * petición a ese dominio se contesta con el archivo de assets/www, así fetch()
 * puede cargar los sonidos igual que en el navegador.
 */
public class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        // La pantalla no se apaga mientras practicas
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ponerTema("dark");

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0F0F12"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setUserAgentString(s.getUserAgentString() + " CompasApp");
        web.addJavascriptInterface(new Puente(), "CompasApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                return servir(req.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri u = Uri.parse(url);
                if (HOST.equals(u.getHost())) return false;
                // Enlaces externos (créditos) en el navegador
                try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, u)); } catch (Exception e) { }
                return true;
            }
        });
        setContentView(web);
        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("https://" + HOST + "/index.html");
    }

    private WebResourceResponse servir(Uri u) {
        if (!HOST.equals(u.getHost())) return null;
        String ruta = u.getPath();
        if (ruta == null || ruta.equals("/") || ruta.isEmpty()) ruta = "/index.html";
        if (ruta.contains("..")) return null;
        try {
            InputStream in = getAssets().open("www" + ruta);
            return new WebResourceResponse(tipo(ruta), null, in);
        } catch (IOException e) {
            WebResourceResponse r = new WebResourceResponse("text/plain", "utf-8", null);
            r.setStatusCodeAndReasonPhrase(404, "Not Found");
            return r;
        }
    }

    private static String tipo(String ruta) {
        String r = ruta.toLowerCase();
        if (r.endsWith(".html")) return "text/html";
        if (r.endsWith(".js")) return "application/javascript";
        if (r.endsWith(".css")) return "text/css";
        if (r.endsWith(".json") || r.endsWith(".webmanifest")) return "application/json";
        if (r.endsWith(".png")) return "image/png";
        if (r.endsWith(".svg")) return "image/svg+xml";
        if (r.endsWith(".wav")) return "audio/wav";
        if (r.endsWith(".mp3")) return "audio/mpeg";
        return "application/octet-stream";
    }

    private void ponerTema(String tema) {
        boolean claro = "light".equals(tema);
        Window w = getWindow();
        int color = Color.parseColor(claro ? "#F4F4F2" : "#0F0F12");
        w.setStatusBarColor(color);
        w.setNavigationBarColor(color);
        if (Build.VERSION.SDK_INT >= 23) {
            View d = w.getDecorView();
            int f = d.getSystemUiVisibility();
            f = claro ? (f | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) : (f & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            d.setSystemUiVisibility(f);
        }
    }

    /** Lo que la página puede pedirle a la app. */
    private class Puente {
        @JavascriptInterface
        public void tema(final String t) {
            runOnUiThread(new Runnable() { public void run() { ponerTema(t); } });
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.compasAtras ? window.compasAtras() : false", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String v) {
                if (!"true".equals(v)) MainActivity.super.onBackPressed();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
