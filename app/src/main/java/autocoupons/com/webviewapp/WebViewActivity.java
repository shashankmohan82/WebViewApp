package autocoupons.com.webviewapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import autocoupons.com.webviewapp.receivers.OverlayObservable;
import autocoupons.com.webviewapp.receivers.WebviewObservable;

public class WebViewActivity extends AppCompatActivity implements Observer {
    private static final String TAG = WebViewActivity.class.getName();
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 120;
    private WebView webView;
    private ProgressBar progressBar;
    private EditText addressBar;
    private String url = "";
    private String myntraCouponString = "MYNTRANEW300~MYNTRANEW1000~MYNTRANEW600" +
            "~SKECH1000~SKECH2500~DESIFUSION20~SPECIALBIG20~VEROMODA10~";

    private boolean flag = true;
    private boolean isServiceOn = false;
    private boolean callBackStatus;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_home_black_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        if (!getIntent().getStringExtra("url").isEmpty()) {
            url = getIntent().getStringExtra("url");
        }

        /*Check for screen overlay permission*/
        if(!Utils.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        } else {
            initLayoutChild();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        WebviewObservable.getInstance().addObserver(this);
    }





    private void initLayoutChild() {
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        addressBar = (EditText) findViewById(R.id.addressBar);
        webView = (WebView) findViewById(R.id.webView);
        initWebView();
    }

    private void initWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        WebView.setWebContentsDebuggingEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new MyJavaScriptInterface(this), "android");
        webView.setWebChromeClient(new MyWebChromeClient(this));
        webView.setWebViewClient(new MyWebViewClient());
        webView.setHorizontalScrollBarEnabled(false);
        webView.loadUrl(url);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                        stopService(new Intent(WebViewActivity.this, AutoCouponsHeadService.class));
                        callBackStatus = false;
                        flag = true;
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                initLayoutChild();
            } else { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void update(Observable observable, Object o) {
        if(observable instanceof WebviewObservable) {
            Log.d("broadcast", "data received from overlay");
            myntraCouponString = String.valueOf(o);
            setAutoCouponsHeadStatus(true);
        }
    }


    private class MyWebChromeClient extends WebChromeClient {
        Context context;

        public MyWebChromeClient(Context context) {
            super();
            this.context = context;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            if (newProgress == 100)
                progressBar.setVisibility(View.GONE);
        }
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (flag && url.equals(RelevantSites.MYNTRA_CART.site)) {
                Intent in = new Intent(WebViewActivity.this, AutoCouponsHeadService.class);
                startService(in);
                flag = false;
                isServiceOn = true;
            } else if (!flag && !url.equals(RelevantSites.MYNTRA_CART.site)) {
                stopService(new Intent(WebViewActivity.this, AutoCouponsHeadService.class));
                isServiceOn = false;
                flag = true;
            }
            view.loadUrl(url);
            return true;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
            addressBar.setText(url);
            progressBar.setProgress(0);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (isServiceOn && callBackStatus && url.equals(RelevantSites.MYNTRA_CART.site)) {
                Log.d("test", url);
                String js = loadJSFile();
                view.loadUrl("javascript:" + js);
            }
        }

        private String loadJSFile() {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader fs = new BufferedReader(new InputStreamReader(getAssets().open("myntra.js")))) {
                String line;
                while ((line = fs.readLine()) != null) {
                    sb.append(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }


    }

    private void setAutoCouponsHeadStatus(boolean status) {
        callBackStatus = status;
        webView.reload();
    }

    private class MyJavaScriptInterface {

        private Context ctx;
        private List<Coupons> data;

        MyJavaScriptInterface(Context ctx) {
            this.ctx = ctx;
            this.data = Collections.EMPTY_LIST;
        }

        @JavascriptInterface
        public String getCouponString() {
            return myntraCouponString;
        }

        @JavascriptInterface
        public void sendDiscountCoupons(String json) {
            Toast.makeText(ctx, "Value received from js", Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(ctx).setTitle("HTML").setMessage(json)
                    .setPositiveButton(android.R.string.ok, null).setCancelable(false).create().show();
        }

        @JavascriptInterface
        public void sendCouponNumber(String coupon) {

//            Intent intent = new Intent(AutoCouponsHeadService.WebviewToSvcReceiver.WEBVIEW_TO_SVC);
//            intent.putExtra("COUPON_NUM", num);
//            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
            OverlayObservable.getInstance().updateValue(coupon);
        }
    }

    private enum RelevantSites {

        AMAZON("www.amazon.in/gp/buy/payselect/handlers/display.html"),
        JABONG("m.jabong.com/cart/"),
        MYNTRA_CART("https://secure.myntra.com/checkout/cart");

        private String site;

        RelevantSites(String s) {
            this.site = s;
        }
    }

    private class Coupons {
        private String coupon;
        private double discount;

        public Coupons(String coupon, double discount) {
            this.coupon = coupon;
            this.discount = discount;
        }

        public String getCoupon() {
            return coupon;
        }

        public double getDiscount() {
            return discount;
        }
    }



}