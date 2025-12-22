package com.linx.mylibrary.view.webView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.chad.library.BuildConfig;
import com.linx.mylibrary.action.ActivityAction;
import com.linx.mylibrary.view.webView.iface.IJsBridge;
import com.linx.mylibrary.view.webView.manager.JsBridgeManager;

import java.util.Locale;


/**
 * 【使用方法文档】
 * ==============================================
 * 1. 快速开始
 * Step 1: 在布局文件中添加或代码中创建 BrowserWebView
 * Step 2: 关联生命周期（setLifecycleOwner） mWebView.setLifecycleOwner(this);
 * Step 3: 注册 JS 交互接口（registerJsInterface）
 * mAppJsBridge = new AppJsBridge(this, this);
 * 使用默认JS名：Android，
 * mWebView.registerJsInterface(mAppJsBridge);
 * 可自定义第二个参数 自定义示例：
 * mWebView.registerJsInterface(mAppJsBridge, "MyAndroidJsBridge");
 * Step 4: 加载网页（loadUrl）
 * *
 * 2. 核心功能
 * - JS 交互：注册接口后，JS 可通过 window.Android.xxx() 调用 Android 方法
 * - Android 调用 JS：使用 loadJsMethod 系列方法，自动拼接参数，支持无参/单参/多参/对象
 * - 数据接收：支持 OnJsDataCallback 回调和 LiveData 两种方式接收 JS 传递的数据
 * OnJsDataCallback 在new AppJsBridge(this, this);中第二个参数
 * - LiveData 方式：mAppJsBridge.getJsMessageLiveData().observe(this, jsMessage -> {
 * jsMessage处理 JS 传递的数据
 * });
 * - 生命周期：关联 Activity/Fragment 后，自动执行 resume/pause/destroy
 * *
 * 3. 注意事项
 * - JS 调用的方法必须添加 @JavascriptInterface 注解
 * - 避免持有 Activity 上下文，优先使用 Application 上下文或弱引用
 */
public final class BrowserWebView extends NestedScrollWebView
        implements LifecycleEventObserver, ActivityAction {

    // 常量抽离（仅保留核心常量）
    // JS 交互管理器
    private JsBridgeManager mJsBridgeManager;
    // 成员变量精简
    private LifecycleOwner mLifecycleOwner;

    static {
        // 仅Debug模式开启WebView调试
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
    }

    // 构造方法（保留核心）
    public BrowserWebView(Context context) {
        this(context, null);
    }

    public BrowserWebView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.webViewStyle);
    }

    public BrowserWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(getFixedContext(context), attrs, defStyleAttr, 0);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public BrowserWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initWebSettings();
        // 2. 初始化 JS 交互管理器
        mJsBridgeManager = new JsBridgeManager(this);
        initBasicConfig();
    }
    // ======================== 对外暴露的 JS 交互 API（简洁易用） ========================

    /**
     * 注册 JS 交互接口（使用默认 JS 对象名）
     */
    public void registerJsInterface(IJsBridge jsBridge) {
        mJsBridgeManager.registerJsInterface(jsBridge);
    }

    /**
     * 注册 JS 交互接口（自定义 JS 对象名）
     */
    public void registerJsInterface(IJsBridge jsBridge, String jsObjectName) {
        mJsBridgeManager.registerJsInterface(jsBridge, jsObjectName);
    }

    /**
     * 执行无参数的 JS 方法
     */
    public void loadJsMethod(String methodName) {
        mJsBridgeManager.loadJsMethod(methodName);
    }

    /**
     * 执行单个参数的 JS 方法
     */
    public void loadJsMethod(String methodName, Object param) {
        mJsBridgeManager.loadJsMethod(methodName, param);
    }

    /**
     * 执行多个参数的 JS 方法
     */
    public void loadJsMethod(String methodName, Object... params) {
        mJsBridgeManager.loadJsMethod(methodName, params);
    }

    /**
     * 执行自定义 JS 代码（复杂场景）
     */
    public void loadJsMethodCustom(String jsCode) {
        mJsBridgeManager.loadJsMethodCustom(jsCode);
    }

    /**
     * 初始化WebSettings（精简版：仅保留核心配置）
     */
    private void initWebSettings() {
        WebSettings webSettings = getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);//加载缓存否则网络
        webSettings.setLoadsImagesAutomatically(true);//图片自动缩放 打开

        // 解决 Android 5.0 上 WebView 默认不允许加载 Http 与 Https 混合内容
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        // 允许文件访问
        webSettings.setAllowFileAccess(true);
        // 允许网页定位
        webSettings.setGeolocationEnabled(true);
        // setMediaPlaybackRequiresUserGesture(boolean require) //是否需要用户手势来播放Media，默认true

        webSettings.setJavaScriptEnabled(true); // 设置支持javascript脚本
        // 允许网页弹对话框
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
//        webSettings.setPluginState(WebSettings.PluginState.ON);
        webSettings.setSupportZoom(false);// 设置可以支持缩放
        webSettings.setBuiltInZoomControls(false);// 设置出现缩放工具 是否使用WebView内置的缩放组件，由浮动在窗口上的缩放控制和手势缩放控制组成，默认false

        webSettings.setDisplayZoomControls(false);//隐藏缩放工具
        webSettings.setUseWideViewPort(true);// 扩大比例的缩放

        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);//自适应屏幕
        webSettings.setLoadWithOverviewMode(true);

        webSettings.setDatabaseEnabled(true);//
        webSettings.setSavePassword(true);//保存密码
        webSettings.setDomStorageEnabled(true);//是否开启本地DOM存储  鉴于它的安全特性（任何人都能读取到它，尽管有相应的限制，将敏感数据存储在这里依然不是明智之举），Android 默认是关闭该功能的。

    }

    /**
     * 初始化基础UI配置
     */
    private void initBasicConfig() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);//软件解码
        setLayerType(View.LAYER_TYPE_HARDWARE, null);//硬件解码
        setSaveEnabled(true);
        setKeepScreenOn(true);
        // 隐藏滚动条
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setBrowserViewClient(new BrowserViewClient());
        setBrowserChromeClient(new BrowserChromeClient(this));
    }

    /**
     * 修复Android 5.x WebView崩溃问题
     */
    private static Context getFixedContext(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                return new ContextThemeWrapper(context, context.getTheme());
            } catch (Exception e) {
                return context;
            }
        }
        return context;
    }

    // 生命周期管理（精简版：保留核心销毁逻辑）
    public void setLifecycleOwner(LifecycleOwner owner) {
        if (mLifecycleOwner != null) {
            mLifecycleOwner.getLifecycle().removeObserver(this);
        }
        mLifecycleOwner = owner;
        owner.getLifecycle().addObserver(this);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_RESUME:
                onResume();
                break;
            case ON_PAUSE:
                onPause();
                break;
            case ON_DESTROY:
                onDestroy();
                source.getLifecycle().removeObserver(this);
                mLifecycleOwner = null;
                break;
            default:
                break;
        }
    }

    /**
     * 销毁WebView（精简版：移除小众数据清除）
     */
    public void onDestroy() {
        stopLoading();
        clearCache(true);
        clearHistory();
        clearFormData();
        mJsBridgeManager.unregisterJsInterface();
        setBrowserChromeClient(null);
        setBrowserViewClient(null);

        // 销毁WebView（主线程执行）
        post(() -> {
            removeAllViews();
            destroy();
        });
    }

    // 获取URL（保留核心逻辑）
    @Override
    public String getUrl() {
        String originalUrl = super.getOriginalUrl();
        return TextUtils.isEmpty(originalUrl) ? super.getUrl() : originalUrl;
    }

    // 回调方法封装（保留核心）
    @Deprecated
    @Override
    public void setWebViewClient(@NonNull WebViewClient client) {
        super.setWebViewClient(client);
    }

    public void setBrowserViewClient(BrowserViewClient client) {
        super.setWebViewClient(client != null ? client : new BrowserViewClient());
    }

    @Deprecated
    @Override
    public void setWebChromeClient(WebChromeClient client) {
        super.setWebChromeClient(client);
    }

    public void setBrowserChromeClient(BrowserChromeClient client) {
        super.setWebChromeClient(client != null ? client : new BrowserChromeClient(this));
    }

    // ======================== 内部类：BrowserViewClient（精简版）========================
    public static class BrowserViewClient extends WebViewClient {

        /**
         * SSL错误处理（保留核心，简化提示）
         */
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {

            // 注：此处简化了自定义对话框逻辑，你可根据业务需求替换为自己的对话框
            // 生产环境建议添加用户确认逻辑，此处为了简化直接继续（仅作示例）
            Context context = view.getContext();
            if (context == null) {
                // 上下文为空时，取消请求
                handler.cancel();
            }
            // 忽略SSL错误 继续加载
            //handler.proceed();

        }

        /**
         * 加载错误（保留日志） 同名 API 兼容处理
         */
        @androidx.annotation.RequiresApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                onReceivedError(view, error.getErrorCode(), error.getDescription().toString(), request.getUrl().toString());
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        /**
         * 链接跳转（精简版：仅保留http/https/tel）
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return shouldOverrideUrlLoading(view, request.getUrl().toString());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Context context = view.getContext();
            if (TextUtils.isEmpty(url)) return false;

            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if (scheme == null) {
                view.loadUrl(url);
                return true;
            }

            switch (scheme.toLowerCase(Locale.getDefault())) {
                case "http":
                case "https":
                    view.loadUrl(url);
                    break;
                case "tel":
                    goTel(url, context);
                    break;
                default:
                    // 其他scheme（如tel、mailto）交给系统默认处理
                    goOther(view, uri);
                    break;
            }
            return true;
        }

        private static void goOther(WebView view, Uri uri) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                view.getContext().startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static void goTel(String url, Context context) {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ======================== 内部类：BrowserChromeClient（精简版）========================
    public static class BrowserChromeClient extends WebChromeClient {

        private final BrowserWebView mWebView;
        private final Context mContext;

        public BrowserChromeClient(BrowserWebView view) {
            mWebView = view;
            if (mWebView == null) {
                throw new IllegalArgumentException("BrowserView 不能为空!");
            }
            mContext = view.getContext();
        }
    }
}