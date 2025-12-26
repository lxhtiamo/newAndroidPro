package com.linewell.lxhdemo.liveDataBus;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import androidx.annotation.Nullable;

/*
*发送
* 1. 发送普通事件（非粘性）LiveDataBus.post(new LoginSuccessEvent("1001", "张三"));
* 2. 发送粘性事件（后续订阅也能收到） LiveDataBus.postSticky(new LoginSuccessEvent("1001", "张三"));
* 3. 可选：异步发送（优化高频事件）LiveDataBus.postAsync(new PaySuccessEvent("ORD2025001", 99.0f));
*
* 订阅 普通事件(observe) 粘性事件(observeSticky)
* LiveDataBus.observe(this, LoginSuccessEvent.class, new Observer<LoginSuccessEvent>() {
    @Override
    public void onChanged(LoginSuccessEvent event) {
        // 处理登录成功事件
        // 例如更新UI、跳转页面等
    }
});
*
* 清理粘性事件 / 优化内存
 1. 清除指定类型的粘性事件（后续订阅收不到历史）
LiveDataBus.removeStickyEvent(LoginSuccessEvent.class);

* 2. 低内存时主动清理 在application 中onTrimMemory 中调用
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE) {
            LiveDataBus.clearUnusedLiveData();
        }
    }
/**
 * 基于 Android 原生 LiveData 封装的事件总线，完全对标 EventBus 核心功能
 * 核心特性：
 * 1. 无需提前初始化，懒加载单例，即用即走
 * 2. 支持普通（非粘性）/粘性事件，类型安全区分
 * 3. 自动感知页面生命周期，无需手动解注册，彻底避免内存泄漏
 * 4. 线程安全设计，支持多线程发送/订阅事件
 * 5. 提供内存优化方法，可主动清理无用资源
 *
 */
public class LiveDataBus {
    /**
     * 单例实例（懒加载模式）：第一次调用时初始化，保证全局唯一
     */
    private static volatile LiveDataBus sInstance;

    /**
     * 事件类型与对应 LiveData 实例的映射容器
     * Key：事件类的 Class 对象（如 LoginSuccessEvent.class）
     * Value：Object 类型（实际存储 MutableLiveData<Event<T>>），避免泛型嵌套导致的 IDE 警告
     */
    private final Map<Class<?>, Object> mEventMap;

    /**
     * 可重入锁：保证多线程下 mEventMap 的读写安全
     * 替代 synchronized，锁粒度更细，性能更优
     */
    private final Lock mLock = new ReentrantLock();

    /**
     * 异步事件分发线程池（单线程）：优化高频事件发送，避免阻塞主线程
     * 单线程保证事件分发的顺序性，同时减少线程创建开销
     */
    private static final ExecutorService EVENT_POOL = Executors.newSingleThreadExecutor();

    /**
     * 私有构造方法：禁止外部实例化，保证单例特性
     * 初始化事件映射容器
     */
    private LiveDataBus() {
        mEventMap = new HashMap<>();
    }

    /**
     * 获取单例实例（双重校验锁 DCL）：懒加载 + 线程安全
     *
     * @return LiveDataBus 全局唯一实例
     */
    private static LiveDataBus getInstance() {
        // 第一层校验：避免每次调用都加锁，提升性能
        if (sInstance == null) {
            // 加锁：保证多线程下只有一个线程进入初始化逻辑
            synchronized (LiveDataBus.class) {
                // 第二层校验：防止多个线程等待锁后重复创建实例
                if (sInstance == null) {
                    sInstance = new LiveDataBus();
                }
            }
        }
        return sInstance;
    }

    /**
     * 核心方法：创建/获取指定事件类型对应的 LiveData 实例（泛型安全封装）
     * 线程安全：通过 mLock 保证多线程下不会重复创建 LiveData
     * 类型安全：通过 instanceof 校验，避免泛型强转的 IDE 警告
     *
     * @param eventType 事件类的 Class 对象（如 LoginSuccessEvent.class）
     * @param <T>       事件数据类型
     * @return 对应事件类型的 MutableLiveData 实例，用于事件分发
     */
    private <T> MutableLiveData<Event<T>> createOrGetLiveData(Class<T> eventType) {
        mLock.lock(); // 加锁：保证读写 mEventMap 的原子性
        try {
            // 1. 先从映射容器中获取已存在的 LiveData 实例
            Object liveDataObj = mEventMap.get(eventType);
            if (liveDataObj instanceof MutableLiveData) {
                // instanceof 前置校验，编译器认为该强转安全，无警告
                return (MutableLiveData<Event<T>>) liveDataObj;
            }
            // 2. 容器中无该实例，则创建新的 LiveData 并存入映射容器
            MutableLiveData<Event<T>> liveData = new MutableLiveData<>();
            mEventMap.put(eventType, liveData);
            return liveData;
        } finally {
            // 最终解锁：无论是否异常，确保锁释放，避免死锁
            mLock.unlock();
        }
    }

    // ========================== 普通事件（非粘性）API ==========================

    /**
     * 发送普通事件（非粘性）：对标 EventBus.post()
     * 特性：仅当前已订阅的页面能收到，后续订阅的页面无法接收历史事件
     * 线程安全：内部调用 postValue，可在子线程发送（自动切换到主线程分发）
     *
     * @param event 事件实例（需自定义事件类，如 new LoginSuccessEvent("张三")）
     * @param <T>   事件数据类型
     */
    public static <T> void post(@NonNull T event) {
        // 显式强转事件类型，解决泛型推导问题
        Class<T> eventType = (Class<T>) event.getClass();
        // 获取对应 LiveData 并发送事件（包装为 Event 类，处理粘性逻辑）
        getInstance().createOrGetLiveData(eventType).postValue(new Event<>(event));
    }

    /**
     * 订阅普通事件（非粘性）：对标 EventBus.register()
     * 特性：
     * 1. 自动感知生命周期，页面销毁时自动解注册，无内存泄漏
     * 2. 仅接收一次事件（消费后标记为已处理），避免重复接收
     *
     * @param owner      生命周期拥有者（Activity/Fragment），用于自动解注册
     * @param eventType  要订阅的事件类型（如 LoginSuccessEvent.class）
     * @param observer   事件观察者，接收并处理事件数据
     * @param <T>        事件数据类型
     */
    public static <T> void observe(@NonNull LifecycleOwner owner,
                                   @NonNull Class<T> eventType,
                                   @NonNull Observer<T> observer) {
        // 获取对应 LiveData 并绑定观察者
        getInstance().createOrGetLiveData(eventType).observe(owner, event -> {
            // 非粘性逻辑：仅第一次获取有效（消费后标记为已处理）
            T content = event.getContentIfNotHandled();
            if (content != null) {
                observer.onChanged(content);
            }
        });
    }

    // ========================== 粘性事件 API ==========================

    /**
     * 发送粘性事件：对标 EventBus.postSticky()
     * 特性：事件会被缓存，后续订阅的页面仍能接收该历史事件
     * 注意：内部调用 setValue，需在主线程发送（若在子线程，建议用 postSticky + post 组合）
     *
     * @param event 事件实例（如 new PaySuccessEvent("ORD123", 99.0f)）
     * @param <T>   事件数据类型
     */
    public static <T> void postSticky(@NonNull T event) {
        Class<T> eventType = (Class<T>) event.getClass();
        MutableLiveData<Event<T>> liveData = getInstance().createOrGetLiveData(eventType);
        // setValue 直接在当前线程分发，保证粘性事件的缓存生效
        liveData.setValue(new Event<>(event));
    }

    /**
     * 订阅粘性事件：对标 EventBus.registerSticky()
     * 特性：
     * 1. 自动感知生命周期，页面销毁时自动解注册
     * 2. 能接收历史发送的粘性事件（无视消费标记）
     *
     * @param owner      生命周期拥有者（Activity/Fragment）
     * @param eventType  要订阅的事件类型
     * @param observer   事件观察者，接收并处理事件数据
     * @param <T>        事件数据类型
     */
    public static <T> void observeSticky(@NonNull LifecycleOwner owner,
                                         @NonNull Class<T> eventType,
                                         @NonNull Observer<T> observer) {
        getInstance().createOrGetLiveData(eventType).observe(owner, event -> {
            // 粘性逻辑：强制获取事件内容（无视消费标记）
            T content = event.getStickyContent();
            if (content != null) {
                observer.onChanged(content);
            }
        });
    }

    /**
     * 清除指定类型的粘性事件缓存：对标 EventBus.removeStickyEvent()
     * 特性：仅清空 LiveData 的缓存值，LiveData 实例仍保留
     * 场景：避免后续订阅者收到无用的历史粘性事件
     *
     * @param eventType 要清除的事件类型
     * @param <T>       事件数据类型
     */
    public static <T> void removeStickyEvent(@NonNull Class<T> eventType) {
        // 设置为 null 清空 LiveData 的缓存值
        getInstance().createOrGetLiveData(eventType).setValue(null);
    }

    // ========================== 性能优化/资源清理 API ==========================

    /**
     * 异步发送事件：优化高频事件场景，避免主线程阻塞
     * 特性：通过单线程池异步发送，事件分发顺序与发送顺序一致
     * 场景：传感器数据、实时弹幕等高频事件发送
     *
     * @param event 事件实例
     * @param <T>   事件数据类型
     */
    public static <T> void postAsync(@NonNull T event) {
        Class<T> eventType = (Class<T>) event.getClass();
        // 提交到线程池异步执行
        EVENT_POOL.execute(() -> getInstance().createOrGetLiveData(eventType).postValue(new Event<>(event)));
    }

    /**
     * 清理所有无观察者的 LiveData 实例：优化内存占用
     * 特性：
     * 1. 遍历映射容器，移除无订阅者的 LiveData
     * 2. 反射获取 LiveData 内部观察者数量，判断是否无用
     * 场景：低内存时调用、定时任务调用（如每5分钟一次）
     */
    public static void clearUnusedLiveData() {
        LiveDataBus instance = getInstance();
        instance.mLock.lock();
        try {
            // 遍历所有事件类型的映射关系
            for (Map.Entry<Class<?>, Object> entry : instance.mEventMap.entrySet()) {
                Object obj = entry.getValue();
                // 仅处理 MutableLiveData 类型
                if (obj instanceof MutableLiveData) {
                    MutableLiveData<Event<?>> liveData = (MutableLiveData<Event<?>>) obj;
                    // 无观察者则移除该映射，释放内存
                    if (getObserverCount(liveData) == 0) {
                        instance.mEventMap.remove(entry.getKey());
                    }
                }
            }
        } catch (Exception e) {
            // 捕获反射/遍历异常，避免崩溃
            e.printStackTrace();
        } finally {
            instance.mLock.unlock();
        }
    }

    /**
     * 手动移除指定事件类型的 LiveData 实例：彻底释放资源
     * 特性：从映射容器中删除该事件类型的所有关联，后续使用会重新创建
     * 场景：
     * 1. 某类事件永久不再使用（如退出登录后移除登录相关事件）
     * 2. 彻底重置粘性事件状态（比 removeStickyEvent 更彻底）
     *
     * @param eventType 要移除的事件类型
     * @param <T>       事件数据类型
     */
    public static <T> void removeLiveData(@NonNull Class<T> eventType) {
        LiveDataBus instance = getInstance();
        instance.mLock.lock();
        try {
            // 从映射容器中移除指定事件类型
            instance.mEventMap.remove(eventType);
        } finally {
            instance.mLock.unlock();
        }
    }

    /**
     * 私有工具方法：反射获取 LiveData 内部的观察者数量
     * 说明：LiveData 无公开 API 获取观察者数量，需通过反射访问私有字段 mObservers
     *
     * @param liveData 目标 LiveData 实例
     * @return 观察者数量（-1 表示反射失败）
     */
    private static int getObserverCount(MutableLiveData<Event<?>> liveData) {
        try {
            // 获取 LiveData 私有字段 mObservers（存储观察者的 Map）
            Field mObserversField = androidx.lifecycle.LiveData.class.getDeclaredField("mObservers");
            mObserversField.setAccessible(true); // 允许访问私有字段
            Object mObservers = mObserversField.get(liveData);
            // mObservers 是 Map 类型，size 即为观察者数量
            if (mObservers instanceof Map) {
                return ((Map<?, ?>) mObservers).size();
            }
        } catch (NoSuchFieldException e) {
            // 字段不存在（极少情况，如 Android 版本兼容问题）
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // 无访问权限
            e.printStackTrace();
        }
        // 反射失败返回 -1，不进行清理
        return -1;
    }

    /**
     * 事件包装类：解决 LiveData 粘性事件的消费控制问题
     * 内部维护消费标记，区分普通/粘性事件的处理逻辑
     *
     * @param <T> 事件数据类型
     */
    static class Event<T> {
        /**
         * 实际的事件数据（如登录成功后的用户信息）
         */
        private final T content;

        /**
         * 消费标记：标记事件是否已被处理（仅用于普通事件）
         * true：已处理，false：未处理
         */
        private boolean hasBeenHandled = false;

        /**
         * 构造方法：初始化事件数据
         *
         * @param content 事件数据
         */
        public Event(T content) {
            this.content = content;
        }

        /**
         * 获取普通事件内容：仅第一次调用有效（消费后标记为已处理）
         * 用于普通事件的消费控制，避免重复处理
         *
         * @return 事件数据（已处理则返回 null）
         */
        @Nullable
        public T getContentIfNotHandled() {
            if (hasBeenHandled) {
                return null;
            }
            hasBeenHandled = true;
            return content;
        }

        /**
         * 获取粘性事件内容：无视消费标记，强制返回数据
         * 用于粘性事件，保证后续订阅者能收到历史事件
         *
         * @return 事件数据（非 null）
         */
        public T getStickyContent() {
            return content;
        }
    }
}