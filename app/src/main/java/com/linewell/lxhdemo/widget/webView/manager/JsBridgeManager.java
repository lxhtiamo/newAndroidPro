package com.linewell.lxhdemo.widget.webView.manager;

import android.annotation.SuppressLint;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.linewell.lxhdemo.widget.webView.iface.IJsBridge;

/**
 * JS 与 Android 交互的管理类（封装核心逻辑，对外暴露简洁API）
 * 普通webview 中使用
 * 创建 JsBridgeManager 实例（关联普通 WebView）
 * mJsBridgeManager = new JsBridgeManager(mNormalWebView);
 * 创建 AppJsBridge 实例（传入回调，支持 LiveData）LiveData观察者模式 getJsDataLiveData().observe(this,)
 * mAppJsBridge = new AppJsBridge(this, this); 第二参数为监听回调
 * 注册 JS 交互接口（复用封装的逻辑）
 * mJsBridgeManager.registerJsInterface(mAppJsBridge);
 * 调用 JS：使用 loadJsMethod
 */
public class JsBridgeManager {
    // 全局默认的 JS 对象名（JS中通过 window.Android.xxx() 调用）
    public static final String DEFAULT_JS_OBJECT_NAME = "Android";
    private final WebView mWebView;

    private IJsBridge mJsBridge;
    private String mJsObjectName;
    // Gson 用于将对象/数组转 JSON 字符串（需添加 Gson 依赖）
    private static final Gson GSON = new Gson();

    public JsBridgeManager(WebView webView) {
        mWebView = webView;
    }

    // ======================== JS 接口注册/注销 ========================

    /**
     * 注册 JS 交互接口（使用默认 JS 对象名）
     *
     * @param jsBridge 实现了 IJsBridge 的交互类
     */
    public void registerJsInterface(IJsBridge jsBridge) {
        registerJsInterface(jsBridge, DEFAULT_JS_OBJECT_NAME);
    }

    /**
     * 注册 JS 交互接口（自定义 JS 对象名）
     *
     * @param jsBridge     实现了 IJsBridge 的交互类
     * @param jsObjectName JS 中调用的对象名
     */
    @SuppressLint("JavascriptInterface")
    public void registerJsInterface(IJsBridge jsBridge, String jsObjectName) {
        // 先注销旧接口，避免重复注册
        unregisterJsInterface();

        mJsBridge = jsBridge;
        mJsObjectName = jsObjectName;
        // 核心：注册 JS 接口（仅 API 17+ 支持 @JavascriptInterface，安全）
        mWebView.addJavascriptInterface(jsBridge, jsObjectName);
    }

    /**
     * 注销 JS 交互接口（关键：防止内存泄漏）
     */
    public void unregisterJsInterface() {
        if (mJsBridge != null) {
            mJsBridge.onDestroy();
            mJsBridge = null;
            mJsObjectName = null;
        }
    }

    // ======================== 封装 loadJsMethod：自动拼接参数 安卓执行js的方法的交互 ========================

    /**
     * 执行无参数的 JS 方法
     *
     * @param methodName JS 方法名（如：jsFunction）
     */
    public void loadJsMethod(String methodName) {
        String jsCode = String.format("javascript:%s()", methodName);
        loadUrlJsMethod(jsCode);
    }

    /**
     * 执行单个参数的 JS 方法
     *
     * @param methodName JS 方法名
     * @param param      单个参数（支持字符串、数字、布尔、对象/数组）
     */
    public void loadJsMethod(String methodName, Object param) {
        String paramStr = formatParam(param);
        String jsCode = String.format("javascript:%s(%s)", methodName, paramStr);
        loadUrlJsMethod(jsCode);
    }

    /**
     * 执行多个参数的 JS 方法
     *
     * @param methodName JS 方法名
     * @param params     多个参数（支持任意类型）
     */
    public void loadJsMethod(String methodName, Object... params) {
        StringBuilder paramStr = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            paramStr.append(formatParam(params[i]));
            if (i != params.length - 1) {
                paramStr.append(",");
            }
        }
        String jsCode = String.format("javascript:%s(%s)", methodName, paramStr);
        loadUrlJsMethod(jsCode);
    }

    /**
     * 保留：执行自定义 JS 代码（用于复杂场景，如多行为 DOM 操作）
     *
     * @param jsCode 自定义 JS 代码（如：javascript:document.getElementById('title').innerText = '新标题'）
     */
    public void loadJsMethodCustom(String jsCode) {
        loadUrlJsMethod(jsCode);
    }

    // ======================== 内部工具方法 ========================

    /**
     * 格式化参数，适配 JS 语法（解决参数类型错误问题）
     *
     * @param param 任意类型的参数
     * @return 符合 JS 语法的参数字符串
     */
    private String formatParam(Object param) {
        if (param == null) {
            return "null";
        }
        // 字符串：添加单引号，转义内部单引号（避免 JS 语法错误）
        if (param instanceof String) {
            String str = ((String) param).replace("'", "\\'");
            return "'" + str + "'";
        }
        // 数字、布尔：直接转字符串
        if (param instanceof Number || param instanceof Boolean) {
            return param.toString();
        }
        // 对象、数组：转 JSON 字符串（支持复杂参数传递）
        return GSON.toJson(param);
    }

    /**
     * 内部执行 JS 代码的核心逻辑（适配 API 19+，处理线程问题）
     */
    private void loadUrlJsMethod(String jsCode) {
        if (mWebView == null || jsCode == null || jsCode.isEmpty()) {
            return;
        }
        // API 19+ 使用 evaluateJavascript（高效，无弹窗，异步执行）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript(jsCode, null);
        } else {
            // 低版本使用 loadUrl（兼容处理）
            mWebView.loadUrl(jsCode);
        }
    }
}