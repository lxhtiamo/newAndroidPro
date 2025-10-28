package com.linewell.lxhdemo.thirdAppUtil;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.hjq.permissions.permission.base.IPermission;
import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.app.MyApplication;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.Tencent;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.socialize.PlatformConfig;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.media.UMMin;
import com.umeng.socialize.media.UMQQMini;
import com.umeng.socialize.media.UMVideo;
import com.umeng.socialize.media.UMWeb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class UMShareManager {
    private static final String TAG = "UMShareManager";
    // 权限请求相关
    private static final String[] PERMISSIONS_STORAGE = {READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE};
    private static final String[] PERMISSIONS_MEDIA_13 = {READ_MEDIA_IMAGES, READ_MEDIA_VIDEO};
    // 单例实例
    private static volatile UMShareManager instance;
    // 上下文与核心对象
    private final Context mAppContext;
    private final UMShareAPI mShareAPI;
    private Tencent mTencent; // QQ单例管理
    // 配置参数
    private final Config mConfig;

    // ======================== 配置实体类 ========================
    public static class Config {
        // 必传参数
        public String umAppKey; // 替换为你的友盟AppKey（从友盟后台获取）
        public String fileProvider; // 替换为你的FileProvider（如：com.linewell.lxhdemo.fileprovider）
        // 平台可选参数
        public String wechatAppId; // 替换为你的微信AppId
        public String qqAppId; // 替换为你的QQ AppId
        public String qqAppKey; // 替换为你的QQ AppKey
        public String sinaAppKey; // 替换为你的微博AppKey
        public String sinaAppSecret; // 替换为你的微博AppSecret
        public String sinaRedirectUrl; // 替换为你的微博回调页（需与微博开放平台一致）
        public String douyinClientKey; // 替换为你的抖音ClientKey
        public String dingtalkAppId; // 替换为你的钉钉AppId
        // 图标资源（需在res/drawable中添加对应图标）
        public int wechatIcon = R.drawable.share_wechat_ic;
        public int wechatCircleIcon = R.drawable.share_moment_ic;
        public int qqIcon = R.drawable.share_qq_ic;
        public int qzoneIcon = R.drawable.share_qzone_ic;
        public int sinaIcon = R.drawable.share_webo_ic;
        public int douyinIcon = R.drawable.share_douyin_ic;
        public int dingtalkIcon = R.drawable.share_ding_ic;
        public int copyLinkIcon = R.drawable.share_link_ic;
    }

    // ======================== 分享平台枚举 ========================
    public enum Platform {
        WECHAT(SHARE_MEDIA.WEIXIN),
        WECHAT_CIRCLE(SHARE_MEDIA.WEIXIN_CIRCLE),
        QQ(SHARE_MEDIA.QQ),
        QZONE(SHARE_MEDIA.QZONE),
        SINA(SHARE_MEDIA.SINA),
        DOUYIN(SHARE_MEDIA.BYTEDANCE),
        DINGTALK(SHARE_MEDIA.DINGTALK),
        COPY_LINK(null);

        public final SHARE_MEDIA media;

        Platform(SHARE_MEDIA media) {
            this.media = media;
        }
    }

    // ======================== 分享内容实体类 ========================
    public static class ShareContent {
        public String title; // 标题（网页/视频/图片分享用）
        public String desc; // 描述（网页分享用，朋友圈除外）
        public String url; // 网页链接/复制链接
        public String text; // 纯文本内容（文本分享用）
        public Bitmap imageBitmap; // 本地图片（Bitmap）
        public String imageLocalPath; // 本地图片路径（优先级：Bitmap > 本地路径 > 网络URL）
        public String imageUrl; // 网络图片URL
        public String videoUrl; // 视频路径（仅支持本地文件，如：/storage/emulated/0/xxx.mp4）
        public Type type; // 分享类型
        public String miniUserName; // 微信小程序原始ID（如 gh_xxxxxxxxxxxx，微信小程序必传）
        public String miniAppId; // QQ小程序AppId（QQ小程序必传）
        public String miniPath; // 小程序页面路径（如 pages/index/index，必传）

        public enum Type {WEB, IMAGE, VIDEO, TEXT, COPY_LINK, WECHAT_MINI, QQ_MINI}
    }

    // ======================== 平台信息实体类 ========================
    public static class PlatformInfo {
        public Platform platform;
        public String name;
        public int icon;
        public boolean isInstalled; // 是否安装对应应用

        public PlatformInfo(Platform platform, String name, int icon, boolean isInstalled) {
            this.platform = platform;
            this.name = name;
            this.icon = icon;
            this.isInstalled = isInstalled;
        }
    }

    // ======================== 结果回调接口 ========================
    public interface ResultListener {
        void onSuccess(String msg);

        void onCancel(String msg);

        void onError(String msg);
    }

    // ======================== 初始化方法 ========================
    public static void init(Context context, Config config) {
        // 必传参数校验
        if (config == null || TextUtils.isEmpty(config.umAppKey) || TextUtils.isEmpty(config.fileProvider)) {
            throw new IllegalArgumentException("友盟AppKey和FileProvider不能为空，请检查Config配置");
        }
        // 初始化友盟SDK
        Context appContext = context.getApplicationContext();
        UMConfigure.init(appContext, config.umAppKey, "umeng", UMConfigure.DEVICE_TYPE_PHONE, "");
        // 平台配置
        initPlatformConfig(config);
        // 初始化单例
        instance = new UMShareManager(appContext, config);
    }

    // 初始化各平台配置
    private static void initPlatformConfig(Config config) {
        String fileProvider = MyApplication.getInstance().getPackageName() + ".fileprovider";

        PlatformConfig.setFileProvider(fileProvider);
        // 微信配置
        if (!TextUtils.isEmpty(config.wechatAppId)) {
            PlatformConfig.setWeixin(config.wechatAppId, "");

        }
        // QQ配置
        if (!TextUtils.isEmpty(config.qqAppId) && !TextUtils.isEmpty(config.qqAppKey)) {
            PlatformConfig.setQQZone(config.qqAppId, config.qqAppKey);
        }
        // 微博配置
        if (!TextUtils.isEmpty(config.sinaAppKey) && !TextUtils.isEmpty(config.sinaAppSecret) && !TextUtils.isEmpty(config.sinaRedirectUrl)) {
            PlatformConfig.setSinaWeibo(config.sinaAppKey, config.sinaAppSecret, config.sinaRedirectUrl);
        }
        // 抖音配置
        if (!TextUtils.isEmpty(config.douyinClientKey)) {
            PlatformConfig.setBytedance(config.douyinClientKey, "", "", config.fileProvider);
        }
        // 钉钉配置
        if (!TextUtils.isEmpty(config.dingtalkAppId)) {
            PlatformConfig.setDing(config.dingtalkAppId);
        }
        // 智能适配开关
        UMShareAPI.setSmartEnable(true);
    }

    // 获取单例（需先调用init()）
    public static UMShareManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("UMShareManager未初始化，请先在Application中调用init()方法");
        }
        return instance;
    }

    // 私有构造（禁止外部实例化）
    private UMShareManager(Context appContext, Config config) {
        this.mAppContext = appContext;
        this.mConfig = config;
        this.mShareAPI = UMShareAPI.get(appContext);
        // 初始化QQ单例
        if (!TextUtils.isEmpty(config.qqAppId)) {
            this.mTencent = Tencent.createInstance(config.qqAppId, appContext);
        }
    }

    // ======================== 核心功能方法 ========================

    /**
     * 获取支持的分享平台列表（自动过滤未安装应用）
     */
    public List<PlatformInfo> getPlatforms(Activity activity) {
        List<PlatformInfo> platformList = new ArrayList<>();
        // 添加各平台（图标为0时不添加，避免空图标）
        addPlatform(platformList, activity, Platform.WECHAT, "微信好友", mConfig.wechatIcon);
        addPlatform(platformList, activity, Platform.WECHAT_CIRCLE, "朋友圈", mConfig.wechatCircleIcon);
        addPlatform(platformList, activity, Platform.QQ, "QQ好友", mConfig.qqIcon);
        addPlatform(platformList, activity, Platform.QZONE, "QQ空间", mConfig.qzoneIcon);
        addPlatform(platformList, activity, Platform.SINA, "微博", mConfig.sinaIcon);
        addPlatform(platformList, activity, Platform.DOUYIN, "抖音", mConfig.douyinIcon);
        addPlatform(platformList, activity, Platform.DINGTALK, "钉钉", mConfig.dingtalkIcon);
        // 复制链接（始终显示，无需检测安装）
        platformList.add(new PlatformInfo(Platform.COPY_LINK, "复制链接", mConfig.copyLinkIcon, true));
        return platformList;
    }

    /**
     * 执行分享/复制操作（入口方法，已适配XXPermissions异步权限）
     *
     * @param activity 上下文（需传入Activity，用于XXPermissions和UI线程）
     * @param platform 目标平台
     * @param content  分享内容
     * @param listener 分享结果回调
     */
    public void doAction(Activity activity, Platform platform, ShareContent content, ResultListener listener) {
        // 1. 基础参数校验
        if (activity == null || content == null || content.type == null || listener == null) {
            if (listener != null) {
                listener.onError("参数异常：Activity/Content/Type/Listener不能为空");
            }
            return;
        }
        if (activity == null || activity.isFinishing()) {
            listener.onError("Activity无效或已销毁");
            return;
        }
        String validationError = validateShareContent(content);
        if (validationError != null) {
            listener.onError(validationError);
            return;
        }
        // 2. 复制链接特殊处理（无需权限）
        if (platform == Platform.COPY_LINK) {
            copyLink(content.url, listener);
            return;
        }

        // 3. 应用安装检测
        if (platform.media == null || !mShareAPI.isInstall(activity, platform.media)) {
            listener.onError(platform.name() + "未安装");
            jumpToMarket(activity, platform); // 跳转应用商店
            return;
        }

        // 4. 平台类型支持校验
        if (!isPlatformSupportType(platform, content.type)) {
            listener.onError(platform.name() + "不支持" + content.type.name() + "类型");
            return;
        }

        // 5. 调用XXPermissions申请权限（异步），权限成功后再执行分享
        checkAndRequestPermissions(activity, content, new OnPermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                // 权限通过：执行分享逻辑
                executeShare(activity, platform, content, listener);
            }

            @Override
            public void onPermissionDenied(String errorMsg) {
                // 权限拒绝：通知分享失败
                listener.onError(errorMsg);
            }
        });
    }

    /**
     * 验证分享内容是否有效
     */
    public String validateShareContent(ShareContent content) {
        if (content == null) {
            return "分享内容不能为空";
        }

        if (content.type == null) {
            return "分享类型不能为空";
        }

        switch (content.type) {
            // -------------------- 新增：微信小程序校验 --------------------
            case WECHAT_MINI:
                if (TextUtils.isEmpty(content.url)) return "微信小程序需传入兼容网页链接";
                if (TextUtils.isEmpty(content.miniUserName)) return "微信小程序原始ID（miniUserName）不能为空";
                if (TextUtils.isEmpty(content.miniPath)) return "微信小程序页面路径（miniPath）不能为空";
                if (content.imageBitmap == null && TextUtils.isEmpty(content.imageLocalPath) && TextUtils.isEmpty(content.imageUrl)) {
                    return "微信小程序缩略图不能为空";
                }
                break;

            // -------------------- 新增：QQ小程序校验 --------------------
            case QQ_MINI:
                if (TextUtils.isEmpty(content.url)) return "QQ小程序需传入兼容网页链接";
                if (TextUtils.isEmpty(content.miniAppId)) return "QQ小程序AppId（miniAppId）不能为空";
                if (TextUtils.isEmpty(content.miniPath)) return "QQ小程序页面路径（miniPath）不能为空";
                if (content.imageBitmap == null && TextUtils.isEmpty(content.imageLocalPath) && TextUtils.isEmpty(content.imageUrl)) {
                    return "QQ小程序缩略图不能为空";
                }
                break;
            case TEXT:
                if (TextUtils.isEmpty(content.text)) {
                    return "文本内容不能为空";
                }
                break;
            case WEB:
                if (TextUtils.isEmpty(content.url)) {
                    return "网页链接不能为空";
                }
                break;
            case IMAGE:
                if (content.imageBitmap == null &&
                        TextUtils.isEmpty(content.imageLocalPath) &&
                        TextUtils.isEmpty(content.imageUrl)) {
                    return "图片资源不能为空";
                }
                break;
            case VIDEO:
                if (TextUtils.isEmpty(content.videoUrl)) {
                    return "视频路径不能为空";
                }
                break;
            case COPY_LINK:
                if (TextUtils.isEmpty(content.url)) {
                    return "复制链接不能为空";
                }
                break;
        }

        return null; // 验证通过
    }

    /**
     * 处理Activity回调（需在Activity的onActivityResult中调用）
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mShareAPI != null) {
            mShareAPI.onActivityResult(requestCode, resultCode, data);
        }
        // QQ登录回调处理（与分享共用回调）
        if (requestCode == Constants.REQUEST_LOGIN && mTencent != null) {
            Tencent.onActivityResultData(requestCode, resultCode, data, null);
        }
    }
// ======================== 权限回调接口 ========================

    /**
     * 权限申请结果回调（适配XXPermissions异步回调）
     */
    public interface OnPermissionResultListener {
        /**
         * 权限全部授予
         */
        void onPermissionGranted();

        /**
         * 权限被拒绝
         */
        void onPermissionDenied(String errorMsg);
    }

    private boolean isLocalImageNeedPermission(ShareContent content) {
        // 明确列出需要权限的情况
        if (content.type != ShareContent.Type.IMAGE) {
            return false;
        }
        // 场景1：Bitmap不为空（默认视为本地图片，需权限）
        if (content.imageBitmap != null) {
            return true;
        }
        // 场景2：本地图片路径不为空（需权限）
        if (!TextUtils.isEmpty(content.imageLocalPath)) {
            return true;
        }
        // 场景3：图片URL不为空——判断是否为网络URL（网络URL无需权限，本地路径需权限）
        if (!TextUtils.isEmpty(content.imageUrl)) {
            return !content.imageUrl.startsWith("http://") && !content.imageUrl.startsWith("https://");
        }
        // 场景4：无图片（无需权限）
        return false;
    }
    // ======================== 权限相关方法 ========================

    /**
     * 基于XXPermissions框架，检查并申请分享所需权限（异步）
     *
     * @param activity           上下文（必须传入Activity，XXPermissions需基于Activity发起）
     * @param content            分享类型（IMAGE/VIDEO需存储权限，其他类型无需申请）
     * @param permissionListener 权限结果回调
     */
    public void checkAndRequestPermissions(Activity activity, ShareContent content, OnPermissionResultListener permissionListener) {
        // 1. 非图片/视频分享，无需申请权限，直接回调成功
        if (content.type != ShareContent.Type.IMAGE && content.type != ShareContent.Type.VIDEO) {
            permissionListener.onPermissionGranted();
            return;
        }
        // 图片分享：判断是否为“需要权限的本地图片”
        if (!isLocalImageNeedPermission(content)) {
            permissionListener.onPermissionGranted(); // 网络图片/应用内图片，无需权限
            return;
        }

        List<IPermission> permissionList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上
            permissionList.add(PermissionLists.getReadMediaImagesPermission());
            permissionList.add(PermissionLists.getReadMediaVideoPermission());
        } else {
            // Android 13以下
            permissionList.add(PermissionLists.getReadExternalStoragePermission());
            permissionList.add(PermissionLists.getWriteExternalStoragePermission());
        }
        XXPermissions.with(activity)
                .permissions(permissionList) // 传入组装好的权限列表
                // .unchecked() // 如需关闭错误检测，可解开注释
                .request(new OnPermissionCallback() {
                    @Override
                    public void onResult(@NonNull List<IPermission> grantedList, @NonNull List<IPermission> deniedList) {
                        boolean allGranted = deniedList.isEmpty();
                        if (!allGranted) {
                            boolean doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(activity, deniedList);
                            String errorMsg;
                            if (doNotAskAgain) {
                                // 永久拒绝：提示用户去设置页开启
                                errorMsg = "存储权限已被永久拒绝，请前往手机设置-应用权限中手动开启";
                            } else {
                                // 临时拒绝：提示用户需授予权限才能分享
                                errorMsg = "需授予存储权限才能分享图片/视频，请重新申请";
                            }
                            // 回调失败结果
                            permissionListener.onPermissionDenied(errorMsg);
                            // 在这里处理权限请求失败的逻辑
                            return;
                        }
                        permissionListener.onPermissionGranted();
                    }
                });
    }

    // ======================== 平台适配方法 ========================

    /**
     * 检查平台是否支持当前分享类型
     */
    /**
     * 检查平台是否支持当前分享类型
     */
    private boolean isPlatformSupportType(Platform platform, ShareContent.Type type) {
        // 使用Map维护平台支持的类型，便于维护和扩展
        Map<Platform, Set<ShareContent.Type>> platformSupportMap = new HashMap<>();

        // 微信支持的类型
        Set<ShareContent.Type> wechatTypes = new HashSet<>(Arrays.asList(
                ShareContent.Type.TEXT,
                ShareContent.Type.IMAGE,
                ShareContent.Type.WEB,
                ShareContent.Type.VIDEO,
                ShareContent.Type.WECHAT_MINI
        ));

        // 朋友圈支持的类型（与微信基本相同）
        Set<ShareContent.Type> wechatCircleTypes = new HashSet<>(Arrays.asList(
                ShareContent.Type.TEXT,
                ShareContent.Type.IMAGE,
                ShareContent.Type.WEB,
                ShareContent.Type.VIDEO
        ));

        // QQ支持的类型（不支持纯文本）
        Set<ShareContent.Type> qqTypes = new HashSet<>(Arrays.asList(
                ShareContent.Type.IMAGE,
                ShareContent.Type.WEB,
                ShareContent.Type.VIDEO,
                ShareContent.Type.QQ_MINI
        ));

        // QQ空间支持的类型
        Set<ShareContent.Type> qzoneTypes = new HashSet<>(Arrays.asList(
                ShareContent.Type.TEXT,
                ShareContent.Type.IMAGE,
                ShareContent.Type.WEB,
                ShareContent.Type.VIDEO
        ));

        // 微博支持的类型
        Set<ShareContent.Type> sinaTypes = new HashSet<>(Arrays.asList(
                ShareContent.Type.TEXT,
                ShareContent.Type.IMAGE,
                ShareContent.Type.WEB,
                ShareContent.Type.VIDEO
        ));

        // 抖音支持的类型（不支持纯文本）
        Set<ShareContent.Type> douyinTypes = new HashSet<>(Arrays.asList(
                ShareContent.Type.IMAGE,
                ShareContent.Type.WEB,
                ShareContent.Type.VIDEO
        ));

        // 钉钉支持的类型
        Set<ShareContent.Type> dingtalkTypes = new HashSet<>(Arrays.asList(
                ShareContent.Type.TEXT,
                ShareContent.Type.IMAGE,
                ShareContent.Type.WEB,
                ShareContent.Type.VIDEO
        ));

        // 初始化映射关系
        platformSupportMap.put(Platform.WECHAT, wechatTypes);
        platformSupportMap.put(Platform.WECHAT_CIRCLE, wechatCircleTypes);
        platformSupportMap.put(Platform.QQ, qqTypes);
        platformSupportMap.put(Platform.QZONE, qzoneTypes);
        platformSupportMap.put(Platform.SINA, sinaTypes);
        platformSupportMap.put(Platform.DOUYIN, douyinTypes);
        platformSupportMap.put(Platform.DINGTALK, dingtalkTypes);
        // 获取平台支持的类型集合
        Set<ShareContent.Type> supportedTypes = platformSupportMap.get(platform);

        // 如果平台不在映射中或类型不在支持列表中，返回false
        return supportedTypes != null && supportedTypes.contains(type);
    }

    /**
     * 跳转应用商店下载对应平台应用
     */
    private void jumpToMarket(Activity activity, Platform platform) {
        String packageName = getPlatformPackageName(platform);
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        // 1. 尝试跳转应用商店
        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
        marketIntent.setData(Uri.parse("market://details?id=" + packageName));
        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (marketIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(marketIntent);
        }
    }

    /**
     * 获取平台应用包名
     */
    private String getPlatformPackageName(Platform platform) {
        switch (platform) {
            case WECHAT:
            case WECHAT_CIRCLE:
                return "com.tencent.mm"; // 微信
            case QQ:
            case QZONE:
                return "com.tencent.mobileqq"; // QQ
            case SINA:
                return "com.sina.weibo"; // 微博
            case DOUYIN:
                return "com.ss.android.ugc.aweme"; // 抖音
            case DINGTALK:
                return "com.alibaba.android.rimet"; // 钉钉
            default:
                return "";
        }
    }

    // ======================== 分享执行方法 ========================

    /**
     * 分发分享类型（文本/图片/网页/视频）
     */
    private void executeShare(Activity activity, Platform platform, ShareContent content, ResultListener listener) {
        switch (content.type) {
            case WECHAT_MINI:
                shareWechatMini(activity, platform, content, listener);
                break;
            case QQ_MINI:
                shareQQMini(activity, platform, content, listener);
                break;
            case TEXT:
                shareText(activity, platform, content, listener);
                break;
            case WEB:
                shareWeb(activity, platform, content, listener);
                break;
            case IMAGE:
                shareImage(activity, platform, content, listener);
                break;
            case VIDEO:
                shareVideo(activity, platform, content, listener);
                break;
            default:
                listener.onError("不支持的分享类型：" + content.type.name());
        }
    }

    /**
     * 纯文本分享（处理微博文本长度限制）
     */
    private void shareText(Activity activity, Platform platform, ShareContent content, ResultListener listener) {
        // 文本空校验
        if (TextUtils.isEmpty(content.text)) {
            listener.onError("文本内容不能为空");
            return;
        }
        // 微博文本长度限制（140字，预留5字冗余）
        String finalText = content.text;
        if (platform == Platform.SINA && content.text.length() > 135) {
            finalText = content.text.substring(0, 135) + "...";
            Log.d(TAG, "微博文本过长，已截断：" + finalText);
        }
        // 执行分享
        new ShareAction(activity)
                .setPlatform(platform.media)
                .withText(finalText)
                .setCallback(getShareListener(platform, listener, activity))
                .share();
    }

    /**
     * 网页分享（处理朋友圈无描述问题）
     */
    private void shareWeb(Activity activity, Platform platform, ShareContent content, ResultListener listener) {
        // 链接空校验
        if (TextUtils.isEmpty(content.url)) {
            listener.onError("网页链接不能为空");
            return;
        }
        // 构建网页分享对象
        UMWeb web = new UMWeb(content.url);
        // 标题（兜底默认标题）
        web.setTitle(TextUtils.isEmpty(content.title) ? "分享内容" : content.title);
        // 描述（朋友圈不显示描述）
        if (platform != Platform.WECHAT_CIRCLE && !TextUtils.isEmpty(content.desc)) {
            web.setDescription(content.desc);
        }
        // 缩略图（兜底默认图标）
        UMImage thumbImage = buildImage(content);
        if (thumbImage != null) {
            thumbImage.compressStyle = UMImage.CompressStyle.SCALE;//大小压缩，默认为大小压缩，适合普通很大的图
            web.setThumb(thumbImage);
        }
        // 执行分享
        new ShareAction(activity)
                .setPlatform(platform.media)
                .withMedia(web)
                .setCallback(getShareListener(platform, listener, activity))
                .share();
    }

    /**
     * 图片分享（处理微博仅支持本地图片）
     */
    private void shareImage(Activity activity, Platform platform, ShareContent content, ResultListener listener) {
        // 构建图片对象（含兜底）
        UMImage image = buildImage(content);
        if (image == null) {
            listener.onError("图片资源无效（请传入Bitmap/本地路径/网络URL）");
            return;
        }
        // 微博仅支持本地图片（网络URL拦截）
        if (platform == Platform.SINA && !TextUtils.isEmpty(content.imageUrl) && content.imageUrl.startsWith("http")) {
            listener.onError("微博仅支持本地图片，请传入图片Bitmap或本地路径");
            return;
        }
        // 执行分享
        new ShareAction(activity)
                .setPlatform(platform.media)
                .withMedia(image)
                .setCallback(getShareListener(platform, listener, activity))
                .share();
    }

    /**
     * 视频分享（仅支持本地视频，适配分区存储）
     */
    private void shareVideo(Activity activity, Platform platform, ShareContent content, ResultListener listener) {
        // 视频路径空校验
        if (TextUtils.isEmpty(content.videoUrl)) {
            listener.onError("视频路径不能为空");
            return;
        }
        // 本地视频校验（文件存在性）
        File videoFile = new File(content.videoUrl);
        if (!videoFile.exists() || !videoFile.isFile()) {
            listener.onError("视频文件不存在或不是有效文件：" + content.videoUrl);
            return;
        }
        // 构建视频分享对象（适配分区存储）
        try {
            Uri videoUri = getUriForFile(activity, videoFile);
            UMVideo video = new UMVideo(videoUri.toString());
            // 标题（兜底）
            video.setTitle(TextUtils.isEmpty(content.title) ? "分享视频" : content.title);
            // 缩略图（兜底）
            UMImage thumbImage = buildImage(content);
            if (thumbImage != null) {
                video.setThumb(thumbImage);
            }
            // 执行分享
            new ShareAction(activity)
                    .setPlatform(platform.media)
                    .withMedia(video)
                    .setCallback(getShareListener(platform, listener, activity))
                    .share();
        } catch (Exception e) {
            listener.onError("视频分享初始化失败：" + e.getMessage());
            Log.e(TAG, "视频分享异常", e);
        }
    }

    /**
     * 新增：微信小程序分享（基于友盟 UMMin 类）
     */
    private void shareWechatMini(Activity activity, Platform platform, ShareContent content, ResultListener listener) {
        try {
            // 1. 构建微信小程序分享对象（兼容低版本网页链接）
            UMMin umMin = new UMMin(content.url);
            // 2. 设置缩略图（复用原有 buildImage 方法，支持Bitmap/本地路径/网络URL）
            UMImage thumbImage = buildImage(content);
            if (thumbImage != null) {
                thumbImage.compressStyle = UMImage.CompressStyle.SCALE; // 缩放压缩，适配小程序缩略图尺寸
                umMin.setThumb(thumbImage);
            }
            // 3. 设置小程序核心参数（标题、描述、原始ID、页面路径）
            umMin.setTitle(TextUtils.isEmpty(content.title) ? "分享小程序" : content.title);
            umMin.setDescription(TextUtils.isEmpty(content.desc) ? "" : content.desc);
            umMin.setUserName(content.miniUserName); // 微信小程序原始ID（gh_开头）
            umMin.setPath(content.miniPath); // 小程序页面路径（如 pages/index/index）

            // 4. 执行分享
            new ShareAction(activity)
                    .setPlatform(platform.media)
                    .withMedia(umMin)
                    .setCallback(getShareListener(platform, listener, activity))
                    .share();
        } catch (Exception e) {
            listener.onError("微信小程序分享失败：" + e.getMessage());
            Log.e(TAG, "微信小程序分享异常", e);
        }
    }

    /**
     * 新增：QQ小程序分享（基于友盟 UMQQMini 类）
     */
    private void shareQQMini(Activity activity, Platform platform, ShareContent content, ResultListener listener) {
        try {
            // 1. 构建QQ小程序分享对象（兼容低版本网页链接）
            UMQQMini qqMini = new UMQQMini(content.url);
            // 2. 设置缩略图（复用原有 buildImage 方法）
            UMImage thumbImage = buildImage(content);
            if (thumbImage != null) {
                thumbImage.compressStyle = UMImage.CompressStyle.SCALE;
                qqMini.setThumb(thumbImage);
            }
            // 3. 设置QQ小程序核心参数（标题、描述、AppId、页面路径）
            qqMini.setTitle(TextUtils.isEmpty(content.title) ? "分享小程序" : content.title);
            qqMini.setDescription(TextUtils.isEmpty(content.desc) ? "" : content.desc);
            qqMini.setMiniAppId(content.miniAppId); // QQ小程序AppId（11位数字，如 1110429485）
            qqMini.setPath(content.miniPath); // QQ小程序页面路径（如 pages/index/index）

            // 4. 执行分享
            new ShareAction(activity)
                    .setPlatform(platform.media)
                    .withMedia(qqMini)
                    .setCallback(getShareListener(platform, listener, activity))
                    .share();
        } catch (Exception e) {
            listener.onError("QQ小程序分享失败：" + e.getMessage());
            Log.e(TAG, "QQ小程序分享异常", e);
        }
    }
// ======================== 新增：小程序便捷分享方法 ========================

    /**
     * 便捷分享微信小程序
     *
     * @param activity      上下文
     * @param title         小程序标题
     * @param desc          小程序描述
     * @param compatUrl     低版本兼容网页链接（必填）
     * @param miniUserName  微信小程序原始ID（gh_开头，必填）
     * @param miniPath      小程序页面路径（如 pages/index/index，必填）
     * @param thumbImageUrl 缩略图网络URL（或传null，用默认图标）
     * @param listener      结果回调
     */
    public void shareWechatMini(Activity activity, String title, String desc, String compatUrl,
                                String miniUserName, String miniPath, String thumbImageUrl, ResultListener listener) {
        ShareContent content = new ShareContent();
        content.type = ShareContent.Type.WECHAT_MINI;
        content.title = title;
        content.desc = desc;
        content.url = compatUrl; // 低版本兼容链接
        content.miniUserName = miniUserName; // 微信小程序原始ID
        content.miniPath = miniPath; // 小程序路径
        content.imageUrl = thumbImageUrl; // 缩略图

        doAction(activity, Platform.WECHAT, content, listener);
    }

    /**
     * 便捷分享QQ小程序
     *
     * @param activity      上下文
     * @param title         小程序标题
     * @param desc          小程序描述
     * @param compatUrl     低版本兼容网页链接（必填）
     * @param miniAppId     QQ小程序AppId（11位数字，必填）
     * @param miniPath      小程序页面路径（如 pages/index/index，必填）
     * @param thumbImageUrl 缩略图网络URL（或传null，用默认图标）
     * @param listener      结果回调
     */
    public void shareQQMini(Activity activity, String title, String desc, String compatUrl,
                            String miniAppId, String miniPath, String thumbImageUrl, ResultListener listener) {
        ShareContent content = new ShareContent();
        content.type = ShareContent.Type.QQ_MINI;
        content.title = title;
        content.desc = desc;
        content.url = compatUrl; // 低版本兼容链接
        content.miniAppId = miniAppId; // QQ小程序AppId
        content.miniPath = miniPath; // 小程序路径
        content.imageUrl = thumbImageUrl; // 缩略图

        doAction(activity, Platform.QQ, content, listener);
    }

    /**
     * 复制链接到剪贴板
     */
    private void copyLink(String url, ResultListener listener) {
        if (TextUtils.isEmpty(url)) {
            listener.onError("复制链接不能为空");
            return;
        }
        try {
            ClipboardManager clipboard = (ClipboardManager) mAppContext.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                listener.onError("剪贴板服务不可用");
                return;
            }
            // 复制文本
            ClipData clipData = ClipData.newPlainText("share_link", url);
            clipboard.setPrimaryClip(clipData);
            listener.onSuccess("复制链接成功");
        } catch (Exception e) {
            listener.onError("复制链接失败：" + e.getMessage());
            Log.e(TAG, "复制链接异常", e);
        }
    }

    // ======================== 辅助工具方法 ========================

    /**
     * 构建分享图片（优先级：Bitmap > 本地路径 > 网络URL，含默认兜底）
     */
    private UMImage buildImage(ShareContent content) {
        try {
            // 1. 优先使用Bitmap
            if (content.imageBitmap != null) {
                return new UMImage(mAppContext, content.imageBitmap);
            }
            // 2. 其次使用本地图片路径
            if (!TextUtils.isEmpty(content.imageLocalPath)) {
                File localImageFile = new File(content.imageLocalPath);
                if (localImageFile.exists()) {
                    return new UMImage(mAppContext, localImageFile);
                }
            }
            // 3. 最后使用网络图片URL
            if (!TextUtils.isEmpty(content.imageUrl) && content.imageUrl.startsWith("http")) {
                return new UMImage(mAppContext, content.imageUrl);
            }
            // 4. 兜底：使用默认分享图标（需在res/drawable中添加ic_share_default.png）
            return getDefaultImage();
        } catch (Exception e) {
            Log.e(TAG, "构建图片失败", e);
            return null;
        }
    }

    private UMImage getDefaultImage() {
        Log.w(TAG, "使用默认图标");
        return new UMImage(mAppContext, R.mipmap.ic_launcher);
    }

    /**
     * 适配分区存储，获取文件的Content Uri（7.0+用FileProvider，低版本用File Uri）
     */
    private Uri getUriForFile(Context context, File file) throws FileNotFoundException {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("文件不存在：" + file.getAbsolutePath());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 7.0+ 用FileProvider（需与Config中的fileProvider一致）
            return FileProvider.getUriForFile(context, mConfig.fileProvider, file);
        } else {
            // 7.0以下直接用File Uri
            return Uri.fromFile(file);
        }
    }

    /**
     * 统一分享回调（自动切换到主线程，避免UI操作异常）
     */
    private UMShareListener getShareListener(Platform platform, ResultListener listener, Activity activity) {
        return new UMShareListener() {
            @Override
            public void onStart(SHARE_MEDIA share_media) {
                Log.d(TAG, platform.name() + "分享开始");
            }

            @Override
            public void onResult(SHARE_MEDIA share_media) {
                activity.runOnUiThread(() -> listener.onSuccess(platform.name() + "分享成功"));
            }

            @Override
            public void onError(SHARE_MEDIA share_media, Throwable throwable) {
                String errorMsg = throwable != null ? throwable.getMessage() : "未知错误";
                Log.e(TAG, platform.name() + "分享失败", throwable);
                activity.runOnUiThread(() -> listener.onError(platform.name() + "分享失败：" + errorMsg));
            }

            @Override
            public void onCancel(SHARE_MEDIA share_media) {
                activity.runOnUiThread(() -> listener.onCancel(platform.name() + "分享已取消"));
            }
        };
    }

    /**
     * 添加平台到列表（过滤图标为空的平台）
     */
    private void addPlatform(List<PlatformInfo> list, Activity activity, Platform platform, String name, int icon) {
        if (icon == 0) {
            Log.w(TAG, "平台图标为空，跳过添加：" + name);
            return;
        }
// 检测应用是否安装
        boolean isInstalled = platform.media != null && mShareAPI.isInstall(activity, platform.media);
        //检查是否配置了相关平台的appid
        boolean platformAvailable = isPlatformAvailable(activity, platform);

        if (isInstalled && platformAvailable)
            list.add(new PlatformInfo(platform, name, icon, isInstalled));
    }

    public boolean isPlatformAvailable(Activity activity, Platform platform) {
        if (platform == Platform.COPY_LINK) {
            return true; // 复制链接始终可用
        }

        if (platform.media == null || !mShareAPI.isInstall(activity, platform.media)) {
            return false;
        }

        // 检查平台配置
        switch (platform) {
            case WECHAT:
            case WECHAT_CIRCLE:
                return !TextUtils.isEmpty(mConfig.wechatAppId);
            case QQ:
            case QZONE:
                return !TextUtils.isEmpty(mConfig.qqAppId);
            case SINA:
                return !TextUtils.isEmpty(mConfig.sinaAppKey);
            case DOUYIN:
                return !TextUtils.isEmpty(mConfig.douyinClientKey);
            case DINGTALK:
                return !TextUtils.isEmpty(mConfig.dingtalkAppId);
            default:
                return true;
        }
    }

    public void shareWeb(Activity activity, Platform platform, String title, String desc,
                         String url, String imageUrl, ResultListener listener) {
        ShareContent content = new ShareContent();
        content.type = ShareContent.Type.WEB;
        content.title = title;
        content.desc = desc;
        content.url = url;
        content.imageUrl = imageUrl;

        doAction(activity, platform, content, listener);
    }

    /**
     * 便捷分享图片
     */
    public void shareImage(Activity activity, Platform platform, Bitmap bitmap, ResultListener listener) {
        ShareContent content = new ShareContent();
        content.type = ShareContent.Type.IMAGE;
        content.imageBitmap = bitmap;

        doAction(activity, platform, content, listener);
    }

    public void release() {
        if (mTencent != null) {
            mTencent = null;
        }
    }
    // ======================== 使用说明 ========================
    /*
    1. 配置依赖（build.gradle）：
       // 友盟基础SDK
       implementation 'com.umeng.umsdk:common:1.5.4'
       implementation 'com.umeng.umsdk:share-core:7.1.4'
       // 各平台分享SDK
       implementation 'com.umeng.umsdk:share-wx:7.1.4'
       implementation 'com.umeng.umsdk:share-qq:7.1.4'
       implementation 'com.umeng.umsdk:share-sina:7.1.4'
       implementation 'com.tencent.tauth:qqopensdk:3.57.0' // QQ依赖

    2. AndroidManifest配置：
       // 1. 权限配置（添加到<manifest>标签内）
       <uses-permission android:name="android.permission.INTERNET" />
       <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
       <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
       <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
       <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
       <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

       // 2. 包可见性（Android 11+，添加到<manifest>标签内）
       <queries>
           <package android:name="com.tencent.mm" /> <!-- 微信 -->
           <package android:name="com.tencent.mobileqq" /> <!-- QQ -->
           <package android:name="com.sina.weibo" /> <!-- 微博 -->
           <package android:name="com.ss.android.ugc.aweme" /> <!-- 抖音 -->
           <package android:name="com.alibaba.android.rimet" /> <!-- 钉钉 -->
       </queries>

       // 3. FileProvider配置（添加到<application>标签内）
       <provider
           android:name="androidx.core.content.FileProvider"
           android:authorities="com.linewell.lxhdemo.fileprovider" <!-- 替换为你的FileProvider -->
           android:exported="false"
           android:grantUriPermissions="true">
           <meta-data
               android:name="android.support.FILE_PROVIDER_PATHS"
               android:resource="@xml/file_paths" />
       </provider>

       // 4. 微信回调Activity（添加到<application>标签内）
       <activity
           android:name="com.umeng.socialize.weixin.view.WXCallbackActivity"
           android:exported="true"
           android:launchMode="singleTask" />

    3. 新增file_paths.xml（res/xml目录下）：
       <?xml version="1.0" encoding="utf-8"?>
       <paths xmlns:android="http://schemas.android.com/apk/res/android">
           <external-path name="external_root" path="." />
           <external-files-path name="app_files" path="." />
           <external-media-path name="app_images" path="Pictures/" />
           <external-media-path name="app_videos" path="Movies/" />
       </paths>

    4. 初始化（在Application的onCreate中）：
       UMShareManager.Config config = new UMShareManager.Config();
       config.umAppKey = "你的友盟AppKey";
       config.fileProvider = "com.linewell.lxhdemo.fileprovider";
       config.wechatAppId = "你的微信AppId";
       config.qqAppId = "你的QQ AppId";
       config.qqAppKey = "你的QQ AppKey";
       // 其他平台参数按需配置...
       UMShareManager.init(this, config);

    5. 发起分享（在Activity中）：
       // 构建分享内容
       UMShareManager.ShareContent content = new UMShareManager.ShareContent();
       content.type = UMShareManager.ShareContent.Type.WEB;
       content.title = "测试分享";
       content.desc = "这是一条测试分享";
       content.url = "https://www.umeng.com";
       content.imageUrl = "https://www.umeng.com/images/pic/home/social.jpg";

       // 执行分享（微信好友）
       UMShareManager.getInstance().doAction(
           this,
           UMShareManager.Platform.WECHAT,
           content,
           new UMShareManager.ResultListener() {
               @Override
               public void onSuccess(String msg) {
                   Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
               }
               @Override
               public void onCancel(String msg) {
                   Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
               }
               @Override
               public void onError(String msg) {
                   Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
               }
           }
       );

    6. 回调处理（在Activity中）：
       @Override
       protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
           super.onActivityResult(requestCode, resultCode, data);
           UMShareManager.getInstance().onActivityResult(requestCode, resultCode, data);
       }
    */
}