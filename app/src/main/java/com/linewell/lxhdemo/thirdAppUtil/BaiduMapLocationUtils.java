package com.linewell.lxhdemo.thirdAppUtil;

import android.content.Context;

import androidx.annotation.NonNull;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.permission.PermissionLists;
import com.hjq.permissions.permission.base.IPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * 百度地图定位工具类（严格适配XXPermissions最新示例）
 * 初始化定位工具类
 * locationUtils = BaiduMapLocationUtils.getInstance(this);
 * locationUtils.setLocationListener(this);
 * 可选：设置定位间隔为2秒
 * locationUtils.setScanSpan(2000);
 * 销毁资源
 * locationUtils.destroy();
 */
public class BaiduMapLocationUtils {
    private static final String TAG = "BaiduLocationUtils";
    private static volatile BaiduMapLocationUtils instance;
    private MyLocationListener myLocationListener;

    private LocationClient mLocationClient;
    private LocationListener mLocationListener;
    private Context mContext;
    private LocationClientOption mOption;


    // 定位结果回调接口
    public interface LocationListener {
        void onLocationSuccess(BDLocation location);

        void onLocationFailure(String errorMsg);

        // 权限被拒绝回调（返回拒绝列表和是否永久拒绝）
        default void onPermissionDenied(List<String> deniedPermissions, boolean doNotAskAgain) {
        }
    }

    private BaiduMapLocationUtils(Context context) {
        this.mContext = context.getApplicationContext();
        SDKInitializer.initialize(mContext);
        //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
        SDKInitializer.setCoordType(CoordType.BD09LL);
        // 初始化内部定位监听器
        myLocationListener = new MyLocationListener();
        initLocationClient();
    }

    public static BaiduMapLocationUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (BaiduMapLocationUtils.class) {
                if (instance == null) {
                    instance = new BaiduMapLocationUtils(context);
                }
            }
        }
        return instance;
    }

    private void initLocationClient() {
        try {
            mLocationClient = new LocationClient(mContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        mOption = new LocationClientOption();
        mOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
//注册监听函数
        //可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        mOption.setCoorType("bd09ll"); // 百度坐标系
        //可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的

        mOption.setScanSpan(1000); // 定位间隔(ms)

        mOption.setIsNeedAddress(true); // 需要地址信息
        //可选，设置是否需要地址描述//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        mOption.setIsNeedLocationDescribe(true);
        //可选，设置是否需要设备方向结果
        mOption.setNeedDeviceDirect(false);
        //可选，默认false，设置是否当卫星定位有效时按照1S1次频率输出卫星定位结果
        mOption.setLocationNotify(false);
        //可选，设置是否使用gps，默认false
        //使用高精度和仅用设备两种定位模式的，参数必须设置为true
        //可选，默认false，设置是否开启卫星定位
        mOption.setOpenGnss(true);
        //可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        mOption.setIsNeedAltitude(false);
        //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回调给开发者
        mOption.setOpenAutoNotifyMode(3000, 1, LocationClientOption.LOC_SENSITIVITY_HIGHT);

        //可选，设置是否当GPS有效时按照1S/1次频率输出GPS结果，默认false
        mOption.setLocationNotify(true);
        //可选，定位SDK内部是一个service，并放到了独立进程。
        //设置是否在stop的时候杀死这个进程，默认（建议）不杀死，即setIgnoreKillProcess(true)
        mOption.setIgnoreKillProcess(false);
        //可选，设置是否收集Crash信息，默认收集，即参数为false
        mOption.SetIgnoreCacheException(false);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        //可选，V7.2版本新增能力
        //如果设置了该接口，首次启动定位时，会先判断当前Wi-Fi是否超出有效期，若超出有效期，会先重新扫描Wi-Fi，然后定位
        mOption.setWifiCacheTimeOut(5 * 60 * 1000);
        //可选，设置是否需要过滤GPS仿真结果，默认需要，即参数为false
        mOption.setEnableSimulateGps(false);
        mOption.setIsNeedLocationPoiList(true); // 需要POI信息
        mLocationClient.registerLocationListener(myLocationListener);
        mLocationClient.setLocOption(mOption);
    }

    /**
     * 开始定位（带权限申请）
     *
     * @param activity 用于发起权限请求的Activity
     */
    public void startLocation(android.app.Activity activity) {
        if (mLocationClient == null) {
            initLocationClient();
        }
        List<IPermission> list = new ArrayList<>();
// 检查权限是否已授予
        list.add(PermissionLists.getAccessFineLocationPermission());
        list.add(PermissionLists.getAccessCoarseLocationPermission());
        if (XXPermissions.isGrantedPermissions(mContext, list)) {
            startLocationClient();
            return;
        }

        // 权限申请（严格遵循示例风格）
        XXPermissions.with(activity)
                // 传入定位所需权限列表（模仿示例中的PermissionLists调用）
                .permission(PermissionLists.getAccessFineLocationPermission())
                .permission(PermissionLists.getAccessCoarseLocationPermission())
                // 如需关闭错误检测，可添加此行
                //.unchecked()
                .request(new OnPermissionCallback() {
                    @Override
                    public void onResult(@NonNull List<IPermission> grantedList, @NonNull List<IPermission> deniedList) {
                        // 判断是否全部授予
                        boolean allGranted = deniedList.isEmpty();
                        if (!allGranted) {
                            // 转换为字符串权限列表（方便回调使用）
                            List<String> deniedPermissions = new ArrayList<>();
                            for (IPermission permission : deniedList) {
                                deniedPermissions.add(permission.getPermissionName());
                            }

                            // 判断是否有永久拒绝的权限（使用示例中的判断方式）
                            boolean doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(activity, deniedList);

                            // 通知权限被拒绝
                            if (mLocationListener != null) {
                                mLocationListener.onPermissionDenied(deniedPermissions, doNotAskAgain);
                            }
                            notifyFailure("没有获得定位权限，无法获取位置");
                            return;
                        }

                        // 全部权限授予，启动定位
                        startLocationClient();
                    }
                });
    }

    // 实际启动定位客户端
    private void startLocationClient() {
        if (!mLocationClient.isStarted()) {
            mLocationClient.start();
        }
    }

    /**
     * 停止定位
     */
    public void stopLocation() {
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.stop();
        }
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        if (mLocationClient != null) {
            // 移除监听器
            mLocationClient.unRegisterLocationListener(myLocationListener);
            mLocationClient.stop();
            mLocationClient = null;
        }
        mLocationListener = null;
        myLocationListener = null;
        instance = null;
    }

    // 通知定位失败
    private void notifyFailure(String msg) {
        if (mLocationListener != null) {
            mLocationListener.onLocationFailure(msg);
        }
    }

    // 配置方法
    public void setLocationListener(LocationListener listener) {
        this.mLocationListener = listener;
    }

    public void setScanSpan(int span) {
        if (mOption != null && span >= 1000) {
            mOption.setScanSpan(span);
            mLocationClient.setLocOption(mOption);
        }
    }

    public void setNeedAddress(boolean need) {
        if (mOption != null) {
            mOption.setIsNeedAddress(need);
            mLocationClient.setLocOption(mOption);
        }
    }

    // 百度定位内部监听类（显式定义）
    private class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null) {
                notifyFailure("定位结果为空");
                return;
            }

            // 处理定位结果
            switch (location.getLocType()) {
                case BDLocation.TypeGpsLocation:
                case BDLocation.TypeNetWorkLocation:
                case BDLocation.TypeOffLineLocation:
                    if (mLocationListener != null) {
                        mLocationListener.onLocationSuccess(location);
                    }
                    break;
                case BDLocation.TypeServerError:
                    notifyFailure("服务端错误");
                    break;
                case BDLocation.TypeNetWorkException:
                    notifyFailure("网络异常");
                    break;
                case BDLocation.TypeCriteriaException:
                    notifyFailure("无法获取定位信号");
                    break;
                default:
                    notifyFailure("定位失败（错误码：" + location.getLocType() + "）");
            }
        }
    }

}