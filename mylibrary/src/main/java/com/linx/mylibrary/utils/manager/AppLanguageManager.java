package com.linx.mylibrary.utils.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/*-------------------使用方法--------------------*/
/*1.自定义 Application 类（全局语言配置）添加
*  @Override
    protected void attachBaseContext(Context base) {
        // 注入语言配置后的Context
        super.attachBaseContext(LanguageManager.updateContextLocale(base));
    }
*
*2.在res目录创建对应语言文件夹并添加strings.xml：
中文：res/values-zh/strings.xml
英文：res/values-en/strings.xml
（所有字符串用@string/xxx引用，禁止硬编码）
*
* 3.扩展新语言
1.LanguageType.java新增枚举（如日语）：JAPANESE("ja", "日本語", Locale.JAPAN)；
创建res/values-ja/strings.xml；
2.LanguageManager.getSelectableLanguages()中添加list.add(LanguageType.JAPANESE)。
* 4.获取可选语言列表
List<LanguageType> languageList = LanguageManager.getSelectableLanguages();
*
* 5.保存选择的语言
        LanguageManager.saveSelectedLanguage(context, selected);
        // 重启主页面使全局生效
        LanguageManager.restartMainActivity(context);

* */
/**
 * 适配绝大多数安卓场景的语言管理类
 * 核心能力：扩展语言仅改枚举、获取可选语言列表、全版本兼容、容错
 */
public class AppLanguageManager {
    private static final String TAG = "LanguageManager";
    private static final String SP_NAME = "app_language_sp";
    private static final String KEY_SELECTED_LANG = "selected_language_key";

    // ==================== 兼容：获取系统Locale（替换过时API） ====================
    private static Locale getSystemLocale() {
        Configuration config = Resources.getSystem().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = config.getLocales();
            return localeList.isEmpty() ? Locale.getDefault() : localeList.get(0);
        } else {
            // 低版本保留旧逻辑（虽过时但无替代，安卓允许低版本使用）
            return config.locale;
        }
    }

    // ==================== 获取目标Locale（引用兼容方法） ====================
    private static Locale getTargetLocale(Context context) {
        LanguageType selectedType = getSelectedLanguage(context);
        if (selectedType == LanguageType.FOLLOW_SYSTEM) {
            // 改用兼容方法
            return getSystemLocale();
        } else {
            return selectedType.getLocale() != null ? selectedType.getLocale() : Locale.getDefault();
        }
    }

    // ==================== 其他原有方法（无需修改） ====================
    public static void saveSelectedLanguage(Context context, LanguageType languageType) {
        if (context == null || languageType == null) {
            Log.w(TAG, "保存语言失败：上下文/语言类型为空");
            return;
        }
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_SELECTED_LANG, languageType.getLangKey()).apply();
        Log.d(TAG, "保存语言成功：" + languageType.getDisplayName());
    }

    public static LanguageType getSelectedLanguage(Context context) {
        if (context == null) {
            return LanguageType.FOLLOW_SYSTEM;
        }
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String langKey = sp.getString(KEY_SELECTED_LANG, LanguageType.FOLLOW_SYSTEM.getLangKey());
        return LanguageType.getByLangKey(langKey);
    }

    public static List<LanguageType> getSelectableLanguages() {
        List<LanguageType> list = new ArrayList<>();
        list.add(LanguageType.FOLLOW_SYSTEM);
        list.add(LanguageType.CHINESE);
        list.add(LanguageType.ENGLISH);
        return list;
    }

    public static Context updateContextLocale(Context context) {
        if (context == null) {
            return null;
        }
        Locale targetLocale = getTargetLocale(context);
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        DisplayMetrics dm = resources.getDisplayMetrics();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(targetLocale);
            LocaleList localeList = new LocaleList(targetLocale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
            context = context.createConfigurationContext(config);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(targetLocale);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = targetLocale;
            resources.updateConfiguration(config, dm);
        }
        return context;
    }

    public static void restartCurrentActivity(Context context) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).recreate();
        }
    }

    public static void restartMainActivity(Context context, Class<?> mainActivityClass) {
        if (context == null || mainActivityClass == null) {
            return;
        }
        android.content.Intent intent = new android.content.Intent(context, mainActivityClass);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).finish();
        }
    }
}