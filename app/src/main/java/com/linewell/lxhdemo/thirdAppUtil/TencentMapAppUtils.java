package com.linewell.lxhdemo.thirdAppUtil;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

/**
 * 腾讯地图工具类（支持referer参数动态传入或使用默认固定值）
 */
public class TencentMapAppUtils {

    // 腾讯地图包名
    private static final String TENCENT_MAP_PACKAGE_NAME = "com.tencent.map";
    // 协议前缀
    private static final String SCHEME_PREFIX = "qqmap://map/";
    // 路线规划路径
    private static final String ROUTE_PLAN_PATH = "routeplan";
    // 地点标注路径
    private static final String MARKER_PATH = "marker";
    private static final String MARKER_AUTO_PATH = "geocoder";
    // 下载页链接（开发者Key占位符）
    private static final String DOWNLOAD_URL = "https://pr.map.qq.com/j/tmap/download?key=%s";

    // 【默认开发者Key（必填）】未传入referer参数时使用此固定值，需替换为实际Key
    private static final String DEFAULT_REFERER = "OB4BZ-D4W3U-*****"; // 替换为你的真实Key


    /**
     * 检测是否安装腾讯地图
     */
    public static boolean isTencentMapInstalled(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(TENCENT_MAP_PACKAGE_NAME, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 打开路线规划（referer支持传入或使用默认值）
     *
     * @param context   上下文
     * @param type      出行方式（bus/drive/walk/bike，必填）
     * @param fromcoord 起点坐标（格式：lat,lng 或 CurrentLocation，必填）
     * @param tocoord   终点坐标（格式：lat,lng，必填）
     * @param from      起点名称（可选）
     * @param to        终点名称（可选）
     * @param passes    途经点（可选，格式：name:名称;coord:经纬度|...）
     * @param referer   开发者Key（可选，传入则使用，否则用默认值）
     */
    public static void openRoutePlan(Context context, String type, String fromcoord, String tocoord,
                                     String from, String to, String passes, String referer) {
        if (!isTencentMapInstalled(context)) {
            // 未安装时打开下载页，传入的referer优先，否则用默认
            openDownloadPage(context, referer);
            return;
        }

        // 在openRoutePlan方法开头添加
        if (isEmpty(type) || isEmpty(fromcoord) || isEmpty(tocoord)) {
            throw new IllegalArgumentException("type、fromcoord、tocoord为必填参数，不可为空");
        }

        // 确定最终使用的referer：传入不为空则用传入的，否则用默认
        String finalReferer = isEmpty(referer) ? DEFAULT_REFERER : referer;

        StringBuilder sb = new StringBuilder(SCHEME_PREFIX).append(ROUTE_PLAN_PATH).append("?");
        sb.append("type=").append(type);
        sb.append("&fromcoord=").append(fromcoord);
        sb.append("&tocoord=").append(tocoord);
        if (!isEmpty(from)) {
            sb.append("&from=").append(from);
        }
        if (!isEmpty(to)) {
            sb.append("&to=").append(to);
        }
        if (!isEmpty(passes)) {
            sb.append("&passes=").append(passes);
        }
        sb.append("&referer=").append(finalReferer);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(TENCENT_MAP_PACKAGE_NAME);
        context.startActivity(intent);
    }

    /**
     * 打开地点标注（referer支持传入或使用默认值）
     *
     * @param context 上下文
     * @param coord   标注坐标（格式：lat,lng，必填）
     * @param title   标注名称（可选）
     * @param addr    地址（可选）
     * @param referer 开发者Key（可选，传入则使用，否则用默认值）
     */
    public static void openMarker(Context context, String coord, String title, String addr, String referer) {
        if (!isTencentMapInstalled(context)) {
            // 未安装时打开下载页，传入的referer优先，否则用默认
            openDownloadPage(context, referer);
            return;
        }

        // 确定最终使用的referer
        String finalReferer = isEmpty(referer) ? DEFAULT_REFERER : referer;

        StringBuilder sb = new StringBuilder(SCHEME_PREFIX).append(MARKER_PATH).append("?");
        sb.append("marker=coord:").append(coord);
        if (!isEmpty(title)) {
            sb.append(";title:").append(title);
        }
        if (!isEmpty(addr)) {
            sb.append(";addr:").append(addr);
        }
        sb.append("&referer=").append(finalReferer);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 添加标志
        intent.setPackage(TENCENT_MAP_PACKAGE_NAME);
        context.startActivity(intent);
    }


    /**
     * 打开自动地点标注（referer支持传入或使用默认值）
     * @param context  上下文
     * @param coord 标注坐标（格式：lat,lng，必填）
     * @param referer 发者Key（可选，传入则使用，否则用默认值）
     */
    public static void openMarker(Context context, String coord, String referer) {
        if (!isTencentMapInstalled(context)) {
            // 未安装时打开下载页，传入的referer优先，否则用默认
            openDownloadPage(context, referer);
            return;
        }

        // 确定最终使用的referer
        String finalReferer = isEmpty(referer) ? DEFAULT_REFERER : referer;

        StringBuilder sb = new StringBuilder(SCHEME_PREFIX).append(MARKER_AUTO_PATH).append("?");
        sb.append("marker=coord:").append(coord);
        sb.append("&referer=").append(finalReferer);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 添加标志
        intent.setPackage(TENCENT_MAP_PACKAGE_NAME);
        context.startActivity(intent);
    }


    /**
     * 打开腾讯地图下载页（referer支持传入或使用默认值）
     *
     * @param context 上下文
     * @param referer 开发者Key（可选）
     */
    public static void openDownloadPage(Context context, String referer) {
        try {
            if (context == null) return;
            // 优先尝试系统默认应用市场
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + TENCENT_MAP_PACKAGE_NAME));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(marketIntent);
            return;
        } catch (Exception e) {
            // 应用市场跳转失败，使用官网下载
        }

        String finalReferer = isEmpty(referer) ? DEFAULT_REFERER : referer;
        String url = String.format(DOWNLOAD_URL, finalReferer);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 添加标志
        context.startActivity(intent);
    }

    /**
     * 辅助判断字符串是否为空（null或空串）
     */
    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}