package com.linx.mylibrary.view.webView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ViewCompat;

/**
 *    desc   : 支持嵌套滚动的 WebView
 */
public class NestedScrollWebView extends WebView implements NestedScrollingChild {

    private NestedScrollingChildHelper mChildHelper;
    private int mLastMotionY;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedOffsetY;
    private boolean mChange;


    // 统一初始化方法（修复问题2）
    private void init() {
        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    public NestedScrollWebView(Context context) {
        super(context);
        init();
    }

    public NestedScrollWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public NestedScrollWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();

    }
    public NestedScrollWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = false;
        // 标记是否需要回收trackedEvent
        boolean needRecycle = false;
        MotionEvent trackedEvent = null;

        try {
            trackedEvent = MotionEvent.obtain(event);
            needRecycle = true;

            final int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN) {
                mNestedOffsetY = 0;
            }

            int y = (int) event.getY();
            event.offsetLocation(0, mNestedOffsetY);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mChange = false;
                    mLastMotionY = y;
                    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                    result = super.onTouchEvent(event);
                    break;

                case MotionEvent.ACTION_MOVE:
                    int deltaY = mLastMotionY - y;

                    // 分发嵌套预滚动
                    if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                        deltaY -= mScrollConsumed[1];
                        trackedEvent.offsetLocation(0, mScrollOffset[1]);
                        mNestedOffsetY += mScrollOffset[1];
                    }

                    mLastMotionY = y - mScrollOffset[1];

                    // 计算WebView自身的滚动消耗
                    int oldY = getScrollY();
                    int newScrollY = Math.max(0, oldY + deltaY);
                    int dyConsumed = newScrollY - oldY;
                    int dyUnconsumed = deltaY - dyConsumed;

                    // 分发嵌套滚动
                    if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, mScrollOffset)) {
                        mLastMotionY -= mScrollOffset[1];
                        trackedEvent.offsetLocation(0, mScrollOffset[1]);
                        mNestedOffsetY += mScrollOffset[1];
                    }

                    // 优化事件处理逻辑（修复问题3）
                    if (mScrollConsumed[1] == 0 && mScrollOffset[1] == 0) {
                        if (mChange) {
                            mChange = false;
                            trackedEvent.setAction(MotionEvent.ACTION_DOWN);
                            result = super.onTouchEvent(trackedEvent);
                        } else {
                            result = super.onTouchEvent(trackedEvent);
                        }
                    } else {
                        if (!mChange) {
                            mChange = true;
                            // 修复问题1：回收ACTION_CANCEL的MotionEvent
                            MotionEvent cancelEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
                            super.onTouchEvent(cancelEvent);
                            cancelEvent.recycle(); // 手动回收
                        }
                    }
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopNestedScroll();
                    // 处理Fling事件（修复问题4：支持嵌套Fling）
                    if (action == MotionEvent.ACTION_UP) {
                        // 可根据需要添加fling速度计算，这里简化处理
                        dispatchNestedFling(0, -mLastMotionY, false);
                    }
                    result = super.onTouchEvent(event);
                    break;

                default:
                    result = super.onTouchEvent(event);
                    break;
            }
        } finally {
            // 修复问题1：确保MotionEvent被回收
            if (needRecycle && trackedEvent != null) {
                trackedEvent.recycle();
            }
        }

        return result;
    }

    /**
     * {@link NestedScrollingChild} 接口实现
     */
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }
}