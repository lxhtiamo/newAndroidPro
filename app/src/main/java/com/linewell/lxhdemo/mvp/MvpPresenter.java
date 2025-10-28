package com.linewell.lxhdemo.mvp;

import android.os.Bundle;

/**
 * @author xh
 * @Description (描述: )
 * @date 2018/6/1 16:10
 */
public interface MvpPresenter <V extends MvpView> {
    void onMvpAttachView(V view, Bundle savedInstanceState);

    void onMvpStart();

    void onMvpResume();

    void onMvpPause();

    void onMvpStop();

    void onMvpSaveInstanceState(Bundle savedInstanceState);

    void onMvpDetachView(boolean retainInstance);

    void onMvpDestroy();
}
