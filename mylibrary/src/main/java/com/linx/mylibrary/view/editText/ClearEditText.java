package com.linx.mylibrary.view.editText;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.linx.mylibrary.R;


/**
 *    desc   : 带清除按钮的 EditText（支持自定义清除图标、图标着色、触摸反馈）
 */
public final class ClearEditText extends RegexEditText
        implements OnTouchListener, OnFocusChangeListener, TextWatcher {

    // 清除图标相关
    private Drawable mClearDrawable;
    private int mClearIconResId = R.drawable.input_delete_ic; // 默认清除图标
    private int mClearIconTint = -1; // 清除图标着色（-1表示不设置）

    // 外部监听代理
    @Nullable
    private OnTouchListener mTouchListener;
    @Nullable
    private OnFocusChangeListener mFocusChangeListener;

    // 标记是否正在触摸清除图标（用于处理触摸反馈）
    private boolean mIsTouchingClearIcon = false;

    public ClearEditText(Context context) {
        this(context, null);
    }

    public ClearEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    @SuppressWarnings("all")
    public ClearEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 初始化自定义属性
        initCustomAttrs(context, attrs);
        // 初始化清除图标
        initClearDrawable();
        // 设置监听
        setupListeners();
    }

    /**
     * 初始化自定义属性（支持XML中设置清除图标和着色）
     */
    private void initCustomAttrs(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray array = null;
        try {
            array = context.obtainStyledAttributes(attrs, R.styleable.ClearEditText);
            // 获取自定义清除图标
            int iconResId = array.getResourceId(R.styleable.ClearEditText_clearIcon, mClearIconResId);
            if (iconResId != 0) { // 0表示未设置
                mClearIconResId = iconResId;
            }
            // 获取自定义图标着色
            mClearIconTint = array.getColor(R.styleable.ClearEditText_clearIconTint, mClearIconTint);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (array != null) {
                array.recycle();
            }
        }
    }

    /**
     * 初始化清除图标（处理空安全、尺寸、着色）
     */
    private void initClearDrawable() {
        // 获取清除图标（处理资源为null的情况）
        mClearDrawable = ContextCompat.getDrawable(getContext(), mClearIconResId);
        if (mClearDrawable == null) {
            // 若资源不存在，使用默认的系统图标（降级处理）
            mClearDrawable = ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_close_clear_cancel);
            // 若仍为null，直接返回，不显示清除图标
            if (mClearDrawable == null) {
                return;
            }
        }

        // 图标着色（可选）
        if (mClearIconTint != -1) {
            DrawableCompat.setTint(mClearDrawable, mClearIconTint);
        }

        // 设置图标bounds（处理固有尺寸为-1的情况）
        int drawableWidth = mClearDrawable.getIntrinsicWidth();
        int drawableHeight = mClearDrawable.getIntrinsicHeight();
        // 容错：若固有尺寸为-1，使用默认尺寸（如24dp，适配Material Design）
        final int defaultSize = dp2px(24);
        if (drawableWidth <= 0) {
            drawableWidth = defaultSize;
        }
        if (drawableHeight <= 0) {
            drawableHeight = defaultSize;
        }
        mClearDrawable.setBounds(0, 0, drawableWidth, drawableHeight);

        // 初始隐藏清除图标
        setDrawableVisible(false);
    }

    /**
     * 设置监听（抽离方法，代码更清晰）
     */
    private void setupListeners() {
        super.setOnTouchListener(this);
        super.setOnFocusChangeListener(this);
        super.addTextChangedListener(this);
    }

    /**
     * dp转px（工具方法，适配不同屏幕）
     */
    private int dp2px(float dp) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * 判断输入内容是否非空（提取通用方法，避免重复代码）
     */
    private boolean isContentNotEmpty() {
        return getText() != null && getText().length() > 0; // 保留getText()判断，兼容极端情况
    }

    /**
     * 设置清除图标的显示/隐藏
     */
    private void setDrawableVisible(boolean visible) {
        // 若drawable为null，直接返回
        if (mClearDrawable == null) {
            return;
        }
        // 避免重复设置
        if (mClearDrawable.isVisible() == visible) {
            return;
        }

        mClearDrawable.setVisible(visible, false);
        // 获取当前的复合drawable
        Drawable[] drawables = getCompoundDrawablesRelative();
        // 设置右侧/左侧的drawable（根据布局方向）
        setCompoundDrawablesRelative(
                drawables[0], // 开始位置
                drawables[1], // 顶部
                visible ? mClearDrawable : null, // 结束位置（清除图标）
                drawables[3]  // 底部
        );
    }

    // ======================== 外部监听代理 ========================
    @Override
    public void setOnFocusChangeListener(@Nullable OnFocusChangeListener onFocusChangeListener) {
        mFocusChangeListener = onFocusChangeListener;
    }

    @Override
    public void setOnTouchListener(@Nullable OnTouchListener onTouchListener) {
        mTouchListener = onTouchListener;
    }

    // ======================== OnFocusChangeListener ========================
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        // 优化：使用提取的通用方法判断内容是否非空
        setDrawableVisible(hasFocus && isContentNotEmpty());
        // 代理外部监听
        if (mFocusChangeListener != null) {
            mFocusChangeListener.onFocusChange(view, hasFocus);
        }
    }

    // ======================== OnTouchListener ========================
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // 若drawable为null，直接代理外部监听
        if (mClearDrawable == null || !mClearDrawable.isVisible()) {
            return mTouchListener != null && mTouchListener.onTouch(view, event);
        }

        int action = event.getAction();
        // 计算触摸位置是否在清除图标范围内（优化：使用更精准的范围计算）
        boolean isTouchingIcon = isTouchPointInClearIcon((int) event.getX(), (int) event.getY());

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 按下时标记触摸状态，更新图标视觉反馈
                mIsTouchingClearIcon = isTouchingIcon;
                updateClearIconTint();
                break;
            case MotionEvent.ACTION_MOVE:
                // 移动时更新触摸状态，更新图标视觉反馈
                if (mIsTouchingClearIcon != isTouchingIcon) {
                    mIsTouchingClearIcon = isTouchingIcon;
                    updateClearIconTint();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 抬起/取消时，重置触摸状态，清空内容（仅ACTION_UP时）
                if (mIsTouchingClearIcon && isTouchingIcon && action == MotionEvent.ACTION_UP) {
                    // 优化：使用getText().clear()代替setText("")，更高效（避免重新创建Editable）
                    getText().clear();
                }
                mIsTouchingClearIcon = false;
                updateClearIconTint();
                break;
        }

        // 若触摸了图标，消费事件；否则代理外部监听
        return mIsTouchingClearIcon || (mTouchListener != null && mTouchListener.onTouch(view, event));
    }

    /**
     * 判断触摸点是否在清除图标范围内（优化：精准计算，包含x和y轴）
     */
    private boolean isTouchPointInClearIcon(int x, int y) {
        // 获取清除图标的实际显示区域（相对于View的坐标）
        Rect iconRect = getClearIconVisibleRect();
        return iconRect.contains(x, y);
    }

    /**
     * 获取清除图标的实际显示区域（相对于View的坐标）
     */
    private Rect getClearIconVisibleRect() {
        Rect rect = new Rect();
        if (mClearDrawable == null) {
            return rect;
        }

        int drawableWidth = mClearDrawable.getBounds().width();
        int drawableHeight = mClearDrawable.getBounds().height();
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // 根据布局方向计算图标位置
        int layoutDirection = getLayoutDirection();
        if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            // 从左到右：图标在右侧（end）
            rect.left = viewWidth - getPaddingEnd() - drawableWidth;
            rect.right = viewWidth - getPaddingEnd();
        } else {
            // 从右到左：图标在左侧（start）
            rect.left = getPaddingStart();
            rect.right = getPaddingStart() + drawableWidth;
        }

        // 垂直方向：居中显示
        rect.top = (viewHeight - drawableHeight) / 2;
        rect.bottom = rect.top + drawableHeight;

        return rect;
    }

    /**
     * 更新清除图标的着色（触摸反馈：按下时变浅）
     */
    private void updateClearIconTint() {
        if (mClearDrawable == null || mClearIconTint == -1) {
            return;
        }

        // 按下时使用半透明色，正常时使用原色调
        int tintColor = mIsTouchingClearIcon ?
                adjustAlpha(mClearIconTint, 0.5f) : mClearIconTint;
        DrawableCompat.setTint(mClearDrawable, tintColor);
        // 刷新显示
        invalidate();
    }

    /**
     * 调整颜色的透明度（工具方法）
     */
    private int adjustAlpha(int color, float alpha) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        return (color & 0xFF000000) | (red << 16) | (green << 8) | blue;
    }

    // ======================== TextWatcher ========================
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (isFocused()) {
            // 优化：使用提取的通用方法
            setDrawableVisible(isContentNotEmpty());
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void afterTextChanged(Editable s) {}

    // ======================== 扩展方法（可选） ========================
    /**
     * 动态设置清除图标
     */
    public void setClearIcon(int resId) {
        mClearIconResId = resId;
        initClearDrawable();
        // 刷新显示
        setDrawableVisible(isFocused() && isContentNotEmpty());
    }

    /**
     * 动态设置清除图标着色
     */
    public void setClearIconTint(int color) {
        mClearIconTint = color;
        if (mClearDrawable != null) {
            DrawableCompat.setTint(mClearDrawable, color);
            invalidate();
        }
    }
}