package com.linewell.lxhdemo.widget.webView.iface;

import android.content.Context;

/**
 * JS 与 Android 交互的基础接口（规范约束）
 */
public interface IJsBridge {
    /**
     * 获取上下文（建议使用 Application 上下文，避免内存泄漏）
     */
    Context getContext();

    /**
     * 注销接口时的回调（用于释放资源）
     */
    default void onDestroy() {}
}