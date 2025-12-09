package com.linewell.lxhdemo.base;

import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *    desc   :ViewPager FragmentPagerAdapter 通用封装（移除重复判断，支持同类型多实例）
 *    核心修改：删除所有重复检测逻辑，允许添加多个同类型Fragment实例
 */
public final class BaseFragmentPagerAdapter<F extends Fragment> extends FragmentPagerAdapter {

    // 日志标签
    private static final String TAG = "BaseFragmentPagerAdapter";
    // 懒加载阈值：超过该数量时，不再设置offscreenPageLimit=总数量
    private static final int LAZY_THRESHOLD = 3;

    /** 核心修复：保存FragmentManager引用 */
    @NonNull
    private final FragmentManager mFragmentManager;
    /** Fragment 集合（私有化） */
    private final List<F> mFragmentList = new ArrayList<>();
    /** Fragment 标题集合 */
    private final List<CharSequence> mFragmentTitleList = new ArrayList<>();
    /** 当前显示的Fragment */
    @Nullable
    private F mCurrentFragment;
    /** 绑定的ViewPager */
    @Nullable
    private ViewPager mViewPager;
    /** 懒加载模式开关 */
    private boolean mIsLazyMode = true;
    /** 懒加载阈值（可自定义） */
    private int mLazyThreshold = LAZY_THRESHOLD;

    // 懒加载回调（供Fragment实现）
    public interface LazyLoadCallback {
        void onLazyLoad();
    }

    // ========== 原有构造器 ==========
    public BaseFragmentPagerAdapter(@NonNull FragmentActivity activity) {
        this(activity.getSupportFragmentManager());
    }

    public BaseFragmentPagerAdapter(@NonNull Fragment fragment) {
        this(fragment.getChildFragmentManager());
    }

    public BaseFragmentPagerAdapter(@NonNull FragmentManager manager) {
        super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.mFragmentManager = manager;
    }

    // ========== 新增：支持列表初始化的构造器（移除重复过滤） ==========
    /**
     * 构造器：FragmentActivity + Fragment列表
     */
    public BaseFragmentPagerAdapter(@NonNull FragmentActivity activity, @NonNull List<F> fragmentList) {
        this(activity.getSupportFragmentManager(), fragmentList);
    }

    /**
     * 构造器：FragmentActivity + Fragment列表 + 标题列表
     */
    public BaseFragmentPagerAdapter(@NonNull FragmentActivity activity, @NonNull List<F> fragmentList, @Nullable List<CharSequence> titleList) {
        this(activity.getSupportFragmentManager(), fragmentList, titleList);
    }

    /**
     * 构造器：Fragment + Fragment列表
     */
    public BaseFragmentPagerAdapter(@NonNull Fragment fragment, @NonNull List<F> fragmentList) {
        this(fragment.getChildFragmentManager(), fragmentList);
    }

    /**
     * 构造器：Fragment + Fragment列表 + 标题列表
     */
    public BaseFragmentPagerAdapter(@NonNull Fragment fragment, @NonNull List<F> fragmentList, @Nullable List<CharSequence> titleList) {
        this(fragment.getChildFragmentManager(), fragmentList, titleList);
    }

    /**
     * 构造器：FragmentManager + Fragment列表
     */
    public BaseFragmentPagerAdapter(@NonNull FragmentManager manager, @NonNull List<F> fragmentList) {
        this(manager, fragmentList, null);
    }

    /**
     * 构造器：FragmentManager + Fragment列表 + 标题列表（核心修改：移除重复过滤）
     */
    public BaseFragmentPagerAdapter(@NonNull FragmentManager manager, @NonNull List<F> fragmentList, @Nullable List<CharSequence> titleList) {
        super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.mFragmentManager = manager;

        // 【核心修改】直接添加所有Fragment，不再过滤重复
        mFragmentList.addAll(fragmentList);

        // 初始化标题列表（不再关联重复判断）
        if (titleList != null && !titleList.isEmpty()) {
            for (int i = 0; i < mFragmentList.size(); i++) {
                mFragmentTitleList.add(i < titleList.size() ? titleList.get(i) : null);
            }
        } else {
            mFragmentTitleList.addAll(Collections.nCopies(mFragmentList.size(), null));
        }

        refreshOffscreenPageLimit();
    }

    @NonNull
    @Override
    public F getItem(int position) {
        return mFragmentList.get(position);
    }

    /**
     * 优化ItemId：避免Fragment列表变化时复用错误实例
     * 【可选优化】如果需要更精准的ItemId，可结合Fragment的唯一标识（比如Tag）
     */
    @Override
    public long getItemId(int position) {
        F fragment = mFragmentList.get(position);
        // 保留原有逻辑，或改为：Objects.hash(fragment, position)（按实例+位置）
        return Objects.hash(fragment.getClass().getName(), position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return position < mFragmentTitleList.size() ? mFragmentTitleList.get(position) : null;
    }

    // ======================== 扩展：增删改查 API（移除重复判断） ========================
    /**
     * 添加Fragment（【核心修改】移除重复判断）
     */
    public void addFragment(@NonNull F fragment) {
        addFragment(fragment, null);
    }

    public void addFragment(@NonNull F fragment, @Nullable CharSequence title) {
        // 【删除】原有重复判断逻辑
        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);
        notifyDataSetChanged();
        refreshOffscreenPageLimit();
    }

    /**
     * 批量添加Fragment（【核心修改】移除重复判断）
     */
    public void addFragments(@NonNull List<F> fragmentList) {
        addFragments(fragmentList, null);
    }

    public void addFragments(@NonNull List<F> fragmentList, @Nullable List<CharSequence> titleList) {
        if (fragmentList.isEmpty()) {
            return;
        }

        // 【核心修改】直接添加所有Fragment，不再过滤重复
        mFragmentList.addAll(fragmentList);

        // 处理标题列表
        int addedCount = fragmentList.size();
        if (titleList != null && !titleList.isEmpty()) {
            int startIndex = mFragmentTitleList.size();
            for (int i = 0; i < addedCount; i++) {
                mFragmentTitleList.add(startIndex + i < titleList.size() ? titleList.get(startIndex + i) : null);
            }
        } else {
            mFragmentTitleList.addAll(Collections.nCopies(addedCount, null));
        }

        notifyDataSetChanged();
        refreshOffscreenPageLimit();
    }

    /**
     * 移除指定位置的Fragment
     */
    public void removeFragment(int position) {
        if (position < 0 || position >= mFragmentList.size()) {
            return;
        }
        // 移除前销毁Fragment（避免内存泄漏）
        F fragment = mFragmentList.remove(position);
        mFragmentTitleList.remove(position);

        if (!mFragmentManager.isDestroyed() && !mFragmentManager.isStateSaved()) {
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.remove(fragment);
            try {
                transaction.commitNow();
            } catch (IllegalStateException e) {
                transaction.commitAllowingStateLoss();
            }
        }
        notifyDataSetChanged();
        refreshOffscreenPageLimit();
    }

    /**
     * 替换指定位置的Fragment
     */
    public void replaceFragment(int position, @NonNull F newFragment) {
        replaceFragment(position, newFragment, null);
    }

    public void replaceFragment(int position, @NonNull F newFragment, @Nullable CharSequence title) {
        if (position < 0 || position >= mFragmentList.size()) {
            return;
        }
        // 移除旧Fragment
        F oldFragment = mFragmentList.get(position);
        if (!mFragmentManager.isDestroyed() && !mFragmentManager.isStateSaved()) {
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.remove(oldFragment);
            try {
                transaction.commitNow();
            } catch (IllegalStateException e) {
                transaction.commitAllowingStateLoss();
            }
        }
        // 添加新Fragment
        mFragmentList.set(position, newFragment);
        mFragmentTitleList.set(position, title);
        notifyDataSetChanged();
        refreshOffscreenPageLimit();
    }

    /**
     * 清空所有Fragment（释放资源）
     */
    public void clearFragments() {
        if (!mFragmentManager.isDestroyed() && !mFragmentManager.isStateSaved()) {
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            for (F fragment : mFragmentList) {
                transaction.remove(fragment);
            }
            try {
                transaction.commitNow();
            } catch (IllegalStateException e) {
                transaction.commitAllowingStateLoss();
            }
        }
        mFragmentList.clear();
        mFragmentTitleList.clear();
        mCurrentFragment = null;
        notifyDataSetChanged();
        refreshOffscreenPageLimit();
    }

    // ======================== 工具方法（保留getFragmentIndex，仅用于索引查询） ========================
    /**
     * 获取当前显示的Fragment
     */
    @Nullable
    public F getCurrentFragment() {
        return mCurrentFragment;
    }

    /**
     * 获取指定类的第一个Fragment索引（仅用于查询，不再用于重复判断）
     */
    public int getFragmentIndex(@Nullable Class<? extends Fragment> clazz) {
        if (clazz == null) return -1;
        for (int i = 0; i < mFragmentList.size(); i++) {
            if (clazz.isInstance(mFragmentList.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 设置懒加载模式（优化：增加阈值控制）
     */
    public void setLazyMode(boolean lazy) {
        mIsLazyMode = lazy;
        refreshOffscreenPageLimit();
    }

    /**
     * 自定义懒加载阈值（默认3）
     */
    public void setLazyThreshold(int threshold) {
        if (threshold < 1) {
            threshold = LAZY_THRESHOLD;
        }
        mLazyThreshold = threshold;
        refreshOffscreenPageLimit();
    }

    // ======================== 内部逻辑 ========================
    @Override
    public void startUpdate(@NonNull ViewGroup container) {
        super.startUpdate(container);
        if (container instanceof ViewPager) {
            mViewPager = (ViewPager) container;
            refreshOffscreenPageLimit();
        }
    }

    /**
     * 刷新OffscreenPageLimit（核心优化：避免内存过高）
     */
    private void refreshOffscreenPageLimit() {
        if (mViewPager == null) return;

        if (mIsLazyMode) {
            int limit = getCount() <= mLazyThreshold ? getCount() : mLazyThreshold;
            if (mViewPager != null) {
                mViewPager.setOffscreenPageLimit(limit);
            }
        } else {
            mViewPager.setOffscreenPageLimit(1);
        }
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.destroyItem(container, position, object);
        if (object instanceof Fragment && mCurrentFragment == object) {
            mCurrentFragment = null;
        }
    }

    /**
     * 对外暴露FragmentManager（可选）
     */
    @NonNull
    public FragmentManager getManager() {
        return mFragmentManager;
    }

    // 补充：更新当前显示的Fragment，触发懒加载
    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.setPrimaryItem(container, position, object);
        if (object instanceof Fragment) {
            mCurrentFragment = (F) object;
            if (mCurrentFragment instanceof LazyLoadCallback) {
                ((LazyLoadCallback) mCurrentFragment).onLazyLoad();
            }
        }
    }
}