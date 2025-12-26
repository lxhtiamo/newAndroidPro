package com.linx.mylibrary.utils.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
/*
* 统领登录用工具类 具体登录状态的管理和更新都在LoginStatusManager中处理 用法loginHelper = LoginHelper.getInstance(this);
* 登录状态的变化会实时通知到LoginStatusManager
* */
public class LoginHelper {
    private static volatile LoginHelper instance;
    private final LoginStatusManager loginManager;

    private LoginHelper(Context context) {
        loginManager = LoginStatusManager.getInstance(context.getApplicationContext());
    }

    public static LoginHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (LoginHelper.class) {
                if (instance == null) {
                    instance = new LoginHelper(context);
                }
            }
        }
        return instance;
    }

    /**
     * 观察登录状态变化（推荐使用）
     * @param owner LifecycleOwner (Activity/Fragment)
     * @param listener 状态变化监听器
     */
    public void observeLoginStatus(LifecycleOwner owner, OnLoginStatusChangeListener listener) {
        loginManager.getLoginStatusLiveData().observe(owner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoggedIn) {
                if (isLoggedIn) {
                    listener.onLogin();
                } else {
                    listener.onLogout();
                }
            }
        });
    }
    //原始版
    public void observeLoginStatus(LifecycleOwner owner, Observer<Boolean> observer) {
        loginManager.getLoginStatusLiveData().observe(owner, observer);
    }
    /**
     * 观察登录状态变化（返回原始 LiveData，更灵活）
     */
    public LiveData<Boolean> getLoginStatusLiveData() {
        return loginManager.getLoginStatusLiveData();
    }

    /**
     * 检查登录状态并执行相应操作
     */
    public void checkLoginStatus(OnLoginCheckListener listener) {
        if (loginManager.isLoggedIn()) {
            listener.onLoggedIn();
        } else {
            listener.onNotLoggedIn();
        }
    }

    /**
     * 要求登录，如果未登录则跳转到登录页面
     */
    public void requireLogin(Activity activity) {
        /*if (!loginManager.isLoggedIn()) {
            Intent intent = new Intent(activity, LoginActivity.class);
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }*/
    }

    /**
     * 要求登录并等待结果
     */
    public void requireLoginForResult(Activity activity, int requestCode) {
       /* if (!loginManager.isLoggedIn()) {
            Intent intent = new Intent(activity, LoginActivity.class);
           activity.startActivityForResult(intent, requestCode);
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }*/
    }

    /**
     * 要求登录并传递额外数据
     */
    public void requireLoginWithExtras(Activity activity, Bundle extras) {
        /*if (!loginManager.isLoggedIn()) {
            Intent intent = new Intent(activity, LoginActivity.class);
            if (extras != null) {
                intent.putExtras(extras);
            }
           activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }*/
    }

    /**
     * 执行登录操作 保存token 保存 用户信息 登录状态信息等
     */
    public void performLogin(String token, String userInfo) {
        loginManager.setLoggedIn(true);
        if (token != null) {
            loginManager.saveUserToken(token);
        }
        if (userInfo != null) {
            loginManager.saveUserInfo(userInfo);
        }
    }

    /**
     * 执行退出登录操作
     */
    public void performLogout() {
        loginManager.setLoggedIn(false);
        loginManager.clearUserData();
    }

    /**
     * 静默登录（不触发UI更新）
     */
    public void silentLogin(String token, String userInfo) {
        // 直接保存数据，不触发LiveData更新
        if (token != null) {
            loginManager.saveUserToken(token);
        }
        if (userInfo != null) {
            loginManager.saveUserInfo(userInfo);
        }
        // 最后设置登录状态，触发一次更新
        loginManager.setLoggedIn(true);
    }

    /**
     * 获取当前登录状态
     */
    public boolean isLoggedIn() {
        return loginManager.isLoggedIn();
    }

    /**
     * 获取用户token
     */
    public String getUserToken() {
        return loginManager.getUserToken();
    }

    /**
     * 获取用户信息
     */
    public String getUserInfo() {
        return loginManager.getUserInfo();
    }

    /**
     * 验证token是否有效（示例方法，根据实际需求实现）
     */
    public boolean isTokenValid() {
        String token = getUserToken();
        // 这里可以添加token过期时间检查等逻辑
        return !TextUtils.isEmpty(token) && token.startsWith("token_");
    }

    /**
     * 清理清空所有登录相关数据
     */
    public void clearAllLoginData() {
        loginManager.clearAllLoginData();
    }

    // 接口定义
    public interface OnLoginCheckListener {
        void onLoggedIn();
        void onNotLoggedIn();
    }

    public interface OnLoginStatusChangeListener {
        void onLogin();
        void onLogout();
    }

    // 常量
    public static final int REQUEST_CODE_LOGIN = 1001;
    public static final String EXTRA_REDIRECT_URL = "redirect_url";
    public static final String EXTRA_REQUIRE_AUTH = "require_auth";
}