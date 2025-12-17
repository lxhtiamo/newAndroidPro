package com.linx.mylibrary.view.loadingStateView;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.linx.mylibrary.R;

/**
 * 加载状态的自定义View（支持成功、加载中、网络错误、无网络、空数据状态）
 * 加载中状态仅处理文本，不处理图片
 */
public class NetworkStateView extends FrameLayout {

    // 当前的加载状态
    private int mCurrentState;
    public static final int STATE_SUCCESS = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_NETWORK_ERROR = 2;
    public static final int STATE_NO_NETWORK = 3;
    public static final int STATE_EMPTY = 4;

    // 布局资源ID
    private int mLoadingViewId;
    private int mErrorViewId;
    private int mNoNetworkViewId;
    private int mEmptyViewId;

    // XML配置的属性：文本（加载中，仅保留文本）
    private String mLoadingText;

    // XML配置的属性：图片、文本（错误）
    private int mErrorImageId;
    private String mErrorText;

    // XML配置的属性：图片、文本（无网络）
    private int mNoNetworkImageId;
    private String mNoNetworkText;

    // XML配置的属性：图片、文本（空数据）
    private int mEmptyImageId;
    private String mEmptyText;

    // XML配置的属性：刷新图片
    private int mRefreshViewId;

    // 文本样式（全局共用）
    private int mTextColor;
    private int mTextSize;

    // 动态设置的值（优先级高于XML配置）
    private String mDynamicLoadingText; // 仅保留加载中文本的动态值
    private String mDynamicErrorText;
    private int mDynamicErrorImageId = NO_ID;
    private String mDynamicNoNetworkText;
    private int mDynamicNoNetworkImageId = NO_ID;
    private String mDynamicEmptyText;
    private int mDynamicEmptyImageId = NO_ID;
    private int mDynamicRefreshImageId = NO_ID;

    // 各状态的View实例
    private View mLoadingView;
    private View mErrorView;
    private View mNoNetworkView;
    private View mEmptyView;

    // 刷新监听
    private OnRefreshListener mRefreshListener;
    private LayoutInflater mInflater;

    // ======================== 构造方法 ========================
    public NetworkStateView(@NonNull Context context) {
        this(context, null);
    }

    public NetworkStateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkStateView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs, defStyleAttr);
        initView(context);
    }

    // ======================== 初始化方法 ========================
    /**
     * 解析自定义属性（移除加载中图片的解析）
     */
    private void initAttrs(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.NetworkStateView, defStyleAttr, R.style.NetworkStateView_Style);

        // 布局资源ID
        mLoadingViewId = typedArray.getResourceId(R.styleable.NetworkStateView_loadingView, R.layout.view_loading);
        mErrorViewId = typedArray.getResourceId(R.styleable.NetworkStateView_errorView, R.layout.view_network_error);
        mNoNetworkViewId = typedArray.getResourceId(R.styleable.NetworkStateView_noNetworkView, R.layout.view_no_network);
        mEmptyViewId = typedArray.getResourceId(R.styleable.NetworkStateView_emptyView, R.layout.view_empty);

        // 加载中状态：仅解析文本，删除图片解析
        mLoadingText = typedArray.getString(R.styleable.NetworkStateView_nsvLoadingText);

        // 错误状态：图片+文本
        mErrorImageId = typedArray.getResourceId(R.styleable.NetworkStateView_nsvErrorImage, NO_ID);
        mErrorText = typedArray.getString(R.styleable.NetworkStateView_nsvErrorText);

        // 无网络状态：图片+文本
        mNoNetworkImageId = typedArray.getResourceId(R.styleable.NetworkStateView_nsvNoNetworkImage, NO_ID);
        mNoNetworkText = typedArray.getString(R.styleable.NetworkStateView_nsvNoNetworkText);

        // 空数据状态：图片+文本
        mEmptyImageId = typedArray.getResourceId(R.styleable.NetworkStateView_nsvEmptyImage, NO_ID);
        mEmptyText = typedArray.getString(R.styleable.NetworkStateView_nsvEmptyText);

        // 刷新图片
        mRefreshViewId = typedArray.getResourceId(R.styleable.NetworkStateView_nsvRefreshImage, NO_ID);
        int defaultColor = ContextCompat.getColor(context, R.color.color_333);
        // 文本样式
        mTextColor = typedArray.getColor(R.styleable.NetworkStateView_nsvTextColor, defaultColor);
        mTextSize = typedArray.getDimensionPixelSize(R.styleable.NetworkStateView_nsvTextSize, dp2px(context, 14));

        typedArray.recycle(); // 回收TypedArray，避免内存泄漏
    }

    /**
     * 初始化基础View配置
     */
    private void initView(Context context) {
        mInflater = LayoutInflater.from(context);
        // 设置背景色（兼容低版本）
        setBackgroundColor(ContextCompat.getColor(context, R.color.white));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // 初始状态为成功（隐藏状态View）
        showSuccess();
    }

    // ======================== 状态切换方法 ========================
    /**
     * 加载成功，隐藏所有状态View
     */
    public void showSuccess() {
        mCurrentState = STATE_SUCCESS;
        updateViewVisibility();
    }

    /**
     * 显示加载中状态
     */
    public void showLoading() {
        mCurrentState = STATE_LOADING;
        if (mLoadingView == null) {
            mLoadingView = inflateView(mLoadingViewId);
            initLoadingView(mLoadingView); // 仅初始化文本
            addView(mLoadingView);
        }
        updateViewVisibility();
    }

    /**
     * 显示网络错误状态
     */
    public void showError() {
        mCurrentState = STATE_NETWORK_ERROR;
        if (mErrorView == null) {
            mErrorView = inflateView(mErrorViewId);
            initErrorView(mErrorView);
            addView(mErrorView);
        }
        updateViewVisibility();
    }

    /**
     * 显示无网络状态
     */
    public void showNoNetwork() {
        mCurrentState = STATE_NO_NETWORK;
        if (mNoNetworkView == null) {
            mNoNetworkView = inflateView(mNoNetworkViewId);
            initNoNetworkView(mNoNetworkView);
            addView(mNoNetworkView);
        }
        updateViewVisibility();
    }

    /**
     * 显示空数据状态
     */
    public void showEmpty() {
        mCurrentState = STATE_EMPTY;
        if (mEmptyView == null) {
            mEmptyView = inflateView(mEmptyViewId);
            initEmptyView(mEmptyView);
            addView(mEmptyView);
        }
        updateViewVisibility();
    }

    // ======================== 各状态View的初始化方法 ========================
    /**
     * 初始化加载中状态View（仅处理文本，删除图片相关逻辑）
     */
    private void initLoadingView(View view) {
        TextView loadingText = view.findViewById(R.id.loading_text);
        // 仅设置文本，优先使用动态值
        setTextViewAttrs(loadingText, mLoadingText, mDynamicLoadingText);
    }

    /**
     * 初始化错误状态View
     */
    private void initErrorView(View view) {
        ImageView errorImage = view.findViewById(R.id.error_image);
        TextView errorText = view.findViewById(R.id.error_text);
        ImageView refreshView = view.findViewById(R.id.refresh_view);

        setImageResource(errorImage, mErrorImageId, mDynamicErrorImageId);
        setTextViewAttrs(errorText, mErrorText, mDynamicErrorText);
        setImageResource(refreshView, mRefreshViewId, mDynamicRefreshImageId);
        setRefreshClickListener(refreshView);
    }

    /**
     * 初始化无网络状态View
     */
    private void initNoNetworkView(View view) {
        ImageView noNetworkImage = view.findViewById(R.id.no_network_image);
        TextView noNetworkText = view.findViewById(R.id.no_network_text);
        ImageView refreshView = view.findViewById(R.id.refresh_view);

        setImageResource(noNetworkImage, mNoNetworkImageId, mDynamicNoNetworkImageId);
        setTextViewAttrs(noNetworkText, mNoNetworkText, mDynamicNoNetworkText);
        setImageResource(refreshView, mRefreshViewId, mDynamicRefreshImageId);
        setRefreshClickListener(refreshView);
    }

    /**
     * 初始化空数据状态View
     */
    private void initEmptyView(View view) {
        ImageView emptyImage = view.findViewById(R.id.empty_image);
        TextView emptyText = view.findViewById(R.id.empty_text);
        ImageView refreshView = view.findViewById(R.id.refresh_view);

        setImageResource(emptyImage, mEmptyImageId, mDynamicEmptyImageId);
        setTextViewAttrs(emptyText, mEmptyText, mDynamicEmptyText);
        setImageResource(refreshView, mRefreshViewId, mDynamicRefreshImageId);
        setRefreshClickListener(refreshView);
    }

    // ======================== 工具方法 ========================
    /**
     * 加载布局（指定父布局为当前FrameLayout，LayoutParams生效）
     */
    private View inflateView(int layoutId) {
        return mInflater.inflate(layoutId, this, false);
    }

    /**
     * 设置ImageView的资源（空安全，优先使用动态值）
     */
    private void setImageResource(ImageView imageView, int xmlResId, int dynamicResId) {
        if (imageView != null) {
            int resId = (dynamicResId != NO_ID) ? dynamicResId : xmlResId;
            if (resId != NO_ID) {
                imageView.setImageResource(resId);
            }
        }
    }

    /**
     * 设置TextView的内容与样式（空安全，优先使用动态值）
     */
    private void setTextViewAttrs(TextView textView, String xmlText, String dynamicText) {
        if (textView == null) {
            return;
        }
        // 动态文本优先级更高
        String text = TextUtils.isEmpty(dynamicText) ? xmlText : dynamicText;
        if (!TextUtils.isEmpty(text)) {
            textView.setText(text);
        }
        // 应用文本样式
        textView.setTextColor(mTextColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
    }

    /**
     * 设置刷新按钮的点击事件（公共逻辑，减少冗余）
     */
    private void setRefreshClickListener(View refreshView) {
        if (refreshView != null) {
            refreshView.setOnClickListener(v -> {
                if (mRefreshListener != null) {
                    showLoading(); // 点击后先显示加载中
                    mRefreshListener.onRefresh();
                }
            });
        }
    }

    /**
     * 根据当前状态更新所有View的显示/隐藏
     */
    private void updateViewVisibility() {
        // 成功状态：整个View隐藏；其他状态显示
        setVisibility(mCurrentState == STATE_SUCCESS ? GONE : VISIBLE);

        // 各子View根据状态显示/隐藏
        setViewVisibility(mLoadingView, STATE_LOADING);
        setViewVisibility(mErrorView, STATE_NETWORK_ERROR);
        setViewVisibility(mNoNetworkView, STATE_NO_NETWORK);
        setViewVisibility(mEmptyView, STATE_EMPTY);
    }

    /**
     * 辅助方法：设置单个View的显示/隐藏
     */
    private void setViewVisibility(View view, int targetState) {
        if (view != null) {
            view.setVisibility(mCurrentState == targetState ? VISIBLE : GONE);
        }
    }

    /**
     * dp转px（安卓原生兼容方法，替换UIUtils）
     */
    private int dp2px(Context context, float dpValue) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                context.getResources().getDisplayMetrics()
        );
    }

    /**
     * 动态设置加载中的文本内容（仅保留文本的动态设置）
     */
    public void setLoadingText(String text) {
        mDynamicLoadingText = text;
        if (mLoadingView != null) {
            TextView loadingText = mLoadingView.findViewById(R.id.loading_text);
            setTextViewAttrs(loadingText, mLoadingText, mDynamicLoadingText);
        }
    }

    /**
     * 动态设置网络错误的文本内容
     */
    public void setErrorText(String text) {
        mDynamicErrorText = text;
        if (mErrorView != null) {
            TextView errorText = mErrorView.findViewById(R.id.error_text);
            setTextViewAttrs(errorText, mErrorText, mDynamicErrorText);
        }
    }

    /**
     * 动态设置网络错误的图片资源
     */
    public void setErrorImageResId(int resId) {
        mDynamicErrorImageId = resId;
        if (mErrorView != null) {
            ImageView errorImage = mErrorView.findViewById(R.id.error_image);
            setImageResource(errorImage, mErrorImageId, mDynamicErrorImageId);
        }
    }

    /**
     * 动态设置无网络的文本内容
     */
    public void setNoNetworkText(String text) {
        mDynamicNoNetworkText = text;
        if (mNoNetworkView != null) {
            TextView noNetworkText = mNoNetworkView.findViewById(R.id.no_network_text);
            setTextViewAttrs(noNetworkText, mNoNetworkText, mDynamicNoNetworkText);
        }
    }

    /**
     * 动态设置无网络的图片资源
     */
    public void setNoNetworkImageResId(int resId) {
        mDynamicNoNetworkImageId = resId;
        if (mNoNetworkView != null) {
            ImageView noNetworkImage = mNoNetworkView.findViewById(R.id.no_network_image);
            setImageResource(noNetworkImage, mNoNetworkImageId, mDynamicNoNetworkImageId);
        }
    }

    /**
     * 动态设置空数据的文本内容
     */
    public void setEmptyText(String text) {
        mDynamicEmptyText = text;
        if (mEmptyView != null) {
            TextView emptyText = mEmptyView.findViewById(R.id.empty_text);
            setTextViewAttrs(emptyText, mEmptyText, mDynamicEmptyText);
        }
    }

    /**
     * 动态设置空数据的图片资源
     */
    public void setEmptyImageResId(int resId) {
        mDynamicEmptyImageId = resId;
        if (mEmptyView != null) {
            ImageView emptyImage = mEmptyView.findViewById(R.id.empty_image);
            setImageResource(emptyImage, mEmptyImageId, mDynamicEmptyImageId);
        }
    }

    /**
     * 动态设置刷新按钮的图片资源
     */
    public void setRefreshImageResId(int resId) {
        mDynamicRefreshImageId = resId;
        // 同时更新所有状态的刷新按钮
        updateRefreshImage(mErrorView);
        updateRefreshImage(mNoNetworkView);
        updateRefreshImage(mEmptyView);
    }

    /**
     * 辅助方法：更新单个View的刷新图片
     */
    private void updateRefreshImage(View view) {
        if (view != null) {
            ImageView refreshView = view.findViewById(R.id.refresh_view);
            setImageResource(refreshView, mRefreshViewId, mDynamicRefreshImageId);
        }
    }

    // ======================== 监听接口 ========================
    public void setOnRefreshListener(OnRefreshListener listener) {
        mRefreshListener = listener;
    }

    public interface OnRefreshListener {
        void onRefresh();
    }
}