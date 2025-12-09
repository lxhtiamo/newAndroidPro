package com.linewell.lxhdemo.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ViewPager2 专用 Fragment 适配器（替代 FragmentPagerAdapter）
 * 特性：支持增删改查、批量添加、无重复判断、懒加载回调
 */
public final class BaseFragmentStateAdapter<F extends Fragment> extends FragmentStateAdapter {
    // 懒加载回调（供 Fragment 实现）
    public interface LazyLoadCallback {
        void onLazyLoad();
    }

    /** Fragment 集合 */
    private final List<F> mFragmentList = new ArrayList<>();
    /** 标题集合 */
    private final List<CharSequence> mTitleList = new ArrayList<>();
    /** 当前显示的 Fragment */
    @Nullable
    private F mCurrentFragment;

    // ========== 构造器 ==========
    public BaseFragmentStateAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    public BaseFragmentStateAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    public BaseFragmentStateAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    /** 构造器：初始化 Fragment 列表 + 标题列表 */
    public BaseFragmentStateAdapter(@NonNull FragmentActivity activity, @NonNull List<F> fragmentList) {
        this(activity, fragmentList, null);
    }

    public BaseFragmentStateAdapter(@NonNull FragmentActivity activity, @NonNull List<F> fragmentList, @Nullable List<CharSequence> titleList) {
        super(activity);
        initFragments(fragmentList, titleList);
    }

    public BaseFragmentStateAdapter(@NonNull Fragment fragment, @NonNull List<F> fragmentList) {
        this(fragment, fragmentList, null);
    }

    public BaseFragmentStateAdapter(@NonNull Fragment fragment, @NonNull List<F> fragmentList, @Nullable List<CharSequence> titleList) {
        super(fragment);
        initFragments(fragmentList, titleList);
    }

    // ========== 初始化逻辑 ==========
    private void initFragments(@NonNull List<F> fragmentList, @Nullable List<CharSequence> titleList) {
        // 无重复判断，直接添加所有 Fragment
        mFragmentList.addAll(fragmentList);
        // 初始化标题列表
        if (titleList != null && !titleList.isEmpty()) {
            for (int i = 0; i < mFragmentList.size(); i++) {
                mTitleList.add(i < titleList.size() ? titleList.get(i) : null);
            }
        } else {
            mTitleList.addAll(Collections.nCopies(mFragmentList.size(), null));
        }
    }

    // ========== 核心方法 ==========
    @NonNull
    @Override
    public F createFragment(int position) {
        F fragment = mFragmentList.get(position);
        // 标记当前 Fragment（供懒加载回调）
        mCurrentFragment = fragment;
        // 触发懒加载（ViewPager2 懒加载特性：只有当前页会执行到这里）
        if (fragment instanceof LazyLoadCallback) {
            ((LazyLoadCallback) fragment).onLazyLoad();
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return mFragmentList.size();
    }

    /**
     * 优化 ItemId：避免 Fragment 复用错误（每个实例唯一）
     */
    @Override
    public long getItemId(int position) {
        F fragment = mFragmentList.get(position);
        // 按实例 + 位置生成唯一 Id（避免同类型多实例复用错误）
        return Objects.hash(System.identityHashCode(fragment), position);
    }

    /**
     * 检查 ItemId 是否对应位置（解决刷新不生效问题）
     */
    @Override
    public boolean containsItem(long itemId) {
        for (int i = 0; i < mFragmentList.size(); i++) {
            if (getItemId(i) == itemId) {
                return true;
            }
        }
        return false;
    }

    // ========== 增删改查 API ==========
    /** 添加单个 Fragment */
    public void addFragment(@NonNull F fragment) {
        addFragment(fragment, null);
    }

    public void addFragment(@NonNull F fragment, @Nullable CharSequence title) {
        mFragmentList.add(fragment);
        mTitleList.add(title);
        notifyItemInserted(mFragmentList.size() - 1);
    }

    /** 批量添加 Fragment */
    public void addFragments(@NonNull List<F> fragmentList) {
        addFragments(fragmentList, null);
    }

    public void addFragments(@NonNull List<F> fragmentList, @Nullable List<CharSequence> titleList) {
        if (fragmentList.isEmpty()) {
            return;
        }
        int startPos = mFragmentList.size();
        mFragmentList.addAll(fragmentList);
        // 补充标题
        if (titleList != null && !titleList.isEmpty()) {
            for (int i = 0; i < fragmentList.size(); i++) {
                mTitleList.add(startPos + i < titleList.size() ? titleList.get(startPos + i) : null);
            }
        } else {
            mTitleList.addAll(Collections.nCopies(fragmentList.size(), null));
        }
        notifyItemRangeInserted(startPos, fragmentList.size());
    }

    /** 移除指定位置的 Fragment */
    public void removeFragment(int position) {
        if (position < 0 || position >= mFragmentList.size()) {
            return;
        }
        // 移除并销毁 Fragment（避免内存泄漏）
        F fragment = mFragmentList.remove(position);
        mTitleList.remove(position);
        destroyFragment(fragment);
        notifyItemRemoved(position);
        // 刷新后续位置的 ItemId
        notifyItemRangeChanged(position, mFragmentList.size() - position);
    }

    /** 替换指定位置的 Fragment */
    public void replaceFragment(int position, @NonNull F newFragment) {
        replaceFragment(position, newFragment, null);
    }

    public void replaceFragment(int position, @NonNull F newFragment, @Nullable CharSequence title) {
        if (position < 0 || position >= mFragmentList.size()) {
            return;
        }
        // 销毁旧 Fragment
        F oldFragment = mFragmentList.get(position);
        destroyFragment(oldFragment);
        // 替换新 Fragment
        mFragmentList.set(position, newFragment);
        mTitleList.set(position, title);
        notifyItemChanged(position);
    }

    /** 清空所有 Fragment */
    public void clearFragments() {
        // 销毁所有 Fragment
        for (F fragment : mFragmentList) {
            destroyFragment(fragment);
        }
        mFragmentList.clear();
        mTitleList.clear();
        mCurrentFragment = null;
        notifyDataSetChanged();
    }

    // ========== 工具方法 ==========
    /** 获取指定位置的 Fragment */
    @Nullable
    public F getFragment(int position) {
        if (position < 0 || position >= mFragmentList.size()) {
            return null;
        }
        return mFragmentList.get(position);
    }

    /** 获取当前显示的 Fragment */
    @Nullable
    public F getCurrentFragment() {
        return mCurrentFragment;
    }

    /** 获取指定类的第一个 Fragment 索引 */
    public int getFragmentIndex(@Nullable Class<? extends Fragment> clazz) {
        if (clazz == null) return -1;
        for (int i = 0; i < mFragmentList.size(); i++) {
            if (clazz.isInstance(mFragmentList.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /** 获取标题列表 */
    @NonNull
    public List<CharSequence> getTitleList() {
        return new ArrayList<>(mTitleList);
    }

    /** 获取指定位置的标题 */
    @Nullable
    public CharSequence getTitle(int position) {
        if (position < 0 || position >= mTitleList.size()) {
            return null;
        }
        return mTitleList.get(position);
    }

    // ========== 内部逻辑 ==========
    /** 销毁 Fragment（释放资源） */
    private void destroyFragment(@NonNull F fragment) {
        FragmentManager fm = fragment.getParentFragmentManager();
        if (!fm.isDestroyed() && !fm.isStateSaved()) {
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.remove(fragment);
            try {
                transaction.commitNow();
            } catch (IllegalStateException e) {
                transaction.commitAllowingStateLoss();
            }
        }
    }
}