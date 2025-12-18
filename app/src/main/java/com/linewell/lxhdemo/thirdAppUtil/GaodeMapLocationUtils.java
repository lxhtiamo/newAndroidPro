package com.linewell.lxhdemo.thirdAppUtil;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.IReGeoLocationCallback;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.permission.PermissionLists;
import com.hjq.permissions.permission.base.IPermission;
import com.linewell.lxhdemo.thirdAppUtil.model.UnifiedLocationInfo;
import com.linewell.lxhdemo.thirdAppUtil.util.LocationConvertUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 高德地图定位工具类（模仿百度定位工具类结构，适配XXPermissions最新示例）
 * 使用步骤：
 * 1. 初始化定位工具类
 *    AMapLocationUtils locationUtils = AMapLocationUtils.getInstance(this);
 * 2. 设置定位监听器
 *    locationUtils.setLocationListener(this);
 * 3. 可选配置：
 *    - 设置定位间隔为2秒：locationUtils.setScanSpan(2000);
 *    - 设置定位模式为高精度：locationUtils.setLocationMode(AMapLocationMode.Hight_Accuracy);
 *    - 设置是否需要地址：locationUtils.setNeedAddress(true);
 *    - 设置定位场景（签到）：locationUtils.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);
 * 4. 启动定位（传入Activity用于权限申请）
 *    locationUtils.startLocation(this);
 * 5. 销毁资源（如在Activity的onDestroy中）
 *    locationUtils.destroy();
 */
public class GaodeMapLocationUtils {
    private static final String TAG = "AMapLocationUtils";
    // 单例实例（volatile保证多线程可见性）
    private static volatile GaodeMapLocationUtils instance;

    // 上下文（使用ApplicationContext避免内存泄漏）
    private Context mContext;
    // 高德定位客户端
    private AMapLocationClient mLocationClient;
    // 高德定位选项
    private AMapLocationClientOption mLocationOption;
    // 外部定位监听器
    private LocationListener mLocationListener;
    // 内部高德定位监听器
    private MyAMapLocationListener mInnerLocationListener;
    // 逆地理编码回调（高德6.4.9+版本支持）
    private IReGeoLocationCallback mReGeoCallback;

    /**
     * 定位结果回调接口（与百度工具类保持一致的风格）
     */
    public interface LocationListener {
        /**
         * 定位成功回调
         * @param location 高德定位结果对象
         */
        void onLocationSuccess(UnifiedLocationInfo location);

        /**
         * 定位失败回调
         * @param errorMsg 失败原因描述
         */
        void onLocationFailure(String errorMsg);

        /**
         * 权限被拒绝回调（返回拒绝列表和是否永久拒绝，默认空实现）
         * @param deniedPermissions 被拒绝的权限列表
         * @param doNotAskAgain 是否永久拒绝（不再询问）
         */
        default void onPermissionDenied(List<String> deniedPermissions, boolean doNotAskAgain) {
        }
    }

    /**
     * 私有化构造方法
     * @param context 上下文（建议传入Activity或Application）
     */
    private GaodeMapLocationUtils(Context context) {
        this.mContext = context.getApplicationContext();
        // 初始化内部监听器
        mInnerLocationListener = new MyAMapLocationListener();
        // 初始化逆地理编码回调（可选）
        initReGeoCallback();
        // 初始化定位客户端和选项
        initLocationClientAndOption();
    }

    /**
     * 获取单例实例（双重检查锁保证线程安全）
     * @param context 上下文
     * @return 高德定位工具类实例
     */
    public static GaodeMapLocationUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (GaodeMapLocationUtils.class) {
                if (instance == null) {
                    instance = new GaodeMapLocationUtils(context);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化定位客户端和定位选项
     */
    private void initLocationClientAndOption() {
        try {
            // 初始化定位客户端（必须传入ApplicationContext）
            mLocationClient = new AMapLocationClient(mContext);
            // 初始化定位选项
            mLocationOption = new AMapLocationClientOption();

            // 设置默认定位参数（与百度工具类默认参数对齐）
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy); // 高精度模式
            mLocationOption.setInterval(2000); // 定位间隔1000ms（最低1000ms）
            mLocationOption.setNeedAddress(true); // 需要地址信息
            mLocationOption.setHttpTimeOut(20000); // 定位超时时间20秒
            mLocationOption.setLocationCacheEnable(true); // 开启定位缓存
            mLocationOption.setMockEnable(true); //是否允许模拟软件Mock位置结果 不允许模拟位置,默认为true

            // 设置定位监听器
            mLocationClient.setLocationListener(mInnerLocationListener);
            // 设置逆地理编码回调（高德6.4.9+版本）
            mLocationClient.setReGeoLocationCallback(mReGeoCallback);

        } catch (Exception e) {
            Log.e(TAG, "初始化定位客户端失败", e);
            throw new RuntimeException("初始化高德定位客户端失败：" + e.getMessage());
        }
    }

    /**
     * 初始化逆地理编码回调（可选）
     */
    private void initReGeoCallback() {
        mReGeoCallback = new IReGeoLocationCallback() {
            @Override
            public void onReGeoLocation(AMapLocation reGeoLocation) {
                // 逆地理编码结果回调（可根据需求处理，这里仅打印日志）
                if (reGeoLocation != null) {
                    Log.d(TAG, "逆地理编码结果：" + reGeoLocation.toStr());
                }
            }
        };
    }

    /**
     * 开始定位（包含权限申请逻辑，与百度工具类一致）
     * @param activity 用于发起权限请求的Activity
     */
    public void startLocation(android.app.Activity activity) {
        if (mLocationClient == null) {
            // 若定位客户端被销毁，重新初始化
            initLocationClientAndOption();
        }

        // 收集所需权限
        List<IPermission> permissionList = new ArrayList<>();
        permissionList.add(PermissionLists.getAccessFineLocationPermission());
        permissionList.add(PermissionLists.getAccessCoarseLocationPermission());
        // Android 10+（API 29+）如果需要后台定位，添加后台定位权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionList.add(PermissionLists.getAccessBackgroundLocationPermission());
        }

        // 检查权限是否已授予
        if (XXPermissions.isGrantedPermissions(mContext, permissionList)) {
            // 权限已授予，直接启动定位
            startLocationClient();
            return;
        }

        // 权限未授予，发起权限申请
        XXPermissions.with(activity)
                .permissions(permissionList)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onResult(@NonNull List<IPermission> grantedList, @NonNull List<IPermission> deniedList) {
                        if (deniedList.isEmpty()) {
                            // 所有权限授予，启动定位
                            startLocationClient();
                        } else {
                            // 权限被拒绝，转换为字符串权限列表回调
                            List<String> deniedPermissions = new ArrayList<>();
                            for (IPermission permission : deniedList) {
                                deniedPermissions.add(permission.getPermissionName());
                            }
                            // 判断是否永久拒绝
                            boolean doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(activity, deniedList);
                            // 回调权限拒绝事件
                            if (mLocationListener != null) {
                                mLocationListener.onPermissionDenied(deniedPermissions, doNotAskAgain);
                            }
                            // 通知定位失败
                            notifyFailure("未获得定位权限，无法获取位置信息");
                        }
                    }
                });
    }

    /**
     * 实际启动定位客户端
     */
    private void startLocationClient() {
        if (mLocationClient != null && !mLocationClient.isStarted()) {
            // 应用定位选项
            mLocationClient.setLocationOption(mLocationOption);
            //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
            mLocationClient.stopLocation();
            // 启动定位
            mLocationClient.startLocation();
            Log.d(TAG, "高德定位客户端已启动");
        }
    }

    /**
     * 停止定位
     */
    public void stopLocation() {
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.stopLocation();
            Log.d(TAG, "高德定位客户端已停止");
        }
    }

    /**
     * 销毁定位资源（释放内存，避免泄漏）
     */
    public void destroy() {
        if (mLocationClient != null) {
            // 停止定位
            stopLocation();
            // 销毁定位客户端（销毁后需重新new实例才能再次定位）
            mLocationClient.onDestroy();
            mLocationClient = null;
        }
        // 置空监听器和实例
        mLocationListener = null;
        mInnerLocationListener = null;
        mReGeoCallback = null;
        instance = null;
        Log.d(TAG, "高德定位资源已销毁");
    }

    /**
     * 通知定位失败
     * @param errorMsg 失败信息
     */
    private void notifyFailure(String errorMsg) {
        Log.e(TAG, errorMsg);
        if (mLocationListener != null) {
            mLocationListener.onLocationFailure(errorMsg);
        }
    }

    // ====================== 对外配置方法（与百度工具类对齐，扩展高德特有功能） ======================

    /**
     * 设置定位监听器
     * @param listener 外部监听器
     */
    public void setLocationListener(LocationListener listener) {
        this.mLocationListener = listener;
    }

    /**
     * 设置定位间隔（单位：毫秒，最低1000ms）
     * @param span 定位间隔
     */
    public void setScanSpan(int span) {
        if (mLocationOption != null && span >= 1000) {
            mLocationOption.setInterval(span);
            // 应用新的选项（如果已启动定位，需要重新设置）
            if (mLocationClient != null) {
                mLocationClient.setLocationOption(mLocationOption);
            }
        }
    }

    /**
     * 设置是否需要地址信息
     * @param need 是否需要
     */
    public void setNeedAddress(boolean need) {
        if (mLocationOption != null) {
            mLocationOption.setNeedAddress(need);
            if (mLocationClient != null) {
                mLocationClient.setLocationOption(mLocationOption);
            }
        }
    }

    /**
     * 设置定位模式（高精度、低功耗、仅设备）
     * @param mode 定位模式
     */
    public void setLocationMode(AMapLocationClientOption.AMapLocationMode mode) {
        if (mLocationOption != null) {
            mLocationOption.setLocationMode(mode);
            if (mLocationClient != null) {
                mLocationClient.setLocationOption(mLocationOption);
            }
        }
    }

    /**
     * 设置定位场景（签到、出行、运动，高德3.7.0+支持）
     * @param purpose 定位场景
     */
    public void setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose purpose) {
        if (mLocationOption != null && mLocationClient != null) {
            mLocationOption.setLocationPurpose(purpose);
            mLocationClient.setLocationOption(mLocationOption);
            // 高德建议：设置场景后先stop再start以保证生效
            mLocationClient.stopLocation();
            mLocationClient.startLocation();
        }
    }

    /**
     * 设置单次定位（默认连续定位）
     * @param isOnce 是否单次定位
     */
    public void setOnceLocation(boolean isOnce) {
        if (mLocationOption != null) {
            mLocationOption.setOnceLocation(isOnce);
            if (mLocationClient != null) {
                mLocationClient.setLocationOption(mLocationOption);
            }
        }
    }

    // ====================== 内部高德定位监听器实现 ======================

    /**
     * 内部高德定位监听器（处理定位结果）
     */
    private class MyAMapLocationListener implements AMapLocationListener {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation == null) {
                notifyFailure("定位结果为空");
                return;
            }
            // 转换为统一对象
            UnifiedLocationInfo unifiedInfo = LocationConvertUtils.convertAMapLocationToUnified(amapLocation);


            // 高德定位成功的标志：错误码为0
            if (amapLocation.getErrorCode() == 0) {
                Log.d(TAG, "定位成功：" + amapLocation.toStr());

                // 高德6.4.9+版本：如果未开启地址，主动请求逆地理编码
                if (!mLocationOption.isNeedAddress()) {
                    if (mLocationClient != null) {
                        mLocationClient.getReGeoLocation(amapLocation);
                    }
                }

                // 回调定位成功结果
                if (mLocationListener != null) {
                    mLocationListener.onLocationSuccess(unifiedInfo);
                }
            } else {
                // 定位失败：获取错误码和错误信息
                String errorMsg = String.format("定位失败（错误码：%d，错误信息：%s）", amapLocation.getErrorCode(), amapLocation.getErrorInfo());
                notifyFailure(errorMsg);
            }
        }
    }
}