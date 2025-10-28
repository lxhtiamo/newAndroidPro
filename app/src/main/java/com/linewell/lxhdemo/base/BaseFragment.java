package com.linewell.lxhdemo.base;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import com.gyf.immersionbar.ImmersionBar;
import com.hjq.toast.Toaster;
import com.lin.networkstateview.NetworkStateView;
import com.linewell.lxhdemo.R;
import com.linx.mylibrary.utils.klog.KLog;
import com.linx.mylibrary.view.dialog.ProgressLoadingDialog;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Field;

/**
 * 基础Fragment：整合通用能力，适配主流开发场景
 * 核心能力：沉浸式状态栏、懒加载、网络状态View、加载对话框、Fragment通信、EventBus
 */
public abstract class BaseFragment extends Fragment implements NetworkStateView.OnRefreshListener {
    protected View rootView;
    protected FragmentActivity mActivity;
    private ImmersionBar mImmersionBar; // 状态栏沉浸实例
    private NetworkStateView networkStateView;
    private FrameLayout flContent; // 子类布局容器（规范命名）
    private FrameLayout flBar; // 顶部导航栏容器
    private ProgressLoadingDialog progressDialog;

    // 懒加载核心状态
    private boolean isLazyLoaded = false; // 是否已完成懒加载
    private boolean isViewCreated = false; // 视图是否已创建
    private boolean isCurrentVisible = false; // 当前是否对用户可见


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (FragmentActivity) context;
    }

    /**
     * 获取依附的Activity（避免getActivity()为空）
     */
    public FragmentActivity getFragmentActivity() {
        return mActivity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载基础布局（包含导航栏、网络状态View、子类布局容器）
        rootView = inflater.inflate(R.layout.fragment_base, container, false);
        // 避免重复添加视图（修复视图复用导致的异常）
        ViewGroup parent = (ViewGroup) rootView.getParent();
        if (parent != null) {
            parent.removeView(rootView);
        }
        initDialog(); // 初始化加载对话框
        initBaseView(); // 初始化基础控件（网络状态View、容器等）
        addChildLayout(inflater); // 添加子类布局
        initBarVisibility(); // 控制顶部导航栏显示
        return rootView;
    }

    /**
     * 初始化基础控件（网络状态View、布局容器）
     */
    private void initBaseView() {
        networkStateView = rootView.findViewById(R.id.nsv_state_view);
        flContent = rootView.findViewById(R.id.fl_content);
        flBar = rootView.findViewById(R.id.fl_bar);
        // 按需显示网络状态View（默认显示）
        if (isNeedNetworkStateView()) {
            networkStateView.setVisibility(View.VISIBLE);
        } else {
            networkStateView.setVisibility(View.GONE);
        }
    }

    /**
     * 初始化加载对话框（避免重复创建）
     */
    private void initDialog() {
        if (progressDialog == null && mActivity != null) {
            progressDialog = new ProgressLoadingDialog(mActivity, R.style.dialog_transparent_style);
        }
    }

    /**
     * 添加子类布局到容器（修复原布局参数错误）
     */
    private void addChildLayout(LayoutInflater inflater) {
        int childLayoutId = getLayoutId();
        if (childLayoutId <= 0) return;

        View childView = inflater.inflate(childLayoutId, null);
        // 用FrameLayout.LayoutParams适配容器（原代码用RelativeLayout参数导致布局异常）
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        flContent.addView(childView, params);
    }

    /**
     * 控制顶部导航栏显示（确保控件初始化后操作）
     */
    private void initBarVisibility() {
        if (flBar != null) {
            flBar.setVisibility(ShowBar() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isViewCreated = true; // 标记视图已创建
        initEventBus(); // 初始化EventBus（按需注册）
        initImmersion(); // 初始化沉浸式状态栏
        initFragmentResultListener(); // 初始化Fragment通信（按需重写）
        checkLazyLoad(); // 检查懒加载条件
    }

    /**
     * 初始化EventBus（按需注册，避免冗余）
     */
    private void initEventBus() {
        if (isNeedEventBus() && !EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
            KLog.d("BaseFragment", "EventBus注册成功：" + getClass().getSimpleName());
        }
    }

    /**
     * 初始化沉浸式状态栏（子类通过重写开关控制）
     */
    private void initImmersion() {
        if (isStatusBarEnabled() && mActivity != null) {
            mImmersionBar = ImmersionBar.with(this)
                    .statusBarDarkFont(statusBarDarkFont()) // 状态栏字体颜色（默认黑色）
                    .keyboardEnable(true) // 解决软键盘与布局冲突
                    .fitsSystemWindows(true); // 避免布局侵入状态栏（按需重写关闭）
            mImmersionBar.init();
        }
    }

    /**
     * 初始化Fragment通信（现代推荐方式，替代接口回调）
     * 子类需重写此方法，通过setFragmentResultListener接收数据
     */
    protected void initFragmentResultListener() {
        // 示例：子类可重写接收数据
        // getParentFragmentManager().setFragmentResultListener("key", this, (requestKey, result) -> {});
    }

    /**
     * 懒加载核心判断：视图创建+可见+未加载
     */
    private void checkLazyLoad() {
        if (isViewCreated && isCurrentVisible && !isLazyLoaded) {
            initLazyLoad(); // 执行懒加载（初始化视图和数据）
            isLazyLoaded = true; // 标记为已加载（避免重复执行）
        }
    }

    /**
     * 懒加载执行内容（首次可见时触发）
     */
    protected void initLazyLoad() {
        initView(); // 初始化控件（findViewById、监听等）
        initData(); // 初始化数据（网络请求、数据绑定等）
    }


    // ====================== 生命周期与可见性处理 ======================
    @Override
    public void onResume() {
        super.onResume();
        // 适配ViewPager2：RESUMED状态且未隐藏，视为可见
        if (!isHidden() && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            isCurrentVisible = true;
            checkLazyLoad();
            onVisible(); // 可见回调
            // 切换可见时刷新沉浸式配置（避免状态异常）
            if (isStatusBarEnabled() && mImmersionBar != null) {
                mImmersionBar.init();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isCurrentVisible) {
            isCurrentVisible = false;
            onInvisible(); // 不可见回调
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            isCurrentVisible = false;
            onInvisible();
        } else {
            isCurrentVisible = true;
            checkLazyLoad();
            onVisible();
            refreshImmersion(); // 显示时刷新沉浸式
        }
    }

    /**
     * 兼容旧版ViewPager（ViewPager2建议用Lifecycle判断，此方法已过时）
     */
    @Override
    @Deprecated
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isViewCreated) {
            isCurrentVisible = isVisibleToUser;
            if (isVisibleToUser) {
                checkLazyLoad();
                onVisible();
                refreshImmersion();
            } else {
                onInvisible();
            }
        }
    }

    /**
     * 刷新沉浸式状态栏配置（避免切换可见时状态异常）
     */
    private void refreshImmersion() {
        if (isStatusBarEnabled() && mImmersionBar != null) {
            mImmersionBar.init();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 重置懒加载状态（视图销毁后，下次创建需重新加载）
        isViewCreated = false;
        isLazyLoaded = false;
        isCurrentVisible = false;
        // 销毁加载对话框（避免窗口泄漏）
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismissProgressDialog();
            progressDialog = null;
        }
        // 释放沉浸式资源（避免内存泄漏）
        if (mImmersionBar != null) {
            mImmersionBar = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 注销EventBus（避免内存泄漏）
        if (isNeedEventBus() && EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
            KLog.d("BaseFragment", "EventBus注销：" + getClass().getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // 修复旧版Fragment内存泄漏（mChildFragmentManager引用问题）
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            try {
                Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
                childFragmentManager.setAccessible(true);
                childFragmentManager.set(this, null);
            } catch (Exception e) {
                KLog.e("BaseFragment", "onDetach反射异常：" + e.getMessage());
            }
        }
        mActivity = null; // 释放Activity引用（避免内存泄漏）
    }


    // ====================== 抽象方法（子类必须实现） ======================
    /**
     * 获取子类布局ID
     */
    protected abstract int getLayoutId();

    /**
     * 初始化控件（findViewById、设置监听等）
     */
    protected abstract void initView();

    /**
     * 初始化数据（网络请求、数据绑定等）
     */
    protected abstract void initData();


    // ====================== 可配置开关（子类按需重写） ======================
    /**
     * 是否显示顶部导航栏（默认隐藏）
     */
    protected boolean ShowBar() {
        return false;
    }

    /**
     * 是否需要EventBus（默认不需要，子类需时重写为true）
     */
    protected boolean isNeedEventBus() {
        return false;
    }

    /**
     * 是否需要网络状态View（默认需要，子类无需时重写为false）
     */
    protected boolean isNeedNetworkStateView() {
        return true;
    }

    /**
     * 是否启用沉浸式状态栏（默认不启用，子类需时重写为true）
     */
    protected boolean isStatusBarEnabled() {
        return false;
    }

    /**
     * 状态栏字体颜色（默认黑色，白色需重写为false）
     */
    protected boolean statusBarDarkFont() {
        return true;
    }


    // ====================== 可见性回调（子类按需重写） ======================
    /**
     * Fragment对用户可见时触发（如：恢复播放、刷新数据）
     */
    protected void onVisible() {}

    /**
     * Fragment对用户不可见时触发（如：暂停播放、释放资源）
     */
    protected void onInvisible() {}


    // ====================== 工具方法（直接调用） ======================
    /**
     * 页面跳转（无参数）
     */
    public void jumpTo(@NonNull Class<?> targetCls) {
        jumpTo(targetCls, null);
    }

    /**
     * 页面跳转（带参数）
     */
    public void jumpTo(@NonNull Class<?> targetCls, @Nullable Bundle bundle) {
        if (mActivity == null) return;
        Intent intent = new Intent(mActivity, targetCls);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    /**
     * 页面跳转（带返回结果）
     */
    public void jumpForResult(@NonNull Class<?> targetCls, int requestCode) {
        jumpForResult(targetCls, null, requestCode);
    }

    /**
     * 页面跳转（带参数+返回结果）
     */
    public void jumpForResult(@NonNull Class<?> targetCls, @Nullable Bundle bundle, int requestCode) {
        if (mActivity == null) return;
        Intent intent = new Intent(mActivity, targetCls);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivityForResult(intent, requestCode);
    }

    /**
     * 关闭当前Fragment所在Activity
     */
    public void finishActivity() {
        if (mActivity != null) {
            mActivity.finish();
        }
    }

    /**
     * 返回键拦截（需在Activity中转发，示例：Activity的onKeyDown调用fragment.onKeyDown）
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false; // 默认不拦截，返回给Activity
    }

    /**
     * 显示吐司（支持String、资源ID、Object）
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


    // ====================== 网络状态View控制（直接调用） ======================
    /**
     * 显示加载中
     */
    public void showLoading() {
        if (networkStateView != null && isNeedNetworkStateView()) {
            networkStateView.showLoading();
        }
    }

    /**
     * 显示内容（子类布局）
     */
    public void showContent() {
        if (networkStateView != null && isNeedNetworkStateView()) {
            networkStateView.showSuccess();
        }
    }

    /**
     * 显示无网络（带刷新）
     */
    public void showNoNetwork() {
        if (networkStateView != null && isNeedNetworkStateView()) {
            networkStateView.showNoNetwork();
            networkStateView.setOnRefreshListener(this);
        }
    }

    /**
     * 显示空数据（带刷新）
     */
    public void showEmpty() {
        if (networkStateView != null && isNeedNetworkStateView()) {
            networkStateView.showEmpty();
            networkStateView.setOnRefreshListener(this);
        }
    }

    /**
     * 显示错误（带刷新）
     */
    public void showError() {
        if (networkStateView != null && isNeedNetworkStateView()) {
            networkStateView.showError();
            networkStateView.setOnRefreshListener(this);
        }
    }

    /**
     * 网络状态View刷新回调（子类按需重写，处理重试逻辑）
     */
    @Override
    public void onRefresh() {
        // 示例：子类重写实现重试（如重新请求网络）
        // initData();
    }


    // ====================== 加载对话框控制（直接调用） ======================
    /**
     * 显示加载对话框（无文字）
     */
    public void showProgress() {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.showProgressDialog();
        }
    }

    /**
     * 显示加载对话框（带文字）
     */
    public void showProgress(@NonNull String text) {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.showProgressDialogWithText(text);
        }
    }

    /**
     * 显示加载成功（带文字，自动消失）
     */
    public void showProgressSuccess(@NonNull String text) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.showProgressSuccess(text);
        }
    }

    /**
     * 显示加载失败（带文字，自动消失）
     */
    public void showProgressFail(@NonNull String text) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.showProgressFail(text);
        }
    }

    /**
     * 隐藏加载对话框
     */
    public void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismissProgressDialog();
        }
    }


    // ====================== 沉浸式状态栏扩展（子类按需使用） ======================
    /**
     * 获取沉浸式配置实例（子类自定义配置，如设置状态栏颜色）
     * 示例：getStatusBarConfig().statusBarColor(R.color.white).init();
     */
    protected ImmersionBar getImmersionConfig() {
        return mImmersionBar;
    }
}