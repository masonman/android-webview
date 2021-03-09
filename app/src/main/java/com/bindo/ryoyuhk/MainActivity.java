package com.bindo.ryoyuhk;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static final String BASE_URL = BuildConfig.baseUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setStatusBarFullTransparent();

        getSupportActionBar().hide();

        WebView webView = findViewById(R.id.my_web_view);
        initWebSettings(webView, this);
        // JS调用 window.bindo_utils.getDeviceStatusBarHeight()，返回当前设备状态栏的高度（单位像素）
        // JS调用 window.bindo_utils.setStatusBarTextColor(String color)，通过传递参数（黑色："black"，白色："white"）来设置当前设备的状态栏字体颜色
        webView.addJavascriptInterface(this, "bindo_utils");
        webView.loadUrl(BASE_URL);
    }

    protected static void initWebSettings( WebView webView, Context context) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setDefaultTextEncodingName("UTF-8");//设置默认为utf-8
        webSettings.setTextZoom(100);//设置WebView中加载页面字体变焦百分比，默认100
        //属性可以让webview只显示一列，也就是自适应页面大小,不能左右滑动
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        } else {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        }

        //设置此属性，可任意比例缩放
        webSettings.setUseWideViewPort(false);
        webSettings.setLoadWithOverviewMode(true);

        //页面支持缩放
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setDisplayZoomControls(false);
        //设置支持js
        webSettings.setJavaScriptEnabled(true);
        //设置 缓存模式
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // 开启 DOM storage API 功能
        webSettings.setDomStorageEnabled(true);

        webView.setVerticalScrollBarEnabled(false);
        webView.setVerticalScrollbarOverlay(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setHorizontalScrollbarOverlay(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        // 适配刘海屏
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) webView.getLayoutParams();
        lp.topMargin = getStatusBarHeight(context);
        webView.setLayoutParams(lp);
    }

    /**
     * 全透状态栏
     */
    protected void setStatusBarFullTransparent() {
        if (Build.VERSION.SDK_INT >= 21) {//21表示5.0
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= 19) {//19表示4.4
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //虚拟键盘也透明
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    /**
     * 获取状态栏高度
     * @param context
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }

    /**
     * 提供H5来获取设备的顶部状态栏的高度
     * @return
     */
    @JavascriptInterface
    public int getDeviceStatusBarHeight() {
        return getStatusBarHeight(this);
    }

    /**
     * 提供给H5来设置当前设备的状态栏字体颜色，参数"black"或者"white"
     * @param color
     */
    @JavascriptInterface
    public void setStatusBarTextColor(String color) {
        Window window = getWindow();
        if (color.equalsIgnoreCase("black")) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else if (color.equalsIgnoreCase("white")) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
}
