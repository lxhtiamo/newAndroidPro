package com.linewell.lxhdemo.widget.webView.bean;

import android.os.Build;

import java.util.HashMap;
import java.util.Map;

/**
 * 优化版：Builder 模式适配 JS 空参数场景
 * 1. 仅 actionType 为业务核心字段，强制非空校验
 * 2. data、sourceMethod 支持空值，或设置默认值
 * 3. 保留不可变性、链式调用、参数校验
 */
public class JsInteractionData {
    // 业务类型枚举（不变）
    public enum ActionType {
        TOAST,          // 提示弹窗
        IS_LOGIN,          // 是否登录
        GET_DEVICE_INFO,// 获取设备信息
        GET_USER_INFO,  // 获取用户信息
        PAGE_JUMP,      // 页面跳转
        CUSTOM          // 自定义业务类型
    }

    // ======================== 字段（私有化，final 保证不可变性） ========================
    private final ActionType actionType; // 核心：业务类型（非空）
    private final String data;           // 可选：JS 传递的字符串（可空）
    private final String sourceMethod;   // 可选：触发的 JS 方法名（可空，默认空字符串）
    private final String callbackId;     // 可选：回调ID（可空）
    private final Map<String, String> extra; // 可选：额外参数（默认空 Map）

    // ======================== 构造方法（私有化，仅 Builder 能调用） ========================
    private JsInteractionData(Builder builder) {
        this.actionType = builder.actionType;
        // data 支持空，若为 null 则赋值为空字符串（避免后续处理空指针）
        this.data = builder.data == null ? "" : builder.data;
        // sourceMethod 支持空，默认空字符串
        this.sourceMethod = builder.sourceMethod == null ? "" : builder.sourceMethod;
        this.callbackId = builder.callbackId;
        // extra 默认为空 Map，避免空指针
        this.extra = builder.extra == null ? new HashMap<>() : new HashMap<>(builder.extra);
    }

    // ======================== Builder 内部类（优化核心：去掉强制必选参数） ========================
    public static class Builder {
        // 1. 仅保留 actionType 作为 Builder 的核心字段（初始为 null）
        private ActionType actionType;
        // 2. 其他字段均为可选，初始为 null
        private String data;
        private String sourceMethod;
        private String callbackId;
        private Map<String, String> extra;

        // ======================== 链式设置方法（所有字段都是可选的） ========================
        /**
         * 设置业务类型（核心，必须调用）
         */
        public Builder actionType(ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        /**
         * 设置 JS 传递的数据（可空，支持空字符串）
         */
        public Builder data(String data) {
            this.data = data;
            return this;
        }

        /**
         * 设置触发的 JS 方法名（可空，默认空字符串）
         */
        public Builder sourceMethod(String sourceMethod) {
            this.sourceMethod = sourceMethod;
            return this;
        }

        /**
         * 设置回调ID（可空）
         */
        public Builder callbackId(String callbackId) {
            this.callbackId = callbackId;
            return this;
        }

        /**
         * 设置额外参数（可空）
         */
        public Builder extra(Map<String, String> extra) {
            this.extra = extra;
            return this;
        }

        /**
         * 便捷添加单个额外参数
         */
        public Builder addExtra(String key, String value) {
            if (this.extra == null) {
                this.extra = new HashMap<>();
            }
            this.extra.put(key, value);
            return this;
        }

        // ======================== 构建方法（核心：业务层面的参数校验） ========================
        public JsInteractionData build() {
            // 仅校验核心字段：actionType 不能为 null（否则无法处理业务逻辑）
            if (actionType == null) {
                throw new IllegalArgumentException("actionType 不能为空（业务类型是处理逻辑的核心）");
            }
            // 其他字段不校验，支持空值
            return new JsInteractionData(this);
        }
    }

    // ======================== Getter 方法（保留不可变性，无 setter） ========================
    public ActionType getActionType() {
        return actionType;
    }

    public String getData() {
        return data;
    }

    public String getSourceMethod() {
        return sourceMethod;
    }

    public String getCallbackId() {
        return callbackId;
    }

    public Map<String, String> getExtra() {
        return new HashMap<>(extra); // 返回新的 Map，防止外部修改
    }

    // ======================== 便捷方法（避免空指针） ========================
    public String getExtraValue(String key) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return extra.getOrDefault(key, "");
        }
        return key;
    }
}