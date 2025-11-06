package com.linx.mylibrary.utils.rvItemDecoration;

import android.content.res.Resources;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GridSpaceItemDecorationSame extends RecyclerView.ItemDecoration {
    private int spacing; // 统一的间距（dp转px后的值，行、列间距相同）
    private int spanCount; // GridLayoutManager的列数

    // 构造方法：传入统一的间距（dp）和列数
    public GridSpaceItemDecorationSame(int spacingDp, int spanCount) {
        this.spacing = dp2px(spacingDp); // 转换为px
        this.spanCount = spanCount;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        // 计算当前条目所在的列索引（0-based，从左到右）
        int column = position % spanCount;
        // 计算当前条目所在的行索引（0-based，从上到下）
        int row = position / spanCount;

        // 重置所有方向间距
        outRect.set(0, 0, 0, 0);

        // 水平方向（列间距）：除最后一列，其他列右侧添加间距
        // 相邻列之间的间距 = spacing
        if (column < spanCount - 1) {
            outRect.right = spacing; // 非最后一列，右侧留间距
        }

        // 垂直方向（行间距）：除第一行，其他行顶部添加间距
        // 相邻行之间的间距 = spacing
        if (row > 0) {
            outRect.top = spacing; // 非第一行，顶部留间距
        }
    }

    // dp转px工具方法（更准确的实现）
    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                Resources.getSystem().getDisplayMetrics()
        );
    }
}