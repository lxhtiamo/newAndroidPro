package com.linx.mylibrary.utils.rvItemDecoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int verticalSpaceHeight;
    private final boolean includeEdge; // 是否在顶部和底部也添加间隔

    public VerticalSpaceItemDecoration(android.content.Context context, int verticalSpaceHeight) {

        this(context, verticalSpaceHeight, false);
    }

    public VerticalSpaceItemDecoration(android.content.Context context, int verticalSpaceHeight, boolean includeEdge) {

        this.verticalSpaceHeight = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                verticalSpaceHeight,
                context.getResources().getDisplayMetrics());
        this.includeEdge = includeEdge;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        int itemCount = parent.getAdapter().getItemCount();

        // 如果需要包含边缘，第一个item添加顶部间隔
        if (includeEdge && position == 0) {
            outRect.top = verticalSpaceHeight;
        }

        // 最后一个item是否添加底部间隔，根据includeEdge决定
        if (!includeEdge && position == itemCount - 1) {
            outRect.bottom = 0;
        } else {
            outRect.bottom = verticalSpaceHeight;
        }
    }
}