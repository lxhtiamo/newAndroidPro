package com.linewell.lxhdemo.wxapi;

import android.app.Activity;
import android.os.Bundle;

import com.linewell.lxhdemo.thirdAppUtil.WeChatHelper;

public class WXEntryActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 将intent传递给WeChatHelper处理
        WeChatHelper.getInstance().handleIntent(getIntent());
        finish();
    }
}