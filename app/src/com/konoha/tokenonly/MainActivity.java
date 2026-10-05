package com.konoha.tokenonly;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String GAME_HOST = "konohagame.com";
    private static final String GAME_URL = "https://konohagame.com/";
    private static final String TOKEN_KEY = "konoha_pwa_token";
    private static final String PREFS = "konoha_token_only_prefs";
    private static final String PREF_TOKEN = "saved_token";

    private WebView webView;
    private FrameLayout root;
    private View inputScreen;
    private SharedPreferences prefs;
    private String pendingToken = null;
    private boolean tokenInjected = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        root = new FrameLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(Color.WHITE);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setUserAgentString(ws.getUserAgentString());

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.contains(GAME_HOST)) {
                    return false;
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url == null || !url.contains(GAME_HOST)) return;
                if (pendingToken == null || pendingToken.length() == 0) return;
                if (!tokenInjected) {
                    tokenInjected = true;
                    view.evaluateJavascript(buildTokenJs(pendingToken), null);
                    view.reload();
                }
            }
        });

        root.addView(webView);

        String saved = prefs.getString(PREF_TOKEN, "");
        if (saved != null && saved.length() > 0) {
            openGame(saved);
        } else {
            showInputScreen("");
        }
    }

    private void showInputScreen(String initial) {
        if (inputScreen != null && inputScreen.getParent() != null) {
            return;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#F5F7F5"));
        scroll.setFillViewport(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(24);
        box.setPadding(pad, dp(48), pad, dp(24));
        scroll.addView(box, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Konoha Token");
        title.setTextColor(Color.parseColor("#1B5E20"));
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Masukkan token PWA (konoha_pwa_token)");
        sub.setTextColor(Color.parseColor("#555555"));
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(8);
        subLp.bottomMargin = dp(24);
        box.addView(sub, subLp);

        final EditText input = new EditText(this);
        input.setHint("Tempel token di sini");
        input.setText(initial);
        input.setSingleLine(false);
        input.setMinLines(3);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setBackgroundColor(Color.WHITE);
        input.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        box.addView(input, inLp);

        Button save = new Button(this);
        save.setText("Simpan & Buka");
        save.setAllCaps(false);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        btnLp.topMargin = dp(20);
        box.addView(save, btnLp);

        final TextView hint = new TextView(this);
        hint.setTextColor(Color.parseColor("#888888"));
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(12);
        blockAdd(box, hint, hintLp);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String token = input.getText().toString().trim();
                if (token.length() == 0) {
                    Toast.makeText(MainActivity.this, "Token tidak boleh kosong", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.edit().putString(PREF_TOKEN, token).apply();
                openGame(token);
            }
        });

        inputScreen = scroll;
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void blockAdd(LinearLayout parent, View child, LinearLayout.LayoutParams lp) {
        parent.addView(child, lp);
    }

    private void openGame(String token) {
        if (inputScreen != null && inputScreen.getParent() != null) {
            root.removeView(inputScreen);
        }
        webView.setVisibility(View.VISIBLE);
        pendingToken = token;
        tokenInjected = false;
        webView.loadUrl(GAME_URL);
    }

    private String buildTokenJs(String token) {
        return "(function(){try{localStorage.setItem('" + TOKEN_KEY + "',"
                + jsString(token) + ");return 'ok';}catch(e){return 'err:'+e;}})();";
    }

    private static String jsString(String s) {
        StringBuilder sb = new StringBuilder("'");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '<': sb.append("\\u003c"); break;
                default: sb.append(c);
            }
        }
        sb.append("'");
        return sb.toString();
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    @Override
    public void onBackPressed() {
        if (inputScreen != null && inputScreen.getParent() != null) {
            super.onBackPressed();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
