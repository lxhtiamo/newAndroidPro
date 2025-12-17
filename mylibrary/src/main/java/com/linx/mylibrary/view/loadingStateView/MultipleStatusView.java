package com.linx.mylibrary.view.loadingStateView;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.linx.mylibrary.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 类描述：  一个方便在多种状态切换的view（优化版：继承FrameLayout，轻量化、高性能）
 * <p>
 * 创建人:   续写经典
 */
@SuppressWarnings("unused")
public class MultipleStatusView extends FrameLayout {
    // 保留原有的状态常量，逻辑不变
    public static final int STATUS_CONTENT = 0x00;
    public static final int STATUS_LOADING = 0x01;
    public static final int STATUS_EMPTY = 0x02;
    public static final int STATUS_ERROR = 0x03;
    public static final int STATUS_NO_NETWORK = 0x04;

    private static final int NULL_RESOURCE_ID = -1;
    // 改为FrameLayout的布局参数（适配父类修改，性能更优）
    private static final LayoutParams DEFAULT_LAYOUT_PARAMS =
            new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    // 状态与视图的映射（优化：替代原有的多个单独视图变量，减少冗余，便于管理）
    private final Map<Integer, View> mStateViewMap = new HashMap<>();
    // 保留原有的mOtherIds（兼容原有逻辑，用于ContentView的显示判断）
    private final ArrayList<Integer> mOtherIds = new ArrayList<>();

    // 布局资源ID（保留原有的配置逻辑）
    private int mEmptyViewResId;
    private int mErrorViewResId;
    private int mLoadingViewResId;
    private int mNoNetworkViewResId;
    private int mContentViewResId;

    // 当前状态（保留原变量名，兼容原有逻辑）
    private int mViewStatus;
    private LayoutInflater mInflater;

    // 重试回调（保留原有的接口和逻辑）
    private OnRefreshListener mRefreshListener;

    // ====================================== 构造方法（仅调整父类相关，逻辑不变） ======================================
    public MultipleStatusView(Context context) {
        this(context, null);
    }

    public MultipleStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultipleStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 保留原有的属性解析逻辑
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultipleStatusView, defStyleAttr, 0);
        mEmptyViewResId = a.getResourceId(R.styleable.MultipleStatusView_emptyView, R.layout.empty_view);
        mErrorViewResId = a.getResourceId(R.styleable.MultipleStatusView_errorView, R.layout.error_view);
        mLoadingViewResId = a.getResourceId(R.styleable.MultipleStatusView_loadingView, R.layout.loading_view);
        mNoNetworkViewResId = a.getResourceId(R.styleable.MultipleStatusView_noNetworkView, R.layout.no_network_view);
        mContentViewResId = a.getResourceId(R.styleable.MultipleStatusView_contentView, NULL_RESOURCE_ID);
        a.recycle(); // 保留及时回收的逻辑

        mInflater = LayoutInflater.from(getContext());
        // 初始化ContentView：处理布局内的子视图（原逻辑缺失的关键优化）
        initContentViewFromChildren();
    }

    // ====================================== 生命周期方法（优化资源管理） ======================================
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // 保留原有的默认显示逻辑
        showContent();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 优化：批量清理视图和映射关系，更彻底
        clearAllStateViews();
        mOtherIds.clear();
        mRefreshListener = null; // 解除回调引用
        mInflater = null;
    }

    // ====================================== 公共方法（完全保留原有签名和逻辑） ======================================
    /**
     * 获取当前状态
     */
    public int getViewStatus() {
        return mViewStatus;
    }

    /**
     * 显示空视图
     */
    public final void showEmpty() {
        showEmpty(mEmptyViewResId, DEFAULT_LAYOUT_PARAMS);
    }

    public final void showEmpty(int layoutId, ViewGroup.LayoutParams layoutParams) {
        showEmpty(inflateView(layoutId), layoutParams);
    }

    public final void showEmpty(View view, ViewGroup.LayoutParams layoutParams) {
        // 抽取通用方法：处理空视图的显示（复用逻辑，减少冗余）
        showStateView(STATUS_EMPTY, view, layoutParams, R.id.empty_retry_view);
    }

    /**
     * 显示错误视图
     */
    public final void showError() {
        showError(mErrorViewResId, DEFAULT_LAYOUT_PARAMS);
    }

    public final void showError(int layoutId, ViewGroup.LayoutParams layoutParams) {
        showError(inflateView(layoutId), layoutParams);
    }

    public final void showError(View view, ViewGroup.LayoutParams layoutParams) {
        showStateView(STATUS_ERROR, view, layoutParams, R.id.error_retry_view);
    }

    /**
     * 显示加载中视图
     */
    public final void showLoading() {
        showLoading(mLoadingViewResId, DEFAULT_LAYOUT_PARAMS);
    }

    public final void showLoading(int layoutId, ViewGroup.LayoutParams layoutParams) {
        showLoading(inflateView(layoutId), layoutParams);
    }

    public final void showLoading(View view, ViewGroup.LayoutParams layoutParams) {
        // 加载视图无重试按钮，传NULL_RESOURCE_ID
        showStateView(STATUS_LOADING, view, layoutParams, NULL_RESOURCE_ID);
    }

    /**
     * 显示无网络视图
     */
    public final void showNoNetwork() {
        showNoNetwork(mNoNetworkViewResId, DEFAULT_LAYOUT_PARAMS);
    }

    public final void showNoNetwork(int layoutId, ViewGroup.LayoutParams layoutParams) {
        showNoNetwork(inflateView(layoutId), layoutParams);
    }

    public final void showNoNetwork(View view, ViewGroup.LayoutParams layoutParams) {
        showStateView(STATUS_NO_NETWORK, view, layoutParams, R.id.no_network_retry_view);
    }

    /**
     * 显示内容视图
     */
    public final void showContent() {
        mViewStatus = STATUS_CONTENT;
        // 优化：ContentView的显示逻辑更健壮
        showContentView();
    }

    // ====================================== 回调设置（保留原有逻辑） ======================================
    public void setOnRefreshListener(OnRefreshListener listener) {
        mRefreshListener = listener;
    }

    public interface OnRefreshListener {
        void onClickRefreshListener();
    }

    // ====================================== 私有优化方法（核心优化点，不影响原有逻辑） ======================================
    /**
     * 初始化ContentView：处理布局内直接写的子视图（原逻辑缺失的优化）
     */
    private void initContentViewFromChildren() {
        // 如果通过属性配置了ContentView，不处理子视图；否则将第一个子视图作为ContentView
        if (mContentViewResId == NULL_RESOURCE_ID && getChildCount() > 0) {
            View contentView = getChildAt(0);
            // 移除并缓存（避免与状态视图层叠）
            removeView(contentView);
            mStateViewMap.put(STATUS_CONTENT, contentView);
        }
    }

    /**
     * 通用状态视图显示方法（核心优化：抽取重复逻辑，减少代码冗余）
     * @param status 状态值
     * @param view 视图
     * @param layoutParams 布局参数
     * @param retryViewId 重试按钮ID（无则传NULL_RESOURCE_ID）
     */
    private void showStateView(int status, View view, ViewGroup.LayoutParams layoutParams, int retryViewId) {
        checkNull(view, getStatusName(status) + " view is null!");

        mViewStatus = status;
        View targetView = mStateViewMap.get(status);

        if (targetView == null) {
            targetView = view;
            // 处理重试按钮的点击逻辑（保留原有逻辑）
            if (retryViewId != NULL_RESOURCE_ID) {
                View retryView = targetView.findViewById(retryViewId);
                if (mRefreshListener != null && retryView != null) {
                    retryView.setOnClickListener(v -> {
                        showLoading();
                        mRefreshListener.onClickRefreshListener();
                    });
                }
            }
            // 优化：处理视图ID为NO_ID的情况，避免加入mOtherIds后判断异常
            if (targetView.getId() == View.NO_ID) {
                // 给视图设置唯一ID（防止ID冲突）
                targetView.setId(View.generateViewId());
            }
            mStateViewMap.put(status, targetView);
            mOtherIds.add(targetView.getId());
            // 添加视图到布局（保留原有的索引0，确保层叠在最上层）
            addView(targetView, 0, layoutParams);
        }

        // 显示指定状态的视图（保留原有逻辑）
        showViewById(targetView.getId());
    }

    /**
     * 优化布局加载：避免丢失布局参数（原代码的核心缺陷修复）
     */
    private View inflateView(int layoutId) {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(getContext());
        }
        // 改为inflate(layoutId, this, false)：保留布局文件中的layout_*参数，且不立即添加到父布局
        return mInflater.inflate(layoutId, this, false);
    }

    /**
     * 保留原有的showViewById逻辑
     */
    private void showViewById(int viewId) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            view.setVisibility(view.getId() == viewId ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 优化ContentView的显示逻辑（兼容布局内子视图和属性配置的视图）
     */
    private void showContentView() {
        // 1. 如果是属性配置的ContentView，先初始化并添加
        if (mContentViewResId != NULL_RESOURCE_ID && mStateViewMap.get(STATUS_CONTENT) == null) {
            View contentView = inflateView(mContentViewResId);
            mStateViewMap.put(STATUS_CONTENT, contentView);
            addView(contentView, 0, DEFAULT_LAYOUT_PARAMS);
        }

        // 2. 显示ContentView，隐藏其他状态视图（保留原有的mOtherIds判断逻辑）
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            view.setVisibility(mOtherIds.contains(view.getId()) ? View.GONE : View.VISIBLE);
        }

        // 3. 如果有缓存的ContentView，确保其显示
        View contentView = mStateViewMap.get(STATUS_CONTENT);
        if (contentView != null) {
            contentView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 保留原有的空判断逻辑
     */
    private void checkNull(Object object, String hint) {
        if (object == null) {
            throw new NullPointerException(hint);
        }
    }

    /**
     * 优化：批量清理状态视图（替代原有的clear方法，更优雅）
     */
    private void clearAllStateViews() {
        // 遍历状态视图并移除
        for (View view : mStateViewMap.values()) {
            if (view != null && view.getParent() == this) {
                removeView(view);
            }
        }
        mStateViewMap.clear();
    }

    /**
     * 辅助方法：获取状态名称（用于错误提示）
     */
    private String getStatusName(int status) {
        switch (status) {
            case STATUS_EMPTY:
                return "Empty";
            case STATUS_ERROR:
                return "Error";
            case STATUS_LOADING:
                return "Loading";
            case STATUS_NO_NETWORK:
                return "NoNetwork";
            default:
                return "Unknown";
        }
    }
}