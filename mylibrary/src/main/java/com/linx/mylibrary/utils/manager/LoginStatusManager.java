package com.linx.mylibrary.utils.manager;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.linx.mylibrary.utils.RxSharePreferenceTool;

// 登录成功时
//LoginStatusManager.getInstance(context).setLoggedIn(true);

// 退出登录时
//LoginStatusManager.getInstance(context).setLoggedIn(false);

public class LoginStatusManager {
    private static volatile LoginStatusManager instance;
    private boolean isLoggedIn = false;
    private MutableLiveData<Boolean> loginStatusLiveData = new MutableLiveData<>();
    private Context context;

    // SharedPreferences 键名
    private static final String KEY_LOGIN_STATUS = "login_status";
    private static final String KEY_USER_TOKEN = "user_token"; // 可选：存储用户token
    private static final String KEY_USER_INFO = "user_info"; // 可选：存储用户信息

    private LoginStatusManager(Context context) {
        this.context = context.getApplicationContext(); // 使用Application Context避免内存泄漏
        // 初始化时从SharedPreferences读取登录状态
        initializeLoginStatus();
    }

    public static LoginStatusManager getInstance(Context context) {
        if (instance == null) {
            synchronized (LoginStatusManager.class) {
                if (instance == null) {
                    instance = new LoginStatusManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化登录状态 - 从SharedPreferences读取
     */
    private void initializeLoginStatus() {
        Boolean savedStatus = (Boolean) RxSharePreferenceTool.get(context, KEY_LOGIN_STATUS, false);
        isLoggedIn = savedStatus != null ? savedStatus : false;
        loginStatusLiveData.setValue(isLoggedIn);

        // 可以在这里添加其他初始化逻辑，比如验证token是否过期等
    }

    /**
     * 设置登录状态
     */
    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
        loginStatusLiveData.setValue(loggedIn);

        // 保存到SharedPreferences
        RxSharePreferenceTool.put(context, KEY_LOGIN_STATUS, loggedIn);

        // 如果退出登录，清除相关用户数据
        if (!loggedIn) {
            clearUserData();
        }
    }

    /**
     * 获取当前登录状态
     */
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    /**
     * 获取登录状态的LiveData
     */
    public LiveData<Boolean> getLoginStatusLiveData() {
        return loginStatusLiveData;
    }

    /**
     * 保存用户token（可选）
     */
    public void saveUserToken(String token) {
        RxSharePreferenceTool.put(context, KEY_USER_TOKEN, token);
    }

    /**
     * 获取用户token（可选）
     */
    public String getUserToken() {
        return (String) RxSharePreferenceTool.get(context, KEY_USER_TOKEN, "");
    }

    /**
     * 保存用户信息（可选）
     */
    public void saveUserInfo(String userInfo) {
        RxSharePreferenceTool.put(context, KEY_USER_INFO, userInfo);
    }

    /**
     * 获取用户信息（可选）
     */
    public String getUserInfo() {
        return (String) RxSharePreferenceTool.get(context, KEY_USER_INFO, "");
    }

    /**
     * 清除用户数据（退出登录时调用）
     */
    public void clearUserData() {
        RxSharePreferenceTool.remove(context, KEY_USER_TOKEN);
        RxSharePreferenceTool.remove(context, KEY_USER_INFO);
        // 可以根据需要清除其他用户相关数据
    }

    /**
     * 完全清除所有登录相关数据
     */
    public void clearAllLoginData() {
        setLoggedIn(false);
        clearUserData();
        RxSharePreferenceTool.remove(context, KEY_LOGIN_STATUS);
    }

    /**
     * 检查是否包含登录状态数据
     */
    public boolean hasLoginData() {
        return RxSharePreferenceTool.contains(context, KEY_LOGIN_STATUS);
    }
}