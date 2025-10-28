package com.linewell.lxhdemo.mvp.example;

import com.linewell.lxhdemo.mvp.MvpPresenter;
import com.linewell.lxhdemo.mvp.MvpView;

/**
 * @author xh
 * @Description (描述: )
 * @date 2018/6/1 16:30
 */
public class MainContract {

    public interface IMainView extends MvpView {

        /**
         * 测试
         */
        void onSuccess();
        void onFailure();
    }

    public interface IMainPresenter extends MvpPresenter<IMainView> {

        /**
         * 请求参数
         * @param parameters
         */
        void requestTestContent(String parameters);
    }
}
