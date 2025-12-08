package com.linx.mylibrary.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.linx.mylibrary.utils.klog.KLog;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * RxJava 定时器工具类（安卓全场景适配 · 无死角版）
 * 核心特性：
 * 1. 多任务安全管理（无覆盖/并发问题）
 * 2. 生命周期深度绑定（自动释放，杜绝内存泄漏）
 * 3. 完善的边界条件校验（0值/负数/空参数）
 * 4. 精准的 RxJava 语义（修复 intervalRange 注释/行为错误）
 * 5. 安卓场景快捷API（倒计时、主线程兼容）
 * 6. 零并发安全风险（快照遍历、原子操作）
 * 7. 友好的异常兜底（避免上层回调崩溃）
 * 8. 兼容 RxJava 2.x 全版本 + 安卓全组件（Activity/Fragment/Service）
 */
public final class RxTimerUtil {
    // ===================== 核心常量 =====================
    private static final String TAG = "RxTimerUtil";
    // 并发Map管理任务（任务ID -> Disposable），ConcurrentHashMap保证线程安全
    private final Map<String, Disposable> disposableMap = new ConcurrentHashMap<>(4);

    // ===================== 单例模式（绝对线程安全） =====================
    private RxTimerUtil() {}
    private static class SingletonHolder {
        // 类加载时初始化，JVM 保证线程安全，且懒加载
        private static final RxTimerUtil INSTANCE = new RxTimerUtil();
    }
    public static RxTimerUtil getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // ===================== 核心回调接口（异常兜底） =====================
    /**
     * 定时器回调（所有方法均有异常兜底，避免上层崩溃）
     */
    public interface RxTimerCallback {
        /**
         * 单次/轮询执行回调（主线程）
         * @param taskId 任务唯一标识
         * @param value  发射值：
         *              - timer：固定为0
         *              - interval：从0开始递增
         *              - intervalRange：从start开始递增
         */
        void onNext(@NonNull String taskId, long value);

        /**
         * 异常回调（默认实现：打印日志，避免上层未实现导致崩溃）
         * @param taskId 任务唯一标识
         * @param e      异常信息
         */
        default void onError(@NonNull String taskId, @NonNull Throwable e) {
            try {
                KLog.e(TAG, "Task[" + taskId + "] execute error: " + e.getMessage(), e);
            } catch (Throwable ignore) {
                // 兜底：即使日志工具异常，也不崩溃
            }
        }

        /**
         * 完成回调（单次/有限轮询触发，默认空实现）
         * @param taskId 任务唯一标识
         */
        default void onComplete(@NonNull String taskId) {
            try {
                KLog.d(TAG, "Task[" + taskId + "] execute completed");
            } catch (Throwable ignore) {}
        }
    }

    // ===================== 单次执行（基础版） =====================
    /**
     * 单次执行定时器（默认毫秒）
     * @param delay    延迟时间（≥0）
     * @param callback 回调（非空）
     * @return 任务ID（取消/绑定生命周期用）
     * @throws IllegalArgumentException 延迟时间为负时抛出
     * @throws NullPointerException     回调为空时抛出
     */
    @NonNull
    public String scheduleOnce(long delay, @NonNull RxTimerCallback callback) {
        return scheduleOnce(delay, TimeUnit.MILLISECONDS, callback);
    }

    /**
     * 单次执行定时器（自定义时间单位）
     * @param delay     延迟时间（≥0）
     * @param timeUnit  时间单位（非空）
     * @param callback  回调（非空）
     * @return 任务ID
     */
    @NonNull
    public String scheduleOnce(long delay, @NonNull TimeUnit timeUnit, @NonNull RxTimerCallback callback) {
        // 1. 严格参数校验（提前暴露错误，避免运行时崩溃）
        validateNonNegative(delay, "Delay time");
        validateNonNull(timeUnit, "TimeUnit");
        validateNonNull(callback, "RxTimerCallback");

        String taskId = generateUniqueTaskId();
        try {
            Disposable disposable = Observable.timer(delay, timeUnit, Schedulers.io())
                    // 兼容安卓主线程不存在的场景（如后台Service）
                    .observeOn(getSafeMainScheduler())
                    .subscribeWith(new DisposableObserver<Long>() {
                        @Override
                        public void onNext(Long value) {
                            // 回调异常兜底：避免上层onNext崩溃导致定时器终止
                            try {
                                callback.onNext(taskId, value);
                            } catch (Throwable e) {
                                KLog.e(TAG, "Task[" + taskId + "] onNext callback error: " + e.getMessage(), e);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            try {
                                callback.onError(taskId, e);
                            } catch (Throwable ignore) {}
                            cancelTask(taskId); // 异常时强制释放资源
                        }

                        @Override
                        public void onComplete() {
                            try {
                                callback.onComplete(taskId);
                            } catch (Throwable ignore) {}
                            cancelTask(taskId); // 完成后自动释放
                        }
                    });
            disposableMap.put(taskId, disposable);
            KLog.d(TAG, "Once task[" + taskId + "] scheduled: delay=" + delay + " " + timeUnit.name());
        } catch (Throwable e) {
            KLog.e(TAG, "Create once task[" + taskId + "] failed: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create timer task", e);
        }
        return taskId;
    }

    // ===================== 无限轮询（基础版） =====================
    /**
     * 无限轮询定时器（默认毫秒，初始延迟0）
     * @param interval 轮询间隔（≥0）
     * @param callback 回调（非空）
     * @return 任务ID
     */
    @NonNull
    public String scheduleLoop(long interval, @NonNull RxTimerCallback callback) {
        return scheduleLoop(0, interval, TimeUnit.MILLISECONDS, callback);
    }

    /**
     * 无限轮询定时器（自定义初始延迟+时间单位）
     * @param initialDelay 初始延迟（≥0）
     * @param interval     轮询间隔（≥0）
     * @param timeUnit     时间单位（非空）
     * @param callback     回调（非空）
     * @return 任务ID
     */
    @NonNull
    public String scheduleLoop(long initialDelay, long interval, @NonNull TimeUnit timeUnit, @NonNull RxTimerCallback callback) {
        validateNonNegative(initialDelay, "Initial delay");
        validateNonNegative(interval, "Interval time");
        validateNonNull(timeUnit, "TimeUnit");
        validateNonNull(callback, "RxTimerCallback");

        String taskId = generateUniqueTaskId();
        try {
            Disposable disposable = Observable.interval(initialDelay, interval, timeUnit, Schedulers.io())
                    .observeOn(getSafeMainScheduler())
                    .subscribeWith(new DisposableObserver<Long>() {
                        @Override
                        public void onNext(Long value) {
                            try {
                                callback.onNext(taskId, value);
                            } catch (Throwable e) {
                                KLog.e(TAG, "Task[" + taskId + "] onNext callback error: " + e.getMessage(), e);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            try {
                                callback.onError(taskId, e);
                            } catch (Throwable ignore) {}
                            cancelTask(taskId);
                        }

                        @Override
                        public void onComplete() {
                            // 无限轮询不会触发onComplete，仅兜底
                            try {
                                callback.onComplete(taskId);
                            } catch (Throwable ignore) {}
                            cancelTask(taskId);
                        }
                    });
            disposableMap.put(taskId, disposable);
            KLog.d(TAG, "Loop task[" + taskId + "] scheduled: initialDelay=" + initialDelay + ", interval=" + interval + " " + timeUnit.name());
        } catch (Throwable e) {
            KLog.e(TAG, "Create loop task[" + taskId + "] failed: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create loop timer task", e);
        }
        return taskId;
    }

    // ===================== 有限次数轮询（intervalRange · 核心修正版） =====================
    /**
     * 有限次数轮询（intervalRange 原生递增，注释精准）
     * @param start        起始发射值（可任意整数）
     * @param count        发射次数（≥0，0则直接onComplete）
     * @param initialDelay 初始延迟（≥0）
     * @param period       轮询间隔（≥0）
     * @param timeUnit     时间单位（非空）
     * @param callback     回调（非空）
     * @return 任务ID
     */
    @NonNull
    public String scheduleRangeLoop(long start, long count, long initialDelay, long period, @NonNull TimeUnit timeUnit, @NonNull RxTimerCallback callback) {
        validateNonNegative(count, "Emit count");
        validateNonNegative(initialDelay, "Initial delay");
        validateNonNegative(period, "Period time");
        validateNonNull(timeUnit, "TimeUnit");
        validateNonNull(callback, "RxTimerCallback");

        String taskId = generateUniqueTaskId();
        try {
            // count=0时，RxJava会直接触发onComplete，提前日志提示
            if (count == 0) {
                KLog.w(TAG, "Task[" + taskId + "] emit count is 0, will complete immediately");
            }

            Disposable disposable = Observable.intervalRange(start, count, initialDelay, period, timeUnit, Schedulers.io())
                    .observeOn(getSafeMainScheduler())
                    .subscribeWith(new DisposableObserver<Long>() {
                        @Override
                        public void onNext(Long value) {
                            try {
                                callback.onNext(taskId, value);
                            } catch (Throwable e) {
                                KLog.e(TAG, "Task[" + taskId + "] onNext callback error: " + e.getMessage(), e);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            try {
                                callback.onError(taskId, e);
                            } catch (Throwable ignore) {}
                            cancelTask(taskId);
                        }

                        @Override
                        public void onComplete() {
                            try {
                                callback.onComplete(taskId);
                            } catch (Throwable ignore) {}
                            cancelTask(taskId);
                        }
                    });
            disposableMap.put(taskId, disposable);
            KLog.d(TAG, "RangeLoop task[" + taskId + "] scheduled: start=" + start + ", count=" + count + ", initialDelay=" + initialDelay + ", period=" + period + " " + timeUnit.name());
        } catch (Throwable e) {
            KLog.e(TAG, "Create range loop task[" + taskId + "] failed: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create range loop timer task", e);
        }
        return taskId;
    }

    /**
     * 有限次数轮询（简化版：初始延迟0 + 毫秒单位）
     * @param start    起始发射值
     * @param count    发射次数
     * @param period   轮询间隔（毫秒）
     * @param callback 回调
     * @return 任务ID
     */
    @NonNull
    public String scheduleRangeLoop(long start, long count, long period, @NonNull RxTimerCallback callback) {
        return scheduleRangeLoop(start, count, 0, period, TimeUnit.MILLISECONDS, callback);
    }

    // ===================== 安卓场景快捷API（倒计时 · 无需上层计算） =====================
    /**
     * 倒计时快捷方法（安卓高频场景，直接返回递减值，无需上层计算）
     * @param totalSeconds 总倒计时秒数（如60）
     * @param callback     回调（onNext返回递减值：totalSeconds→0）
     * @return 任务ID
     */
    @NonNull
    public String scheduleCountdown(long totalSeconds, @NonNull RxTimerCallback callback) {
        validateNonNegative(totalSeconds, "Total countdown seconds");
        validateNonNull(callback, "RxTimerCallback");

        // 利用 intervalRange 递增特性，内部封装递减计算，简化上层使用
        return scheduleRangeLoop(0, totalSeconds + 1, 1000, (taskId, incrementValue) -> {
            long decrementValue = totalSeconds - incrementValue;
            callback.onNext(taskId, decrementValue);
        });
    }

    // ===================== 生命周期绑定（深度优化 · 支持LifecycleOwner） =====================


    /**
     * 绑定任务到生命周期（推荐：LifecycleOwner + DefaultLifecycleObserver）
     * 适配 Lifecycle 2.4.0+，无过时注解，类型安全
     * @param owner   生命周期持有者（Activity/Fragment）
     * @param taskId  任务ID
     */
    public void bindToLifecycle(@NonNull LifecycleOwner owner, @NonNull String taskId) {
        validateNonNull(owner, "LifecycleOwner");
        validateNonNull(taskId, "TaskId");

        // 使用 DefaultLifecycleObserver（推荐，编译期校验）
        owner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                // 生命周期销毁时取消任务
                if (isTaskExists(taskId)) {
                    cancelTask(taskId);
                    KLog.d(TAG, "Task[" + taskId + "] cancelled by Lifecycle ON_DESTROY");
                }
                // 自动移除观察者（DefaultLifecycleObserver 无需手动remove，Lifecycle会自动管理）
                owner.getLifecycle().removeObserver(this);
            }
        });
    }

    /**
     * 兼容旧版 Lifecycle 绑定（可选，适配 2.4.0 以下版本）
     * @param lifecycle 生命周期
     * @param taskId    任务ID
     */
    public void bindToLifecycle(@NonNull Lifecycle lifecycle, @NonNull String taskId) {
        validateNonNull(lifecycle, "Lifecycle");
        validateNonNull(taskId, "TaskId");

        // 备选方案：LifecycleEventObserver（适配所有版本，事件回调更灵活）
        lifecycle.addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_DESTROY) { // 或其他指定事件
                    if (isTaskExists(taskId)) {
                        cancelTask(taskId);
                        KLog.d(TAG, "Task[" + taskId + "] cancelled by Lifecycle " + event);
                    }
                    source.getLifecycle().removeObserver(this);
                    //lifecycle.removeObserver(this);
                }
            }
        });
    }
    // ===================== 任务管理（零并发风险） =====================
    /**
     * 取消单个任务（原子操作，无并发问题）
     * @param taskId 任务ID（可为空，空则直接返回）
     */
    public void cancelTask(@Nullable String taskId) {
        if (taskId == null || disposableMap.isEmpty()) {
            KLog.w(TAG, "Cancel task failed: taskId is null or no tasks exist");
            return;
        }

        // 原子移除：避免remove后dispose前被其他线程操作
        Disposable disposable = disposableMap.remove(taskId);
        if (disposable != null) {
            try {
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                    KLog.d(TAG, "Task[" + taskId + "] cancelled successfully");
                } else {
                    KLog.w(TAG, "Task[" + taskId + "] already disposed");
                }
            } catch (Throwable e) {
                KLog.e(TAG, "Cancel task[" + taskId + "] failed: " + e.getMessage(), e);
            }
        } else {
            KLog.w(TAG, "Task[" + taskId + "] not found in task map");
        }
    }

    /**
     * 取消所有任务（遍历快照，避免并发修改异常）
     */
    public void cancelAllTasks() {
        if (disposableMap.isEmpty()) {
            KLog.d(TAG, "Cancel all tasks: no tasks to cancel");
            return;
        }

        // 遍历快照（entrySet().toArray()），避免遍历中map修改导致ConcurrentModificationException
        Map.Entry<String, Disposable>[] entries = disposableMap.entrySet().toArray(new Map.Entry[0]);
        int cancelledCount = 0;
        for (Map.Entry<String, Disposable> entry : entries) {
            String taskId = entry.getKey();
            Disposable disposable = entry.getValue();
            try {
                if (disposable != null && !disposable.isDisposed()) {
                    disposable.dispose();
                    cancelledCount++;
                    KLog.d(TAG, "Batch cancel task[" + taskId + "] success");
                }
                disposableMap.remove(taskId); // 确保移除
            } catch (Throwable e) {
                KLog.e(TAG, "Batch cancel task[" + taskId + "] failed: " + e.getMessage(), e);
            }
        }
        // 兜底清理：防止漏删
        disposableMap.clear();
        KLog.d(TAG, "Cancel all tasks completed: total=" + entries.length + ", cancelled=" + cancelledCount);
    }

    /**
     * 检查任务是否有效（存在且未被释放）
     * @param taskId 任务ID（非空）
     * @return true=有效，false=无效/不存在
     */
    public boolean isTaskExists(@NonNull String taskId) {
        validateNonNull(taskId, "TaskId");
        Disposable disposable = disposableMap.get(taskId);
        return disposable != null && !disposable.isDisposed();
    }

    // ===================== 私有工具方法（全场景兼容） =====================
    /**
     * 生成唯一任务ID（UUID全长度，避免碰撞）
     * 优化点：原截取8位有极低碰撞风险，改为全UUID
     */
    @NonNull
    private String generateUniqueTaskId() {
        return "RxTimer_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 安全的主线程调度器（兼容后台Service/无主线程场景）
     * 优化点：避免AndroidSchedulers.mainThread()在后台线程抛异常
     */
    @NonNull
    private io.reactivex.Scheduler getSafeMainScheduler() {
        try {
            return AndroidSchedulers.mainThread();
        } catch (Throwable e) {
            // 主线程不存在时（如后台Service），使用IO线程兜底
            KLog.w(TAG, "Main thread scheduler not available, use IO scheduler instead: " + e.getMessage());
            return Schedulers.io();
        }
    }

    /**
     * 非负校验（统一封装，减少冗余）
     */
    private void validateNonNegative(long value, @NonNull String paramName) {
        if (value < 0) {
            throw new IllegalArgumentException(paramName + " cannot be negative: " + value);
        }
    }

    /**
     * 非空校验（统一封装，错误信息精准）
     */
    private void validateNonNull(@Nullable Object obj, @NonNull String paramName) {
        if (obj == null) {
            throw new NullPointerException(paramName + " cannot be null");
        }
    }
}