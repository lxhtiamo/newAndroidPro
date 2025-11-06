package com.linx.mylibrary.utils.rvItemDecoration;

import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GridSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private int rowSpacing; // 行之间的间距（dp转px后的值）
    private int spanCount;  // GridLayoutManager的列数

    public GridSpaceItemDecoration(int rowSpacingDp, int spanCount) {
        this.rowSpacing = dp2px(rowSpacingDp);
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

        // 计算当前条目所在的行索引（从0开始）
        int rowIndex = position / spanCount;

        // 重置所有方向间距
        outRect.set(0, 0, 0, 0);

        // 核心修改：仅第一行顶部无间距，其他所有行（包括最后一行）顶部都添加间距
        // 这样即使最后一行只有1个条目，也会与上一行保持间距
        if (rowIndex > 0) {
            // 非第一行：顶部添加行间距
            outRect.top = rowSpacing;
        }
        // 所有行底部都不添加间距，避免最后一行底部出现多余间隔
    }

    // dp转px工具方法
    private int dp2px(int dp) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
