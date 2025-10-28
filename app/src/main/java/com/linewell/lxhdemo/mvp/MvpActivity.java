package com.linewell.lxhdemo.mvp;


import android.os.Bundle;

import com.linewell.lxhdemo.base.BaseActivity;

/**
 *MVP Activity 基类
 */
public abstract class MvpActivity <P extends MvpPresenter>  extends BaseActivity implements MvpView {



    private P presenter;
    @Override
    protected void initActivity(Bundle savedInstanceState) {
        presenter = createPresenter();//创建Presenter层
        if (presenter == null) {
            throw new NullPointerException("Presenter is null! Do you return null in createPresenter()?");
        }
        presenter.onMvpAttachView(this, savedInstanceState);//做绑定
        super.initActivity(savedInstanceState);
    }

    public P getPresenter() {
        return presenter;
    }

    protected abstract P createPresenter();

    //绑定view生命周期同步
    @Override
    protected void onStart() {
        if (presenter != null) {
            presenter.onMvpStart();
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        if (presenter != null) {
            presenter.onMvpResume();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (presenter != null) {
            presenter.onMvpPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {

        if (presenter != null) {
            presenter.onMvpStop();
        } super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (presenter != null) {
            presenter.onMvpDetachView(false);
            presenter.onMvpDestroy();
        }
        super.onDestroy();
    }
}