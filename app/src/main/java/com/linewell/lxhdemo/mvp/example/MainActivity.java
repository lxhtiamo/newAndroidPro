package com.linewell.lxhdemo.mvp.example;
import android.os.Bundle;
import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.mvp.MvpActivity;

public class MainActivity extends MvpActivity<MainContract.IMainPresenter> implements MainContract.IMainView {


    @Override
    protected void getBundleExtras(Bundle extras) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {

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
