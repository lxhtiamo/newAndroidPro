package com.linx.mylibrary.view.webView;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import androidx.lifecycle.MutableLiveData;

import com.linx.mylibrary.view.webView.bean.JsInteractionData;
import com.linx.mylibrary.view.webView.iface.IJsBridge;
import com.linx.mylibrary.view.webView.iface.OnJsDataCallback;

import java.lang.ref.WeakReference;


/**
 * 同时支持 OnJsDataCallback 回调和 LiveData 数据传递
 * 具体的 JS 交互实现类（包含供 JS 调用的方法，以及数据回调逻辑）
 * 【核心能力】
 * 1. 提供供 JS 调用的方法（如 showToast、getDeviceInfo）
 * 2. 支持 OnJsDataCallback 回调传递数据到 Activity
 * 3. 支持 LiveData 传递数据（生命周期感知，推荐 Jetpack 项目使用）
 *JavascriptInterface   调用是在子线程 , 不能直接操作 UI 线程
 */
public class AppJsBridge implements IJsBridge {
    // Application 上下文（防止内存泄漏）
    private final Context mAppContext;
    // 弱引用持有回调接口（兼容传统回调）
    private WeakReference<OnJsDataCallback> mDataCallbackRef;
    // MutableLiveData：存储 JS 传递的消息（支持生命周期感知）
    private MutableLiveData<JsInteractionData> mJsDataLiveData = new MutableLiveData<>();

    // ======================== 构造方法（保留原有回调参数，兼容旧代码） ========================
    public AppJsBridge(Context context, OnJsDataCallback callback) {
        mAppContext = context.getApplicationContext();
        mDataCallbackRef = new WeakReference<>(callback);
    }

    // ======================== 对外暴露 LiveData（供外部观察） ========================
    /**
     * 获取 JS 任意类型数据的 LiveData（观察复杂类型数据） getJsDataLiveData().observe  监听
     */
    public MutableLiveData<JsInteractionData> getJsDataLiveData() {
        return mJsDataLiveData;
    }

    // ======================== 实现 IJsBridge 接口 ========================
    @Override
    public Context getContext() {
        return mAppContext;
    }

    @Override
    public void onDestroy() {
        // 清空回调弱引用
        if (mDataCallbackRef != null) {
            mDataCallbackRef.clear();
            mDataCallbackRef = null;
        }
        // LiveData 无需手动清空，生命周期感知会自动处理
        mJsDataLiveData = null;
    }

    // ======================== 供 JS 调用的方法 示例========================
    @JavascriptInterface
    public void showToast(String message) {
        // 传递文本消息（同时触发回调和 LiveData）
        JsInteractionData jsData = new JsInteractionData.Builder()
                .actionType(JsInteractionData.ActionType.TOAST)
                .data(message)
                .build();
        postJsDataToActivity(jsData);
        // 执行 Toast 逻辑
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mAppContext, message, Toast.LENGTH_SHORT).show()
        );
    }
    @JavascriptInterface
    public String isLogin() {
        return "isLogin";
    }

    @JavascriptInterface
    public String getDeviceInfo() {
        return "Android - " + android.os.Build.MODEL + " / SDK " + android.os.Build.VERSION.SDK_INT;
    }

    // ======================== 核心：通用的数据传递方法（唯一的传递逻辑，无需维护多个方法） ========================
    private void postJsDataToActivity(JsInteractionData jsData) {
        // 1. 触发 OnJsDataCallback 回调（切换到主线程，Activity 操作必须在主线程）
        OnJsDataCallback callback = mDataCallbackRef.get();
        if (callback != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                OnJsDataCallback mainCallback = mDataCallbackRef.get();
                if (mainCallback != null) {
                    mainCallback.onJsDataReceived(jsData);
                }
            });
        }

        // 2. 发送到 LiveData（子线程用 postValue，主线程用 setValue）
        mJsDataLiveData.postValue(jsData);
    }
}