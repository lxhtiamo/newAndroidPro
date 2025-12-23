package com.linx.mylibrary.view.editText;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.linx.mylibrary.R;


/**
 *    desc   : 密码隐藏显示 EditText（支持自定义切换图标、触摸反馈、空安全）
 */
public final class PasswordEditText extends RegexEditText
        implements View.OnTouchListener, View.OnFocusChangeListener, TextWatcher {

    // 密码状态枚举（语义化，替代直接判断Drawable）
    private enum PasswordStatus {
        VISIBLE,   // 密码可见
        INVISIBLE  // 密码不可见
    }

    // 密码切换图标
    private Drawable mPasswordVisibleIcon;   // 密码可见时的图标（原mInvisibleDrawable）
    private Drawable mPasswordInvisibleIcon; // 密码不可见时的图标（原mVisibleDrawable）
    private Drawable mCurrentToggleIcon;     // 当前显示的切换图标（原mCurrentDrawable）

    // 自定义属性：支持XML设置图标和着色
    private int mVisibleIconResId = R.drawable.password_on_ic;    // 注意：与原代码图标命名对应关系调整（语义化）
    private int mInvisibleIconResId = R.drawable.password_off_ic;
    private int mIconTint = -1; // 图标着色（-1表示不设置）

    // 密码当前状态
    private PasswordStatus mPasswordStatus = PasswordStatus.INVISIBLE;

    // 外部监听代理
    @Nullable
    private View.OnTouchListener mTouchListener;
    @Nullable
    private View.OnFocusChangeListener mFocusChangeListener;

    // 标记是否正在触摸切换图标（用于触摸反馈）
    private boolean mIsTouchingIcon = false;

    public PasswordEditText(Context context) {
        this(context, null);
    }

    public PasswordEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    @SuppressWarnings("all")
    public PasswordEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 初始化自定义属性
        initCustomAttrs(context, attrs);
        // 初始化切换图标
        initToggleIcons();
        // 初始化密码输入类型和正则
        initPasswordConfig();
        // 设置监听
        setupListeners();
    }

    /**
     * 初始化自定义属性（支持XML设置图标和着色）
     */
    private void initCustomAttrs(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray array = null;
        try {
            array = context.obtainStyledAttributes(attrs, R.styleable.PasswordEditText);
            // 获取自定义图标
            int visibleIcon = array.getResourceId(R.styleable.PasswordEditText_passwordVisibleIcon, mVisibleIconResId);
            if (visibleIcon != 0) {
                mVisibleIconResId = visibleIcon;
            }
            int invisibleIcon = array.getResourceId(R.styleable.PasswordEditText_passwordInvisibleIcon, mInvisibleIconResId);
            if (invisibleIcon != 0) {
                mInvisibleIconResId = invisibleIcon;
            }
            // 获取图标着色
            mIconTint = array.getColor(R.styleable.PasswordEditText_passwordIconTint, mIconTint);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (array != null) {
                array.recycle();
            }
        }
    }

    /**
     * 初始化切换图标（处理空安全、尺寸、着色）
     */
    private void initToggleIcons() {
        // 初始化密码不可见图标（原mVisibleDrawable）
        mPasswordInvisibleIcon = getTintedDrawable(getContext(), mInvisibleIconResId);
        // 初始化密码可见图标（原mInvisibleDrawable）
        mPasswordVisibleIcon = getTintedDrawable(getContext(), mVisibleIconResId);

        // 处理图标为null的情况：若两个图标都为null，直接返回（不显示切换图标）
        if (mPasswordInvisibleIcon == null && mPasswordVisibleIcon == null) {
            return;
        }

        // 设置图标bounds（处理固有尺寸为-1的情况，默认24dp）
        int defaultSize = dp2px(24);
        setDrawableBounds(mPasswordInvisibleIcon, defaultSize);
        setDrawableBounds(mPasswordVisibleIcon, defaultSize);

        // 初始显示密码不可见图标
        mCurrentToggleIcon = mPasswordInvisibleIcon;
        // 初始隐藏图标
        setToggleIconVisible(false);
    }

    /**
     * 获取着色后的Drawable（处理空安全）
     */
    // PasswordEditText中的getTintedDrawable方法修改
    private Drawable getTintedDrawable(Context context, int resId) {
        Drawable drawable = ContextCompat.getDrawable(context, resId);
        if (drawable == null) {
            // 移除系统资源降级，直接返回null（代码中有容错，不会崩溃）
            return null;
        }
        // 包装Drawable（支持矢量图和着色）
        drawable = DrawableCompat.wrap(drawable).mutate();
        // 图标着色
        if (mIconTint != -1) {
            DrawableCompat.setTint(drawable, mIconTint);
        }
        return drawable;
    }
    /**
     * 设置Drawable的bounds（容错：固有尺寸为-1时使用默认尺寸）
     */
    private void setDrawableBounds(Drawable drawable, int defaultSize) {
        if (drawable == null) {
            return;
        }
        int width = drawable.getIntrinsicWidth() <= 0 ? defaultSize : drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight() <= 0 ? defaultSize : drawable.getIntrinsicHeight();
        drawable.setBounds(0, 0, width, height);
    }

    /**
     * 初始化密码输入类型和正则规则
     */
    private void initPasswordConfig() {
        // 规范设置密码输入类型：TYPE_CLASS_TEXT + TYPE_TEXT_VARIATION_PASSWORD
        // 保留原有输入类型的基础上，添加密码变体类型
        int inputType = getInputType();
        if ((inputType & InputType.TYPE_CLASS_TEXT) == 0) {
            inputType |= InputType.TYPE_CLASS_TEXT;
        }
        inputType |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
        setInputType(inputType);

        // 设置默认正则：非空字符（若未设置自定义正则）
        if (getInputRegex() == null) {
            setInputRegex(String.valueOf(PATTERN_NONNULL));
        }

        // 初始设置密码不可见
        setPasswordStatus(PasswordStatus.INVISIBLE, false);
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
     * dp转px（工具方法）
     */
    private int dp2px(float dp) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * 判断输入内容是否非空（提取通用方法，避免重复代码）
     */
    private boolean isContentNotEmpty() {
        return getText().length() > 0;
    }

    /**
     * 设置切换图标的显示/隐藏
     */
    private void setToggleIconVisible(boolean visible) {
        if (mCurrentToggleIcon == null) {
            return;
        }
        // 避免重复设置
        if (mCurrentToggleIcon.isVisible() == visible) {
            return;
        }

        mCurrentToggleIcon.setVisible(visible, false);
        Drawable[] drawables = getCompoundDrawablesRelative();
        setCompoundDrawablesRelative(
                drawables[0],
                drawables[1],
                visible ? mCurrentToggleIcon : null,
                drawables[3]
        );
    }

    /**
     * 切换密码显示状态（核心方法，抽离逻辑）
     */
    private void setPasswordStatus(PasswordStatus status, boolean updateIcon) {
        mPasswordStatus = status;
        // 设置密码变换方法
        if (status == PasswordStatus.VISIBLE) {
            setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            if (updateIcon) {
                mCurrentToggleIcon = mPasswordVisibleIcon;
            }
        } else {
            setTransformationMethod(PasswordTransformationMethod.getInstance());
            if (updateIcon) {
                mCurrentToggleIcon = mPasswordInvisibleIcon;
            }
        }
        // 刷新图标显示
        if (updateIcon) {
            setToggleIconVisible(isFocused() && isContentNotEmpty());
            // 更新图标着色（触摸反馈）
            updateIconTint();
        }
        // 保持光标在文本末尾（优化：直接用Editable.length()，无需转字符串）
        Editable editable = getText();
        if (editable != null) {
            setSelection(editable.length());
        }
    }

    /**
     * 判断触摸点是否在切换图标范围内（精准计算：X+Y轴，考虑布局方向和Padding）
     */
    private boolean isTouchPointInIcon(int x, int y) {
        if (mCurrentToggleIcon == null || !mCurrentToggleIcon.isVisible()) {
            return false;
        }

        Rect iconRect = new Rect();
        int drawableWidth = mCurrentToggleIcon.getBounds().width();
        int drawableHeight = mCurrentToggleIcon.getBounds().height();
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // 根据布局方向计算图标X轴范围
        int layoutDirection = getLayoutDirection();
        if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            // 从左到右：图标在右侧
            iconRect.left = viewWidth - getPaddingEnd() - drawableWidth;
            iconRect.right = viewWidth - getPaddingEnd();
        } else {
            // 从右到左：图标在左侧
            iconRect.left = getPaddingStart();
            iconRect.right = getPaddingStart() + drawableWidth;
        }

        // 图标Y轴范围：垂直居中
        iconRect.top = (viewHeight - drawableHeight) / 2;
        iconRect.bottom = iconRect.top + drawableHeight;

        return iconRect.contains(x, y);
    }

    /**
     * 更新图标着色（触摸反馈：按下时半透明）
     */
    private void updateIconTint() {
        if (mCurrentToggleIcon == null || mIconTint == -1) {
            return;
        }
        // 触摸时设置半透明，否则恢复原色调
        int tintColor = mIsTouchingIcon ? adjustAlpha(mIconTint, 0.5f) : mIconTint;
        DrawableCompat.setTint(mCurrentToggleIcon, tintColor);
        invalidate();
    }

    /**
     * 调整颜色透明度（工具方法，修正位运算问题）
     */
    private int adjustAlpha(int color, float alpha) {
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        // 正确获取原颜色的Alpha通道（若有），否则使用传入的alpha
        int originalAlpha = (color >> 24) & 0xFF;
        int newAlpha = (int) (originalAlpha * alpha); // 或直接使用 (int) (alpha * 255)，根据需求选择
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        // 修正：Alpha通道是最高8位（24-31位），顺序不能错
        return (newAlpha << 24) | (red << 16) | (green << 8) | blue;
    }

    // ======================== 外部监听代理 ========================
    @Override
    public void setOnFocusChangeListener(@Nullable View.OnFocusChangeListener onFocusChangeListener) {
        mFocusChangeListener = onFocusChangeListener;
    }

    @Override
    public void setOnTouchListener(@Nullable View.OnTouchListener onTouchListener) {
        mTouchListener = onTouchListener;
    }

    // ======================== OnFocusChangeListener ========================
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        // 优化：使用通用方法判断内容非空
        setToggleIconVisible(hasFocus && isContentNotEmpty());
        // 代理外部监听
        if (mFocusChangeListener != null) {
            mFocusChangeListener.onFocusChange(view, hasFocus);
        }
    }

    // ======================== OnTouchListener ========================
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (mCurrentToggleIcon == null || !mCurrentToggleIcon.isVisible()) {
            return mTouchListener != null && mTouchListener.onTouch(view, event);
        }

        int action = event.getAction();
        boolean isTouchingIcon = isTouchPointInIcon((int) event.getX(), (int) event.getY());

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 按下时标记触摸状态，更新反馈
                mIsTouchingIcon = isTouchingIcon;
                updateIconTint();
                break;
            case MotionEvent.ACTION_MOVE:
                // 移动时更新触摸状态，更新反馈
                if (mIsTouchingIcon != isTouchingIcon) {
                    mIsTouchingIcon = isTouchingIcon;
                    updateIconTint();
                }
                break;
            case MotionEvent.ACTION_UP:
                // 抬起时处理密码切换
                if (mIsTouchingIcon) {
                    // 切换密码状态
                    PasswordStatus newStatus = mPasswordStatus == PasswordStatus.VISIBLE ?
                            PasswordStatus.INVISIBLE : PasswordStatus.VISIBLE;
                    setPasswordStatus(newStatus, true);
                }
                // 重置触摸状态，恢复图标色调
                mIsTouchingIcon = false;
                updateIconTint();
                break;
            case MotionEvent.ACTION_CANCEL:
                // 取消时重置触摸状态，恢复图标色调
                mIsTouchingIcon = false;
                updateIconTint();
                break;
        }

        // 若触摸了图标，消费事件；否则代理外部监听
        return mIsTouchingIcon || (mTouchListener != null && mTouchListener.onTouch(view, event));
    }

    // ======================== TextWatcher ========================
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (isFocused()) {
            setToggleIconVisible(isContentNotEmpty());
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void afterTextChanged(Editable s) {}

    // ======================== 扩展方法（外部调用） ========================
    /**
     * 动态设置密码是否可见
     */
    public void setPasswordVisible(boolean visible) {
        setPasswordStatus(visible ? PasswordStatus.VISIBLE : PasswordStatus.INVISIBLE, true);
    }

    /**
     * 获取密码是否可见
     */
    public boolean isPasswordVisible() {
        return mPasswordStatus == PasswordStatus.VISIBLE;
    }

    /**
     * 动态设置切换图标着色
     */
    public void setPasswordIconTint(int color) {
        mIconTint = color;
        // 更新现有图标着色
        if (mPasswordVisibleIcon != null) {
            DrawableCompat.setTint(mPasswordVisibleIcon, color);
        }
        if (mPasswordInvisibleIcon != null) {
            DrawableCompat.setTint(mPasswordInvisibleIcon, color);
        }
        invalidate();
    }
}