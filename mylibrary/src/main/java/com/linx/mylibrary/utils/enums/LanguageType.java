package com.linx.mylibrary.utils.enums;

import java.util.Locale;

/**
 * 语言枚举：新增语言仅需加一个枚举项
 * 对应res/values-zh、values-en等文件夹
 */
public enum LanguageType {
    // 跟随系统（特殊项，无对应values文件夹）
    FOLLOW_SYSTEM("follow_system", "跟随系统", null),
    // 中文（对应values-zh文件夹）
    CHINESE("zh", "中文", Locale.SIMPLIFIED_CHINESE),
    // 英文（对应values-en文件夹）
    ENGLISH("en", "英文", Locale.ENGLISH);

    // 可扩展：新增日语只需加这一行 + 创建values-ja文件夹
    // JAPANESE("ja", "日本語", Locale.JAPAN),

    // 语言唯一标识（对应values-xx的xx）
    private final String langKey;
    // 给用户显示的名称（如“中文”“英文”）
    private final String displayName;
    // 对应的Locale（用于设置语言）
    private final Locale locale;

    LanguageType(String langKey, String displayName, Locale locale) {
        this.langKey = langKey;
        this.displayName = displayName;
        this.locale = locale;
    }

    // Getter
    public String getLangKey() {
        return langKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Locale getLocale() {
        return locale;
    }

    // 根据langKey匹配枚举（用于SP读取后解析）
    public static LanguageType getByLangKey(String langKey) {
        for (LanguageType type : values()) {
            if (type.langKey.equals(langKey)) {
                return type;
            }
        }
        return FOLLOW_SYSTEM; // 兜底：跟随系统
    }
}