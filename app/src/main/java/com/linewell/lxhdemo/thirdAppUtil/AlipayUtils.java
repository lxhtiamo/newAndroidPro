package com.linewell.lxhdemo.thirdAppUtil;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import com.alipay.sdk.app.AuthTask;
import com.alipay.sdk.app.PayTask;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;



/**
 * 支付宝工具类（与官方示例对齐版）
 * 核心修正：按官方示例将authV2/payV2放在子线程调用，避免阻塞主线程
 *
 *
 *  mAlipayUtils = AlipayUtils.getInstance(this);//初始化
 *  protected void onDestroy() {
 *         super.onDestroy();
 *         mAlipayUtils.release(); // 释放资源
 *     }
 */
public class AlipayUtils {
    private static final int SDK_AUTH_FLAG = 1; // 登录结果标识
    private static final int SDK_PAY_FLAG = 2;  // 支付结果标识

    private static volatile AlipayUtils instance;
    private final Context mContext;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false); // 并发控制

    // 登录/支付回调接口（同之前，新增注释）
    public interface LoginCallback {
        void onLoginSuccess(String authCode); // 登录成功（authCode用于服务端换用户信息）
        void onLoginProcessing();             // 处理中（8000状态，需查服务端）
        void onLoginFailed(String errorMsg);  // 失败
        void onLoginCanceled();               // 取消
    }

    public interface PayCallback {
        void onPaySuccess(String tradeNo);    // 支付成功
        void onPayProcessing();               // 处理中
        void onPayFailed(String errorMsg);    // 失败
        void onPayCanceled();                 // 取消
        void onPayUnknown();                  // 结果未知（6004，需查服务端）
    }

    private LoginCallback mLoginCallback;
    private PayCallback mPayCallback;
    private WeakReference<Activity> mActivityRef; // 弱引用避免内存泄漏

    // 主线程Handler，用于接收子线程的结果并分发回调
    private final Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SDK_AUTH_FLAG:
                    // 处理登录结果（主线程）
                    Map<String, String> authResult = (Map<String, String>) msg.obj;
                    handleLoginResult(authResult);
                    break;
                case SDK_PAY_FLAG:
                    // 处理支付结果（主线程）
                    Map<String, String> payResult = (Map<String, String>) msg.obj;
                    handlePayResult(payResult);
                    break;
                default:
                    break;
            }
        }
    };

    private AlipayUtils(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static AlipayUtils getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context不能为null");
        }
        if (instance == null) {
            synchronized (AlipayUtils.class) {
                if (instance == null) {
                    instance = new AlipayUtils(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * 发起支付宝登录（完全对齐官方示例线程模型）
     */
    public void startAlipayLogin(Activity activity, String authInfo, LoginCallback callback) {
        // 并发控制：同一时间只能有一个操作
        if (isProcessing.getAndSet(true)) {
            postLoginFailed(callback, "正在处理中，请稍后再试");
            return;
        }

        // 参数校验
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            postLoginFailed(callback, "Activity状态异常");
            isProcessing.set(false);
            return;
        }
        if (TextUtils.isEmpty(authInfo)) {
            postLoginFailed(callback, "登录参数为空（请从服务端获取）");
            isProcessing.set(false);
            return;
        }
        if (callback == null) {
            isProcessing.set(false);
            throw new IllegalArgumentException("LoginCallback不能为null");
        }

        // 保存回调和Activity弱引用
        this.mLoginCallback = callback;
        this.mActivityRef = new WeakReference<>(activity);

        // 官方示例：子线程调用authV2（避免阻塞主线程）
        new Thread(() -> {
            Activity act = mActivityRef.get();
            if (act == null || act.isFinishing() || act.isDestroyed()) {
                mMainHandler.post(() -> {
                    postLoginFailed(mLoginCallback, "Activity已销毁");
                    resetState();
                });
                return;
            }
            // 调用支付宝授权接口（同步方法，子线程执行）
            AuthTask authTask = new AuthTask(act);
            Map<String, String> result = authTask.authV2(authInfo, true);
            // 通过Handler将结果发送到主线程处理
            Message msg = new Message();
            msg.what = SDK_AUTH_FLAG;
            msg.obj = result;
            mMainHandler.sendMessage(msg);
        }).start();
    }

    /**
     * 发起支付宝支付（同登录线程模型）
     */
    public void startAlipayPay(Activity activity, String orderInfo, PayCallback callback) {
        if (isProcessing.getAndSet(true)) {
            postPayFailed(callback, "正在处理中，请稍后再试");
            return;
        }

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            postPayFailed(callback, "Activity状态异常");
            isProcessing.set(false);
            return;
        }
        if (TextUtils.isEmpty(orderInfo)) {
            postPayFailed(callback, "订单信息为空（请从服务端获取）");
            isProcessing.set(false);
            return;
        }
        if (callback == null) {
            isProcessing.set(false);
            throw new IllegalArgumentException("PayCallback不能为null");
        }

        this.mPayCallback = callback;
        this.mActivityRef = new WeakReference<>(activity);

        // 子线程调用payV2（官方推荐）
        new Thread(() -> {
            Activity act = mActivityRef.get();
            if (act == null || act.isFinishing() || act.isDestroyed()) {
                mMainHandler.post(() -> {
                    postPayFailed(mPayCallback, "Activity已销毁");
                    resetState();
                });
                return;
            }
            PayTask payTask = new PayTask(act);
            Map<String, String> result = payTask.payV2(orderInfo, true);
            Message msg = new Message();
            msg.what = SDK_PAY_FLAG;
            msg.obj = result;
            mMainHandler.sendMessage(msg);
        }).start();
    }

    /**
     * 处理登录结果（主线程执行）
     */
    private void handleLoginResult(Map<String, String> result) {
        if (result == null) {
            postLoginFailed(mLoginCallback, "登录结果为空");
            resetState();
            return;
        }

        String resultStatus = result.get("resultStatus");
        String resultData = result.get("result");
        String memo = result.get("memo");

        // 状态码参考官方文档：https://opendocs.alipay.com/open/218/105326
        switch (resultStatus) {
            case "9000": // 成功
                String authCode = parseAuthCodeFromJson(resultData);
                if (mLoginCallback != null) {
                    mLoginCallback.onLoginSuccess(authCode);
                }
                break;
            case "8000": // 处理中
                if (mLoginCallback != null) {
                    mLoginCallback.onLoginProcessing();
                }
                break;
            case "6001": // 取消
                if (mLoginCallback != null) {
                    mLoginCallback.onLoginCanceled();
                }
                break;
            default: // 失败（4000/5000/6002等）
                String errorMsg = TextUtils.isEmpty(memo) ? "登录失败，状态码：" + resultStatus : memo;
                postLoginFailed(mLoginCallback, errorMsg);
                break;
        }
        resetState(); // 处理完成后重置状态
    }

    /**
     * 处理支付结果（主线程执行）
     */
    private void handlePayResult(Map<String, String> result) {
        if (result == null) {
            postPayFailed(mPayCallback, "支付结果为空");
            resetState();
            return;
        }

        String resultStatus = result.get("resultStatus");
        String resultData = result.get("result");
        String memo = result.get("memo");

        // 状态码参考：https://opendocs.alipay.com/open/204/105301
        switch (resultStatus) {
            case "9000": // 成功
                String tradeNo = parseTradeNoFromJson(resultData);
                if (mPayCallback != null) {
                    mPayCallback.onPaySuccess(tradeNo);
                }
                break;
            case "8000": // 处理中
                if (mPayCallback != null) {
                    mPayCallback.onPayProcessing();
                }
                break;
            case "6001": // 取消
                if (mPayCallback != null) {
                    mPayCallback.onPayCanceled();
                }
                break;
            case "6004": // 结果未知
                if (mPayCallback != null) {
                    mPayCallback.onPayUnknown();
                }
                break;
            default: // 失败
                String errorMsg = TextUtils.isEmpty(memo) ? "支付失败，状态码：" + resultStatus : memo;
                postPayFailed(mPayCallback, errorMsg);
                break;
        }
        resetState();
    }

    // JSON解析方法（同之前，确保可靠性）
    private String parseAuthCodeFromJson(String resultData) {
        if (TextUtils.isEmpty(resultData)) return "";
        try {
            JSONObject json = new JSONObject(resultData);
            return json.optString("auth_code", "");
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String parseTradeNoFromJson(String resultData) {
        if (TextUtils.isEmpty(resultData)) return "";
        try {
            JSONObject json = new JSONObject(resultData);
            if ("10000".equals(json.optString("code"))) { // 支付宝内部成功标识
                return json.optString("trade_no", "");
            }
            return "";
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    // 回调分发辅助方法（避免空指针）
    private void postLoginFailed(LoginCallback callback, String msg) {
        if (callback != null) {
            mMainHandler.post(() -> callback.onLoginFailed(msg));
        }
    }

    private void postPayFailed(PayCallback callback, String msg) {
        if (callback != null) {
            mMainHandler.post(() -> callback.onPayFailed(msg));
        }
    }

    // 重置状态（释放回调和并发锁）
    private void resetState() {
        mLoginCallback = null;
        mPayCallback = null;
        mActivityRef = null;
        isProcessing.set(false);
    }

    /**
     * 释放资源（Application退出时调用）
     */
    public void release() {
        resetState();
        mMainHandler.removeCallbacksAndMessages(null);
    }
}