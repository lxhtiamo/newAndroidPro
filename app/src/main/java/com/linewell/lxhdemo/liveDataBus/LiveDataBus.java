package com.linewell.lxhdemo.liveDataBus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * 发送
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
     * Value：MutableLiveData<Event<?>> 类型，使用 ConcurrentHashMap 保证线程安全
     */
    private final ConcurrentHashMap<Class<?>, MutableLiveData<?>> mEventMap;

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
        // 关键修改：Map 值类型改为 MutableLiveData<?>，进一步降低泛型冲突
        mEventMap = new ConcurrentHashMap<>();
    }

    /**
     * 获取单例实例（双重校验锁 DCL）：懒加载 + 线程安全
     *
     * @return LiveDataBus 全局唯一实例
     */
    private static LiveDataBus getInstance() {
        if (sInstance == null) {
            synchronized (LiveDataBus.class) {
                if (sInstance == null) {
                    sInstance = new LiveDataBus();
                }
            }
        }
        return sInstance;
    }

    /**
     * 快速路径方法：获取已存在的 LiveData 实例，避免不必要的创建
     * 解决泛型强转编译错误：增加类型校验 + 桥接转换
     *
     * @param eventType 事件类的 Class 对象
     * @param <T>       事件数据类型
     * @return 已存在的 LiveData 实例，不存在则返回 null
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> MutableLiveData<Event<T>> getLiveDataIfExists(Class<T> eventType) {
        // 1. 先取出 Map 中的 LiveData（原始类型，避开泛型检查）
        MutableLiveData rawLiveData = (MutableLiveData) mEventMap.get(eventType);
        if (rawLiveData == null) {
            return null;
        }
        // 2. 运行时类型校验（兜底，确保转换安全）
        Object value = rawLiveData.getValue();
        if (value == null || eventType.isInstance(((Event) value).getContent())) {
            return (MutableLiveData<Event<T>>) rawLiveData;
        }
        return null;
    }

    /**
     * 核心方法：创建/获取指定事件类型对应的 LiveData 实例
     * 解决泛型强转编译错误：先创建泛型匹配的 LiveData，再存入 Map
     *
     * @param eventType 事件类的 Class 对象（如 LoginSuccessEvent.class）
     * @param <T>       事件数据类型
     * @return 对应事件类型的 MutableLiveData 实例，用于事件分发
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> MutableLiveData<Event<T>> createOrGetLiveData(Class<T> eventType) {
        // 1. 先尝试获取已存在的实例（避免重复创建）
        MutableLiveData<Event<T>> existingLiveData = getLiveDataIfExists(eventType);
        if (existingLiveData != null) {
            return existingLiveData;
        }

        // 2. 不存在则创建「泛型精准匹配」的 LiveData
        MutableLiveData<Event<T>> newLiveData = new MutableLiveData<>();
        // 3. 存入 Map（转为原始类型，避开泛型检查）
        mEventMap.put(eventType, (MutableLiveData) newLiveData);
        return newLiveData;
    }

    /**
     * 检查事件是否为空
     *
     * @param event 事件对象
     * @param <T>   事件类型
     */
    private static <T> void checkEventNotNull(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
    }

    // ========================== 普通事件（非粘性）API ==========================
    public static <T> void post(@NonNull T event) {
        checkEventNotNull(event);
        Class<T> eventType = (Class<T>) event.getClass();
        getInstance().createOrGetLiveData(eventType).postValue(new Event<>(event));
    }

    public static <T> void observe(@NonNull LifecycleOwner owner,
                                   @NonNull Class<T> eventType,
                                   @NonNull Observer<T> observer) {
        getInstance().createOrGetLiveData(eventType).observe(owner, event -> {
            T content = event.getContentIfNotHandled();
            if (content != null) {
                observer.onChanged(content);
            }
        });
    }

    // ========================== 粘性事件 API ==========================
    public static <T> void postSticky(@NonNull T event) {
        checkEventNotNull(event);
        Class<T> eventType = (Class<T>) event.getClass();
        MutableLiveData<Event<T>> liveData = getInstance().createOrGetLiveData(eventType);
        liveData.setValue(new Event<>(event));
    }

    public static <T> void observeSticky(@NonNull LifecycleOwner owner,
                                         @NonNull Class<T> eventType,
                                         @NonNull Observer<T> observer) {
        getInstance().createOrGetLiveData(eventType).observe(owner, event -> {
            T content = event.getStickyContent();
            if (content != null) {
                observer.onChanged(content);
            }
        });
    }

    public static <T> void removeStickyEvent(@NonNull Class<T> eventType) {
        MutableLiveData<Event<T>> liveData = getInstance().getLiveDataIfExists(eventType);
        if (liveData != null) {
            liveData.setValue(null);
        }
    }

    // ========================== 性能优化/资源清理 API ==========================
    public static <T> void postAsync(@NonNull T event) {
        checkEventNotNull(event);
        Class<T> eventType = (Class<T>) event.getClass();
        EVENT_POOL.execute(() -> getInstance().createOrGetLiveData(eventType).postValue(new Event<>(event)));
    }

    public static void clearUnusedLiveData() {
        LiveDataBus instance = getInstance();
        try {
            for (ConcurrentHashMap.Entry<Class<?>, MutableLiveData<?>> entry : instance.mEventMap.entrySet()) {
                MutableLiveData<?> liveData = entry.getValue();
                if (liveData != null && getObserverCount(liveData) == 0) {
                    instance.mEventMap.remove(entry.getKey());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> void removeLiveData(@NonNull Class<T> eventType) {
        getInstance().mEventMap.remove(eventType);
    }

    /**
     * 私有工具方法：反射获取 LiveData 内部的观察者数量
     */
    private static int getObserverCount(MutableLiveData<?> liveData) {
        try {
            Field mObserversField = androidx.lifecycle.LiveData.class.getDeclaredField("mObservers");
            mObserversField.setAccessible(true);
            Object mObservers = mObserversField.get(liveData);
            if (mObservers instanceof java.util.Map) {
                return ((java.util.Map<?, ?>) mObservers).size();
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 事件包装类：解决 LiveData 粘性事件的消费控制问题
     *
     * @param <T> 事件数据类型
     */
    static class Event<T> {
        private final T content;
        private boolean hasBeenHandled = false;

        public Event(T content) {
            this.content = content;
        }

        // 新增：获取原始内容（用于类型校验）
        public T getContent() {
            return content;
        }

        @Nullable
        public T getContentIfNotHandled() {
            if (hasBeenHandled) {
                return null;
            }
            hasBeenHandled = true;
            return content;
        }

        public T getStickyContent() {
            return content;
        }
    }
}