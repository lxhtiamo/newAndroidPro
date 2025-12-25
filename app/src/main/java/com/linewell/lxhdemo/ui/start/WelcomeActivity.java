package com.linewell.lxhdemo.ui.start;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.base.BaseActivity;
import com.linx.mylibrary.utils.klog.KLog;
import com.linx.mylibrary.utils.manager.AppActivityManager;

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
       setOnClickListener(R.id.bt,R.id.bt_h,R.id.bt,R.id.bt_b);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()){
            case R.id.bt:
                Activity currentActivity = AppActivityManager.getInstance().getCurrentActivity();

                break;
            case R.id.bt_h:
                Activity topActivity = AppActivityManager.getInstance().getTopActivity();
                break;
            case R.id.bt_b:
                AppActivityManager.getInstance().finishAllActivities();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }
    @Override
    protected boolean isNeedEventBus() {

        return super.isNeedEventBus();
    }

    @Override
    protected boolean showBar() {
        return super.showBar();
    }

    @Override
    protected void getBundleExtras(Bundle extras) {

    }
}
