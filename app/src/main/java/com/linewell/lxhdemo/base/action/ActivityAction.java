package com.linewell.lxhdemo.base.action;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

/**
 */
public interface ActivityAction {
    /**
     * 获取 Context 对象
     */
    Context getContext();

    /**
     * 获取 Activity 对象
     */
    default Activity getActivity() {
        Context context = getContext();
        do {
            if (context instanceof Activity) {
                return (Activity) context;
            } else if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else {
                return null;
            }
        } while (context != null);
        return null;
    }
}