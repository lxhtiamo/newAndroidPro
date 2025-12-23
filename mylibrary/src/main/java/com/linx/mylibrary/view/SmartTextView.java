package com.linx.mylibrary.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 *    desc   : 自动显示和隐藏的 TextView
 */
public final class SmartTextView extends AppCompatTextView {

    private boolean mAutoVisibility = true;

    public SmartTextView(Context context) {
        this(context, null);
    }

    public SmartTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public SmartTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        refreshVisibilityStatus();
    }

    @Override
    public void setVisibility(int visibility) {
        // 当用户手动设置可见性时，禁用自动可见性控制
        if (visibility == VISIBLE || visibility == INVISIBLE || visibility == GONE) {
            mAutoVisibility = false;
        }
        super.setVisibility(visibility);
    }

    /**
     * 启用/禁用自动可见性控制
     */
    public void setAutoVisibilityEnabled(boolean enabled) {
        mAutoVisibility = enabled;
        if (enabled) {
            refreshVisibilityStatus();
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        // 如果文本内容相同，避免不必要的刷新
        if (TextUtils.equals(getText(), text)) {
            return;
        }
        super.setText(text, type);
        refreshVisibilityStatus();
    }

    @Override
    public void setCompoundDrawables(@Nullable Drawable left, @Nullable Drawable top,
                                     @Nullable Drawable right, @Nullable Drawable bottom) {
        super.setCompoundDrawables(left, top, right, bottom);
        refreshVisibilityStatus();
    }

    @Override
    public void setCompoundDrawablesRelative(@Nullable Drawable start, @Nullable Drawable top,
                                             @Nullable Drawable end, @Nullable Drawable bottom) {
        super.setCompoundDrawablesRelative(start, top, end, bottom);
        refreshVisibilityStatus();
    }

    /**
     * 刷新当前可见状态
     */
    private void refreshVisibilityStatus() {
        if (!mAutoVisibility) {
            return; // 如果禁用了自动控制，则不执行
        }

        if (isEmptyContent()) {
            if (getVisibility() != GONE) {
                super.setVisibility(GONE);
            }
        } else {
            if (getVisibility() != VISIBLE) {
                super.setVisibility(VISIBLE);
            }
        }
    }

    /**
     * TextView 内容是否为空
     */
    private boolean isEmptyContent() {
        // 如果有文本，不为空
        if (!TextUtils.isEmpty(getText())) {
            return false;
        }

        // 检查 Drawables
        Drawable[] compoundDrawables = getCompoundDrawables();
        for (Drawable drawable : compoundDrawables) {
            if (drawable != null) {
                return false;
            }
        }

        Drawable[] compoundDrawablesRelative = getCompoundDrawablesRelative();
        for (Drawable drawable : compoundDrawablesRelative) {
            if (drawable != null) {
                return false;
            }
        }

        return true;
    }

    /**
     * 清除所有内容（包括文本、Hint、Drawable）并隐藏
     */
    public void clearAndHide() {
        setText("");
        setHint("");
        setCompoundDrawables(null, null, null, null);
        setCompoundDrawablesRelative(null, null, null, null);
        refreshVisibilityStatus();
    }
}