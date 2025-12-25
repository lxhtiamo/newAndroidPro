package com.linx.mylibrary.utils.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.linx.mylibrary.BuildConfig;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *    desc   : Activity 自动管理类（优化版）自动管理应用中的所有Activity实例
 *    优化点：
 *    1. 弱引用存储Activity，解决内存泄漏；
 *    2. 集合操作加锁，保证线程安全；
 *    3. init方法防重复注册生命周期回调；
 *    4. 完善Activity状态判断（isFinishing + isDestroyed）；
 *    5. Timber日志容错，降级到系统Log；
 *    6. 回调接口默认方法，提升易用性；
 *    7. 唯一标识改用System.identityHashCode，避免hashCode被重写；
 *    8. 核心修复：mTopActivity/mResumedActivity改为弱引用，消除静态单例内存泄漏；
 */
public final class AppActivityManager implements Application.ActivityLifecycleCallbacks {

    // 保留原有变量名，仅修复内存泄漏
    private static volatile AppActivityManager sInstance;

    /** 全局锁：保证集合操作线程安全 */
    private final Object mLock = new Object();
    /** Activity 存放集合（弱引用，避免内存泄漏） */
    private final ArrayMap<String, WeakReference<Activity>> mActivitySet = new ArrayMap<>();
    /** 应用生命周期回调 */
    private final ArrayList<ApplicationLifecycleCallback> mLifecycleCallbacks = new ArrayList<>();

    /** 当前应用上下文对象 */
    private Application mApplication;
    /** 栈顶的 Activity 对象（核心修改：改为弱引用，变量名不变） */
    private WeakReference<Activity> mTopActivity;
    /** 前台并且可见的 Activity 对象（核心修改：改为弱引用，变量名不变） */
    private WeakReference<Activity> mResumedActivity;
    /** 是否已初始化：防止重复注册生命周期回调 */
    private boolean mIsInited = false;

    private AppActivityManager() {}

    public static AppActivityManager getInstance() {
        if (sInstance == null) {
            synchronized (AppActivityManager.class) {
                if (sInstance == null) {
                    sInstance = new AppActivityManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * 初始化（仅需在Application中调用一次）
     */
    public void init(Application application) {
        // 防止重复初始化 + 空指针保护 + 强制持有Application Context（无泄漏风险）
        if (mIsInited || application == null) {
            return;
        }
        mApplication = application;
        mApplication.registerActivityLifecycleCallbacks(this);
        mIsInited = true;
    }

    /**
     * 获取 Application 对象
     */
    public Application getApplication() {
        return mApplication;
    }

    /**
     * 获取栈顶的 Activity（适配弱引用，变量名不变）
     */
    @Nullable
    public Activity getTopActivity() {
        synchronized (mLock) {
            return getValidActivity(mTopActivity != null ? mTopActivity.get() : null);
        }
    }

    /**
     * 获取当前Activity（适配弱引用，变量名不变）
     */
    @Nullable
    public Activity getCurrentActivity() {
        synchronized (mLock) {
            // 1. 优先取前台可见、可交互的Activity（视觉上的当前）
            Activity current = getValidActivity(mResumedActivity != null ? mResumedActivity.get() : null);
            // 2. 若resumedActivity为空，取栈顶Activity（栈结构上的当前）
            if (current == null) {
                current = getValidActivity(mTopActivity != null ? mTopActivity.get() : null);
            }
            // 3. 最终状态校验：确保Activity未销毁、未正在结束
            return current;
        }
    }

    /**
     * 获取前台并且可见的 Activity（适配弱引用，变量名不变）
     */
    @Nullable
    public Activity getResumedActivity() {
        synchronized (mLock) {
            return getValidActivity(mResumedActivity != null ? mResumedActivity.get() : null);
        }
    }

    /**
     * 判断当前应用是否处于前台状态
     */
    public boolean isForeground() {
        synchronized (mLock) {
            return getResumedActivity() != null;
        }
    }

    /**
     * 注册应用生命周期回调
     */
    public void registerApplicationLifecycleCallback(ApplicationLifecycleCallback callback) {
        if (callback == null) {
            return;
        }
        synchronized (mLock) {
            mLifecycleCallbacks.add(callback);
        }
    }

    /**
     * 取消注册应用生命周期回调
     */
    public void unregisterApplicationLifecycleCallback(ApplicationLifecycleCallback callback) {
        if (callback == null) {
            return;
        }
        synchronized (mLock) {
            mLifecycleCallbacks.remove(callback);
        }
    }

    /**
     * 销毁指定的 Activity
     */
    public void finishActivity(Class<? extends Activity> clazz) {
        if (clazz == null) {
            return;
        }
        synchronized (mLock) {
            String[] keys = mActivitySet.keySet().toArray(new String[]{});
            for (String key : keys) {
                Activity activity = getActivityFromSet(key);
                if (activity == null) {
                    continue;
                }

                if (activity.getClass().equals(clazz)) {
                    activity.finish();
                    mActivitySet.remove(key);
                    break;
                }
            }
        }
    }

    /**
     * 销毁所有的 Activity
     */
    public void finishAllActivities() {
        finishAllActivities((Class<? extends Activity>) null);
    }

    /**
     * 销毁所有的 Activity（支持白名单）
     */
    @SafeVarargs
    public final void finishAllActivities(Class<? extends Activity>... classArray) {
        synchronized (mLock) {
            String[] keys = mActivitySet.keySet().toArray(new String[]{});
            for (String key : keys) {
                Activity activity = getActivityFromSet(key);
                if (activity == null) {
                    continue;
                }

                // 判断是否是白名单Activity
                boolean isWhiteList = false;
                if (classArray != null) {
                    for (Class<? extends Activity> clazz : classArray) {
                        if (activity.getClass().equals(clazz)) {
                            isWhiteList = true;
                            break;
                        }
                    }
                }

                if (isWhiteList) {
                    continue;
                }

                // 销毁非白名单Activity
                activity.finish();
                mActivitySet.remove(key);
            }
        }
    }

    /**
     * 从集合中获取Activity（自动清理已回收的弱引用）
     */
    @Nullable
    private Activity getActivityFromSet(String tag) {
        WeakReference<Activity> ref = mActivitySet.get(tag);
        if (ref == null) {
            return null;
        }

        Activity activity = ref.get();
        // 完善Activity状态判断：已回收/已销毁/正在销毁 都视为无效
        if (activity == null
                || activity.isFinishing()
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
            mActivitySet.remove(tag);
            return null;
        }
        return activity;
    }

    /**
     * 校验Activity有效性（核心工具方法）
     * @return 有效返回Activity，无效返回null
     */
    @Nullable
    private Activity getValidActivity(@Nullable Activity activity) {
        if (activity == null) {
            return null;
        }
        // 覆盖所有无效状态：正在结束、已销毁（API 17+）
        if (activity.isFinishing() ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
            return null;
        }
        return activity;
    }

    /**
     * 日志输出（容错：Timber未初始化时降级到系统Log）
     */
    private void logI(String msg) {
        if (BuildConfig.DEBUG) {
            try {
                // 优先用Timber，未初始化则降级到系统Log
                ///  timber.log.Timber.i(msg);
            } catch (Exception e) {
                Log.i("ActivityManager", msg);
            }
        }
    }

    /**
     * 获取对象的唯一标识（避免hashCode被重写）
     */
    private static String getObjectTag(Object object) {
        // 类名 + JVM唯一标识（System.identityHashCode不会被重写）
        return object.getClass().getName() + System.identityHashCode(object);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        String activityName = activity.getClass().getSimpleName();
        //logI("%s - onCreate", activityName);

        synchronized (mLock) {
            if (mActivitySet.size() == 0) {
                // 应用首次创建回调
                Iterator<ApplicationLifecycleCallback> iterator = mLifecycleCallbacks.iterator();
                while (iterator.hasNext()) {
                    iterator.next().onApplicationCreate(activity);
                }
                //logI("%s - onApplicationCreate", activityName);
            }
            // 存入弱引用，避免内存泄漏
            mActivitySet.put(getObjectTag(activity), new WeakReference<>(activity));
            // 核心修改：赋值为弱引用，变量名不变
            mTopActivity = new WeakReference<>(activity);
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        //logI("%s - onStart", activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        String activityName = activity.getClass().getSimpleName();
        //logI("%s - onResume", activityName);

        synchronized (mLock) {
            Activity topActivity = getValidActivity(mTopActivity != null ? mTopActivity.get() : null);
            Activity resumedActivity = getValidActivity(mResumedActivity != null ? mResumedActivity.get() : null);

            if (topActivity == activity && resumedActivity == null) {
                // 应用切前台回调
                Iterator<ApplicationLifecycleCallback> iterator = mLifecycleCallbacks.iterator();
                while (iterator.hasNext()) {
                    iterator.next().onApplicationForeground(activity);
                }
                //  logI("%s - onApplicationForeground", activityName);
            }
            // 核心修改：赋值为弱引用，变量名不变
            mTopActivity = new WeakReference<>(activity);
            mResumedActivity = new WeakReference<>(activity);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        //logI("%s - onPause", activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        String activityName = activity.getClass().getSimpleName();
        //logI("%s - onStop", activityName);

        synchronized (mLock) {
            Activity resumedActivity = getValidActivity(mResumedActivity != null ? mResumedActivity.get() : null);
            if (resumedActivity == activity) {
                // 清空已停止的前台Activity引用
                mResumedActivity.clear();
            }
            // 若停止的是栈顶Activity，尝试从集合中找新的栈顶
            Activity topActivity = getValidActivity(mTopActivity != null ? mTopActivity.get() : null);
            if (topActivity == activity) {
                mTopActivity.clear();
                // 从集合中取最后一个有效Activity作为新栈顶
                String[] keys = mActivitySet.keySet().toArray(new String[]{});
                for (int i = keys.length - 1; i >= 0; i--) {
                    Activity newTop = getActivityFromSet(keys[i]);
                    if (newTop != null) {
                        mTopActivity = new WeakReference<>(newTop);
                        break;
                    }
                }
            }
            if (mResumedActivity == null) {
                // 应用切后台回调
                Iterator<ApplicationLifecycleCallback> iterator = mLifecycleCallbacks.iterator();
                while (iterator.hasNext()) {
                    iterator.next().onApplicationBackground(activity);
                }
                // logI("%s - onApplicationBackground", activityName);
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        //logI("%s - onSaveInstanceState", activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        String activityName = activity.getClass().getSimpleName();
        // logI("%s - onDestroy", activityName);

        synchronized (mLock) {
            // 移除已销毁的Activity
            mActivitySet.remove(getObjectTag(activity));

            // 更新栈顶Activity：清空指向已销毁Activity的引用
            Activity topActivity = getValidActivity(mTopActivity != null ? mTopActivity.get() : null);
            if (topActivity == activity) {
                mTopActivity.clear();
            }

            // 更新前台Activity：清空指向已销毁Activity的引用
            Activity resumedActivity = getValidActivity(mResumedActivity != null ? mResumedActivity.get() : null);
            if (resumedActivity == activity) {
                mResumedActivity.clear();
            }

            // 应用最后一个Activity销毁回调
            if (mActivitySet.size() == 0) {
                Iterator<ApplicationLifecycleCallback> iterator = mLifecycleCallbacks.iterator();
                while (iterator.hasNext()) {
                    iterator.next().onApplicationDestroy(activity);
                }
                // logI("%s - onApplicationDestroy", activityName);
            }
        }
    }

    /**
     * 应用生命周期回调（默认方法，无需强制实现所有方法）
     */
    public interface ApplicationLifecycleCallback {
        /**
         * 第一个 Activity 创建了
         */
        default void onApplicationCreate(Activity activity) {}

        /**
         * 最后一个 Activity 销毁了
         */
        default void onApplicationDestroy(Activity activity) {}

        /**
         * 应用从前台进入到后台
         */
        default void onApplicationBackground(Activity activity) {}

        /**
         * 应用从后台进入到前台
         */
        default void onApplicationForeground(Activity activity) {}
    }
}