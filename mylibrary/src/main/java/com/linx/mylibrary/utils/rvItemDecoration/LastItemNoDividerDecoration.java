package com.linx.mylibrary.utils.rvItemDecoration;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LastItemNoDividerDecoration extends RecyclerView.ItemDecoration {
    private final Drawable divider;
    private int orientation;

    // 构造方法：传入上下文、分割线drawable、布局方向（竖向）
    public LastItemNoDividerDecoration(Context context, int dividerResId, int orientation) {
        this.divider = ContextCompat.getDrawable(context, dividerResId);
        this.orientation = orientation;
        if (this.divider == null) {
            throw new IllegalArgumentException("请设置有效的分割线drawable资源");
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (orientation == LinearLayoutManager.VERTICAL) {
            drawVerticalDividers(c, parent);
        }
    }

    // 绘制竖向分割线（只绘制到倒数第二项）
    private void drawVerticalDividers(Canvas c, RecyclerView parent) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        // 遍历所有item，但最后一个item不绘制分割线
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            // 计算分割线的位置（item底部）
            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + divider.getIntrinsicHeight();

            divider.setBounds(left, top, right, bottom);
            divider.draw(c);
        }
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        // 最后一个item不预留分割线空间
        int position = parent.getChildAdapterPosition(view);
        if (position == parent.getAdapter().getItemCount() - 1) {
            outRect.set(0, 0, 0, 0);
        } else {
            // 其他item预留分割线高度的空间
            if (orientation == LinearLayoutManager.VERTICAL) {
                outRect.set(0, 0, 0, divider.getIntrinsicHeight());
            }
        }
    }
}