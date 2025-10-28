package com.linx.mylibrary.view.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.linx.mylibrary.R;


/**
 * 加载中对话框工具类
 */

public class ProgressLoadingDialog {

    private static final long TIME_DISMISS_DEFAULT = 1200;
    private Dialog mDialog;
    private View mDialogContentView;
    private TextView tv_loadText;
    private ImageView iv_loadImage;
    private ProgressBar pb_loadProgress;
    private LinearLayout ll_content;
    private ViewGroup.LayoutParams layoutParams;

    public ProgressLoadingDialog(Context context) {
        this(context, R.style.dialog_transparent_style);
    }

    @SuppressLint("InflateParams")
    public ProgressLoadingDialog(Context context, int style) {
        mDialog = new Dialog(context, style);
        mDialogContentView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        ll_content = (LinearLayout) mDialogContentView.findViewById(R.id.ll_content);
        tv_loadText = (TextView) mDialogContentView.findViewById(R.id.tv_loading_text);
        iv_loadImage = (ImageView) mDialogContentView.findViewById(R.id.iv_load_image);
        pb_loadProgress = (ProgressBar) mDialogContentView.findViewById(R.id.pb_load_progress);

        layoutParams = ll_content.getLayoutParams();
        //  mDialog.setCancelable(false);// 不可以用“返回键”取消
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setContentView(mDialogContentView);
        Window window = mDialog.getWindow();
        if (null != window) {
//            window.getAttributes().width = (int) (UIUtils.getScreenWidth() * 0.5);
//            window.getAttributes().height = (int) (UIUtils.getScreenHegith() * 0.2);
            window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        }
    }

    /**
     * 显示加载的ProgressDialog
     */
    public void showProgressDialog() {
        if (mDialog != null && !mDialog.isShowing()) {
            pb_loadProgress.setVisibility(View.VISIBLE);
            iv_loadImage.setVisibility(View.GONE);
            tv_loadText.setVisibility(View.GONE);
            mDialog.show();
            setSize();
        }
    }

    /**
     * 设置弹窗大小为正方形
     */
    private void setSize() {
        if (layoutParams != null) {
            ll_content.measure(0, 0);
            int width = ll_content.getMeasuredWidth();
            int height = ll_content.getMeasuredHeight();
            if (width < height) {
                layoutParams.width = height;
                layoutParams.height = height;

            } else {
                layoutParams.height = width;
                layoutParams.width = width;

            }
            ll_content.setLayoutParams(layoutParams);
        }
    }

    /**
     * 显示有加载文字ProgressDialog，文字显示在ProgressDialog的下面
     *
     * @param text 需要显示的文字
     */
    public void showProgressDialogWithText(String text) {
        if (TextUtils.isEmpty(text)) {
            showProgressDialog();
        } else {
            if (mDialog != null && !mDialog.isShowing()) {
                pb_loadProgress.setVisibility(View.VISIBLE);
                iv_loadImage.setVisibility(View.GONE);
                tv_loadText.setText(text);
                tv_loadText.setVisibility(View.VISIBLE);
                mDialog.show();
                setSize();
            }
        }
    }

    /**
     * 显示加载成功的ProgressDialog，文字显示在ProgressDialog的下面
     *
     * @param message 加载成功需要显示的文字
     * @param time    需要显示的时间长度(以毫秒为单位)
     */
    public void showProgressSuccess(String message, long time) {
        if (TextUtils.isEmpty(message)) {
            return;
        }

        if (mDialog != null && !mDialog.isShowing()) {
            pb_loadProgress.setVisibility(View.GONE);
            iv_loadImage.setImageResource(R.mipmap.ic_load_success_white);
            iv_loadImage.setVisibility(View.VISIBLE);
            tv_loadText.setText(message);
            tv_loadText.setVisibility(View.VISIBLE);
            mDialog.show();
            setSize();
            mDialogContentView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDialog.dismiss();
                }
            }, time);
        }
    }

    /**
     * 显示加载成功的ProgressDialog，文字显示在ProgressDialog的下面
     * ProgressDialog默认消失时间为1秒(1000毫秒)
     *
     * @param message 加载成功需要显示的文字
     */
    public void showProgressSuccess(String message) {
        showProgressSuccess(message, TIME_DISMISS_DEFAULT);
    }

    /**
     * 显示加载失败的ProgressDialog，文字显示在ProgressDialog的下面
     *
     * @param message 加载失败需要显示的文字
     * @param time    需要显示的时间长度(以毫秒为单位)
     */
    public void showProgressFail(String message, long time) {
        if (TextUtils.isEmpty(message)) {
            return;
        }

        if (mDialog != null && !mDialog.isShowing()) {
            pb_loadProgress.setVisibility(View.GONE);
            iv_loadImage.setImageResource(R.mipmap.ic_load_fail_white);
            iv_loadImage.setVisibility(View.VISIBLE);
            tv_loadText.setText(message);
            tv_loadText.setVisibility(View.VISIBLE);
            mDialog.show();
            setSize();
            mDialogContentView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDialog.dismiss();
                }
            }, time);
        }
    }

    /**
     * 显示加载失败的ProgressDialog，文字显示在ProgressDialog的下面
     * ProgressDialog默认消失时间为1秒(1000毫秒)
     *
     * @param message 加载成功需要显示的文字
     */
    public void showProgressFail(String message) {
        showProgressFail(message, TIME_DISMISS_DEFAULT);
    }

    /**
     * 隐藏加载的ProgressDialog
     */
    public void dismissProgressDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    public boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }


    /**
     * 获取mDialog对象
     *
     * @return
     */
    public Dialog getProgressDialog() {
        if (mDialog != null) {
            return mDialog;
        }
        return null;
    }
}
