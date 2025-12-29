package com.linewell.lxhdemo.liveDataBus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * 发送
 * 1. 发送普通事件（非粘性）LiveDataBus.post(new LoginSuccessEvent("1001", "张三")); 也可以用MyEvent 数据类来统一发送接收
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
  1. 清除指定类型的粘性事件缓存（后续订阅收不到历史）
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
 * 最终版：兼顾多观察者接收 + 精准区分普通/粘性事件
 * 核心特性：
 * 1. 普通事件（post）：仅接收订阅后发送的事件，多观察者均可接收
 * 2. 粘性事件（postSticky）：接收历史事件，多观察者均可接收
 * 3. 自动生命周期感知，无内存泄漏
 * 4. 线程安全，支持多线程发送/订阅
 */
public class LiveDataBus {
    private static volatile LiveDataBus sInstance;
    private final ConcurrentHashMap<Class<?>, MutableLiveData<?>> mEventMap;
    private static final ExecutorService EVENT_POOL = Executors.newSingleThreadExecutor();

    private LiveDataBus() {
        mEventMap = new ConcurrentHashMap<>();
    }

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> MutableLiveData<Event<T>> getLiveDataIfExists(Class<T> eventType) {
        MutableLiveData rawLiveData = (MutableLiveData) mEventMap.get(eventType);
        if (rawLiveData == null) {
            return null;
        }
        Object value = rawLiveData.getValue();
        if (value == null || eventType.isInstance(((Event) value).getContent())) {
            return (MutableLiveData<Event<T>>) rawLiveData;
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> MutableLiveData<Event<T>> createOrGetLiveData(Class<T> eventType) {
        MutableLiveData<Event<T>> existingLiveData = getLiveDataIfExists(eventType);
        if (existingLiveData != null) {
            return existingLiveData;
        }
        MutableLiveData<Event<T>> newLiveData = new MutableLiveData<>();
        mEventMap.put(eventType, (MutableLiveData) newLiveData);
        return newLiveData;
    }

    private static <T> void checkEventNotNull(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
    }

    // ========================== 普通事件（非粘性）API ==========================
    /**
     * 发送普通事件：仅订阅后发送的事件会被接收，多观察者均可接收
     */
    public static <T> void post(@NonNull T event) {
        checkEventNotNull(event);
        Class<T> eventType = (Class<T>) event.getClass();
        // 标记为「非粘性事件」
        getInstance().createOrGetLiveData(eventType).postValue(new Event<>(event, false));
    }

    /**
     * 订阅普通事件：仅接收订阅后发送的普通事件，过滤历史缓存的普通事件
     */
    public static <T> void observe(@NonNull LifecycleOwner owner,
                                   @NonNull Class<T> eventType,
                                   @NonNull Observer<T> observer) {
        MutableLiveData<Event<T>> liveData = getInstance().createOrGetLiveData(eventType);
        long subscribeTime = System.currentTimeMillis(); // 记录订阅时间
        liveData.observe(owner, event -> {
            // 普通事件：只处理「非粘性事件」+「订阅后发送的事件」
            if (!event.isSticky()) {
                if (event.getSendTime() > subscribeTime) {
                    T content = event.getContent();
                    if (content != null) {
                        observer.onChanged(content);
                    }
                }
            }
        });
    }

    // ========================== 粘性事件 API ==========================
    /**
     * 发送粘性事件：历史事件会被缓存，新订阅的观察者也能接收
     */
    public static <T> void postSticky(@NonNull T event) {
        checkEventNotNull(event);
        Class<T> eventType = (Class<T>) event.getClass();
        // 标记为「粘性事件」
        MutableLiveData<Event<T>> liveData = getInstance().createOrGetLiveData(eventType);
        liveData.setValue(new Event<>(event, true));
    }

    /**
     * 订阅粘性事件：完全对标EventBus的sticky=true
     * 1. 订阅时：接收历史的postSticky粘性事件
     * 2. 订阅后：接收所有post/postSticky发送的事件
     */
    public static <T> void observeSticky(@NonNull LifecycleOwner owner,
                                         @NonNull Class<T> eventType,
                                         @NonNull Observer<T> observer) {
        MutableLiveData<Event<T>> liveData = getInstance().createOrGetLiveData(eventType);
        // 核心修改1：记录订阅时间戳，区分历史事件和新事件
        long subscribeTime = System.currentTimeMillis();

        liveData.observe(owner, event -> {
            if (event == null || event.getContent() == null) {
                return;
            }
            // 核心修改2：对标EventBus的sticky=true逻辑
            if (event.getSendTime() <= subscribeTime) {
                // 情况1：订阅时触发的历史事件 → 仅处理postSticky的粘性事件
                if (event.isSticky()) {
                    observer.onChanged(event.getContent());
                }
            } else {
                // 情况2：订阅后发送的新事件 → 处理所有事件（post/postSticky）
                observer.onChanged(event.getContent());
            }
        });
    }

    /**
     * 清除指定类型的粘性事件缓存
     */
    public static <T> void removeStickyEvent(@NonNull Class<T> eventType) {
        MutableLiveData<Event<T>> liveData = getInstance().getLiveDataIfExists(eventType);
        if (liveData != null) {
            liveData.setValue(null);
        }
    }

    // ========================== 性能优化 API ==========================
    public static <T> void postAsync(@NonNull T event) {
        checkEventNotNull(event);
        Class<T> eventType = (Class<T>) event.getClass();
        EVENT_POOL.execute(() -> getInstance().createOrGetLiveData(eventType).postValue(new Event<>(event, false)));
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
     * 事件包装类：新增粘性标记 + 发送时间戳
     * @param <T> 事件数据类型
     */
    static class Event<T> {
        private final T content;
        // 核心新增：标记是否是粘性事件
        private final boolean isSticky;
        private final long sendTime; // 事件发送时间戳

        /**
         * 构造方法
         * @param content 事件内容
         * @param isSticky 是否是粘性事件
         */
        public Event(T content, boolean isSticky) {
            this.content = content;
            this.isSticky = isSticky;
            this.sendTime = System.currentTimeMillis(); // 记录发送时间
        }

        // 获取事件内容（无消费标记，多观察者均可接收）
        public T getContent() {
            return content;
        }

        // 判断是否是粘性事件
        public boolean isSticky() {
            return isSticky;
        }

        @Nullable
        public T getContentIfNotHandled() {
            return content;
        }

        public T getStickyContent() {
            return content;
        }

        public long getSendTime() {
            return sendTime;
        }
    }
}