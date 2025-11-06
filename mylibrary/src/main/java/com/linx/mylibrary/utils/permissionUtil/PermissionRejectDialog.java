package com.linx.mylibrary.utils.permissionUtil;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.linx.mylibrary.R;
import com.linx.mylibrary.view.dialog.CustomAlertDialogBuilder;


/**
 * 自定义弹窗工具类
 * 封装通用弹窗逻辑，支持自定义标题、内容、按钮文本及点击事件
 */
public class PermissionRejectDialog {

    /**
     * 取消按钮点击回调接口
     */
    public interface OnCancelClickListener {
        void onCancel();
    }

    /**
     * 确定按钮点击回调接口
     */
    public interface OnConfirmClickListener {
        void onConfirm();
    }

    /**
     * 显示自定义弹窗
     *
     * @param context        上下文（需传入context，避免内存泄漏）
     * @param title           弹窗标题
     * @param content         弹窗内容
     * @param cancelText      取消按钮文本
     * @param confirmText     确定按钮文本
     * @param cancelListener  取消按钮点击回调（可传null）
     * @param confirmListener 确定按钮点击回调（可传null）
     * @param cancelOutside   点击外部是否关闭弹窗
     */
    public static void showCustomDialog(Context context,
                                        String title,
                                        String content,
                                        String cancelText,
                                        String confirmText,
                                        OnCancelClickListener cancelListener,
                                        OnConfirmClickListener confirmListener,
                                        boolean cancelOutside) {
        // 1. 创建弹窗构建器
        CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(context);
        // 2. 加载自定义布局
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_reject, null);
        // 3. 创建弹窗实例
        AlertDialog dialog = builder.create();
        dialog.setView(customView);

        // 4. 获取布局控件并设置数据
        TextView titleTv = customView.findViewById(R.id.tv_title);
        TextView contentTv = customView.findViewById(R.id.tv_content);
        TextView btnCancel = customView.findViewById(R.id.btn_cancel);
        TextView btnConfirm = customView.findViewById(R.id.btn_confirm);

        if (!TextUtils.isEmpty(title)) {
            titleTv.setText(title);
        }
        if (!TextUtils.isEmpty(content)) {
            contentTv.setText(content);
        }
        if (!TextUtils.isEmpty(cancelText)) {
            btnCancel.setText(cancelText);
        }
        if (!TextUtils.isEmpty(confirmText)) {
            btnCancel.setText(confirmText);
        }
        // 5. 设置取消按钮点击事件
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            if (cancelListener != null) {
                cancelListener.onCancel();
            }
        });

        // 6. 设置确定按钮点击事件
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (confirmListener != null) {
                confirmListener.onConfirm();
            }
        });

        // 7. 设置弹窗属性
        dialog.setCanceledOnTouchOutside(cancelOutside);
        // 8. 显示弹窗
        dialog.show();
    }


    public static void showCustomDialog(Context context,
                                        String title,
                                        String content,
                                        String cancelText,
                                        String confirmText,
                                        OnCancelClickListener cancelListener,
                                        OnConfirmClickListener confirmListener) {
        showCustomDialog(context, title, content, cancelText, confirmText,
                cancelListener, confirmListener, false);
    }

    /**
     * 简化版：默认点击外部不关闭弹窗
     */
    public static void showCustomDialog(Context context,
                                        String content,
                                        OnCancelClickListener cancelListener,
                                        OnConfirmClickListener confirmListener) {
        showCustomDialog(context, null, content, null, null,
                cancelListener, confirmListener, false);
    }
}