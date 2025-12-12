package com.linewell.lxhdemo.ui.start;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.base.aop.SingleClick;

import java.util.Date;

public class StartActivity extends AppCompatActivity implements View.OnClickListener {
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button viewById = findViewById(R.id.bt);
        tv = findViewById(R.id.tv);
       // viewById.setOnClickListener(this);
       //// Button bt_h = findViewById(R.id.bt_h);
       // bt_h.setOnClickListener(this);
        Button bt_b = findViewById(R.id.bt_b);
        bt_b.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //tv.setText(new Date().getTime() + "");
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            // 关闭暗黑模式
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            // 开启暗黑模式
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        recreate();
    }
}