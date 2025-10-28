package com.linx.mylibrary.view.dialog;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;

/**
 * 描述说明:自定义AlertDialogBuilder,解决弹窗虚拟导航显示问题
 * 作者: lxh
 * 创建日期: 2018/9/14 16:54
 */
public class CustomAlertDialogBuilder extends AlertDialog.Builder {
    public CustomAlertDialogBuilder(@NonNull Context context) {
        super(context);
    }

    public CustomAlertDialogBuilder(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = create();
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show();
        fullScreenImmersive(dialog.getWindow().getDecorView());
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        return dialog;
    }

    @Override
    public AlertDialog create() {

        return super.create();
    }

    /**
     * 解决弹窗虚拟导航显示问题
     * @param view
     */
    private void fullScreenImmersive(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            view.setSystemUiVisibility(uiOptions);
        }
    }
}
