package com.linewell.lxhdemo.thirdAppUtil;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.tencent.mm.opensdk.constants.Build;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.modelmsg.LaunchFromWX;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

/**
 * 微信功能帮助类（拆分回调为成功/失败/取消等独立方法）
 * 在Application的onCreate中初始化
 * WeChatHelper.getInstance().init(this, "你的微信AppID");
 *
 * 支付参数
 * WeChatHelper.WeChatPayParams params = new WeChatHelper.WeChatPayParams();
 * params.appId = "你的AppID";
 * params.partnerId = "商户号";
 * params.prepayId = "预支付ID";
 * params.nonceStr = "随机字符串";
 * params.timeStamp = "时间戳";
 * params.packageValue = "Sign=WXPay";
 * params.sign = "签名";
 */
public class WeChatHelper implements IWXAPIEventHandler {
    private static final String TAG = "WeChatHelper";
    private static volatile WeChatHelper instance;
    private IWXAPI mWxApi;
    private String mAppId;

    // 回调接口（拆分成功/失败/取消）
    private WeChatLoginCallback mLoginCallback;
    private WeChatPayCallback mPayCallback;
    private WeChatMiniProgramCallback mMiniProgramCallback;

    private WeChatHelper() {
    }

    public static WeChatHelper getInstance() {
        if (instance == null) {
            synchronized (WeChatHelper.class) {
                if (instance == null) {
                    instance = new WeChatHelper();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化微信SDK
     *
     * @param context 上下文
     * @param appId   微信开放平台AppID
     * @return 是否初始化成功
     */
    public boolean init(Context context, String appId) {
        if (context == null || TextUtils.isEmpty(appId)) {
            return false;
        }
        this.mAppId = appId;
        mWxApi = WXAPIFactory.createWXAPI(context, appId, true);
        return mWxApi.registerApp(appId);
    }

    public IWXAPI getWxApi() {
        return mWxApi;
    }

    /**
     * 检查微信是否安装
     */
    public boolean isWeChatInstalled() {
        return mWxApi != null && mWxApi.isWXAppInstalled();
    }

    /**
     * 检查微信是否支持支付
     */
    public boolean isWeChatPaySupported() {
        return mWxApi != null && mWxApi.getWXAppSupportAPI() >= Build.PAY_SUPPORTED_SDK_INT;
    }


    // -------------------------- 微信登录 --------------------------

    /**
     * 发起微信登录
     *
     * @param scope    权限（一般为"snsapi_userinfo"）
     * @param state    状态标识（用于校验回调合法性）
     * @param callback 登录回调
     */
    public void login(String scope, String state, WeChatLoginCallback callback) {
        if (mWxApi == null || TextUtils.isEmpty(mAppId)) {
            if (callback != null) {
                callback.onLoginFail("微信SDK未初始化");
            }
            return;
        }
        if (!isWeChatInstalled()) {
            if (callback != null) {
                callback.onLoginFail("未安装微信客户端");
            }
            return;
        }

        this.mLoginCallback = callback;
        SendAuth.Req req = new SendAuth.Req();
        req.scope = scope;
        req.state = state;
        mWxApi.sendReq(req);
    }

    /**
     * 微信登录回调接口（拆分成功/取消/失败）
     */
    public interface WeChatLoginCallback {
        /**
         * 登录成功（获取到code）
         *
         * @param code  微信返回的授权code（用于获取access_token）
         * @param state 发起登录时传入的state（用于校验）
         */
        void onLoginSuccess(String code, String state);

        /**
         * 用户取消登录
         */
        void onLoginCancel();

        /**
         * 登录失败
         *
         * @param errorMsg 失败原因
         */
        void onLoginFail(String errorMsg);
    }


    // -------------------------- 微信支付 --------------------------

    /**
     * 发起微信支付
     *
     * @param payParams 支付参数（从服务端获取）
     * @param callback  支付回调
     */
    public void pay(WeChatPayParams payParams, WeChatPayCallback callback) {
        if (mWxApi == null || TextUtils.isEmpty(mAppId)) {
            if (callback != null) {
                callback.onPayFail("微信SDK未初始化");
            }
            return;
        }
        if (!isWeChatInstalled()) {
            if (callback != null) {
                callback.onPayFail("未安装微信客户端");
            }
            return;
        }
        if (!isWeChatPaySupported()) {
            if (callback != null) {
                callback.onPayUnsupported("微信版本过低，不支持支付");
            }
            return;
        }

        this.mPayCallback = callback;
        PayReq req = new PayReq();
        req.appId = payParams.appId;
        req.partnerId = payParams.partnerId;
        req.prepayId = payParams.prepayId;
        req.nonceStr = payParams.nonceStr;
        req.timeStamp = payParams.timeStamp;
        req.packageValue = payParams.packageValue;
        req.sign = payParams.sign;
        req.extData = payParams.extData;
        mWxApi.sendReq(req);
    }

    /**
     * 微信支付回调接口（拆分成功/取消/失败/不支持）
     */
    public interface WeChatPayCallback {
        /**
         * 支付成功
         */
        void onPaySuccess();

        /**
         * 用户取消支付
         */
        void onPayCancel();

        /**
         * 支付失败
         *
         * @param errorMsg 失败原因
         */
        void onPayFail(String errorMsg);

        /**
         * 微信不支持支付（版本过低）
         *
         * @param errorMsg 原因说明
         */
        void onPayUnsupported(String errorMsg);
    }


    // -------------------------- 拉起小程序 --------------------------

    /**
     * 拉起微信小程序
     *
     * @param userName 小程序原始ID（如：gh_xxxxxxxxxxxx）
     * @param path     小程序页面路径（可选，如：pages/index?param=123）
     * @param type     小程序类型（0-正式版，1-测试版，2-体验版）
     *                 WXLaunchMiniProgram.Req.MINIPROGRAM_TYPE_TEST;1
     *                 WXLaunchMiniProgram.Req.MINIPROGRAM_TYPE_PREVIEW;2
     *                 WXLaunchMiniProgram.Req.MINIPTOGRAM_TYPE_RELEASE;0
     * @param callback 启动回调
     */
    public void launchMiniProgram(String userName, String path, int type, WeChatMiniProgramCallback callback) {
        if (mWxApi == null || TextUtils.isEmpty(mAppId)) {
            if (callback != null) {
                callback.onLaunchFail("微信SDK未初始化");
            }
            return;
        }
        if (!isWeChatInstalled()) {
            if (callback != null) {
                callback.onLaunchFail("未安装微信客户端");
            }
            return;
        }

        this.mMiniProgramCallback = callback;
        WXLaunchMiniProgram.Req req = new WXLaunchMiniProgram.Req();
        req.userName = userName;
        req.path = TextUtils.isEmpty(path) ? "" : path;
        req.miniprogramType = type;
        mWxApi.sendReq(req);
    }

    /**
     * 小程序启动回调接口（拆分成功/失败）
     */
    public interface WeChatMiniProgramCallback {
        /**
         * 小程序启动成功
         */
        void onLaunchSuccess(String extMsg);

        /**
         * 小程序启动失败
         *
         * @param errorMsg 失败原因
         */
        void onLaunchFail(String errorMsg);
    }


    // -------------------------- 回调处理 --------------------------

    public void handleIntent(Intent intent) {
        if (mWxApi != null && intent != null) {
            mWxApi.handleIntent(intent, this);
        }
    }

    @Override
    public void onReq(BaseReq baseReq) {
        // 处理来自微信的请求（如从微信打开App）
        if (baseReq instanceof LaunchFromWX.Req) {
            // 可根据需求处理从微信启动App的场景
        }
    }

    @Override
    public void onResp(BaseResp baseResp) {
        // 根据响应类型分发回调
        switch (baseResp.getType()) {
            case ConstantsAPI.COMMAND_SENDAUTH:
                handleLoginResp((SendAuth.Resp) baseResp);
                break;
            case ConstantsAPI.COMMAND_PAY_BY_WX:
                handlePayResp(baseResp);
                break;
            case ConstantsAPI.COMMAND_LAUNCH_WX_MINIPROGRAM:
                handleMiniProgramResp(baseResp);
                break;
        }
    }

    /**
     * 处理登录回调结果
     */
    private void handleLoginResp(SendAuth.Resp resp) {
        if (mLoginCallback == null) return;

        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                // 登录成功（返回code和state）
                mLoginCallback.onLoginSuccess(resp.code, resp.state);
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                // 用户取消
                mLoginCallback.onLoginCancel();
                break;
            default:
                // 登录失败（错误码+描述）
                mLoginCallback.onLoginFail("错误码：" + resp.errCode + "，描述：" + resp.errStr);
                break;
        }
        mLoginCallback = null; // 避免重复回调
    }

    /**
     * 处理支付回调结果
     */
    private void handlePayResp(BaseResp resp) {
        if (mPayCallback == null) return;

        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                // 支付成功
                mPayCallback.onPaySuccess();
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                // 用户取消支付
                mPayCallback.onPayCancel();
                break;
            default:
                // 支付失败
                mPayCallback.onPayFail("错误码：" + resp.errCode + "，描述：" + resp.errStr);
                break;
        }
        mPayCallback = null; // 避免重复回调
    }

    /**
     * 处理小程序启动回调结果
     */
    private void handleMiniProgramResp(BaseResp resp) {
        if (mMiniProgramCallback == null) return;

        if (resp.errCode == BaseResp.ErrCode.ERR_OK) {
            String extMsg = "";
            if (resp instanceof WXLaunchMiniProgram.Resp) {
                extMsg = ((WXLaunchMiniProgram.Resp) resp).extMsg;
            }
            // 启动成功
            mMiniProgramCallback.onLaunchSuccess(extMsg);
        } else {
            // 启动失败
            mMiniProgramCallback.onLaunchFail("错误码：" + resp.errCode + "，描述：" + resp.errStr);
        }
        mMiniProgramCallback = null; // 避免重复回调
    }


    /**
     * 微信支付参数实体类
     */
    public static class WeChatPayParams {
        public String appId;         // 应用ID（和初始化的AppID一致）
        public String partnerId;     // 商户号（微信支付商户平台分配）
        public String prepayId;      // 预支付交易会话ID（服务端接口获取）
        public String nonceStr;      // 随机字符串（服务端生成）
        public String timeStamp;     // 时间戳（服务端生成，秒级）
        public String packageValue;  // 固定值"Sign=WXPay"
        public String sign;          // 签名（服务端生成）
        public String extData;       // 商户扩展字段（可选）
    }
}