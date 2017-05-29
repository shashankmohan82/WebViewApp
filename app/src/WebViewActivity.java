package extension.hatke.com.shophatke;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

public class WebViewActivity extends AppCompatActivity {

    private static final String _URL = "https://www.google.com/search?q=%1s";
    private WebView myWebView;
    private ProgressBar progressBar;
    private String[] shoppingCartItems;
    String zalando_tag = "z-coast-fjord_article";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        Intent intent = this.getIntent();
        String query = intent.getStringExtra("query");
//        String search = "";
//        if(!query.isEmpty()) {
//            search = String.format(_URL, query);
//        }else {
//            Toast.makeText(WebViewActivity.this, "Invalid Query", Toast.LENGTH_SHORT).show();
//            finish();
//        }
        myWebView = (WebView) findViewById(R.id.webView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        setUpToolbar();
        try {
            progressBar.setMax(100);
            myWebView.getSettings().setJavaScriptEnabled(true);
            myWebView.setWebChromeClient(new MyWebViewClient_Chrome());
            myWebView.setWebViewClient(new MyWebViewClient());


        }catch (Exception e) {
            e.printStackTrace();
        }
        myWebView.addJavascriptInterface(new MyJavaScriptInterface(this), "ButtonRecognizer");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                loadEvent(clickListener());
            }

            private void loadEvent(String javascript){
                myWebView.loadUrl("javascript:"+javascript);
            }

            private String clickListener(){
                return getButtons()+ "for(var i = 0; i < buttons.length; i++){\n" +
                        "\tbuttons[i].onclick = function(){ console.log('click worked.'); ButtonRecognizer.boundMethod('button clicked'); };\n" +
                        "}";
            }

            private String getButtons(){
                return "var buttons = document.getElementsByClassName('add-to-cart'); console.log(buttons.length + ' buttons');\n";
            }
        });

        //myWebView.loadUrl("http://store.nike.com/ch/de_de/pd/mercurial-superfly-v-tech-cra\u200C\u200Bft-2-herren-fussballschuh-fur-normalen-rasen/pid-11229711/pgid-11626158");
        startService(new Intent(this, HatkeFloatingButton.class));
    }

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        assert toolbar != null;
        toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    private void displayItems() {
        for (String shoppingCartItem : shoppingCartItems) {
            Log.d("ShoppingCart", cleanString(shoppingCartItem));
        }
    }

    private String cleanString(String shoppingCartItem) {
        shoppingCartItem = shoppingCartItem.replace("Ändern", "");
        shoppingCartItem = shoppingCartItem.replace("Entfernen", "");
        shoppingCartItem = shoppingCartItem.replace("Bearbeiten", "");
        shoppingCartItem = shoppingCartItem.replace("Löschen", "");
        shoppingCartItem = shoppingCartItem.replace("icon-cart-minus", "");
        shoppingCartItem = shoppingCartItem.replace("icon-cart-plus", "");
        shoppingCartItem = shoppingCartItem.replace("Service + Zubehör", "");
        shoppingCartItem = shoppingCartItem.replaceAll("(?m)^[ \t]*\r?\n", "");
        return shoppingCartItem;
    }

    class MyJavaScriptInterface {

        private Context ctx;

        MyJavaScriptInterface(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void boundMethod(String html) {
            new AlertDialog.Builder(ctx).setTitle("HTML").setMessage("It worked")
                    .setPositiveButton(android.R.string.ok, null).setCancelable(false).create().show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (myWebView.canGoBack()) {
                        Log.d("back", "OK");
                        myWebView.goBack();
                    }
                    else{
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setProgress(0);

        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            progressBar.setVisibility(View.VISIBLE);
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(WebViewActivity.this, "Oh no! " + description, Toast.LENGTH_SHORT).show();
        }


    }

    private class MyWebViewClient_Chrome extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
            if (newProgress == 100)
                progressBar.setVisibility(View.GONE);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(new Intent(this, HatkeFloatingButton.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(this, HatkeFloatingButton.class));
    }
}
