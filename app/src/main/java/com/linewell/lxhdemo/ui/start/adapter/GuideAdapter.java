package com.linewell.lxhdemo.ui.start.adapter;
import android.annotation.SuppressLint;

import androidx.appcompat.widget.AppCompatImageView;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.linewell.lxhdemo.R;
public class GuideAdapter extends BaseQuickAdapter<Integer, BaseViewHolder> {
    public GuideAdapter() {
        super(R.layout.item_guide);
    }

    @Override
    protected void convert(BaseViewHolder helper, Integer item) {
        AppCompatImageView mImageView = helper.getView(R.id.iv_guide_image);
        mImageView.setImageResource(item);
    }

}
