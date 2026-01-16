package com.linewell.lxhdemo.ui.home.fragment;


import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.base.BaseFragment;
import com.linx.mylibrary.utils.RxTimerUtil;

public class HomeFragment extends BaseFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home;
    }

    @Override
    protected void initView() {
        Log.d("HomeFragment", "onCreateView: position=");
    }

    @Override
    protected void initData() {

        TextView viewById = findViewById(R.id.tv_fragment_content);

        RxTimerUtil.getInstance().scheduleCountdown(60, new RxTimerUtil.RxTimerCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onNext(@NonNull String taskId, long value) {
                viewById.setText(value + "");
            }
        });
    }
}
