package com.linewell.lxhdemo.mvp;


import com.linewell.lxhdemo.base.BaseFragment;

/**
 * time   : 2018/11/17
 * desc   : MVP 懒加载 Fragment 基类
 */
public abstract class MvpFragment<P extends MvpPresenter> extends BaseFragment implements MvpView {

    private P presenter;

    @Override
    protected void initLazyLoad() {
        presenter = createPresenter();
        if (presenter == null) {
            throw new NullPointerException("Presenter is null! Do you return null in createPresenter()?");
        }
        presenter.onMvpAttachView(this, null);
        presenter.onMvpStart();
        super.initLazyLoad();
    }

    @Override
    public void onDestroy() {
        if (presenter != null) {
            presenter.onMvpDetachView(false);
            presenter.onMvpDestroy();
        }
        super.onDestroy();
    }

    public P getPresenter() {
        return presenter;
    }

    protected abstract P createPresenter();
}