package com.bindo.ryoyuhk;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.bindo.ryoyuhk.Utils.PhotoUtils;
import com.bindo.ryoyuhk.zxing.android.CaptureActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "bindo";
    public static final String BASE_URL = BuildConfig.baseUrl;
    private static final String DECODED_CONTENT_KEY = "codedContent";
    private static final String DECODED_BITMAP_KEY = "codedBitmap";
    private static final int REQUEST_CODE_ALBUM = 1;
    private static final int REQUEST_CODE_SCAN = 2;
    private WebView webView;
    private ValueCallback<Uri[]> mUploadCallbackAboveL;
    private final static int PHOTO_RESULT = 100;
    private final static int FILECHOOSER_RESULT = 101;
    private final static int VIDEO_RESULT = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setStatusBarFullTransparent();

        // 隐藏标题栏
        getSupportActionBar().hide();

        // 加载WebView
        loadWebView();
    }

    /**
     * JS调用 window.bindo_utils.getDeviceStatusBarHeight()，返回当前设备状态栏的高度（单位像素）
     * JS调用 window.bindo_utils.setStatusBarTextColor(String color)，通过传递参数（黑色："black"，白色："white"）
     * 来设置当前设备的状态栏字体颜色
     *
     */
    protected void loadWebView() {
        webView = findViewById(R.id.my_web_view);
        // 初始化WebView
        initWebSettings(webView, this);
        // 注入JS接口
        webView.addJavascriptInterface(this, "bindo_utils");
        // 从配置中加载web URL
        webView.loadUrl(BASE_URL);
    }

    protected void initWebSettings(WebView webView, Context context) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setDefaultTextEncodingName("UTF-8");//设置默认为utf-8
        webSettings.setTextZoom(100);//设置WebView中加载页面字体变焦百分比，默认100
        //属性可以让webview只显示一列，也就是自适应页面大小,不能左右滑动
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        } else {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        }

        webSettings.setAllowContentAccess(true); // 是否可访问Content Provider的资源，默认值 true
        webSettings.setAllowFileAccess(true);    // 是否可访问本地文件，默认值 true

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
                Log.i("拦截", "拦截到的url：" + url);
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                mUploadCallbackAboveL = filePathCallback;
                take("openAlbum");
//                take("goScan"); // Fot test
                return true;
            }

        });

        // 适配刘海屏，该处理移除，不在原生处理刘海高度，由H5端获取高度来自行处理
        /**
         LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) webView.getLayoutParams();
         lp.topMargin = getStatusBarHeight(context);
         webView.setLayoutParams(lp);
        */
    }

    /**
     * 动态获取用户权限
     * @param actionType
     */
    private void take(String actionType) {
        if (actionType.equals("openAlbum")) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_CODE_ALBUM);
        } else if (actionType.equals("goScan")) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, REQUEST_CODE_SCAN);
        }
    }

    /**
     * 获取用户权限响应
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            if (requestCode == REQUEST_CODE_ALBUM && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Get permission to open album");
                openAlbum();
            } else if (requestCode == REQUEST_CODE_SCAN && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Get permission to go scan");
                goScan();
            }
        }
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

    /**
     * 提供给H5来来打开相机扫码
     */
    @JavascriptInterface
    public void openCameraScan() {
        goScan();
    }

    /**
     * 打开相册
     */
    private void openAlbum() {
        PhotoUtils.openPic(this, FILECHOOSER_RESULT);
    }

    private Uri imageUri;
    /**
     * 拍照
     */
    private void takePhoto() {
        File file = new File(Environment.getExternalStorageDirectory().getParent() + "/" + SystemClock.currentThreadTimeMillis() + ".jpg");
        imageUri = Uri.fromFile(file);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        }
        // 调用相机拍照
        PhotoUtils.takePicture(this, imageUri, PHOTO_RESULT);
    }

    /**
     * 录像
     */
    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        // 设置视频质量
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        // 设置视频时长限制
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
        // 开启摄像机
        startActivityForResult(intent, VIDEO_RESULT);
    }

    /**
     * 跳转到扫码界面扫码
     */
    private void goScan(){
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SCAN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "requestCode=" + requestCode);
        if (requestCode == FILECHOOSER_RESULT) {
            if (null == mUploadCallbackAboveL) return;
            if (mUploadCallbackAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            }
        } else if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {   // 扫描二维码/条码回传
            if (data != null) {
                //返回的文本内容
                String content = data.getStringExtra(DECODED_CONTENT_KEY);
                // 获取到扫码结果之后需要返回给H5
                Log.i(TAG, "Scan result string=>" + content);
                webView.loadUrl("javascript:scan_result(" + content + ")");

                //返回的BitMap图像
                /*Bitmap bitmap = data.getParcelableExtra(DECODED_BITMAP_KEY);
                Log.i(TAG, "Scan result bitmap=>" + bitmap.toString());*/
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent data) {
        if (requestCode != FILECHOOSER_RESULT || mUploadCallbackAboveL == null) {
            return;
        }
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            String dataString = data.getDataString();
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                results = new Uri[clipData.getItemCount()];
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    results[i] = item.getUri();
                }
            }

            if (dataString != null)
                results = new Uri[]{Uri.parse(dataString)};

            mUploadCallbackAboveL.onReceiveValue(results);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            mUploadCallbackAboveL.onReceiveValue(null);
        }

        mUploadCallbackAboveL = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.setWebViewClient(null);
            webView.setWebChromeClient(null);
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            webView.destroy();
            webView = null;
        }
    }
}
