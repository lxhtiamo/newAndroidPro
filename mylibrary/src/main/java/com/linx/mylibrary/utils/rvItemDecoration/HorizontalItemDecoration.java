package com.linx.mylibrary.utils.rvItemDecoration;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView 横向滚动时的 item 间距装饰类
 */
public class HorizontalItemDecoration extends RecyclerView.ItemDecoration {
    private final int spacing; // 间距（单位：px）

    /**
     * 构造方法
     * @param spacingDp 间距（单位：dp），会自动转为当前设备的像素
     * @param context 上下文（用于获取屏幕密度）
     */
    public HorizontalItemDecoration(Context context, int spacingDp) {
        // 将 dp 转为像素（适配不同屏幕密度）
        this.spacing = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                spacingDp,
                context.getResources().getDisplayMetrics()
        );
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        // 获取当前 item 的位置
        int position = parent.getChildAdapterPosition(view);
        // 获取 item 总数
        int itemCount = state.getItemCount();

        // 横向滚动时，控制左右间距（垂直方向间距设为0）
        outRect.top = 0;
        outRect.bottom = 0;

        // 逻辑：所有 item 右侧都添加 spacing 间距，最后一个 item 右侧不加（避免右侧多一个间距）
        if (position != itemCount - 1) {
            outRect.right = spacing; // 非最后一个 item，右侧加间距
        } else {
            outRect.right = 0; // 最后一个 item，右侧不加间距
        }

        // 可选：第一个 item 左侧加间距（根据需求决定是否需要）
        // if (position == 0) {
        //     outRect.left = spacing;
        // } else {
        //     outRect.left = 0;
        // }
    }
}
