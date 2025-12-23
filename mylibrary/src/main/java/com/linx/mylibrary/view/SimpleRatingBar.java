package com.linx.mylibrary.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.linx.mylibrary.R;

/**
 * 优化版自定义评分控件（星星评分组件）
 * 功能特性：
 * 1. 支持整颗星/半颗星模式
 * 2. 支持动画效果
 * 3. 支持点击取消/重置
 * 4. 完善的辅助功能
 * 5. 支持尺寸自适应
 */
public final class SimpleRatingBar extends View {

    // 常量定义
    private static final float DEFAULT_GRADE_SPACE_RATIO = 0.25f;
    private static final int STEP_HALF = 0x00;
    private static final int STEP_ONE = 0x01;
    private static final float FLOAT_PRECISION = 1e-6f;
    private static final int DEFAULT_GRADE_SIZE_DP = 24;
    private static final long DEFAULT_ANIMATION_DURATION = 300L;

    // 辅助功能自定义动作ID
    private static final int ACTION_INCREASE_RATING = 0x10000001;
    private static final int ACTION_DECREASE_RATING = 0x10000002;
    private static final int ACTION_SET_RATING = 0x10000003;

    // Drawable资源
    private Drawable mNormalDrawable;
    private Drawable mFillDrawable;
    private Drawable mHalfDrawable;

    // 绘制相关
    private final Rect mGradeBounds = new Rect();
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 配置参数
    private float mCurrentGrade;
    private float mAnimatedGrade;
    private int mGradeCount;
    private int mGradeWidth;
    private int mGradeHeight;
    private int mGradeSpace;
    private GradeStep mGradeStep;

    // 功能参数
    private boolean mTouchable = true;
    private boolean mAnimationEnabled = true;
    private long mAnimationDuration = DEFAULT_ANIMATION_DURATION;
    private boolean mClickToReset = false;
    private boolean mAllowZeroRating = true;
    private float mMinimumGrade = 0f;
    private boolean mAutoSize = false;
    private int mMaxAutoSizeWidth = 0;
    private boolean mEnablePressedEffect = true;

    // 触摸状态
    private int mPressedIndex = -1;
    private boolean mIsTouching = false;

    // 动画
    private ValueAnimator mRatingAnimator;

    // 监听器
    private OnRatingChangeListener mListener;

    // 构造函数
    public SimpleRatingBar(Context context) {
        this(context, null);
    }

    public SimpleRatingBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleRatingBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化属性
     */
    private void initAttrs(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SimpleRatingBar);

        // 加载Drawable
        mNormalDrawable = getDrawableFromAttr(array,
                R.styleable.SimpleRatingBar_normalDrawable,
                R.drawable.rating_star_off_ic);
        mHalfDrawable = getDrawableFromAttr(array,
                R.styleable.SimpleRatingBar_halfDrawable,
                R.drawable.rating_star_half_ic);
        mFillDrawable = getDrawableFromAttr(array,
                R.styleable.SimpleRatingBar_fillDrawable,
                R.drawable.rating_star_fill_ic);

        // 校验Drawable尺寸
        validateDrawableSize();

        // 基本配置
        mCurrentGrade = Math.max(0, array.getFloat(R.styleable.SimpleRatingBar_grade, 0));
        mGradeCount = Math.max(1, array.getInt(R.styleable.SimpleRatingBar_gradeCount, 5));

        // 尺寸设置
        int defaultWidth = mNormalDrawable.getIntrinsicWidth();
        int defaultHeight = mNormalDrawable.getIntrinsicHeight();

        mGradeWidth = array.getDimensionPixelSize(R.styleable.SimpleRatingBar_gradeWidth, defaultWidth);
        mGradeHeight = array.getDimensionPixelSize(R.styleable.SimpleRatingBar_gradeHeight, defaultHeight);
        mGradeSpace = (int) array.getDimension(R.styleable.SimpleRatingBar_gradeSpace,
                (int) (Math.min(mGradeWidth, mGradeHeight) * DEFAULT_GRADE_SPACE_RATIO));

        // 步长设置
        int stepValue = array.getInt(R.styleable.SimpleRatingBar_gradeStep, STEP_HALF);
        mGradeStep = stepValue == STEP_ONE ? GradeStep.ONE : GradeStep.HALF;

        // 功能设置
        mTouchable = array.getBoolean(R.styleable.SimpleRatingBar_touchable, true);
        mAnimationEnabled = array.getBoolean(R.styleable.SimpleRatingBar_animationEnabled, true);
        mAnimationDuration = array.getInt(R.styleable.SimpleRatingBar_animationDuration,
                (int) DEFAULT_ANIMATION_DURATION);
        mClickToReset = array.getBoolean(R.styleable.SimpleRatingBar_clickToReset, false);
        mAllowZeroRating = array.getBoolean(R.styleable.SimpleRatingBar_allowZeroRating, true);
        mMinimumGrade = Math.max(0, array.getFloat(R.styleable.SimpleRatingBar_minimumGrade, 0));
        mAutoSize = array.getBoolean(R.styleable.SimpleRatingBar_autoSize, false);
        mMaxAutoSizeWidth = array.getDimensionPixelSize(R.styleable.SimpleRatingBar_maxAutoSizeWidth, 0);
        mEnablePressedEffect = array.getBoolean(R.styleable.SimpleRatingBar_enablePressedEffect, true);

        array.recycle();

        // 修正初始值
        mCurrentGrade = Math.min(mCurrentGrade, mGradeCount);
        mCurrentGrade = correctGradeByStep(mCurrentGrade);
        mAnimatedGrade = mCurrentGrade;
        mMinimumGrade = Math.min(mMinimumGrade, mGradeCount);

        if (!mAllowZeroRating && mMinimumGrade < 1) {
            mMinimumGrade = 1;
        }
    }

    /**
     * 初始化其他设置
     */
    private void init() {
        setFocusable(true);
        setClickable(true);

        // 设置无障碍功能代理
        ViewCompat.setAccessibilityDelegate(this, new androidx.core.view.AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                          @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                setupAccessibilityInfo(info);
            }

            @Override
            public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                return performAccessibilityActionCompat(action, args);
            }
        });
    }

    /**
     * 从属性获取Drawable
     */
    private Drawable getDrawableFromAttr(TypedArray array, int attrId, @DrawableRes int defaultResId) {
        int resId = array.getResourceId(attrId, defaultResId);
        Drawable drawable = ContextCompat.getDrawable(getContext(), resId);
        if (drawable == null) {
            throw new IllegalStateException("Drawable resource not found: " + resId);
        }
        return drawable;
    }

    /**
     * 校验Drawable尺寸
     */
    private void validateDrawableSize() {
        int normalWidth = mNormalDrawable.getIntrinsicWidth();
        int normalHeight = mNormalDrawable.getIntrinsicHeight();

        if (mFillDrawable.getIntrinsicWidth() != normalWidth ||
                mFillDrawable.getIntrinsicHeight() != normalHeight) {
            throw new IllegalStateException("fillDrawable size must match normalDrawable");
        }

        if (mHalfDrawable != null && (mHalfDrawable.getIntrinsicWidth() != normalWidth ||
                mHalfDrawable.getIntrinsicHeight() != normalHeight)) {
            throw new IllegalStateException("halfDrawable size must match normalDrawable");
        }
    }

    /**
     * 根据步长修正评分
     */
    private float correctGradeByStep(float grade) {
        return mGradeStep == GradeStep.ONE
                ? (float) Math.round(grade)
                : Math.round(grade * 2) / 2.0f;
    }

    /**
     * 检查是否需要自动调整尺寸
     */
    private void checkAutoSize() {
        if (mAutoSize && mMaxAutoSizeWidth > 0) {
            int availableWidth = mMaxAutoSizeWidth - getPaddingLeft() - getPaddingRight();
            if (availableWidth > 0) {
                int totalSpace = mGradeSpace * (mGradeCount + 1);
                int newWidth = (availableWidth - totalSpace) / mGradeCount;
                if (newWidth > 0) {
                    float aspectRatio = mGradeHeight * 1.0f / mGradeWidth;
                    mGradeWidth = newWidth;
                    mGradeHeight = (int) (newWidth * aspectRatio);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        checkAutoSize();

        if (mGradeWidth <= 0 || mGradeHeight <= 0) {
            int defaultSize = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, DEFAULT_GRADE_SIZE_DP,
                    getResources().getDisplayMetrics());
            mGradeWidth = mGradeHeight = defaultSize;
        }

        int desiredWidth = (mGradeWidth * mGradeCount) + (mGradeSpace * (mGradeCount + 1))
                + getPaddingLeft() + getPaddingRight();
        int desiredHeight = mGradeHeight + getPaddingTop() + getPaddingBottom();

        int minWidth = getSuggestedMinimumWidth();
        int minHeight = getSuggestedMinimumHeight();
        desiredWidth = Math.max(desiredWidth, minWidth);
        desiredHeight = Math.max(desiredHeight, minHeight);

        setMeasuredDimension(
                resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec)
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || !mTouchable) return false;

        float touchX = event.getX();
        float touchY = event.getY();

        // 边界检查
        if (touchX < getPaddingLeft() || touchX > getWidth() - getPaddingRight() ||
                touchY < getPaddingTop() || touchY > getHeight() - getPaddingBottom()) {
            resetTouchState();
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsTouching = true;
                if (mEnablePressedEffect) {
                    mPressedIndex = getStarIndex(touchX);
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (!mIsTouching) break;
                if (mEnablePressedEffect) {
                    int newIndex = getStarIndex(touchX);
                    if (newIndex != mPressedIndex) {
                        mPressedIndex = newIndex;
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!mIsTouching) break;
                handleTouchUp(touchX);
                break;

            case MotionEvent.ACTION_CANCEL:
                resetTouchState();
                break;
        }

        return true;
    }

    /**
     * 处理抬起事件
     */
    private void handleTouchUp(float touchX) {
        float newGrade = calculateGrade(touchX);

        // 点击相同位置的处理
        if (Math.abs(newGrade - mCurrentGrade) < FLOAT_PRECISION) {
            if (mClickToReset) {
                newGrade = mMinimumGrade;
            } else if (mAllowZeroRating) {
                newGrade = decreaseGrade(mCurrentGrade);
            }
        }

        // 更新评分
        if (Math.abs(newGrade - mCurrentGrade) > FLOAT_PRECISION) {
            updateGrade(newGrade, true);
        }

        // 发送点击事件
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
        resetTouchState();
    }

    /**
     * 重置触摸状态
     */
    private void resetTouchState() {
        if (mEnablePressedEffect && mPressedIndex != -1) {
            mPressedIndex = -1;
            invalidate();
        }
        mIsTouching = false;
    }

    /**
     * 获取星星索引
     */
    private int getStarIndex(float touchX) {
        float relativeX = touchX - getPaddingLeft() - mGradeSpace;
        if (relativeX <= 0) return -1;

        int totalWidth = mGradeWidth + mGradeSpace;
        int index = (int) (relativeX / totalWidth);

        return Math.max(0, Math.min(index, mGradeCount - 1));
    }

    /**
     * 计算评分
     */
    private float calculateGrade(float touchX) {
        float relativeX = touchX - getPaddingLeft() - mGradeSpace;
        if (relativeX <= 0) return mMinimumGrade;

        int totalWidth = mGradeWidth + mGradeSpace;
        int starIndex = (int) (relativeX / totalWidth);

        if (starIndex >= mGradeCount) return mGradeCount;

        float position = (relativeX % totalWidth) / mGradeWidth;
        if (position > 1.0f) position = 1.0f;

        float grade;
        if (mGradeStep == GradeStep.ONE) {
            grade = starIndex + 1;
        } else {
            grade = starIndex + (position < 0.5f ? 0.5f : 1.0f);
        }

        return Math.max(mMinimumGrade, Math.min(grade, mGradeCount));
    }

    /**
     * 减少评分
     */
    private float decreaseGrade(float currentGrade) {
        float newGrade = currentGrade - (mGradeStep == GradeStep.ONE ? 1 : 0.5f);

        if (newGrade < FLOAT_PRECISION) {
            return mAllowZeroRating ? 0f : mMinimumGrade;
        }

        return Math.max(newGrade, mMinimumGrade);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float displayGrade = (mRatingAnimator != null && mRatingAnimator.isRunning())
                ? mAnimatedGrade : mCurrentGrade;

        for (int i = 0; i < mGradeCount; i++) {
            int startX = mGradeSpace + (mGradeWidth + mGradeSpace) * i;

            mGradeBounds.set(
                    getPaddingLeft() + startX,
                    getPaddingTop(),
                    getPaddingLeft() + startX + mGradeWidth,
                    getPaddingTop() + mGradeHeight
            );

            drawStar(canvas, i, displayGrade);
        }
    }

    /**
     * 绘制单颗星星
     */
    private void drawStar(Canvas canvas, int index, float displayGrade) {
        Drawable drawable = getStarDrawable(index, displayGrade);
        if (drawable == null) return;

        if (mEnablePressedEffect && mPressedIndex == index) {
            canvas.save();
            canvas.scale(0.9f, 0.9f, mGradeBounds.centerX(), mGradeBounds.centerY());
            drawable.setBounds(mGradeBounds);
            drawable.draw(canvas);
            canvas.restore();
        } else {
            drawable.setBounds(mGradeBounds);
            drawable.draw(canvas);
        }
    }

    /**
     * 获取星星Drawable
     */
    private Drawable getStarDrawable(int index, float displayGrade) {
        if (displayGrade <= index + FLOAT_PRECISION) {
            return mNormalDrawable;
        }

        if (mGradeStep == GradeStep.HALF &&
                Math.abs(displayGrade - (index + 0.5f)) < FLOAT_PRECISION &&
                mHalfDrawable != null) {
            return mHalfDrawable;
        }

        return mFillDrawable;
    }

    /**
     * 启动评分动画
     */
    private void startRatingAnimation(float from, float to) {
        if (!mAnimationEnabled || Math.abs(from - to) < FLOAT_PRECISION) {
            mCurrentGrade = to;
            invalidate();
            return;
        }

        if (mRatingAnimator != null) {
            mRatingAnimator.cancel();
        }

        mRatingAnimator = ValueAnimator.ofFloat(from, to);
        mRatingAnimator.setDuration(mAnimationDuration);
        mRatingAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        mRatingAnimator.addUpdateListener(animation -> {
            mAnimatedGrade = (float) animation.getAnimatedValue();
            invalidate();
        });

        mRatingAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentGrade = mAnimatedGrade;
                notifyRatingChanged(false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentGrade = mAnimatedGrade;
            }
        });

        mRatingAnimator.start();
    }

    /**
     * 更新评分
     */
    private void updateGrade(float grade, boolean isTouch) {
        float oldGrade = mCurrentGrade;

        if (!isTouch && mAnimationEnabled) {
            startRatingAnimation(oldGrade, grade);
        } else {
            mCurrentGrade = grade;
            mAnimatedGrade = grade;
            invalidate();
            notifyRatingChanged(isTouch);
        }
    }

    /**
     * 通知评分变化
     */
    private void notifyRatingChanged(boolean isTouch) {
        if (mListener != null) {
            mListener.onRatingChanged(this, mCurrentGrade, isTouch);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    // ===================== 公开API =====================

    /**
     * 设置星星Drawable（资源ID方式）
     */
    public void setRatingDrawable(@DrawableRes int normalId,
                                  @DrawableRes int halfId,
                                  @DrawableRes int fillId) {
        setRatingDrawable(
                getDrawable(normalId),
                getDrawable(halfId),
                getDrawable(fillId)
        );
    }

    private Drawable getDrawable(@DrawableRes int resId) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), resId);
        if (drawable == null) {
            throw new IllegalStateException("Drawable not found: " + resId);
        }
        return drawable;
    }

    /**
     * 设置星星Drawable（Drawable对象方式）
     */
    public void setRatingDrawable(@Nullable Drawable normal,
                                  @Nullable Drawable half,
                                  @Nullable Drawable fill) {
        if (normal == null || fill == null) {
            throw new IllegalArgumentException("normal and fill drawables cannot be null");
        }

        // 校验尺寸
        if (normal.getIntrinsicWidth() != fill.getIntrinsicWidth() ||
                normal.getIntrinsicHeight() != fill.getIntrinsicHeight()) {
            throw new IllegalArgumentException("normal and fill drawables must have same size");
        }

        if (half != null && (half.getIntrinsicWidth() != normal.getIntrinsicWidth() ||
                half.getIntrinsicHeight() != normal.getIntrinsicHeight())) {
            throw new IllegalArgumentException("half drawable must have same size as normal");
        }

        mNormalDrawable = normal;
        mHalfDrawable = half;
        mFillDrawable = fill;
        mGradeWidth = normal.getIntrinsicWidth();
        mGradeHeight = normal.getIntrinsicHeight();

        requestLayout();
        invalidate();
    }

    /**
     * 获取当前评分
     */
    public float getGrade() {
        return mCurrentGrade;
    }

    /**
     * 设置评分
     */
    public void setGrade(float grade) {
        if (grade < 0 || grade > mGradeCount + FLOAT_PRECISION) {
            throw new IllegalArgumentException("grade must be between 0 and " + mGradeCount);
        }

        if (!mAllowZeroRating && grade < 1 - FLOAT_PRECISION) {
            throw new IllegalArgumentException("grade must be at least 1 when allowZeroRating is false");
        }

        if (mGradeStep == GradeStep.HALF) {
            if (Math.abs(grade * 2 - Math.round(grade * 2)) > FLOAT_PRECISION) {
                throw new IllegalArgumentException("grade must be multiple of 0.5 (half step)");
            }
        } else if (Math.abs(grade - Math.round(grade)) > FLOAT_PRECISION) {
            throw new IllegalArgumentException("grade must be integer (one step)");
        }

        grade = Math.max(grade, mMinimumGrade);

        if (Math.abs(grade - mCurrentGrade) > FLOAT_PRECISION) {
            updateGrade(grade, false);
        }
    }

    /**
     * 获取星星总数
     */
    public int getGradeCount() {
        return mGradeCount;
    }

    /**
     * 设置星星总数
     */
    public void setGradeCount(int count) {
        if (count < 1) throw new IllegalArgumentException("grade count must be at least 1");

        mGradeCount = count;
        float newGrade = Math.min(mCurrentGrade, mGradeCount);
        if (Math.abs(newGrade - mCurrentGrade) > FLOAT_PRECISION) {
            updateGrade(newGrade, false);
        }
        requestLayout();
        invalidate();
    }

    /**
     * 设置星星间距
     */
    public void setGradeSpace(int space) {
        if (space < 0) throw new IllegalArgumentException("grade space cannot be negative");

        mGradeSpace = space;
        requestLayout();
        invalidate();
    }

    /**
     * 设置评分步长
     */
    public void setGradeStep(GradeStep step) {
        if (step == null) throw new NullPointerException("grade step cannot be null");

        if (mGradeStep != step) {
            mGradeStep = step;
            float corrected = correctGradeByStep(mCurrentGrade);
            if (Math.abs(corrected - mCurrentGrade) > FLOAT_PRECISION) {
                updateGrade(corrected, false);
            } else {
                invalidate();
            }
        }
    }

    public GradeStep getGradeStep() {
        return mGradeStep;
    }

    /**
     * 设置是否可触摸
     */
    public void setTouchable(boolean touchable) {
        if (mTouchable != touchable) {
            mTouchable = touchable;
            setClickable(touchable);
            setFocusable(touchable);
        }
    }

    public boolean isTouchable() {
        return mTouchable;
    }

    /**
     * 设置是否启用动画
     */
    public void setAnimationEnabled(boolean enabled) {
        mAnimationEnabled = enabled;
    }

    public boolean isAnimationEnabled() {
        return mAnimationEnabled;
    }

    /**
     * 设置动画时长（毫秒）
     */
    public void setAnimationDuration(long duration) {
        if (duration < 0) throw new IllegalArgumentException("animation duration cannot be negative");
        mAnimationDuration = duration;
    }

    public long getAnimationDuration() {
        return mAnimationDuration;
    }

    /**
     * 设置是否点击重置
     */
    public void setClickToReset(boolean enabled) {
        mClickToReset = enabled;
    }

    public boolean isClickToReset() {
        return mClickToReset;
    }

    /**
     * 设置是否允许0分
     */
    public void setAllowZeroRating(boolean allowZeroRating) {
        mAllowZeroRating = allowZeroRating;
        if (!mAllowZeroRating && mMinimumGrade < 1) {
            mMinimumGrade = 1;
        }
        if (mCurrentGrade < mMinimumGrade - FLOAT_PRECISION) {
            setGrade(mMinimumGrade);
        }
    }

    public boolean isAllowZeroRating() {
        return mAllowZeroRating;
    }

    /**
     * 设置最小评分
     */
    public void setMinimumGrade(float minGrade) {
        if (minGrade < 0 || minGrade > mGradeCount + FLOAT_PRECISION) {
            throw new IllegalArgumentException("minimum grade must be between 0 and " + mGradeCount);
        }
        mMinimumGrade = minGrade;
        if (!mAllowZeroRating && mMinimumGrade < 1) {
            mMinimumGrade = 1;
        }
        if (mCurrentGrade < mMinimumGrade - FLOAT_PRECISION) {
            setGrade(mMinimumGrade);
        }
    }

    public float getMinimumGrade() {
        return mMinimumGrade;
    }

    /**
     * 设置自动调整尺寸
     */
    public void setAutoSize(boolean autoSize, int maxWidth) {
        if (mAutoSize != autoSize || mMaxAutoSizeWidth != maxWidth) {
            mAutoSize = autoSize;
            mMaxAutoSizeWidth = maxWidth;
            requestLayout();
            invalidate();
        }
    }

    /**
     * 设置是否启用按下效果
     */
    public void setEnablePressedEffect(boolean enable) {
        if (mEnablePressedEffect != enable) {
            mEnablePressedEffect = enable;
            invalidate();
        }
    }

    public boolean isEnablePressedEffect() {
        return mEnablePressedEffect;
    }

    /**
     * 设置评分变化监听器
     */
    public void setOnRatingBarChangeListener(OnRatingChangeListener listener) {
        mListener = listener;
    }

    // ===================== 辅助功能 =====================

    /**
     * 设置辅助功能信息
     */
    private void setupAccessibilityInfo(@NonNull AccessibilityNodeInfoCompat info) {
        info.setContentDescription(String.format(
                "评分控件，当前评分%.1f分，满分%d分", mCurrentGrade, mGradeCount));

        info.setClickable(mTouchable);
        info.setFocusable(mTouchable);
        info.setEnabled(isEnabled());

        // 添加自定义操作
        if (mCurrentGrade < mGradeCount - FLOAT_PRECISION) {
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    ACTION_INCREASE_RATING, "增加评分"));
        }

        if (mCurrentGrade > mMinimumGrade + FLOAT_PRECISION) {
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    ACTION_DECREASE_RATING, "减少评分"));
        }

        info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                ACTION_SET_RATING, "设置评分"));
        info.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);

        // 设置范围信息
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            info.setRangeInfo(AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
                    AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT,
                    mMinimumGrade, mGradeCount, mCurrentGrade
            ));
        }
    }

    /**
     * 处理辅助功能操作
     */
    private boolean performAccessibilityActionCompat(int action, Bundle args) {
        if (!isEnabled() || !mTouchable) return false;

        switch (action) {
            case ACTION_INCREASE_RATING:
                float newGrade = Math.min(mCurrentGrade +
                        (mGradeStep == GradeStep.ONE ? 1 : 0.5f), mGradeCount);
                setGrade(newGrade);
                return true;

            case ACTION_DECREASE_RATING:
                newGrade = Math.max(mCurrentGrade -
                        (mGradeStep == GradeStep.ONE ? 1 : 0.5f), mMinimumGrade);
                setGrade(newGrade);
                return true;

            case ACTION_SET_RATING:
                if (args != null && args.containsKey("rating")) {
                    setGrade(args.getFloat("rating", mCurrentGrade));
                } else {
                    simulateClick();
                }
                return true;

            case AccessibilityNodeInfoCompat.ACTION_CLICK:
                simulateClick();
                return true;
        }

        return false;
    }

    /**
     * 模拟点击
     */
    private void simulateClick() {
        float newGrade = mCurrentGrade > mMinimumGrade + FLOAT_PRECISION
                ? mMinimumGrade : Math.min(mCurrentGrade + 1, mGradeCount);
        setGrade(newGrade);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
    }

    // ===================== 生命周期 =====================

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mRatingAnimator != null) {
            mRatingAnimator.cancel();
            mRatingAnimator.removeAllListeners();
            mRatingAnimator.removeAllUpdateListeners();
            mRatingAnimator = null;
        }

        mListener = null;

        // 清理Drawable回调
        setDrawableCallback(mNormalDrawable, null);
        setDrawableCallback(mHalfDrawable, null);
        setDrawableCallback(mFillDrawable, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // 设置Drawable回调
        setDrawableCallback(mNormalDrawable, this);
        setDrawableCallback(mHalfDrawable, this);
        setDrawableCallback(mFillDrawable, this);
    }

    private void setDrawableCallback(Drawable drawable, Drawable.Callback callback) {
        if (drawable != null) {
            drawable.setCallback(callback);
        }
    }

    // ===================== 枚举和接口 =====================

    /**
     * 评分步长
     */
    public enum GradeStep {
        HALF, ONE
    }

    /**
     * 评分变化监听器
     */
    public interface OnRatingChangeListener {
        void onRatingChanged(SimpleRatingBar ratingBar, float grade, boolean touch);
    }
}