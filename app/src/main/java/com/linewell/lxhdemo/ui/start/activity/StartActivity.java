package com.linewell.lxhdemo.ui.start.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        // 保持启动屏显示，直到跳转准备完成
        splashScreen.setKeepOnScreenCondition(() -> true);

        // 延迟300ms（和启动屏动画时长匹配，避免跳转过早）
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 跳转前：关闭启动屏保持，让启动屏自然消失
            splashScreen.setKeepOnScreenCondition(() -> false);
            // 跳转到欢迎页/主页面
            startActivity(new Intent(this, GuideActivity.class));
            // 关键：结束启动页，避免返回键回到启动页
            finish();
            // 可选：添加跳转动画（避免页面切换生硬）
            overridePendingTransition(0, 0); // 无动画
        }, 300);
    }
}