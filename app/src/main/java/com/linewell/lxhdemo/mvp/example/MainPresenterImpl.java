package com.linewell.lxhdemo.mvp.example;

import android.os.Bundle;

import com.linewell.lxhdemo.mvp.BaseMvpPresenter;
import com.linx.mylibrary.http.HttpManage;
import com.linx.mylibrary.http.callback.JsonCallback;
import com.linx.mylibrary.http.model.LzyResponse;
import com.lzy.okgo.model.Response;

/**
 * @author xh
 * @Description (描述: )
 * @date 2018/6/1 16:37
 */
public class MainPresenterImpl extends BaseMvpPresenter<MainContract.IMainView> implements MainContract.IMainPresenter {
    @Override
    public void requestTestContent(String s) {
        //先进行非空判断
        if (isViewAttached()) {
            HttpManage.getInstance().postJsonRequets(this, "", "", new JsonCallback<LzyResponse>() {
                @Override
                public void onSuccess(Response<LzyResponse> response) {
                    getView().onSuccess();
                }

                @Override
                protected void onFail(String err) {
                    getView().onFailure();
                }

            });
        }

    }
    @Override
    public void onMvpAttachView(MainContract.IMainView view, Bundle savedInstanceState) {
        super.onMvpAttachView(view, savedInstanceState);
    }

    /**
     *重写P层需要的生命周期，进行相关逻辑操作
     */
    @Override
    public void onMvpResume() {
        super.onMvpResume();
    }

    @Override
    public void onMvpDestroy() {
        super.onMvpDestroy();
        //取消网络请求等
    }
}
