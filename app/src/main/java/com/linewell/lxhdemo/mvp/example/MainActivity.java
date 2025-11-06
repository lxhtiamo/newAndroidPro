package com.linewell.lxhdemo.mvp.example;

import android.Manifest;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.hjq.permissions.permission.base.IPermission;
import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.mvp.MvpActivity;
import com.linewell.lxhdemo.thirdAppUtil.UMShareManager;
import com.linx.mylibrary.utils.RxGlideTool;
import com.linx.mylibrary.utils.permissionUtil.PermissionTipDialogUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends MvpActivity<MainContract.IMainPresenter> implements MainContract.IMainView {

    private UMShareManager mShareManager;

    @Override
    protected void getBundleExtras(Bundle extras) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        // 配置各平台密钥（替换为你的申请值）
        Button button = findViewById(R.id.bt_bt);
        ImageView iv_load_image = findViewById(R.id.iv_load_image);
        ImageView iv_load_image2 = findViewById(R.id.iv_load_image2);
        ImageView iv_load_image3 = findViewById(R.id.iv_load_image3);
        ImageView iv_load_image4 = findViewById(R.id.iv_load_image4);
        ImageView iv_load_image5 = findViewById(R.id.iv_load_image5);
        int borderColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
        RxGlideTool.getInstance().loadCircleRingImage(getContext(),R.mipmap.ic_launcher,iv_load_image4,10,borderColor);
       // RxGlideTool.getInstance().loadCircleRingImage1(getContext(),R.mipmap.ic_launcher,iv_load_image5,10,borderColor);
       // RxGlideTool.getInstance().loadCircleImage3(getContext(),R.mipmap.ic_launcher,iv_load_image3);
        //RxGlideTool.getInstance().loadCircleImage5(getContext(),R.mipmap.ic_launcher,iv_load_image4);
       // RxGlideTool.getInstance().loadCircleImage6(getContext(),R.mipmap.ic_launcher,iv_load_image4,10,borderColor);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XXPermissions.with(MainActivity.this)
                        // 申请多个权限
                        .permission(PermissionLists.getCameraPermission())
                        // 设置不触发错误检测机制（局部设置）
                        //.unchecked()
                        .request(new OnPermissionCallback() {

                            @Override
                            public void onResult(@NonNull List<IPermission> grantedList, @NonNull List<IPermission> deniedList) {
                                boolean allGranted = deniedList.isEmpty();
                                if (!allGranted) {
                                    // 判断请求失败的权限是否被用户勾选了不再询问的选项
                                    boolean doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(MainActivity.this, deniedList);
                                    XXPermissions.startPermissionActivity(MainActivity.this, deniedList, new OnPermissionCallback() {
                                        @Override
                                        public void onResult(@NonNull List<IPermission> grantedList, @NonNull List<IPermission> deniedList) {
                                            Log.d("IPermission", "onResult: ");
                                        }
                                    });
                                    // 在这里处理权限请求失败的逻辑
                                    return;
                                }
                                // 在这里处理权限请求成功的逻辑
                            }
                        });
            }
        });
        Button button1 = findViewById(R.id.bt_bt1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                requestPermissions();
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, new PermissionCallback() {
                    @Override
                    public void PermissionSucceed() {
                        showToast("成功");
                    }

                    @Override
                    public void PermissionFail() {

                    }
                });
            }
        });
        Button button2 = findViewById(R.id.bt_bt2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<IPermission> objects = new ArrayList<>();
                objects.add(PermissionLists.getReadPhoneStatePermission());
                objects.add(PermissionLists.getManageExternalStoragePermission());
                objects.add(PermissionLists.getCameraPermission());
                PermissionTipDialogUtils.getInstance().showPermissionTipDialog(getContext(),"温馨提示","权限的使用说明",objects);
                requestPermissions();

            }
        });

    }

    private void requestPermissions() {


        XXPermissions.with(this)
                .permission(PermissionLists.getReadPhoneStatePermission())
                .permission(PermissionLists.getManageExternalStoragePermission())
                .permission(PermissionLists.getCameraPermission()).request(new OnPermissionCallback() {

                    @Override
                    public void onResult(@NonNull List<IPermission> grantedList, @NonNull List<IPermission> deniedList) {
                        boolean allGranted = deniedList.isEmpty();
                        if (!allGranted) {
                            // 判断请求失败的权限是否被用户勾选了不再询问的选项
                            boolean doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(MainActivity.this, deniedList);
                            // 在这里处理权限请求失败的逻辑
                           // XXPermissions.startPermissionActivity(getContext());
                            return;
                        }
                    }
                });
    }

    private void showPermissionTipDialog() {
        // 1. 创建 Dialog 并设置样式
        Dialog dialog = new Dialog(this, R.style.PermissionTipDialogStyle);
        // 2. 加载布局
        View tipView = LayoutInflater.from(this).inflate(R.layout.permission_tip_layout, null);
        dialog.setContentView(tipView);

        // 3. 设置 Dialog 显示位置（顶部）
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.TOP; // 顶部显示
            params.width = WindowManager.LayoutParams.MATCH_PARENT; // 宽度全屏
            params.height = WindowManager.LayoutParams.WRAP_CONTENT; // 高度自适应
            // 关键：添加允许窗口延伸到状态栏的标志
            params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            window.setAttributes(params);
        }

        // 4. 点击"知道了"按钮，关闭 Dialog 并请求权限
        tipView.setOnClickListener(v -> {
            dialog.dismiss();
            // 检查权限是否已授予，未授予则申请
        });

        // 显示 Dialog
        dialog.show();
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        getPresenter().requestTestContent("111");
    }

    @Override
    protected boolean isNeedEventBus() {
        return super.isNeedEventBus();
    }

    @Override
    protected boolean showBar() {
        return super.showBar();
    }

    @Override
    protected MainContract.IMainPresenter createPresenter() {
        return new MainPresenterImpl();
    }

    @Override
    public void onSuccess() {
    }

    @Override
    public void onFailure() {
    }

    @Override
    public void showLoading() {

    }

    @Override
    public void loadingComplete() {

    }

    @Override
    public void showEmpty() {

    }

    @Override
    public void showError() {

    }
}
