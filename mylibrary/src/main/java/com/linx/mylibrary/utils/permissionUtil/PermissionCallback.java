package com.linx.mylibrary.utils.permissionUtil;

import com.hjq.permissions.permission.base.IPermission;

import java.util.List;

public interface PermissionCallback {
    void PermissionSucceed(List<IPermission> grantedList); // 权限申请成功

    void PermissionFail(List<IPermission> deniedList);    // 权限申请失败（临时拒绝/设置页返回仍拒绝）
}
