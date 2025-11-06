package com.linewell.lxhdemo.base;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gyf.immersionbar.ImmersionBar;
import com.hjq.toast.Toaster;
import com.lin.networkstateview.NetworkStateView;
import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.app.AppConfig;
import com.linx.mylibrary.utils.klog.KLog;
import com.linx.mylibrary.utils.manager.AppDavikActivityMgr;
import com.linx.mylibrary.view.dialog.CustomAlertDialogBuilder;
import com.linx.mylibrary.view.dialog.ProgressLoadingDialog;
import com.lzy.okgo.OkGo;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity基类：封装通用能力（页面管理、沉浸式、网络状态、弹窗、权限、跳转、软键盘控制等）
 * 支持子类灵活重写核心配置，无冗余逻辑，兼容Android 4.4+（API 19+）主流版本
 *
 */
public abstract class BaseActivity extends AppCompatActivity implements NetworkStateView.OnRefreshListener {
    // 静态常量：替代硬编码，提升可维护性
    private static final long JUMP_INTERVAL = 500; // 防重复跳转间隔（毫秒）
    private static final int SOFT_INPUT_MODE = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN; // 软键盘防冲突模式
    private static final FrameLayout.LayoutParams DEFAULT_FRAME_PARAMS = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    private static final int REQUEST_CODE_SETTINGS = 10086; // 设置页返回的请求码
    private static int sNextRequestCode = 1000; // 跳转回调自增请求码（避免重复）

    // 成员变量：按功能归类，减少内存泄漏风险
    // 视图控件
    private NetworkStateView networkStateView;
    private FrameLayout flContent;
    private FrameLayout flBar;
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener; // 独立布局监听
    // 弹窗与管理
    private ProgressLoadingDialog progressDialog;
    private AppDavikActivityMgr activityMgr;
    // 跳转与回调
    private String jumpTag;
    private long jumpTime;
    private ActivityCallback activityCallback;
    private int activityRequestCode;
    private PermissionCallback permissionCallback;
    // 权限相关
    private List<String> mPermissionsToReCheck; // 需要重新检查的权限
    // 沉浸式状态栏
    private ImmersionBar mImmersionBar;

    public Context getContext() {
        return this;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(getScreenOrientation()); // 统一屏幕方向（子类可重写）
        initActivity(savedInstanceState);
    }

    /**
     * 核心初始化流程：按依赖顺序执行，无多余步骤
     */
    protected void initActivity(Bundle savedInstanceState) {
        initFullScreen(); //是否全屏//开机页使用
        initActivityManager();   // 1. 页面栈管理（先初始化，确保addActivity有效）
        initEventBus();          // 2. EventBus注册（无依赖）
        initLayout();            // 3. 加载布局（后续视图操作的基础）
        initImmersionBar();      // 4. 沉浸式状态栏（依赖布局加载完成）
        initBarVisibility();     // 5. 导航栏显示控制（依赖flBar初始化）
        initProgressDialog();    // 6. 加载弹窗（无依赖）
        initBundleData();        // 7. 接收Intent参数（无依赖）
        initView(savedInstanceState); // 8. 子类View初始化（依赖布局）
        initData(savedInstanceState); // 9. 子类数据初始化（依赖View）
    }

    protected boolean isFullScreen() {
        return false;
    }

    protected void initFullScreen() {
        if (isFullScreen()) { //是否需要设置全屏//欢迎界面或开始界面时候需要
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }


    // ====================== 初始化相关：修复依赖顺序与Bug ======================

    /**
     * 初始化页面栈管理：确保Activity能正确加入/移除栈
     */
    private void initActivityManager() {
        activityMgr = AppDavikActivityMgr.getScreenManager();
        if (activityMgr != null) {
            activityMgr.addActivity(this);
        }
    }

    /**
     * 初始化EventBus：子类通过isNeedEventBus()控制是否注册，自动注销
     * 优化：isNeedEventBus()改为默认实现，子类无需强制重写
     */
    private void initEventBus() {
        if (isNeedEventBus()) {
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this);
                KLog.d("EventBus registered success");
            }
        }
    }

    /**
     * 加载布局：先加载基类布局activity_base，再嵌入子类布局
     * 优化：统一设置NetworkStateView刷新监听，避免重复调用
     */
    private void initLayout() {
        // 加载基类布局（包含networkStateView、flContent、flBar）
        View baseView = LayoutInflater.from(this).inflate(R.layout.activity_base, null);
        super.setContentView(baseView);

        // 初始化核心控件
        flContent = findViewById(R.id.fl_content);
        flBar = findViewById(R.id.fl_bar);
        networkStateView = findViewById(R.id.nsv_state_view);

        // 优化：统一设置刷新监听，无需在各状态方法中重复设置
        if (networkStateView != null) {
            networkStateView.setOnRefreshListener(this);
        }

        // 嵌入子类布局
        initChildLayout(getLayoutId());
    }

    /**
     * 嵌入子类布局：确保布局参数匹配，避免显示异常
     * 优化：inflate指定父容器，保留子类布局根节点参数
     */
    private void initChildLayout(@LayoutRes int childLayoutId) {
        if (flContent == null) return;

        // 优化：inflate时指定父容器flContent，第三个参数为false（避免自动addView）
        View childView = LayoutInflater.from(this).inflate(childLayoutId, flContent, false);
        flContent.addView(childView, 0, DEFAULT_FRAME_PARAMS);
    }

    /**
     * 初始化沉浸式状态栏：修复强转OnGlobalLayoutListener的Bug，适配低版本字体颜色
     * 优化1：低版本（API<23）不设置字体颜色；优化2：onDestroy时需调用destroy()
     */
    private void initImmersionBar() {
        if (!isStatusBarEnabled()) return;

        // 独立创建布局监听，避免BaseActivity强转（原代码ClassCastException根源）
        layoutListener = () -> {
        };

        // 配置沉浸式参数
        mImmersionBar = ImmersionBar.with(this)
                // 优化：仅Android 6.0+（API23）支持修改状态栏字体颜色
                .statusBarDarkFont(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && statusBarDarkFont())
                .keyboardEnable(false, SOFT_INPUT_MODE);
        mImmersionBar.init();

        // 绑定布局监听（确保软键盘功能正常）
        getWindow().getDecorView().getViewTreeObserver()
                .addOnGlobalLayoutListener(layoutListener);
    }

    /**
     * 获取状态栏沉浸的配置对象
     */
    public ImmersionBar getImmersionBar() {
        return mImmersionBar;
    }

    /**
     * 控制顶部导航栏显示：确保flBar已初始化后再设置
     */
    private void initBarVisibility() {
        if (flBar != null) {
            flBar.setVisibility(showBar() ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 初始化加载弹窗：避免重复创建
     */
    private void initProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressLoadingDialog(this, R.style.dialog_transparent_style);
        }
    }

    /**
     * 接收Intent参数：简化原initBundle方法，减少冗余
     */
    private void initBundleData() {
        Bundle extras = getIntent().getBundleExtra(AppConfig.bundle);
        if (extras != null) {
            getBundleExtras(extras);
        }
    }
    /**
     * 处理 Activity 复用场景的新 Intent（如 singleTop 模式）
     * 1. 调用 setIntent 更新 Intent 引用，确保后续 getIntent() 拿到最新数据
     * 2. 重新解析 Intent 数据，确保页面数据同步更新
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 关键：将新 Intent 设置为当前 Activity 的 Intent，覆盖原始 Intent
        setIntent(intent);
        // 重新解析新 Intent 中的 Bundle 数据（复用现有 initBundleData 逻辑，无冗余）
        initBundleData();
        // 预留子类扩展：允许子类在接收新 Intent 后做额外操作（如刷新 UI）
        onNewIntentReceived(intent);
    }

    /**
     * 子类可选重写：接收新 Intent 后的额外处理（如刷新视图、重新请求接口）
     * 无需重写时可忽略，避免子类强制实现
     */
    protected void onNewIntentReceived(Intent intent) {
        // 空实现，子类按需重写
    }

    // ====================== 抽象方法：子类必须实现核心逻辑，非核心提供默认实现 ======================

    /**
     * 获取子类布局ID（核心抽象，无替代方案）
     */
    protected abstract int getLayoutId();

    /**
     * 接收Intent传递的Bundle参数
     */
    protected abstract void getBundleExtras(Bundle extras);

    /**
     * 初始化View（findViewById、设置点击监听等）
     */
    protected abstract void initView(Bundle savedInstanceState);

    /**
     * 初始化数据（网络请求、数据绑定等）
     */
    protected abstract void initData(Bundle savedInstanceState);

    /**
     * 是否需要注册EventBus：true=注册，false=不注册
     * 优化：改为非抽象方法，默认不注册，减少子类冗余
     */
    protected boolean isNeedEventBus() {
        return false;
    }

    /**
     * 是否显示顶部导航栏：true=显示，false=隐藏（默认隐藏）
     */
    protected boolean showBar() {
        return false;
    }

    // ====================== 页面跳转：修复重复关闭Bug、请求码重复问题 ======================

    /**
     * 无参跳转
     */
    protected void readyGo(@NonNull Class<?> targetCls) {
        readyGo(targetCls, null);
    }

    /**
     * 带参跳转
     */
    protected void readyGo(@NonNull Class<?> targetCls, @Nullable Bundle bundle) {
        Intent intent = buildIntent(targetCls, bundle);
        if (checkJumpValid(intent)) {
            startActivity(intent);
        }
    }

    /**
     * 带参跳转并关闭当前页
     * 优化：仅跳转成功后执行finish()，避免误关页面
     */
    protected void readyGoThenFinish(@NonNull Class<?> targetCls, @Nullable Bundle bundle) {
        Intent intent = buildIntent(targetCls, bundle);
        if (checkJumpValid(intent)) { // 优化：跳转验证通过才关闭
            startActivity(intent);
            finish();
        }
    }

    /**
     * 无参跳转并关闭当前页
     */
    protected void readyGoThenFinish(@NonNull Class<?> targetCls) {
        readyGoThenFinish(targetCls, null);
    }

    /**
     * 带回调的跳转（startActivityForResult）
     */
    protected void readyGoForResult(@NonNull Class<?> targetCls, int requestCode) {
        readyGoForResult(targetCls, requestCode, null);
    }

    /**
     * 带参+回调的跳转
     */
    protected void readyGoForResult(@NonNull Class<?> targetCls, int requestCode, @Nullable Bundle bundle) {
        Intent intent = buildIntent(targetCls, bundle);
        if (checkJumpValid(intent)) {
            startActivityForResult(intent, requestCode);
        }
    }

    /**
     * 优化版带回调跳转：支持接口回调（替代传统requestCode判断）
     * 优化：用静态自增请求码替代随机数，避免重复
     */
    public void startActivityWithCallback(@NonNull Intent intent, @Nullable ActivityCallback callback) {
        startActivityWithCallback(intent, null, callback);
    }

    public void startActivityWithCallback(@NonNull Intent intent, @Nullable Bundle options, @Nullable ActivityCallback callback) {
        if (activityCallback != null || !checkJumpValid(intent)) {
            return; // 避免重复回调或无效跳转
        }
        activityCallback = callback;
        // 优化：静态自增请求码，确保唯一（替代new Random()）
        activityRequestCode = sNextRequestCode++;
        startActivityForResult(intent, activityRequestCode, options);
    }

    /**
     * 构建Intent：统一参数传递逻辑，减少重复代码
     */
    @NonNull
    private Intent buildIntent(@NonNull Class<?> targetCls, @Nullable Bundle bundle) {
        Intent intent = new Intent(this, targetCls);
        if (bundle != null) {
            intent.putExtra(AppConfig.bundle, bundle);
        }
        return intent;
    }

    /**
     * 检查跳转有效性：防重复跳转，覆盖显式/隐式跳转场景
     * 优化：隐式跳转用toUri生成唯一tag，避免null导致拦截失效
     */
    private boolean checkJumpValid(@Nullable Intent intent) {
        if (intent == null) return false;

        // 优化：隐式跳转用toUri生成唯一标识（避免action为null时拦截失效）
        String tag = intent.getComponent() != null
                ? intent.getComponent().getClassName()
                : (intent.getAction() != null ? intent.getAction() : intent.toUri(0));
        if (tag == null) return true;

        // 500ms内同一页面不重复跳转
        if (tag.equals(jumpTag) && jumpTime >= SystemClock.uptimeMillis() - JUMP_INTERVAL) {
            return false;
        }

        // 记录跳转信息
        jumpTag = tag;
        jumpTime = SystemClock.uptimeMillis();
        return true;
    }

    /**
     * 回调结果分发：确保回调后清空引用，避免内存泄漏
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SETTINGS && mPermissionsToReCheck != null) {
            // 用户从设置页返回后，自动重新检查权限
            reCheckPermissionsAfterSettings();
        } else if (activityCallback != null && activityRequestCode == requestCode) {
            activityCallback.onActivityResult(resultCode, data);
            activityCallback = null; // 清空引用，避免内存泄漏
        }
    }

    /**
     * 从设置页返回后，重新检查权限
     */
    private void reCheckPermissionsAfterSettings() {
        if (mPermissionsToReCheck == null || mPermissionsToReCheck.isEmpty()) return;

        // 将List转换为数组（Permission库要求）
        String[] permissions = mPermissionsToReCheck.toArray(new String[0]);
        // 重新检查权限状态
        AndPermission.with(this).runtime()
                .permission(permissions)
                .onGranted(granted -> {
                    // 权限已在设置中开启
                    if (permissionCallback != null) {
                        permissionCallback.PermissionSucceed();
                    }
                })
                .onDenied(denied -> {
                    // 权限仍未开启
                    if (permissionCallback != null) {
                        permissionCallback.PermissionFail();
                    }
                })
                .start();

        // 清空临时存储的权限列表
        mPermissionsToReCheck.clear();
        mPermissionsToReCheck = null;
    }


    // ====================== 权限申请：修复回调覆盖、优化跳转设置页逻辑 ======================

    /**
     * 申请权限：覆盖权限申请全流程
     * 优化：添加"正在申请中"判断，避免覆盖未完成的回调
     *
     * @param permissions 需申请的权限（如Permission.CAMERA、Permission.STORAGE）
     * @param callback    权限结果回调
     */
    protected void requestPermissions(@NonNull String[] permissions, @NonNull PermissionCallback callback) {
        // 优化：判断是否有未完成的权限申请，避免覆盖回调
        if (this.permissionCallback != null) {
            KLog.w("Permission request is already in progress, please wait");
            showToast("正在申请权限，请稍后");
            return;
        }
        this.permissionCallback = callback;

        // 调用Yanzhenjie Permission库核心API
        AndPermission.with(this).runtime()
                .permission(permissions)
                .onGranted(granted -> {
                    // 权限授予成功
                    if (permissionCallback != null) {
                        permissionCallback.PermissionSucceed();
                    }
                    permissionCallback = null; // 清空引用
                })
                .onDenied(denied -> {
                    // 权限拒绝：区分"永久拒绝"和"临时拒绝"
                    if (AndPermission.hasAlwaysDeniedPermission(this, denied)) {
                        showPermissionSettingDialog(denied); // 永久拒绝→引导去设置
                    } else {
                        // 临时拒绝→回调失败
                        if (permissionCallback != null) {
                            permissionCallback.PermissionFail();
                        }
                        permissionCallback = null; // 清空引用
                    }
                })
                .start();
    }

    /**
     * 显示权限设置引导弹窗：修复空指针风险
     */
    private void showPermissionSettingDialog(@NonNull List<String> deniedPermissions) {
        if (deniedPermissions.isEmpty()) return;

        // 转换权限名称为用户可读文本
        List<String> permissionNames = Permission.transformText(this, deniedPermissions);
        String message = getString(R.string.message_permission_always_failed,
                TextUtils.join("\n", permissionNames));

        new CustomAlertDialogBuilder(this)
                .setTitle(R.string.title_dialog)
                .setMessage(message)
                .setPositiveButton(R.string.setting, (dialog, which) -> {
                    // 引导用户去系统设置开启权限
                    if (jumpToAppSettings()) {
                        // 跳转成功，记录需要重新检查的权限
                        mPermissionsToReCheck = new ArrayList<>(deniedPermissions);
                    } else {
                        // 跳转失败时提示用户手动操作
                        showToast("无法自动跳转至设置页，请手动前往应用设置开启权限");
                        if (permissionCallback != null) {
                            permissionCallback.PermissionFail();
                            permissionCallback = null; // 清空引用
                        }
                    }
                })
                .setNegativeButton(R.string.versionchecklib_cancel, (dialog, which) -> {
                    if (permissionCallback != null) {
                        permissionCallback.PermissionFail();
                        permissionCallback = null; // 清空引用
                    }
                })
                .setCancelable(false)
                .show();
    }

    /**
     * 兼容所有设备的应用设置页跳转方法
     *
     * @return true：跳转成功；false：跳转失败
     */
    private boolean jumpToAppSettings() {
        try {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS); // 用startActivityForResult接收返回
            return true;
        } catch (Exception e) {
            // 捕获所有异常（如设备不支持该Action）
            KLog.e("跳转设置页失败：" + e.getMessage());
            return false;
        }
    }


    // ====================== 弹窗与网络状态：修复WindowLeaked、优化显示逻辑 ======================

    /**
     * 显示加载中视图（网络请求时）
     */
    public void showLoadingView() {
        if (networkStateView != null) {
            networkStateView.showLoading();
        }
    }

    /**
     * 显示正常内容视图（加载成功后）
     */
    public void showContentView() {
        if (networkStateView != null) {
            networkStateView.showSuccess();
        }
    }

    /**
     * 显示无网络视图（断网时）
     */
    public void showNoNetworkView() {
        if (networkStateView != null) {
            networkStateView.showNoNetwork();
        }
    }

    /**
     * 显示空数据视图（请求成功但无数据）
     */
    public void showEmptyView() {
        if (networkStateView != null) {
            networkStateView.showEmpty();
        }
    }

    /**
     * 显示错误视图（网络错误/接口错误）
     */
    public void showErrorView() {
        if (networkStateView != null) {
            networkStateView.showError();
        }
    }

    /**
     * 显示加载弹窗（无文字）
     */
    public void showProgressDialog() {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.showProgressDialog();
        }
    }

    /**
     * 显示带文字的加载弹窗
     */
    public void showProgressDialogWithText(@NonNull String text) {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.showProgressDialogWithText(text);
        }
    }

    /**
     * 显示加载成功弹窗（自定义显示时间）
     */
    public void showProgressSuccess(@NonNull String message, long showTime) {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.showProgressSuccess(message, showTime);
        }
    }

    /**
     * 显示加载成功弹窗（默认1秒）
     */
    public void showProgressSuccess(@NonNull String message) {
        showProgressSuccess(message, 1000);
    }

    /**
     * 显示加载失败弹窗（自定义显示时间）
     */
    public void showProgressFail(@NonNull String message, long showTime) {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.showProgressFail(message, showTime);
        }
    }

    /**
     * 显示加载失败弹窗（默认1秒）
     */
    public void showProgressFail(@NonNull String message) {
        showProgressFail(message, 1000);
    }

    /**
     * 关闭加载弹窗：避免重复dismiss导致异常
     */
    public void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismissProgressDialog();
        }
    }

    /**
     * 弹窗隐藏状态栏/导航栏：修复魔法数字，用显式常量替代
     */
    public void hideBarWhenDialogShow() {
        Dialog dialog = getProgressDialog();
        if (dialog == null) return;

        View decorView = dialog.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            // API 19+ 加入沉浸式粘性导航（避免触摸显示导航栏）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY; // 显式常量，替代魔法数字
            } else {
                uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            }
            decorView.setSystemUiVisibility(uiOptions);
        });
    }

    /**
     * 获取加载弹窗实例：给子类自定义修改（如修改弹窗位置）
     */
    @Nullable
    public Dialog getProgressDialog() {
        return progressDialog != null ? progressDialog.getProgressDialog() : null;
    }


    // ====================== 生命周期与资源释放：修复内存泄漏，覆盖所有场景 ======================
    @Override
    protected void onPause() {
        super.onPause();
        // 优化：页面后台时关闭弹窗，避免WindowLeaked异常
        dismissProgressDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 1. 释放回调引用（避免内存泄漏）
        permissionCallback = null;
        activityCallback = null;
        // 2. 取消网络请求（OkGo库标准用法，避免回调内存泄漏）
        OkGo.getInstance().cancelTag(this);
        // 3. 移除布局监听（兼容API 16+，避免内存泄漏）
        if (layoutListener != null) {
            ViewTreeObserver observer = getWindow().getDecorView().getViewTreeObserver();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                observer.removeOnGlobalLayoutListener(layoutListener);
            } else {
                observer.removeGlobalOnLayoutListener(layoutListener);
            }
            layoutListener = null;
        }
        // 4. 注销EventBus（避免重复注册）
        if (isNeedEventBus() && EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
            KLog.d("EventBus unregistered success");
        }
        // 5. 移除Activity栈（避免内存泄漏）
        if (activityMgr != null) {
            activityMgr.removeActivity(this);
        }
        // 6. 销毁弹窗（释放Window资源）
        dismissProgressDialog();
        progressDialog = null;
        // 7. 优化：销毁ImmersionBar，释放引用（避免内存泄漏）
        if (mImmersionBar != null) {
            mImmersionBar = null;
        }
    }

    /**
     * 关闭页面：隐藏软键盘（避免软键盘内存泄漏）
     */
    @Override
    public void finish() {
        hideSoftInput();
        super.finish();
    }

    /**
     * 隐藏软键盘：覆盖所有需要隐藏软键盘的场景（如关闭页面、切换Fragment）
     */
    public void hideSoftInput() {
        View focusView = getCurrentFocus();
        if (focusView == null) return;

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
        }
    }


    // ====================== 通用工具方法：精简冗余，覆盖日常开发需求 ======================

    /**
     * 显示吐司：支持String/资源ID/任意对象（覆盖所有提示场景）
     */
    public void showToast(@NonNull String text) {
        Toaster.show(text);
    }

    public void showToast(int resId) {
        Toaster.show(resId);
    }

    public void showToast(@NonNull Object obj) {
        Toaster.show(obj.toString());
    }

    /**
     * 退出应用：关闭所有Activity（覆盖应用退出场景）
     */
    protected void exitApp() {
        if (activityMgr != null) {
            activityMgr.removeAllActivity();
        }
    }

    /**
     * 网络状态视图刷新回调：子类重写实现重新请求数据（如断网后点击刷新）
     */
    @Override
    public void onRefresh() {
        onNetworkRefresh();
    }

    /**
     * 子类重写：网络刷新逻辑（如重新调用接口）
     */
    protected void onNetworkRefresh() {
    }

    /**
     * 是否启用沉浸式状态栏：子类可重写为false（如登录页不需要沉浸式）
     */
    protected boolean isStatusBarEnabled() {
        return true;
    }

    /**
     * 状态栏字体颜色：true=黑色，false=白色（子类可重写，适配浅色状态栏）
     */
    protected boolean statusBarDarkFont() {
        return true;
    }

    /**
     * 屏幕方向：默认竖屏，子类可重写为横屏/自动旋转
     * 可选值：ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE（横屏）、SCREEN_ORIENTATION_SENSOR（自动旋转）等
     */
    protected int getScreenOrientation() {
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }


    // ====================== 回调接口：删除冗余方法，覆盖核心场景 ======================

    /**
     * 权限申请回调：覆盖"成功/失败"核心场景
     * 优化：删除冗余的PermissionReRequest()方法（原代码未调用）
     */
    public interface PermissionCallback {
        void PermissionSucceed(); // 权限申请成功

        void PermissionFail();    // 权限申请失败（临时拒绝/设置页返回仍拒绝）
    }

    /**
     * 跳转回调：替代传统requestCode判断，简化子类逻辑
     */
    public interface ActivityCallback {
        void onActivityResult(int resultCode, @Nullable Intent data);
    }
}