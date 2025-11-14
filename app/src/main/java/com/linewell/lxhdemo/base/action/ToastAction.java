package com.linewell.lxhdemo.base.action;

import androidx.annotation.StringRes;

import com.hjq.toast.Toaster;


/**
 *    desc   : 吐司意图
 */
public interface ToastAction {

    default void showToast(CharSequence text) {
        Toaster.show(text);
    }

    default void showToast(@StringRes int id) {
        Toaster.show(id);
    }

    default void showToast(Object object) {
        Toaster.show(object);
    }
}