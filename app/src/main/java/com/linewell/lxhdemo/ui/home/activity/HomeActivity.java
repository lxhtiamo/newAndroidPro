package com.linewell.lxhdemo.ui.home.activity;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.base.BaseActivity;
import com.linewell.lxhdemo.base.BaseFragmentPagerAdapter;
import com.linewell.lxhdemo.ui.home.adapter.NavigationAdapter;
import com.linewell.lxhdemo.ui.home.bean.NavigationItem;
import com.linewell.lxhdemo.ui.home.fragment.HomeFragment;
import com.linewell.lxhdemo.widget.NoScrollViewPager;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends BaseActivity {
    private String[] mTitles = {"首页", "消息", "联系人", "更多"};
    private int[] mIconSelectIds = {
            R.mipmap.tab_home_select, R.mipmap.tab_speech_select,
            R.mipmap.tab_contact_select, R.mipmap.tab_more_select};
    /**
     * 默认非选中图标图标
     */
    private int[] mIconUnselectIds = {
            R.mipmap.tab_home_unselect, R.mipmap.tab_speech_unselect,
            R.mipmap.tab_contact_unselect, R.mipmap.tab_more_unselect};
    private BaseFragmentPagerAdapter<Fragment> mPagerAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_home;
    }

    @Override
    protected void getBundleExtras(Bundle extras) {

    }

    NoScrollViewPager mViewPager;
    RecyclerView mRvNavigationView;

    @Override
    protected void initView(Bundle savedInstanceState) {
        mViewPager = findViewById(R.id.vp_home_pager);
        mRvNavigationView = findViewById(R.id.rv_home_navigation);
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        List<Fragment> list = new ArrayList<>();
        list.add(new HomeFragment());
        list.add(new HomeFragment());
        list.add(new HomeFragment());
        list.add(new HomeFragment());
        mPagerAdapter = new BaseFragmentPagerAdapter<>(getSupportFragmentManager(), list);
        mViewPager.setAdapter(mPagerAdapter);
        List<NavigationItem> navigationList = new ArrayList<>();
        for (int i = 0; i < mTitles.length; i++) {
            NavigationItem navigationItem = new NavigationItem();
            navigationItem.setText(mTitles[i]);
            navigationItem.setSelectedImg(mIconSelectIds[i]);
            navigationItem.setNotSelectedImg(mIconUnselectIds[i]);
            navigationList.add(navigationItem);
        }
        mViewPager.setCurrentItem(0);
        NavigationAdapter navigationAdapter = new NavigationAdapter();
        navigationAdapter.setNewData(navigationList);
        mRvNavigationView.setLayoutManager(new GridLayoutManager(getContext(), list.size()));
        mRvNavigationView.setAdapter(navigationAdapter);

        navigationAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                mViewPager.setCurrentItem(position);
                navigationAdapter.setSelectedPosition(position);
            }
        });
    }
}
