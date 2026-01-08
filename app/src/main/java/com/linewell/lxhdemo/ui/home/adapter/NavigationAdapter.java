package com.linewell.lxhdemo.ui.home.adapter;

import android.annotation.SuppressLint;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.ui.home.bean.NavigationItem;

public class NavigationAdapter extends BaseQuickAdapter<NavigationItem, BaseViewHolder> {

    /**
     * 当前选中条目位置
     */
    private int mSelectedPosition = 0;

    public NavigationAdapter() {
        super(R.layout.item_home_navigation);
    }

    @Override
    protected void convert(BaseViewHolder helper, NavigationItem item) {
        TextView mTextView = helper.getView(R.id.tv_home_navigation_title);
        AppCompatImageView mImageView = helper.getView(R.id.iv_home_navigation_icon);
        mTextView.setText(item.getText());
        if (helper.getLayoutPosition() == mSelectedPosition) {
            mImageView.setImageResource(item.getSelectedImg());
        } else {
            mImageView.setImageResource(item.getNotSelectedImg());
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    public void setSelectedPosition(int position) {
        mSelectedPosition = position;
        notifyDataSetChanged();
    }
}
