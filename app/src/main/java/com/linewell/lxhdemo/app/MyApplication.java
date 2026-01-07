package com.linewell.lxhdemo.app;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.hjq.toast.Toaster;
import com.linewell.lxhdemo.manager.OkGoManager;
import com.linewell.lxhdemo.utils.ThemeUtils;
import com.linx.mylibrary.utils.klog.KLog;
import com.linewell.lxhdemo.manager.AppActivityManager;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.mmkv.MMKV;

/**
 * @author xh
 */
public class MyApplication extends Application {
    private static MyApplication myApplication;
    public String Access_Token;//Token

    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;
        // 基础工具初始化
        initBasicTools();
        // 第三方SDK初始化
        initThirdSDK();


    }

    /**
     * 初始化第三方SDK（Bugly、OkGo、微信等）
     */
    private void initThirdSDK() {
        // 1. Bugly 崩溃收集 异常捕捉 false是否调试模式
        CrashReport.initCrashReport(this, AppConfig.BuglyI, false);
        // 2. OkGo 网络请求（调用封装的管理类）
        OkGoManager.getInstance(this).init();
        // 3. 微信初始化（按需启用）
        // WeChatHelper.getInstance().init(this, AppConfig.WECHAT_App_ID);
    }

    /**
     * 初始化基础工具（日志、吐司、Activity管理、存储等）
     */
    private void initBasicTools() {
        KLog.init(AppConfig.LOG_DEBUG, "KLog");
        // 初始化吐司工具类
        Toaster.init(this);
        // 初始化Activity 栈管理
        AppActivityManager.getInstance().init(this);
        // MMKV 轻量化存储数据 初始化
        MMKV.initialize(this);
        // 应用主题模式
        // ThemeUtils.setDefaultThemeMode(ThemeUtils.MODE_LIGHT);
        ThemeUtils.applyThemeMode(this);
    }

    public void setAccessToken(String accessToken) {
        OkGoManager.getInstance(this).setAccessToken(accessToken);
    }

    public String getAccessToken() {
        return OkGoManager.getInstance(this).getAccessToken();
    }
    /**
     * 外部调用的实例
     */
    public static synchronized MyApplication getInstance() {
        if (myApplication == null) {
            myApplication = new MyApplication();
        }
        return myApplication;
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // 使用 Dex分包
        MultiDex.install(this);
    }
}
