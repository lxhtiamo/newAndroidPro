package com.linewell.lxhdemo.thirdAppUtil;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

/**
 * 高德地图导航工具类（极简版）
 * 简化版方法无需传入sourceApp，自动获取应用名称/包名；保留完整参数版供特殊需求
 */
public class GaodeMapAppUtils {

    // 常量定义
    public static final String GAODE_PACKAGE_NAME = "com.autonavi.minimap";
    private static final String SCHEME_AMAP = "androidamap://";
    private static final String SCHEME_AMAP_URI = "amapuri://";
    private static final String INTENT_ACTION = Intent.ACTION_VIEW;
    private static final String INTENT_CATEGORY = Intent.CATEGORY_DEFAULT;
    private static final String FEATURE_NAVI = "navi";
    private static final String FEATURE_OPEN = "openFeature";
    private static final String FEATURE_ON_FOOT = "OnFootNavi";
    private static final String FEATURE_ON_RIDE = "OnRideNavi";

    // 服务类型常量（新增功能）
    private static final String SERVICE_VIEW_MAP = "viewMap"; // 地图标注
    private static final String SERVICE_ROUTE_PLAN = "route/plan"; // 路径规划
    private static final String SERVICE_MY_LOCATION = "mylocation"; // 我的位置
    private static final String SERVICE_VIEW_REGEO = "viewReGeo"; // 逆地理编码
    private static final String SERVICE_ROOT_MAP = "rootmap"; // 地图主图

    // 路径规划-出行方式（t参数）
    public static final int ROUTE_TYPE_CAR = 0; // 驾车
    public static final int ROUTE_TYPE_BUS = 1; // 公交
    public static final int ROUTE_TYPE_WALK = 2; // 步行
    public static final int ROUTE_TYPE_RIDE = 3; // 骑行
    public static final int ROUTE_TYPE_TRAIN = 4; // 火车
    public static final int ROUTE_TYPE_LONG_DISTANCE = 5; // 长途车

    // 路径规划-导航方式（m参数）
    public static final int ROUTE_STRATEGY_FASTEST = 0; // 速度最快
    public static final int ROUTE_STRATEGY_CHEAPEST = 1; // 费用最少
    public static final int ROUTE_STRATEGY_SHORTEST = 2; // 距离最短
    public static final int ROUTE_STRATEGY_AVOID_HIGHWAY = 3; // 避免高速

    // 骑行类型常量
    public static final String RIDE_TYPE_ELECTRIC = "elebike";
    public static final String RIDE_TYPE_BICYCLE = "bike";

    // 导航方式常量（驾车）
    public static final int STYLE_FASTEST = 0;
    public static final int STYLE_CHEAPEST = 1;
    public static final int STYLE_SHORTEST = 2;
    public static final int STYLE_AVOID_HIGHWAY = 3;

    // 坐标加密常量
    public static final int DEV_NO_ENCRYPT = 0;
    public static final int DEV_ENCRYPT = 1;


    /**
     * 检查高德地图是否安装
     */
    public static boolean isGaodeInstalled(Context context) {
        if (context == null) return false;
        try {
            context.getPackageManager().getPackageInfo(GAODE_PACKAGE_NAME, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


    // ====================== 驾车导航 ======================

    /**
     * 简化版：启动驾车导航（无需sourceApp，自动获取）
     * @param context 上下文
     * @param lat 目的地纬度
     * @param lon 目的地经度
     */
    public static void startDriveNavi(Context context, double lat, double lon) {
        startDriveNavi(context, null, lat, lon, DEV_NO_ENCRYPT, null, STYLE_FASTEST);
    }

    /**
     * 完整参数版：启动驾车导航（支持自定义sourceApp及所有参数）
     * @param context 上下文
     * @param sourceApp 调用方名称（可为空，自动获取）
     * @param lat 目的地纬度
     * @param lon 目的地经度
     * @param dev 坐标加密（0/1，默认0）
     * @param poiname 目的地名称（可选）
     * @param style 导航方式（默认最快路线）
     */
    public static void startDriveNavi(Context context, String sourceApp, double lat, double lon,
                                      int dev, String poiname, int style) {
        checkContextValid(context);
        if (!isGaodeInstalled(context)) {
            throw new IllegalArgumentException("高德地图未安装");
        }

        String actualSource = getActualSourceApp(context, sourceApp);
        StringBuilder uriBuilder = new StringBuilder(SCHEME_AMAP)
                .append(FEATURE_NAVI)
                .append("?sourceApplication=").append(actualSource)
                .append("&lat=").append(lat)
                .append("&lon=").append(lon)
                .append("&dev=").append(dev >= 0 ? dev : DEV_NO_ENCRYPT)
                .append("&style=").append(style >= 0 ? style : STYLE_FASTEST);

        if (!TextUtils.isEmpty(poiname)) {
            uriBuilder.append("&poiname=").append(poiname);
        }

        startNaviIntent(context, uriBuilder.toString());
    }


    // ====================== 步行导航 ======================

    /**
     * 简化版：启动步行导航（无需sourceApp，自动获取）
     * @param context 上下文
     * @param lat 目的地纬度
     * @param lon 目的地经度
     */
    public static void startWalkNavi(Context context, double lat, double lon) {
        startWalkNavi(context, null, lat, lon, DEV_NO_ENCRYPT);
    }

    /**
     * 完整参数版：启动步行导航（支持自定义sourceApp及加密参数）
     * @param context 上下文
     * @param sourceApp 调用方名称（可为空）
     * @param lat 目的地纬度
     * @param lon 目的地经度
     * @param dev 坐标加密（0/1，默认0）
     */
    public static void startWalkNavi(Context context, String sourceApp, double lat, double lon, int dev) {
        checkContextValid(context);
        if (!isGaodeInstalled(context)) {
            throw new IllegalArgumentException("高德地图未安装");
        }

        String actualSource = getActualSourceApp(context, sourceApp);
        String uri = new StringBuilder(SCHEME_AMAP_URI)
                .append(FEATURE_OPEN)
                .append("?featureName=").append(FEATURE_ON_FOOT)
                .append("&sourceApplication=").append(actualSource)
                .append("&lat=").append(lat)
                .append("&lon=").append(lon)
                .append("&dev=").append(dev >= 0 ? dev : DEV_NO_ENCRYPT)
                .toString();

        startNaviIntent(context, uri);
    }


    // ====================== 骑行导航 ======================

    /**
     * 简化版：启动骑行导航（无需sourceApp，自动获取）
     * @param context 上下文
     * @param lat 目的地纬度
     * @param lon 目的地经度
     * @param rideType 骑行类型（用RIDE_TYPE_xxx常量）
     */
    public static void startRideNavi(Context context, double lat, double lon, String rideType) {
        startRideNavi(context, null, lat, lon, rideType, DEV_NO_ENCRYPT);
    }

    /**
     * 完整参数版：启动骑行导航（支持自定义sourceApp及加密参数）
     * @param context 上下文
     * @param sourceApp 调用方名称（可为空）
     * @param lat 目的地纬度
     * @param lon 目的地经度
     * @param rideType 骑行类型（必填）
     * @param dev 坐标加密（0/1，默认0）
     */
    public static void startRideNavi(Context context, String sourceApp, double lat, double lon,
                                     String rideType, int dev) {
        checkContextValid(context);
        if (!isGaodeInstalled(context)) {
            throw new IllegalArgumentException("高德地图未安装");
        }
        if (TextUtils.isEmpty(rideType)) {
            rideType=RIDE_TYPE_BICYCLE;
        }

        String actualSource = getActualSourceApp(context, sourceApp);
        String uri = new StringBuilder(SCHEME_AMAP_URI)
                .append(FEATURE_OPEN)
                .append("?featureName=").append(FEATURE_ON_RIDE)
                .append("&rideType=").append(rideType)
                .append("&sourceApplication=").append(actualSource)
                .append("&lat=").append(lat)
                .append("&lon=").append(lon)
                .append("&dev=").append(dev >= 0 ? dev : DEV_NO_ENCRYPT)
                .toString();

        startNaviIntent(context, uri);
    }
    /**
     * 简化版：启动地图标注（显示POI点）
     * @param context 上下文
     * @param poiname POI名称（必填）
     * @param lat 纬度（必填）
     * @param lon 经度（必填）
     */
    public static void startMapMarker(Context context, String poiname, double lat, double lon) {
        startMapMarker(context, null, poiname, lat, lon, DEV_NO_ENCRYPT);
    }

    /**
     * 完整参数版：启动地图标注
     * @param context 上下文
     * @param sourceApp 调用方名称（可为空，自动获取）
     * @param poiname POI名称（必填）
     * @param lat 纬度（必填）
     * @param lon 经度（必填）
     * @param dev 坐标加密（0/1，默认0）
     */
    public static void startMapMarker(Context context, String sourceApp, String poiname, double lat, double lon, int dev) {
        checkContextValid(context);
        if (!isGaodeInstalled(context)) {
            throw new IllegalArgumentException("高德地图未安装");
        }
        if (TextUtils.isEmpty(poiname)) {
            throw new IllegalArgumentException("POI名称（poiname）不能为空");
        }

        String actualSource = getActualSourceApp(context, sourceApp);
        String uri = new StringBuilder(SCHEME_AMAP)
                .append(SERVICE_VIEW_MAP)
                .append("?sourceApplication=").append(actualSource)
                .append("&poiname=").append(poiname)
                .append("&lat=").append(lat)
                .append("&lon=").append(lon)
                .append("&dev=").append(dev >= 0 ? dev : DEV_NO_ENCRYPT)
                .toString();

        startNaviIntent(context, uri);
    }


    // ====================== 新增功能：路径规划 ======================

    /**
     * 简化版：启动路径规划（仅必填参数）
     * @param context 上下文
     * @param dlat 终点纬度（必填）
     * @param dlon 终点经度（必填）
     * @param routeType 出行方式（用ROUTE_TYPE_xxx常量，必填）
     * @param strategy 导航策略（用ROUTE_STRATEGY_xxx常量，必填）
     */
    public static void startRoutePlan(Context context, double dlat, double dlon, int routeType, int strategy) {
        startRoutePlan(context, null, 0, 0, null, null, dlat, dlon, null,null,
                routeType, DEV_NO_ENCRYPT, strategy, 0, null, null, null, null);
    }

    /**
     * 完整参数版：启动路径规划（支持所有参数）
     * @param context 上下文
     * @param sourceApp 调用方名称（可为空）
     * @param slat 起点纬度（可选，默认当前位置）
     * @param slon 起点经度（可选，默认当前位置）
     * @param sname 起点名称（可选）
     * @param sid 起点POI ID（可选）
     * @param dlat 终点纬度（必填）
     * @param dlon 终点经度（必填）
     * @param dname 终点名称（可选）
     * @param routeType 出行方式（ROUTE_TYPE_xxx，必填）
     * @param dev 坐标加密（0/1，默认0）
     * @param strategy 导航策略（ROUTE_STRATEGY_xxx，必填）
     * @param vian 途经点数量（可选，需与途经点坐标/名称数量一致）
     * @param vialons 途经点经度（可选，用"|"分隔）
     * @param vialats 途经点纬度（可选，用"|"分隔）
     * @param vianames 途经点名称（可选，用"|"分隔）
     * @param rideType 骑行类型（仅routeType=3时有效，用RIDE_TYPE_xxx）
     */
    public static void startRoutePlan(Context context, String sourceApp, double slat, double slon, String sname, String sid,
                                      double dlat, double dlon, String dname, String did, int routeType, int dev, int strategy,
                                      int vian, String vialons, String vialats, String vianames, String rideType) {
        checkContextValid(context);
        if (!isGaodeInstalled(context)) {
            throw new IllegalArgumentException("高德地图未安装");
        }
        // 校验必填参数
        if (routeType < 0 || routeType > 5) {
            throw new IllegalArgumentException("出行方式（routeType）必须是0-5");
        }
        if (strategy < 0) {
            throw new IllegalArgumentException("导航策略（strategy）不能为负数");
        }

        String actualSource = getActualSourceApp(context, sourceApp);
        StringBuilder uriBuilder = new StringBuilder(SCHEME_AMAP_URI)
                .append(SERVICE_ROUTE_PLAN)
                .append("?sourceApplication=").append(actualSource)
                .append("&dlat=").append(dlat)
                .append("&dlon=").append(dlon)
                .append("&t=").append(routeType)
                .append("&dev=").append(dev >= 0 ? dev : DEV_NO_ENCRYPT)
                .append("&m=").append(strategy);

        // 拼接可选参数：起点信息
        if (slat != 0 && slon != 0) {
            uriBuilder.append("&slat=").append(slat).append("&slon=").append(slon);
        }
        if (!TextUtils.isEmpty(sname)) uriBuilder.append("&sname=").append(sname);
        if (!TextUtils.isEmpty(sid)) uriBuilder.append("&sid=").append(sid);
        if (!TextUtils.isEmpty(did)) uriBuilder.append("&did=").append(did);

        // 拼接可选参数：终点名称
        if (!TextUtils.isEmpty(dname)) uriBuilder.append("&dname=").append(dname);

        // 拼接可选参数：途经点（数量需一致）
        if (vian > 0) {
            if (TextUtils.isEmpty(vialons) || TextUtils.isEmpty(vialats)) {
                throw new IllegalArgumentException("途经点数量（vian）>0时，经度（vialons）和纬度（vialats）不能为空");
            }
            uriBuilder.append("&vian=").append(vian)
                    .append("&vialons=").append(vialons)
                    .append("&vialats=").append(vialats);
            if (!TextUtils.isEmpty(vianames)) {
                uriBuilder.append("&vianames=").append(vianames);
            }
        }

        // 拼接可选参数：骑行类型（仅骑行时有效）
        if (routeType == ROUTE_TYPE_RIDE && !TextUtils.isEmpty(rideType)) {
            uriBuilder.append("&ridetype=").append(rideType);
        }

        startNaviIntent(context, uriBuilder.toString());
    }


    // ====================== 新增功能：我的位置 ======================

    /**
     * 简化版：显示我的位置
     * @param context 上下文
     */
    public static void showMyLocation(Context context) {
        showMyLocation(context, null);
    }

    /**
     * 完整参数版：显示我的位置
     * @param context 上下文
     * @param sourceApp 调用方名称（可为空，自动获取）
     */
    public static void showMyLocation(Context context, String sourceApp) {
        checkContextValid(context);
        if (!isGaodeInstalled(context)) {
            throw new IllegalArgumentException("高德地图未安装");
        }

        String actualSource = getActualSourceApp(context, sourceApp);
        String uri = new StringBuilder(SCHEME_AMAP)
                .append(SERVICE_MY_LOCATION)
                .append("?sourceApplication=").append(actualSource)
                .toString();

        startNaviIntent(context, uri);
    }


    // ====================== 新增功能：逆地理编码（坐标转地址） ======================

    /**
     * 简化版：逆地理编码（根据坐标获取地址）
     * @param context 上下文
     * @param lat 纬度（必填）
     * @param lon 经度（必填）
     */
    public static void startReGeoCode(Context context, double lat, double lon) {
        startReGeoCode(context, null, lat, lon, DEV_NO_ENCRYPT);
    }

    /**
     * 完整参数版：逆地理编码
     * @param context 上下文
     * @param sourceApp 调用方名称（可为空）
     * @param lat 纬度（必填）
     * @param lon 经度（必填）
     * @param dev 坐标加密（0/1，默认0）
     */
    public static void startReGeoCode(Context context, String sourceApp, double lat, double lon, int dev) {
        checkContextValid(context);
        if (!isGaodeInstalled(context)) {
            throw new IllegalArgumentException("高德地图未安装");
        }

        String actualSource = getActualSourceApp(context, sourceApp);
        String uri = new StringBuilder(SCHEME_AMAP)
                .append(SERVICE_VIEW_REGEO)
                .append("?sourceApplication=").append(actualSource)
                .append("&lat=").append(lat)
                .append("&lon=").append(lon)
                .append("&dev=").append(dev >= 0 ? dev : DEV_NO_ENCRYPT)
                .toString();

        startNaviIntent(context, uri);
    }


    // ====================== 新增功能：地图主图 ======================

    /**
     * 简化版：打开高德地图主页面
     * @param context 上下文
     */
    public static void openMapHome(Context context) {
        openMapHome(context, null);
    }

    /**
     * 完整参数版：打开高德地图主页面
     * @param context 上下文
     * @param sourceApp 调用方名称（可为空，自动获取）
     */
    public static void openMapHome(Context context, String sourceApp) {
        checkContextValid(context);
        if (!isGaodeInstalled(context)) {
            throw new IllegalArgumentException("高德地图未安装");
        }

        String actualSource = getActualSourceApp(context, sourceApp);
        String uri = new StringBuilder(SCHEME_AMAP)
                .append(SERVICE_ROOT_MAP)
                .append("?sourceApplication=").append(actualSource)
                .toString();

        startNaviIntent(context, uri);
    }


    // ====================== 新增功能：下载高德地图 ======================

    /**
     * 下载高德地图（优先应用市场，失败则跳转官网）
     * @param context 上下文
     */
    public static void downloadGaodeMap(Context context) {
        checkContextValid(context);
        // 优先从应用市场下载
        try {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
            marketIntent.setData(Uri.parse("market://details?id=" + GAODE_PACKAGE_NAME));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(marketIntent);
            return;
        } catch (Exception e) {
            // 应用市场跳转失败，使用官网下载
        }
        // 官网下载地址（高德官方移动端下载页）
        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(Uri.parse("https://mobile.amap.com/"));
        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(webIntent);
    }


    // ====================== 内部工具方法 ======================

    /**
     * 自动获取sourceApp（应用名称优先，失败则用包名）
     */
    private static String getActualSourceApp(Context context, String sourceApp) {
        if (!TextUtils.isEmpty(sourceApp)) {
            return sourceApp;
        }

        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(context.getPackageName(), 0);
            String appName = pm.getApplicationLabel(appInfo).toString();
            if (!TextUtils.isEmpty(appName)) {
                return appName;
            }
        } catch (Exception e) {
            // 获取应用名称失败，降级用包名
        }
        return context.getPackageName();
    }

    /**
     * 校验上下文非空
     */
    private static void checkContextValid(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context不能为空");
        }
    }

    /**
     * 启动导航Intent（处理非Activity上下文场景）
     */
    private static void startNaviIntent(Context context, String uriStr) {
        Intent intent = new Intent(INTENT_ACTION);
        intent.setData(Uri.parse(uriStr));
        intent.setPackage(GAODE_PACKAGE_NAME);
        intent.addCategory(INTENT_CATEGORY);
        if (!(context instanceof android.app.Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }
}