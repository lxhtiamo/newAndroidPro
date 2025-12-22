package com.linx.mylibrary.view.webView.iface;

import com.linx.mylibrary.view.webView.bean.JsInteractionData;

/**
 * 简化版：JS 数据回调接口（仅一个通用方法，接收字符串数据的通用对象）
 */
public interface OnJsDataCallback {
    /**
     * 接收 JS 传递的所有交互数据（通用方法，覆盖所有场景）
     * @param jsData 简化后的通用交互数据对象（仅处理字符串）
     */
    void onJsDataReceived(JsInteractionData jsData);
}