package com.linewell.lxhdemo.thirdAppUtil;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 百度地图工具类，用于调起百度地图客户端的各种功能
 * 支持的功能包括：展示地图、自定义打点点、展示地图图区、地址解析、反向地址解析、
 * 路线规划（公交、驾车、步行、骑行）、公交地铁线路查询、导航（驾车、骑行、步行）
 *
 * @author YourCompanyName
 * @version 1.0
 */
public class BaiduMapAppUtils {
    private static final String TAG = "BaiduMapUtils";

    // 百度地图包名
    public static final String BAIDU_MAP_PACKAGE_NAME = "com.baidu.BaiduMap";

    // 坐标系类型
    public enum CoordType {
        BD09LL("bd09ll"),      // 百度经纬度坐标
        BD09MC("bd09mc"),      // 百度墨卡托坐标
        GCJ02("gcj02"),        // 国测局加密坐标
        WGS84("wgs84");        // GPS设备获取的坐标

        private final String value;

        CoordType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    // 路线规划模式
    public enum RouteMode {
        DRIVING("driving"),    // 驾车
        TRANSIT("transit"),    // 公交
        WALKING("walking"),    // 步行
        RIDING("riding");      // 骑行

        private final String value;

        RouteMode(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    // 公交检索策略
    public enum TransitStrategy {
        RECOMMEND(0),          // 推荐路线
        LESS_TRANSFER(2),      // 少换乘
        LESS_WALK(3),          // 少步行
        NO_SUBWAY(4),          // 不坐地铁
        TIME_FIRST(5),         // 时间短
        SUBWAY_FIRST(6);       // 地铁优先

        private final int value;

        TransitStrategy(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    // 驾车路线规划类型
    public enum DrivingRouteType {
        DEFAULT("DEFAULT"),    // 不选择偏好
        AVOID_CONGESTION("BLK"), // 躲避拥堵
        HIGHWAY_FIRST("TIME"),  // 高速优先
        NO_HIGHWAY("DIS"),      // 不走高速
        LESS_FEE("FEE");        // 少收费

        private final String value;

        DrivingRouteType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    // 导航类型
    public enum NaviType {
        DRIVING("navi"),       // 驾车导航
        WALKING("walknavi"),   // 步行导航
        RIDING("bikenavi");    // 骑行导航

        private final String value;

        NaviType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    // 应用来源标识（必须设置，格式：andr.companyName.appName）
    private final String mAppSource;

    // 坐标类型（默认为百度经纬度坐标）
    private CoordType mCoordType = CoordType.BD09LL;

    /**
     * 构造函数
     *
     * @param appSource 应用来源标识，格式：andr.companyName.appName
     */
    public BaiduMapAppUtils(String appSource, Context context) {
        if (TextUtils.isEmpty(appSource)) {
            throw new IllegalArgumentException("appSource cannot be empty");
        }
        this.mAppSource = context.getPackageName();
    }

    /**
     * 设置坐标类型
     *
     * @param coordType 坐标类型
     */
    public void setCoordType(CoordType coordType) {
        if (coordType != null) {
            this.mCoordType = coordType;
        }
    }

    /**
     * 检查设备是否安装了百度地图
     *
     * @param context 上下文
     * @return true：已安装；false：未安装
     */
    public boolean isBaiduMapInstalled(Context context) {
        if (context == null) {
            return false;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            packageManager.getPackageInfo(BAIDU_MAP_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Baidu Map is not installed");
            return false;
        }
    }

    /**
     * 跳转到应用市场下载百度地图
     *
     * @param context 上下文
     */
    public void goToMarketForBaiduMap(Context context) {
        if (context == null) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + BAIDU_MAP_PACKAGE_NAME));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open market", e);
            // 如果应用市场无法打开，跳转到百度地图官网
            goToBaiduMapWebsite(context);
        }
    }

    /**
     * 跳转到百度地图官网
     *
     * @param context 上下文
     */
    public void goToBaiduMapWebsite(Context context) {
        if (context == null) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // 应用商店不可用（如无安装应用商店），跳百度地图官网下载
            intent.setData(Uri.parse("https://map.baidu.com/zt/client/index/"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Baidu Map website", e);
        }
    }

    /**
     * 展示地图
     *
     * @param context 上下文
     * @param lat     纬度
     * @param lng     经度
     * @param zoom    缩放级别（可选，范围1-21）
     * @return true：调用成功；false：调用失败
     */
    public boolean showMap(Context context, double lat, double lng, Integer zoom) {
        if (context == null) {
            return false;
        }

        try {
            StringBuilder sb = new StringBuilder("baidumap://map/show");
            sb.append("?center=").append(lat).append(",").append(lng);
            sb.append("&coord_type=").append(mCoordType);
            sb.append("&traffic=on").append(mCoordType);
            if (zoom != null && zoom >= 1 && zoom <= 21) {
                sb.append("&zoom=").append(zoom);
            } else {
                sb.append("&zoom=12");
            }
            sb.append("&src=").append(mAppSource);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
            intent.setPackage(BAIDU_MAP_PACKAGE_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to show map", e);
            return false;
        }
    }

    /**
     * 自定义打点
     *
     * @param context 上下文
     * @param lat     纬度
     * @param lng     经度
     * @param title   点标记名称（可选）
     * @param content 点标记描述（可选）
     * @param traffic 是否显示交通状况（可选，默认false）
     * @return true：调用成功；false：调用失败
     */
    public boolean addMarker(Context context, double lat, double lng, String title, String content, Boolean traffic) {
        if (context == null) {
            return false;
        }

        try {
            StringBuilder sb = new StringBuilder("baidumap://map/marker?");
            sb.append("location=").append(lat).append(",").append(lng);
            if (!TextUtils.isEmpty(title)) {
                sb.append("&title=").append(encodeUrl(title));
            }
            if (!TextUtils.isEmpty(content)) {
                sb.append("&content=").append(encodeUrl(content));
            }
            sb.append("&coord_type=").append(mCoordType);
            sb.append("&src=").append(mAppSource);
            if (traffic != null && traffic) {
                sb.append("&traffic=on");
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
            intent.setPackage(BAIDU_MAP_PACKAGE_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to add marker", e);
            return false;
        }
    }

    /**
     * 展示地图图区（根据坐标范围）
     *
     * @param context 上下文
     * @return true：调用成功；false：调用失败
     */
    public boolean showMapArea(Context context) {
        if (context == null) {
            return false;
        }

        try {
            StringBuilder sb = new StringBuilder("baidumap://map?");
            sb.append("&src=").append(mAppSource);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
            intent.setPackage(BAIDU_MAP_PACKAGE_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to show map area", e);
            return false;
        }
    }

    /**
     * 地址解析（地理编码）
     * 注意：此功能需要使用百度地图Web服务API，需要网络请求
     *
     * @param address 地址
     * @param city    城市（可选）
     * @return 地理编码请求URL
     */
    public String getGeocodeUrl(String address, String city) {
        if (TextUtils.isEmpty(address)) {
            return null;
        }

        try {
            StringBuilder sb = new StringBuilder("baidumap://map/geocoder");
            sb.append("?address=").append(encodeUrl(address));
            sb.append("&src=").append(mCoordType);

            if (!TextUtils.isEmpty(city)) {
                sb.append("&city=").append(encodeUrl(city));
            }

            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get geocode url", e);
            return null;
        }
    }

    /**
     * 反向地址解析（逆地理编码）
     * 注意：此功能需要使用百度地图Web服务API，需要网络请求
     *
     * @param lat    纬度
     * @param lng    经度
     * @param pois   是否显示周边POI（可选，默认false）
     * @param radius POI召回半径（可选，默认1000米，范围0-1000）
     * @return 逆地理编码请求URL
     */
    public String getReverseGeocodeUrl(double lat, double lng, Boolean pois, Integer radius) {
        try {
            StringBuilder sb = new StringBuilder("baidumap://map/geocoder");
            sb.append("?location=").append(lat).append(",").append(lng);
            sb.append("&coord_type=").append(mCoordType);
            sb.append("&src=").append(mCoordType);

            if (pois != null && pois) {
                sb.append("&pois=1");
                if (radius != null && radius >= 0 && radius <= 1000) {
                    sb.append("&radius=").append(radius);
                }
            }

            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get reverse geocode url", e);
            return null;
        }
    }

    /**
     * 路线规划
     *
     * @param context          上下文
     * @param mode             路线规划模式
     * @param originName       起点名称（可选）
     * @param originLat        起点纬度
     * @param originLng        起点经度
     * @param destName         终点名称（可选）
     * @param destLat          终点纬度
     * @param destLng          终点经度
     * @param region           城市名（可选）
     * @param transitStrategy  公交检索策略（仅在mode为TRANSIT时有效）
     * @param drivingRouteType 驾车路线类型（仅在mode为DRIVING时有效）
     * @return true：调用成功；false：调用失败
     */
    public boolean planRoute(Context context, RouteMode mode,
                             String originName, double originLat, double originLng,
                             String destName, double destLat, double destLng,
                             String region, TransitStrategy transitStrategy, DrivingRouteType drivingRouteType) {


        try {
            StringBuilder sb = new StringBuilder("baidumap://map/direction?");

            // 起点
            sb.append("origin=");
            if (!TextUtils.isEmpty(originName)) {
                sb.append("name:").append(encodeUrl(originName)).append("|");
            }
            sb.append("latlng:").append(originLat).append(",").append(originLng);

            // 终点
            sb.append("&destination=");
            if (!TextUtils.isEmpty(destName)) {
                sb.append("name:").append(encodeUrl(destName)).append("|");
            }
            sb.append("latlng:").append(destLat).append(",").append(destLng);

            // 路线模式
            sb.append("&mode=").append(mode);

            // 坐标类型
            sb.append("&coord_type=").append(mCoordType);

            // 来源
            sb.append("&src=").append(mAppSource);

            // 城市
            if (!TextUtils.isEmpty(region)) {
                sb.append("region=").append(encodeUrl(region));
            }

            // 公交检索策略
            if (mode == RouteMode.TRANSIT && transitStrategy != null) {
                sb.append("&sy=").append(transitStrategy.getValue());
            }

            // 驾车路线类型
            if (mode == RouteMode.DRIVING && drivingRouteType != null) {
                sb.append("&car_type=").append(drivingRouteType);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
            intent.setPackage(BAIDU_MAP_PACKAGE_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to plan route", e);
            return false;
        }
    }

    /**
     * 公交、地铁线路查询
     *
     * @param context  上下文
     * @param city     城市名
     * @param lineName 线路名称
     * @return true：调用成功；false：调用失败
     */
    public boolean searchLine(Context context, String city, String lineName) {
        if (context == null || TextUtils.isEmpty(city) || TextUtils.isEmpty(lineName)) {
            return false;
        }

        try {
            StringBuilder sb = new StringBuilder("baidumap://map/line?");
            sb.append("region=").append(encodeUrl(city));
            sb.append("&name=").append(encodeUrl(lineName));
            sb.append("&src=").append(mAppSource);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
            intent.setPackage(BAIDU_MAP_PACKAGE_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to search line", e);
            return false;
        }
    }
//导航驾车
    public boolean navigateCar(Context context, String query, double destLat, double destLng) {

        try {
            StringBuilder sb = new StringBuilder("baidumap://map/navi").append("?");

            // 终点
            if (!TextUtils.isEmpty(query)) {
                sb.append("query=").append(query);
            }
            // 终点
            sb.append("&location=").append(destLat).append(",").append(destLng);
            sb.append("&coord_type=").append(mCoordType);
            // 来源
            sb.append("&src=").append(mAppSource);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
            intent.setPackage(BAIDU_MAP_PACKAGE_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate", e);
            return false;
        }
    }

    /**
     * 导航（骑行）
     *
     * @param context          上下文
     * @param destLat          终点纬度
     * @param destLng          终点经度
     * @return true：调用成功；false：调用失败
     */
    public boolean navigateBike(Context context, double startLat, double startLng, double destLat, double destLng) {


        try {
            StringBuilder sb = new StringBuilder("baidumap://map/bikenavi").append("?");
            // 起点
            sb.append("&origin=").append(startLat).append(",").append(startLng);
            // 终点
            sb.append("&destination=").append(destLat).append(",").append(destLng);
            sb.append("&coord_type=").append(mCoordType);
            // 来源
            sb.append("&src=").append(mAppSource);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
            intent.setPackage(BAIDU_MAP_PACKAGE_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate", e);
            return false;
        }
    }

    /**
     * 导航（步行）
     *
     * @param context          上下文
     * @param destLat          终点纬度
     * @param destLng          终点经度
     * @return true：调用成功；false：调用失败
     */
    public boolean navigateWalk(Context context, double startLat, double startLng, double destLat, double destLng) {


        try {
            StringBuilder sb = new StringBuilder("baidumap://map/bikenavi").append("?");
            // 起点
            sb.append("&origin=").append(startLat).append(",").append(startLng);
            // 终点
            sb.append("&destination=").append(destLat).append(",").append(destLng);
            sb.append("&coord_type=").append(mCoordType);
            // 来源
            sb.append("&src=").append(mAppSource);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
            intent.setPackage(BAIDU_MAP_PACKAGE_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate", e);
            return false;
        }
    }


    /**
     * URL编码
     *
     * @param value 要编码的字符串
     * @return 编码后的字符串
     */
    private String encodeUrl(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to encode URL", e);
            return value;
        }
    }
}
