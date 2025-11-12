package com.linx.mylibrary.utils.permissionUtil;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionNames;
import com.hjq.permissions.permission.base.IPermission;
import com.hjq.permissions.tools.PermissionUtils;
import com.linx.mylibrary.R;
import com.linx.mylibrary.utils.RxAppApplicationMgr;

import java.util.ArrayList;
import java.util.List;

public class RxPermissionUtils {


    /**
     * 普通最原本的权限请求
     *
     * @param activity           上下文
     * @param permissions        权限 示例 ArrayList<IPermission> listP = new ArrayList<>(Arrays.asList(PermissionLists.getAccessFineLocationPermission(),PermissionLists.getManageExternalStoragePermission()));
     * @param permissionCallback 回调
     */
    public void requestXXPermission(Activity activity, List<IPermission> permissions, PermissionCallback permissionCallback) {
        XXPermissions.with(activity)
                .permissions(permissions)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onResult(@NonNull List<IPermission> grantedList, @NonNull List<IPermission> deniedList) {
                        boolean allGranted = deniedList.isEmpty();
                        if (!allGranted) {
                            // 判断请求失败的权限是否被用户勾选了不再询问的选项
                            boolean doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(activity, deniedList);
                            // 在这里处理权限请求失败的逻辑
                            permissionCallback.PermissionFail(deniedList);
                            return;
                        }
                        // 在这里处理权限请求成功的逻辑
                        permissionCallback.PermissionSucceed(grantedList);
                    }
                });
    }

    /**
     * 默认普通最原本的权限请求
     *
     * @param activity           上下文
     * @param permissions        权限 示例 IPermission[] permissions = new IPermission[]{PermissionLists.getAccessFineLocationPermission()},
     * @param permissionCallback 回调
     */
    public void requestXXPermission(Activity activity, IPermission[] permissions, PermissionCallback permissionCallback) {
        ArrayList<IPermission> arrayList = PermissionUtils.asArrayList(permissions);
        requestXXPermission(activity, arrayList, permissionCallback);
    }

    /**
     * 单个权限普通
     *
     * @param activity           上下文
     * @param permission         权限 PermissionLists.getAccessFineLocationPermission()
     * @param permissionCallback 回调
     */
    public void requestXXPermission(Activity activity, IPermission permission, PermissionCallback permissionCallback) {
        IPermission[] permissions = new IPermission[]{permission};
        requestXXPermission(activity, permissions, permissionCallback);
    }

    /*同时上面弹窗说明的权限*/
    public void requestXXPermissionShowTopDialog(Activity activity, List<IPermission> permissions,String dialogContent, PermissionCallback permissionCallback) {
        requestXXPermissionShowDialog(activity,permissions,true,"权限使用说明",dialogContent,false,null,permissionCallback);
    }

    /*权限失败去提示弹窗去设置的的权限*/
    public void requestXXPermissionToSetting(Activity activity, List<IPermission> permissions, PermissionCallback permissionCallback) {
        requestXXPermissionShowDialog(activity,permissions,false,null,null,true,null,permissionCallback);
    }

    /**
     * 申请权限要求有提示框和拒绝后去设置界面的提示框
     *
     * @param activity                   上下文
     * @param permissions                权限组 示例 ArrayList<IPermission> listP = new ArrayList<>(Arrays.asList(PermissionLists.getAccessFineLocationPermission(),PermissionLists.getManageExternalStoragePermission()));
     * @param dialogTitle                提示窗标题
     * @param dialogContent              权限提示窗内容
     * @param showPermissionPromptDialog 是否弹出权限使用说明顶部弹窗
     * @param showNoPermissionDialog     是否弹出权限拒绝时候去 设置界面的弹窗
     * @param permissionCallback         权限回调
     */
    public void requestXXPermissionShowDialog(Activity activity, List<IPermission> permissions, boolean showPermissionPromptDialog, String dialogTitle, String dialogContent, boolean showNoPermissionDialog, String noPermissionDialogText, PermissionCallback permissionCallback) {
        if (permissions.isEmpty()) {
            permissionCallback.PermissionFail(null);
            return;
        }

        if (showPermissionPromptDialog) {
            PermissionTipDialogUtils.getInstance().showPermissionTipDialog(activity, dialogTitle, dialogContent, permissions);
        }
        String appName = RxAppApplicationMgr.getAppName(activity);
        XXPermissions.with(activity)
                .permissions(permissions)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onResult(@NonNull List<IPermission> grantedList, @NonNull List<IPermission> deniedList) {
                        PermissionTipDialogUtils.getInstance().dismissDialog();
                        boolean allGranted = deniedList.isEmpty();
                        if (!allGranted) {
                            // 在这里处理权限请求失败的逻辑
                            // 判断请求失败的权限是否被用户勾选了不再询问的选项
                            boolean doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(activity, deniedList);
                            if (showNoPermissionDialog) {
                                String permissionTip;
                                if (TextUtils.isEmpty(noPermissionDialogText)) {
                                    String permissionHint = getHint(activity, deniedList);
                                    permissionTip = activity.getString(R.string.permission_no_content2, permissionHint, appName);
                                } else {
                                    permissionTip = noPermissionDialogText;
                                }
                                PermissionRejectDialog.showCustomDialog(activity, permissionTip, new PermissionRejectDialog.OnCancelClickListener() {
                                    @Override
                                    public void onCancel() {
                                        permissionCallback.PermissionFail(deniedList);
                                    }
                                }, new PermissionRejectDialog.OnConfirmClickListener() {
                                    @Override
                                    public void onConfirm() {
                                        XXPermissions.startPermissionActivity(activity, deniedList, new OnPermissionCallback() {
                                            /*从设置页面返回的*/
                                            @Override
                                            public void onResult(@NonNull List<IPermission> grantedList, @NonNull List<IPermission> deniedList) {
                                                boolean allGranted = deniedList.isEmpty();
                                                if (!allGranted) {
                                                    permissionCallback.PermissionFail(deniedList);
                                                } else {
                                                    permissionCallback.PermissionSucceed(grantedList);
                                                }

                                            }
                                        });
                                    }
                                });
                            } else {
                                permissionCallback.PermissionFail(deniedList);
                            }
                            return;
                        }
                        // 在这里处理权限请求成功的逻辑
                        permissionCallback.PermissionSucceed(grantedList);
                    }
                });
    }

    protected String getHint(Context context, List<IPermission> permissions) {
        List<IPermission> deniedPermissions = XXPermissions.getDeniedPermissions(context, permissions);
        List<String> permissionNames = new ArrayList<>();
        for (IPermission deniedPermission : deniedPermissions) {
            String permissionName = deniedPermission.getPermissionName();
            permissionNames.add(permissionName);
        }
        return getPermissionHint(context, permissionNames, true);
    }

    /**
     * 根据权限获取提示
     */
    protected String getPermissionHint(Context context, List<String> permissions, boolean isOnly) {
        if (permissions == null || permissions.isEmpty()) {
            return context.getString(R.string.common_permission_fail_2);
        }

        List<String> hints = new ArrayList<>();
        for (String permission : permissions) {
            switch (permission) {
                case PermissionNames.READ_EXTERNAL_STORAGE:
                case PermissionNames.WRITE_EXTERNAL_STORAGE:
                case PermissionNames.MANAGE_EXTERNAL_STORAGE: {
                    String hint = context.getString(R.string.common_permission_storage);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case PermissionNames.CAMERA: {
                    String hint = context.getString(R.string.common_permission_camera);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
                    }
                    break;
                }
                case PermissionNames.RECORD_AUDIO: {
                    String hint = context.getString(R.string.common_permission_microphone);
                    if (!hints.contains(hint)) {
                        hints.add(hint);
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
                    builder.append("、")
                            .append(text);
                }
            }
            builder.append(" ");
            if (isOnly) {
                return builder.toString();
            } else {
                return context.getString(R.string.common_permission_fail_3, builder.toString());
            }

        }

        return context.getString(R.string.common_permission_fail_2);
    }
}
