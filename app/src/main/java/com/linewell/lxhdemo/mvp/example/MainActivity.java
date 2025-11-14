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
import com.linx.mylibrary.scaner.ActivityScanerCode;
import com.linx.mylibrary.scaner.ActivityScanerCode_Two;
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
        Button bt_bt3 = findViewById(R.id.bt_bt3);
        Button bt_sm1 = findViewById(R.id.bt_sm1);
        Button bt_sm2 = findViewById(R.id.bt_sm2);
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
                requestPermissions(objects, new PermissionCallback() {
                    @Override
                    public void PermissionSucceed() {
                        showToast("成功");
                    }

                    @Override
                    public void PermissionFail() {
                        showToast("失败");
                    }
                });

            }
        });
        bt_sm1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readyGo(ActivityScanerCode.class);

            }
        });
        bt_sm2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readyGo(ActivityScanerCode_Two.class);

            }
        });

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
