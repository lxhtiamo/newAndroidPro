package com.linewell.lxhdemo.ui.start;

import android.os.Bundle;

import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.base.BaseActivity;
import com.linx.mylibrary.utils.klog.KLog;

public class WelcomeActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_welcome;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        KLog.d("1");
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        KLog.d("2");
    }

    @Override
    protected boolean isNeedEventBus() {

        return super.isNeedEventBus();
    }

    @Override
    protected boolean showBar() {
        return false;
    }

    @Override
    protected void getBundleExtras(Bundle extras) {

    }
}
