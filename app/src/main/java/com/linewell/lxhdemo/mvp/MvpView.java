package com.linewell.lxhdemo.mvp;

/**
 * @author xh
 * @Description (描述: )
 * @date 2018/6/1 15:59
 */
public interface MvpView {
    /**
     * 用于页面请求数据时显示加载状态
     */
    void showLoading();

    /**
     * 用于请求数据完成
     */
    void loadingComplete();

    /**
     * 用于请求的数据为空的状态
     */
    void showEmpty();

    /**
     * 用于请求数据出错
     */
    void showError();
}
