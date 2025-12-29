package com.linewell.lxhdemo.ui.start;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.lifecycle.Observer;

import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.base.BaseActivity;
import com.linewell.lxhdemo.eventbus.MyEvent;
import com.linewell.lxhdemo.liveDataBus.LiveDataBus;
import com.linx.mylibrary.utils.klog.KLog;
import com.linx.mylibrary.utils.manager.AppActivityManager;
import com.linx.mylibrary.utils.manager.AppLogManager;

public class WelcomeActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_welcome;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        KLog.d("1");
        LiveDataBus.postSticky(new MyEvent(MyEvent.What.example, "测试普通事件11111111"));


         LiveDataBus.post(new MyEvent(MyEvent.What.example, "测试普通事件"));
        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
            }
        },300);
  }

    @Override
    protected void initData(Bundle savedInstanceState) {
       setOnClickListener(R.id.bt,R.id.bt_h,R.id.bt,R.id.bt_b);
        LiveDataBus.observeSticky(this, MyEvent.class, new Observer<MyEvent>() {
            @Override
            public void onChanged(MyEvent event) {
                // 处理登录成功事件
                // 例如更新UI、跳转页面等
                AppLogManager.d("onChanged: " + event);
            }
        });
        LiveDataBus.observe(this, MyEvent.class, new Observer<MyEvent>() {
            @Override
            public void onChanged(MyEvent event) {
                // 处理登录成功事件
                // 例如更新UI、跳转页面等
                AppLogManager.d("onChanged: " + event);
            }
        });
        LiveDataBus.observe(this, MyEvent.class, new Observer<MyEvent>() {
            @Override
            public void onChanged(MyEvent event) {
                // 处理登录成功事件
                // 例如更新UI、跳转页面等
                AppLogManager.d("onChanged: " + event);
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()){
            case R.id.bt:
               // Activity currentActivity = AppActivityManager.getInstance().getCurrentActivity();
                LiveDataBus.post(new MyEvent(MyEvent.What.example, "张三"));
                break;
            case R.id.bt_h:
                LiveDataBus.postSticky(new MyEvent(MyEvent.What.example, "张三"));
                //Activity topActivity = AppActivityManager.getInstance().getTopActivity();
                break;
            case R.id.bt_b:
               // AppActivityManager.getInstance().finishAllActivities();
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
