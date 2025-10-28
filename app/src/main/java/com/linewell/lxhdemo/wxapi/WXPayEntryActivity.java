package com.linewell.lxhdemo.wxapi;

import android.app.Activity;
import android.os.Bundle;

import com.linewell.lxhdemo.thirdAppUtil.WeChatHelper;


public class WXPayEntryActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 同样将Intent传递给WeChatHelper处理（支付类回调）
        WeChatHelper.getInstance().handleIntent(getIntent());
        finish(); // 处理完后立即关闭
    }
}