package com.linewell.lxhdemo.ui.start.activity;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import androidx.appcompat.widget.AppCompatButton;
import androidx.viewpager2.widget.ViewPager2;

import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.base.BaseActivity;
import com.linewell.lxhdemo.base.aop.SingleClick;
import com.linewell.lxhdemo.ui.home.activity.HomeActivity;
import com.linewell.lxhdemo.ui.start.adapter.GuideAdapter;

import java.util.ArrayList;

import me.relex.circleindicator.CircleIndicator3;

public class GuideActivity extends BaseActivity {
    private ViewPager2 mViewPager;
    private CircleIndicator3 mIndicatorView;
    private AppCompatButton mCompleteView;
    GuideAdapter mGuideAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_guide;
    }

    @Override
    protected void getBundleExtras(Bundle extras) {

    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mViewPager = findViewById(R.id.vp_guide_pager);
        mIndicatorView = findViewById(R.id.cv_guide_indicator);
        mCompleteView = findViewById(R.id.btn_guide_complete);
        setOnClickListener(mCompleteView);
    }

    @SingleClick
    @Override
    public void onClick(View view) {
        if (view == mCompleteView) {
            showToast("跳转到首页");
            readyGoThenFinish(HomeActivity.class);
        }
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        ArrayList<Integer> imgs = new ArrayList<>();
        mGuideAdapter = new GuideAdapter();
        imgs.add(R.drawable.guide_1_bg);
        imgs.add(R.drawable.guide_2_bg);
        imgs.add(R.drawable.guide_3_bg);
        mGuideAdapter.setNewData(imgs);
        mViewPager.setAdapter(mGuideAdapter);
        mViewPager.registerOnPageChangeCallback(onPageChangeCallback);
        mIndicatorView.setViewPager(mViewPager);

    }

    ViewPager2.OnPageChangeCallback onPageChangeCallback = new ViewPager2.OnPageChangeCallback() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (mViewPager.getCurrentItem() != mGuideAdapter.getItemCount() - 1 || positionOffsetPixels <= 0) {
                return;
            }

            mIndicatorView.setVisibility(View.VISIBLE);
            mCompleteView.setVisibility(View.INVISIBLE);
            mCompleteView.clearAnimation();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state != ViewPager2.SCROLL_STATE_IDLE) {
                return;
            }

            boolean lastItem = mViewPager.getCurrentItem() == mGuideAdapter.getItemCount() - 1;
            mIndicatorView.setVisibility(lastItem ? View.INVISIBLE : View.VISIBLE);
            mCompleteView.setVisibility(lastItem ? View.VISIBLE : View.INVISIBLE);
            if (lastItem) {
                // 按钮呼吸动效
                ScaleAnimation animation = new ScaleAnimation(1.0f, 1.1f, 1.0f, 1.1f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                animation.setDuration(350);
                animation.setRepeatMode(Animation.REVERSE);
                animation.setRepeatCount(Animation.INFINITE);
                mCompleteView.startAnimation(animation);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mViewPager != null && onPageChangeCallback != null) {
            mViewPager.unregisterOnPageChangeCallback(onPageChangeCallback);
        }
    }
}
