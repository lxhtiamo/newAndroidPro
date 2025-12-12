package com.linewell.lxhdemo.utils;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import com.tencent.mmkv.MMKV;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 深色模式工具类（最终版）：
 * 1. 使用MMKV存储（支持多进程，性能更优）
 * 2. 支持浅色、深色、跟随系统三个独立选项
 * 3. 区分偏好模式和实际生效模式
 * 4. 新增模式变化全局监听（线程安全）
 * 5. 防止重复设置同一模式（避免无效操作）
 * 6. 支持自定义默认模式（默认优先浅色模式，可动态调整）
 * 用法Application中初始化ThemeUtils.applyThemeMode(this)
 * 页面中切换ThemeUtils.setAndApplyThemeMode(this, ThemeUtils.MODE_LIGHT);   overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); recreate();
 */
public class ThemeUtils {
    // MMKV的存储键名
    private static final String KEY_THEME_MODE = "theme_mode";

    // 模式常量（与AppCompatDelegate的模式一一对应，便于理解和调用）
    public static final int MODE_FOLLOW_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;       // 浅色模式
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES;      // 深色模式

    // ====================== 自定义默认模式相关（核心：默认优先浅色） ======================
    // 全局默认模式变量，初始值设为浅色模式（可通过setDefaultThemeMode动态修改）
    private static int DEFAULT_THEME_MODE = MODE_LIGHT;

    /**
     * 全局设置默认模式（建议在Application的onCreate中调用，支持动态调整）
     *
     * @param mode 可选值：MODE_FOLLOW_SYSTEM / MODE_LIGHT / MODE_DARK
     */
    public static void setDefaultThemeMode(int mode) {
        // 校验模式合法性，防止传入无效值
        if (mode == MODE_FOLLOW_SYSTEM || mode == MODE_LIGHT || mode == MODE_DARK) {
            DEFAULT_THEME_MODE = mode;
        }
    }

    // ====================== 模式变化监听相关（线程安全） ======================
    // 监听者列表（使用CopyOnWriteArrayList避免并发修改异常）
    private static final List<OnThemeModeChangedListener> LISTENERS = new CopyOnWriteArrayList<>();

    /**
     * 模式变化的回调接口
     *
     * @ newSavedMode 新的用户偏好模式（MODE_FOLLOW_SYSTEM/MODE_LIGHT/MODE_DARK）
     * @ newActiveMode 新的实际生效模式（仅MODE_LIGHT/MODE_DARK）
     */
    public interface OnThemeModeChangedListener {
        void onThemeModeChanged(int newSavedMode, int newActiveMode);
    }

    /**
     * 注册模式监听（建议在组件的onStart/onResume中调用）
     */
    public static void registerListener(OnThemeModeChangedListener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    /**
     * 取消模式监听（必须在组件的onStop/onDestroy中调用，防止内存泄漏）
     */
    public static void unregisterListener(OnThemeModeChangedListener listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    /**
     * 通知所有监听者模式发生变化（私有方法，内部调用）
     */
    private static void notifyModeChanged(Context context, int newSavedMode) {
        int newActiveMode = getCurrentActiveMode(context);
        for (OnThemeModeChangedListener listener : LISTENERS) {
            listener.onThemeModeChanged(newSavedMode, newActiveMode);
        }
    }

    // ====================== MMKV存储相关（私有化，避免外部直接操作） ======================

    /**
     * 获取MMKV全局实例
     */
    private static MMKV getMMKV() {
        return MMKV.defaultMMKV();
    }

    // ====================== 偏好模式相关方法（用户设置的模式） ======================

    /**
     * 保存用户选择的偏好模式
     */
    public static void saveThemeMode(Context context, int mode) {
        // 校验模式合法性，防止传入无效值
        if (mode != MODE_FOLLOW_SYSTEM && mode != MODE_LIGHT && mode != MODE_DARK) {
            mode = DEFAULT_THEME_MODE; // 非法值时使用默认模式
        }
        getMMKV().putInt(KEY_THEME_MODE, mode);
    }

    /**
     * 获取用户保存的偏好模式（无记录时返回自定义的默认模式，默认是浅色）
     */
    public static int getSavedThemeMode(Context context) {
        return getMMKV().getInt(KEY_THEME_MODE, DEFAULT_THEME_MODE);
    }

    // ====================== 实际生效模式相关方法（当前APP显示的模式） ======================

    /**
     * 获取当前实际生效的模式（仅返回MODE_LIGHT或MODE_DARK，不会返回跟随系统）
     */
    public static int getCurrentActiveMode(Context context) {
        int savedMode = getSavedThemeMode(context);
        if (savedMode == MODE_LIGHT) {
            return MODE_LIGHT;
        } else if (savedMode == MODE_DARK) {
            return MODE_DARK;
        } else {
            // 跟随系统时，获取系统当前的实际模式
            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightMode == Configuration.UI_MODE_NIGHT_YES ? MODE_DARK : MODE_LIGHT;
        }
    }

    /**
     * 判断当前是否是深色模式（基于实际生效的模式）
     */
    public static boolean isDarkMode(Context context) {
        return getCurrentActiveMode(context) == MODE_DARK;
    }

    // ====================== 模式应用相关方法（核心业务逻辑） ======================

    /**
     * 应用主题模式（全局生效，建议在Application中初始化时调用）
     */
    public static void applyThemeMode(Context context) {
        int mode = getSavedThemeMode(context);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * 直接设置并应用模式（核心：包含防重复设置逻辑）
     */
    public static void setAndApplyThemeMode(Context context, int mode) {
        // 防重复设置：如果新模式和当前模式一致，直接返回，不执行任何操作
        int currentSavedMode = getSavedThemeMode(context);
        if (currentSavedMode == mode) {
            return;
        }

        // 保存并应用新模式
        saveThemeMode(context, mode);
        applyThemeMode(context);
        // 通知所有监听者模式变化
        notifyModeChanged(context, mode);
    }

    // ====================== 辅助方法（提升开发效率） ======================

    /**
     * 将模式常量转为文字描述（便于UI显示、Toast提示等）
     */
    public static String getModeName(Context context, int mode) {
        switch (mode) {
            case MODE_FOLLOW_SYSTEM:
                return "跟随系统";
            case MODE_LIGHT:
                return "浅色模式";
            case MODE_DARK:
                return "深色模式";
            default:
                return "跟随系统";
        }
    }
}