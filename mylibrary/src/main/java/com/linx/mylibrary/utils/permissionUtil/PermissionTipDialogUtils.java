package com.linx.mylibrary.utils.permissionUtil;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionNames;
import com.hjq.permissions.permission.base.IPermission;
import com.linx.mylibrary.R;
import com.linx.mylibrary.utils.RxAppApplicationMgr;

import java.util.ArrayList;
import java.util.List;

public class PermissionTipDialogUtils {
    private static Dialog mDialog;
    private static PermissionTipDialogUtils instance;

    // 单例模式
    public static synchronized PermissionTipDialogUtils getInstance() {
        if (instance == null) {
            instance = new PermissionTipDialogUtils();
        }
        return instance;
    }

    /**
     * 显示权限提示弹窗
     *
     * @param context           上下文
     * @param title             标题
     * @param content           内容
     * @param permissions           内容
     */
    public void showPermissionTipDialog(Context context, String title, String content, List<IPermission> permissions) {
        // 关闭已存在的弹窗
        dismissDialog();
        String appName = RxAppApplicationMgr.getAppName(context);
        List<IPermission> deniedPermissions = XXPermissions.getDeniedPermissions(context, permissions);
        List<String> permissionNames = new ArrayList<>();
        for (IPermission deniedPermission : deniedPermissions) {
            String permissionName = deniedPermission.getPermissionName();
            permissionNames.add(permissionName);
        }
        String permissionHint = getPermissionHint(context, permissionNames);
        // 创建Dialog
        mDialog = new Dialog(context, R.style.PermissionTipDialogStyle);
        // 加载布局
        View inflate = LayoutInflater.from(context).inflate(R.layout.permission_tip_layout, null);
        mDialog.setContentView(inflate);
        // 设置内容
        TextView titleTv = inflate.findViewById(R.id.tv_tip_title);
        TextView contentTv = inflate.findViewById(R.id.tv_tip_content);
        CardView cv_card = inflate.findViewById(R.id.cv_card);
        if (TextUtils.isEmpty(title)) {
            title = permissionHint + "权限申请说明";
        }
        titleTv.setText(title);
        if (TextUtils.isEmpty(content)) {
            if (!TextUtils.isEmpty(hintText)) {
                content = hintText;
            } else {
                content = context.getString(R.string.permission_content,appName,permissionHint);
            }
        }
        contentTv.setText(content);

        // 设置确认按钮点击事件
        cv_card.setOnClickListener(v -> {
            dismissDialog();
        });

        // 设置窗口属性
        // 3. 设置 Dialog 显示位置（顶部）
        Window window = mDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.TOP; // 顶部显示
            params.width = WindowManager.LayoutParams.MATCH_PARENT; // 宽度全屏
            params.height = WindowManager.LayoutParams.WRAP_CONTENT; // 高度自适应
            // 关键：添加允许窗口延伸到状态栏的标志
            params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            window.setAttributes(params);
        }

        // 设置点击外部不消失
        mDialog.setCanceledOnTouchOutside(false);
        // 显示弹窗
        mDialog.show();
    }
    List<String> hintTextContents;
    String hintText;
    /**
     * 根据权限获取提示
     */
    protected String getPermissionHint(Context context, List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return context.getString(R.string.common_permission_fail_2);
        }

        List<String> hints = new ArrayList<>();
        hintTextContents = new ArrayList<>();
        hintText="";
        for (String permission : permissions) {
            switch (permission) {
                case PermissionNames.READ_EXTERNAL_STORAGE:
                case PermissionNames.WRITE_EXTERNAL_STORAGE:
                case PermissionNames.MANAGE_EXTERNAL_STORAGE: {
                    String hint = context.getString(R.string.common_permission_storage);

                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    String string = context.getString(R.string.permission_storage_text);
                    if (!hintTextContents.contains(string)) {
                        hintTextContents.add(string);
                    }
                    break;
                }
                case PermissionNames.CAMERA: {
                    String hint = context.getString(R.string.common_permission_camera);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    String string = context.getString(R.string.permission_camera_text);
                    if (!hintTextContents.contains(string)) {
                        hintTextContents.add(string);
                    }
                    break;
                }
                case PermissionNames.RECORD_AUDIO: {
                    String hint = context.getString(R.string.common_permission_microphone);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    String string = context.getString(R.string.permission_microphone_text);
                    if (!hintTextContents.contains(string)) {
                        hintTextContents.add(string);
                    }
                    break;
                }
                case PermissionNames.ACCESS_FINE_LOCATION:
                case PermissionNames.ACCESS_COARSE_LOCATION:
                case PermissionNames.ACCESS_BACKGROUND_LOCATION: {
                    String hint;
                    if (!permissions.contains(PermissionNames.ACCESS_FINE_LOCATION) &&
                            !permissions.contains(PermissionNames.ACCESS_COARSE_LOCATION)) {
                        hint = context.getString(R.string.common_permission_location_background);
                    } else {
                        hint = context.getString(R.string.common_permission_location);
                    }
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    String string = context.getString(R.string.permission_location_text);
                    if (!hintTextContents.contains(string)) {
                        hintTextContents.add(string);
                    }
                    break;
                }
                case PermissionNames.READ_PHONE_STATE:
                case PermissionNames.CALL_PHONE:
                case PermissionNames.ADD_VOICEMAIL:
                case PermissionNames.USE_SIP:
                case PermissionNames.READ_PHONE_NUMBERS:
                case PermissionNames.ANSWER_PHONE_CALLS: {
                    String hint = context.getString(R.string.common_permission_phone);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    String string = context.getString(R.string.permission_phone_text);
                    if (!hintTextContents.contains(string)) {
                        hintTextContents.add(string);
                    }
                    break;
                }
                case PermissionNames.GET_ACCOUNTS:
                case PermissionNames.READ_CONTACTS:
                case PermissionNames.WRITE_CONTACTS: {
                    String hint = context.getString(R.string.common_permission_contacts);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case PermissionNames.READ_CALENDAR:
                case PermissionNames.WRITE_CALENDAR: {
                    String hint = context.getString(R.string.common_permission_calendar);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case PermissionNames.READ_CALL_LOG:
                case PermissionNames.WRITE_CALL_LOG:
                case PermissionNames.PROCESS_OUTGOING_CALLS: {
                    String hint = context.getString(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                            R.string.common_permission_call_log : R.string.common_permission_phone);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case PermissionNames.BODY_SENSORS: {
                    String hint = context.getString(R.string.common_permission_sensors);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case PermissionNames.ACTIVITY_RECOGNITION: {
                    String hint = context.getString(R.string.common_permission_activity_recognition);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case PermissionNames.SEND_SMS:
                case PermissionNames.RECEIVE_SMS:
                case PermissionNames.READ_SMS:
                case PermissionNames.RECEIVE_WAP_PUSH:
                case PermissionNames.RECEIVE_MMS: {
                    String hint = context.getString(R.string.common_permission_sms);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case PermissionNames.REQUEST_INSTALL_PACKAGES: {
                    String hint = context.getString(R.string.common_permission_install);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case PermissionNames.NOTIFICATION_SERVICE: {
                    String hint = context.getString(R.string.common_permission_notification);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    String string = context.getString(R.string.permission_notification_text);
                    if (!hintTextContents.contains(string)) {
                        hintTextContents.add(string);
                    }
                    break;
                }
                case PermissionNames.SYSTEM_ALERT_WINDOW: {
                    String hint = context.getString(R.string.common_permission_window);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case PermissionNames.WRITE_SETTINGS: {
                    String hint = context.getString(R.string.common_permission_setting);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        if (!hints.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String text : hints) {
                if (builder.length() == 0) {
                    builder.append(text);
                } else {
                    builder.append("、").append(text);
                }
            }
            builder.append(" ");
            return builder.toString();
        }
        if (!hintTextContents.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String text : hintTextContents) {
                if (builder.length() == 0) {
                    builder.append(text);
                } else {
                    builder.append("、").append(text);
                }
            }
            builder.append(" ");
            hintText= builder.toString();
        }

        return context.getString(R.string.common_permission_fail_2);
    }
    /**
     * 关闭弹窗
     */
    public void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    /**
     * 判断弹窗是否正在显示
     */
    public boolean isDialogShowing() {
        return mDialog != null && mDialog.isShowing();
    }
}
