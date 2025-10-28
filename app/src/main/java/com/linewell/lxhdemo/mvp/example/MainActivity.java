package com.linewell.lxhdemo.mvp.example;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.mvp.MvpActivity;
import com.linewell.lxhdemo.thirdAppUtil.UMShareManager;

public class MainActivity extends MvpActivity<MainContract.IMainPresenter> implements MainContract.IMainView {

    private UMShareManager mShareManager;

    @Override
    protected void getBundleExtras(Bundle extras) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        // 配置各平台密钥（替换为你的申请值）
        Button button = findViewById(R.id.bt_bt);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, new PermissionCallback() {
                    @Override
                    public void PermissionSucceed() {
                        showToast("成功");
                    }

                    @Override
                    public void PermissionFail() {

                    }
                });
            }
        });
        Button button1 = findViewById(R.id.bt_bt1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, new PermissionCallback() {
                    @Override
                    public void PermissionSucceed() {
                        showToast("成功");
                    }

                    @Override
                    public void PermissionFail() {

                    }
                });
            }
        });

    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        getPresenter().requestTestContent("111");
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
    protected MainContract.IMainPresenter createPresenter() {
        return new MainPresenterImpl();
    }

    @Override
    public void onSuccess() {
    }

    @Override
    public void onFailure() {
    }

    @Override
    public void showLoading() {

    }

    @Override
    public void loadingComplete() {

    }

    @Override
    public void showEmpty() {

    }

    @Override
    public void showError() {

    }
}
