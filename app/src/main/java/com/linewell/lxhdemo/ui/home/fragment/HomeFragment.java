package com.linewell.lxhdemo.ui.home.fragment;


import android.util.Log;

import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.base.BaseFragment;

public class HomeFragment extends BaseFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home;
    }

    @Override
    protected void initView() {
        Log.d("HomeFragment", "onCreateView: position=");
    }

    @Override
    protected void initData() {

    }
}
