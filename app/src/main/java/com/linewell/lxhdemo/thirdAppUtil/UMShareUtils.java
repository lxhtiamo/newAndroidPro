package com.linewell.lxhdemo.thirdAppUtil;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.app.MyApplication;
import com.tencent.tauth.Tencent;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.socialize.PlatformConfig;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMEmoji;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.media.UMQQMini;
import com.umeng.socialize.media.UMVideo;
import com.umeng.socialize.media.UMWeb;
import com.umeng.socialize.media.UMMin;
import com.umeng.socialize.media.UMusic;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 友盟分享工具类（回调本地化：哪里调用哪里处理回调）
 * 支持：网页、小程序（微信/QQ）、图片、多图、文本、视频、音乐、GIF表情
 */
public class UMShareUtils {

    /*分享的app信息*/
    public static class ShareAppInfo {
        public UMShareUtils.Platform platform;
        public String name;//名称
        public int icon;//图标
        public boolean isInstalled; // 是否安装对应应用

        public ShareAppInfo(UMShareUtils.Platform platform, String name, int icon, boolean isInstalled) {
            this.platform = platform;
            this.name = name;
            this.icon = icon;
            this.isInstalled = isInstalled;
        }
    }

    public static class ShareContent {
        public String title; // 标题（网页/视频/图片分享用）
        public String desc; // 描述（网页分享用，朋友圈除外）
        public String url; // 网页链接/复制链接
        public String text; // 纯文本内容（文本分享用）
        public String imageLocalPath; // 本地图片路径（优先级：Bitmap > 本地路径 > 网络URL）
        public String imageUrl; // 网络图片URL
        public String videoUrl; // 视频路径（仅支持本地文件，如：/storage/emulated/0/xxx.mp4）
        public String musicUrl; // 音乐的播放链接
        public String targetUrl; // 音乐的跳转链接
        public UMShareUtils.ShareContent.Type type; // 分享类型
        public String miniUserName; // 微信小程序原始ID（如 gh_xxxxxxxxxxxx，微信小程序必传）
        public String miniAppId; // QQ小程序AppId（QQ小程序必传）
        public String miniPath; // 小程序页面路径（如 pages/index/index，必传）

        //分享的类型
        public enum Type {WEB, IMAGE, VIDEO, MUSIC, TEXT, COPY_LINK, WECHAT_MINI, QQ_MINI}
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

    // 常量：默认压缩配置（项目可根据需求调整）
    public static final UMImage.CompressStyle DEFAULT_COMPRESS_STYLE = UMImage.CompressStyle.SCALE; // 大小压缩
    public static final int DEFAULT_THUMB_RES = R.mipmap.ic_launcher; // 默认缩略图资源（需自行添加）

    private static volatile UMShareUtils instance;
    private UMShareAPI mShareAPI;
    private Tencent mTencent;

    // 单例模式（不持有全局上下文，避免内存泄漏）
    private UMShareUtils() {
    }

    public static UMShareUtils getInstance() {
        if (instance == null) {
            synchronized (UMShareUtils.class) {
                if (instance == null) {
                    instance = new UMShareUtils();
                }
            }
        }
        return instance;
    }

    /**
     * 获取支持的分享平台列表（自动过滤未安装应用）
     */
    public List<UMShareUtils.ShareAppInfo> getShareAppLists(Activity activity) {
        List<UMShareUtils.ShareAppInfo> platformList = new ArrayList<>();
        // 添加各平台（图标为0时不添加，避免空图标）
        addShareApp(platformList, activity, UMShareUtils.Platform.WECHAT, "微信好友", UMConfig.wechatIcon);
        addShareApp(platformList, activity, UMShareUtils.Platform.WECHAT_CIRCLE, "朋友圈", UMConfig.wechatCircleIcon);
        addShareApp(platformList, activity, UMShareUtils.Platform.QQ, "QQ好友", UMConfig.qqIcon);
        addShareApp(platformList, activity, UMShareUtils.Platform.QZONE, "QQ空间", UMConfig.qzoneIcon);
        addShareApp(platformList, activity, UMShareUtils.Platform.SINA, "微博", UMConfig.sinaIcon);
        addShareApp(platformList, activity, UMShareUtils.Platform.DOUYIN, "抖音", UMConfig.douyinIcon);
        addShareApp(platformList, activity, UMShareUtils.Platform.DINGTALK, "钉钉", UMConfig.dingtalkIcon);
        // 复制链接（始终显示，无需检测安装）
        platformList.add(new UMShareUtils.ShareAppInfo(UMShareUtils.Platform.COPY_LINK, "复制链接", UMConfig.copyLinkIcon, true));
        return platformList;
    }

    /**
     * 添加平台到列表（过滤图标为空的平台）
     */
    private void addShareApp(List<UMShareUtils.ShareAppInfo> list, Activity activity, UMShareUtils.Platform platform, String name, int icon) {
        if (icon == 0) {
            return;
        }
        // 检测应用是否安装
        boolean isInstalled = platform.media != null && mShareAPI.isInstall(activity, platform.media);
        //检查是否配置了相关平台的appid
        boolean platformAvailable = isPlatformAvailable(activity, platform);

        if (isInstalled && platformAvailable)
            list.add(new UMShareUtils.ShareAppInfo(platform, name, icon, isInstalled));
    }

    public boolean isPlatformAvailable(Activity activity, UMShareUtils.Platform platform) {
        if (platform == UMShareUtils.Platform.COPY_LINK) {
            return true; // 复制链接始终可用
        }

        if (platform.media == null || !mShareAPI.isInstall(activity, platform.media)) {
            return false;
        }
        // 检查平台配置
        switch (platform) {
            case WECHAT:
            case WECHAT_CIRCLE:
                return !TextUtils.isEmpty(UMConfig.wechatAppId);
            case QQ:
            case QZONE:
                return !TextUtils.isEmpty(UMConfig.qqAppId);
            case SINA:
                return !TextUtils.isEmpty(UMConfig.sinaAppKey);
            case DOUYIN:
                return !TextUtils.isEmpty(UMConfig.douyinClientKey);
            case DINGTALK:
                return !TextUtils.isEmpty(UMConfig.dingtalkAppId);
            default:
                return true;
        }
    }

    // ======================== 结果回调接口 ========================
    public interface ResultListener {
        void onStart(String msg, SHARE_MEDIA share_media);

        void onSuccess(String msg, SHARE_MEDIA share_media);

        void onCancel(String msg, SHARE_MEDIA share_media);

        void onError(String msg, SHARE_MEDIA share_media, Throwable throwable);
    }

    /**
     * 执行分享/复制操作（入口方法，已适配XXPermissions异步权限）
     *
     * @param activity 上下文（需传入Activity，用于XXPermissions和UI线程）
     * @param platform 目标平台
     * @param content  分享内容
     * @param listener 分享结果回调
     */
    public void doShare(Activity activity, UMShareUtils.Platform platform, UMShareUtils.ShareContent content, UMShareUtils.ResultListener listener) {
        // 1. 基础参数校验
        if (activity == null || content == null || content.type == null || listener == null) {
            if (listener != null) {
                listener.onError("参数异常：Listener不能为空", null, null);
            }
            return;
        }
        if (activity == null || activity.isFinishing()) {
            listener.onError("Activity无效或已销毁", null, null);
            return;
        }
        String validationError = validateShareContent(content);
        if (validationError != null) {
            listener.onError(validationError, null, null);
            return;
        }
        // 2. 复制链接特殊处理（无需权限）
        if (platform == UMShareUtils.Platform.COPY_LINK) {
            copyLink(activity, content.url, listener);
            return;
        }

        // 3. 应用安装检测
        if (platform.media == null || !mShareAPI.isInstall(activity, platform.media)) {
            listener.onError(platform.name() + "未安装", null, null);
            jumpToMarket(activity, platform); // 跳转应用商店
            return;
        }

        // 4. 平台类型支持校验
        if (!isPlatformSupportType(platform, content.type)) {
            listener.onError(platform.name() + "不支持" + content.type.name() + "类型", null, null);
            return;
        }
        executeShare(activity, platform, content, listener);
    }

    private void executeShare(Activity activity, UMShareUtils.Platform platform, UMShareUtils.ShareContent content, UMShareUtils.ResultListener listener) {
        switch (content.type) {
            case WECHAT_MINI:
                shareWechatMini(activity, content.url, content.title, content.desc, content.miniPath, content.miniUserName, content.imageUrl, listener);
                break;
            case QQ_MINI:
                shareQQMini(activity, content.url, content.title, content.desc, content.miniPath, content.miniAppId, content.imageUrl, listener);
                break;
            case TEXT:
                shareText(activity, platform.media, content.text, listener);
                break;
            case WEB:
                shareWeb(activity, platform.media, content.url, content.title, content.desc, content.imageUrl, listener);
                break;
            case IMAGE:
                shareImage(activity, platform.media, content.imageUrl, content.text, listener);
                break;
            case VIDEO:
                shareVideo(activity, platform.media, content.videoUrl, content.title, content.desc, content.text, content.imageUrl, listener);
                break;
            case MUSIC:
                shareMusic(activity, platform.media, content.musicUrl, content.targetUrl, content.title, content.desc, content.imageUrl, listener);
                break;
            default:
                listener.onError("不支持的分享类型：" + content.type.name(), null, null);
        }
    }

    /**
     * 检查平台是否支持当前分享类型
     */
    private boolean isPlatformSupportType(UMShareUtils.Platform platform, UMShareUtils.ShareContent.Type type) {
        // 使用Map维护平台支持的类型，便于维护和扩展
        Map<UMShareUtils.Platform, Set<UMShareUtils.ShareContent.Type>> platformSupportMap = new HashMap<>();

        // 微信支持的类型
        Set<UMShareUtils.ShareContent.Type> wechatTypes = new HashSet<>(Arrays.asList(
                UMShareUtils.ShareContent.Type.TEXT,
                UMShareUtils.ShareContent.Type.IMAGE,
                UMShareUtils.ShareContent.Type.WEB,
                UMShareUtils.ShareContent.Type.VIDEO,
                UMShareUtils.ShareContent.Type.MUSIC,
                UMShareUtils.ShareContent.Type.WECHAT_MINI
        ));

        // 朋友圈支持的类型（与微信基本相同）
        Set<UMShareUtils.ShareContent.Type> wechatCircleTypes = new HashSet<>(Arrays.asList(
                UMShareUtils.ShareContent.Type.TEXT,
                UMShareUtils.ShareContent.Type.IMAGE,
                UMShareUtils.ShareContent.Type.WEB,
                UMShareUtils.ShareContent.Type.VIDEO,
                UMShareUtils.ShareContent.Type.MUSIC,
                UMShareUtils.ShareContent.Type.WECHAT_MINI
        ));

        // QQ支持的类型（不支持纯文本）
        Set<UMShareUtils.ShareContent.Type> qqTypes = new HashSet<>(Arrays.asList(
                UMShareUtils.ShareContent.Type.IMAGE,
                UMShareUtils.ShareContent.Type.WEB,
                UMShareUtils.ShareContent.Type.VIDEO,
                UMShareUtils.ShareContent.Type.MUSIC,
                UMShareUtils.ShareContent.Type.QQ_MINI
        ));

        // QQ空间支持的类型
        Set<UMShareUtils.ShareContent.Type> qzoneTypes = new HashSet<>(Arrays.asList(
                UMShareUtils.ShareContent.Type.TEXT,
                UMShareUtils.ShareContent.Type.IMAGE,
                UMShareUtils.ShareContent.Type.WEB,
                UMShareUtils.ShareContent.Type.MUSIC,
                UMShareUtils.ShareContent.Type.VIDEO
        ));

        // 微博支持的类型
        Set<UMShareUtils.ShareContent.Type> sinaTypes = new HashSet<>(Arrays.asList(
                UMShareUtils.ShareContent.Type.TEXT,
                UMShareUtils.ShareContent.Type.IMAGE,
                UMShareUtils.ShareContent.Type.WEB,
                UMShareUtils.ShareContent.Type.MUSIC,
                UMShareUtils.ShareContent.Type.VIDEO
        ));

        // 抖音支持的类型（不支持纯文本）
        Set<UMShareUtils.ShareContent.Type> douyinTypes = new HashSet<>(Arrays.asList(
                UMShareUtils.ShareContent.Type.IMAGE,
                UMShareUtils.ShareContent.Type.VIDEO
        ));

        // 钉钉支持的类型
        Set<UMShareUtils.ShareContent.Type> dingtalkTypes = new HashSet<>(Arrays.asList(
                UMShareUtils.ShareContent.Type.TEXT,
                UMShareUtils.ShareContent.Type.IMAGE,
                UMShareUtils.ShareContent.Type.WEB,
                UMShareUtils.ShareContent.Type.MUSIC,
                UMShareUtils.ShareContent.Type.VIDEO
        ));

        // 初始化映射关系
        platformSupportMap.put(UMShareUtils.Platform.WECHAT, wechatTypes);
        platformSupportMap.put(UMShareUtils.Platform.WECHAT_CIRCLE, wechatCircleTypes);
        platformSupportMap.put(UMShareUtils.Platform.QQ, qqTypes);
        platformSupportMap.put(UMShareUtils.Platform.QZONE, qzoneTypes);
        platformSupportMap.put(UMShareUtils.Platform.SINA, sinaTypes);
        platformSupportMap.put(UMShareUtils.Platform.DOUYIN, douyinTypes);
        platformSupportMap.put(UMShareUtils.Platform.DINGTALK, dingtalkTypes);
        // 获取平台支持的类型集合
        Set<UMShareUtils.ShareContent.Type> supportedTypes = platformSupportMap.get(platform);

        // 如果平台不在映射中或类型不在支持列表中，返回false
        return supportedTypes != null && supportedTypes.contains(type);
    }

    /**
     * 跳转应用商店下载对应平台应用
     */
    private void jumpToMarket(Activity activity, UMShareUtils.Platform platform) {
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
    private String getPlatformPackageName(UMShareUtils.Platform platform) {
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

    /**
     * 复制链接到剪贴板
     */
    private void copyLink(Activity activity, String url, UMShareUtils.ResultListener listener) {
        if (TextUtils.isEmpty(url)) {
            listener.onError("复制链接不能为空", null, null);
            return;
        }
        try {
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                listener.onError("剪贴板服务不可用", null, null);
                return;
            }
            // 复制文本
            ClipData clipData = ClipData.newPlainText("share_link", url);
            clipboard.setPrimaryClip(clipData);
            listener.onSuccess("复制链接成功", null);
        } catch (Exception e) {
            listener.onError("复制链接失败：" + e.getMessage(), null, null);
        }
    }

    /**
     * 验证分享内容是否有效
     */
    public String validateShareContent(UMShareUtils.ShareContent content) {
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
                break;

            // -------------------- 新增：QQ小程序校验 --------------------
            case QQ_MINI:
                if (TextUtils.isEmpty(content.url)) return "QQ小程序需传入兼容网页链接";

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
                if (TextUtils.isEmpty(content.imageLocalPath) || TextUtils.isEmpty(content.imageUrl)) {
                    return "图片资源不能为空";
                }
                break;
            case VIDEO:
                if (TextUtils.isEmpty(content.videoUrl)) {
                    return "视频路径不能为空";
                }
                break;
            case MUSIC:
                if (TextUtils.isEmpty(content.musicUrl)) {
                    return "音乐播放路径不能为空";
                }
                break;
            case COPY_LINK:
                if (TextUtils.isEmpty(content.desc) || TextUtils.isEmpty(content.url) || TextUtils.isEmpty(content.text)) {
                    return "复制内容不能为空";
                }
                break;
        }

        return null; // 验证通过
    }

    /*-----------------初始化------------------*/
    public void init(Context context) {
        // 初始化友盟SDK
        Context appContext = context.getApplicationContext();
        UMConfigure.init(appContext, UMConfig.umAppKey, "umeng", UMConfigure.DEVICE_TYPE_PHONE, "");
        this.mShareAPI = UMShareAPI.get(appContext);
        // 平台配置
        initShareAPI(appContext);
        initAppKeyConfig();
    }

    // 友盟的api
    public void initShareAPI(Context appContext) {
        // 初始化QQ单例
        if (!TextUtils.isEmpty(UMConfig.qqAppId)) {
            this.mTencent = Tencent.createInstance(UMConfig.qqAppId, appContext);
        }
    }

    // 初始化各平台配置
    private static void initAppKeyConfig() {
        String fileProvider = MyApplication.getInstance().getPackageName() + ".fileprovider";
        PlatformConfig.setFileProvider(fileProvider);
        // 微信配置
        if (!TextUtils.isEmpty(UMConfig.wechatAppId)) {
            PlatformConfig.setWeixin(UMConfig.wechatAppId, "");

        }
        // QQ配置
        if (!TextUtils.isEmpty(UMConfig.qqAppId) && !TextUtils.isEmpty(UMConfig.qqAppKey)) {
            PlatformConfig.setQQZone(UMConfig.qqAppId, UMConfig.qqAppKey);
        }
        // 微博配置
        if (!TextUtils.isEmpty(UMConfig.sinaAppKey) && !TextUtils.isEmpty(UMConfig.sinaAppSecret) && !TextUtils.isEmpty(UMConfig.sinaRedirectUrl)) {
            PlatformConfig.setSinaWeibo(UMConfig.sinaAppKey, UMConfig.sinaAppSecret, UMConfig.sinaRedirectUrl);
        }
        // 抖音配置
        if (!TextUtils.isEmpty(UMConfig.douyinClientKey)) {
            PlatformConfig.setBytedance(UMConfig.douyinClientKey, "", "", fileProvider);
        }
        // 钉钉配置
        if (!TextUtils.isEmpty(UMConfig.dingtalkAppId)) {
            PlatformConfig.setDing(UMConfig.dingtalkAppId);
        }
        // 智能适配开关
        UMShareAPI.setSmartEnable(true);
    }
    // ========================== 分享网页 ==========================

    /**
     * 分享网页链接
     *
     * @param context     当前上下文（需传入Activity/Fragment，不能用Application）
     * @param platform    分享平台（如SHARE_MEDIA.WEIXIN）
     * @param url         网页链接
     * @param title       标题
     * @param description 描述
     * @param thumb       缩略图（可为null，使用默认）
     * @param listener    当前分享的回调（哪里调用哪里实现）
     */
    public void shareWeb(Activity context, SHARE_MEDIA platform, String url, String title, String description, String thumb, ResultListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context不能为null，需传入当前Activity/Fragment");
        }
        UMWeb web = new UMWeb(url);
        web.setTitle(title);
        web.setDescription(description);
        web.setThumb(getImageThumb(context, thumb));
        new ShareAction(context)
                .setPlatform(platform)
                .withMedia(web)
                .setCallback(getSafeListener(listener, context, platform))
                .share();
    }

    private UMImage getImageThumb(Activity context, String thumb) {
        UMImage image;
        if (TextUtils.isEmpty(thumb)) {
            image = new UMImage(context, R.mipmap.ic_launcher);
        } else if (thumb.startsWith("/")) {
            File file = new File(thumb);
            if (file.exists()) {
                image = new UMImage(context, file);
            } else {
                image = new UMImage(context, R.mipmap.ic_launcher);
            }
        } else if (thumb.startsWith("http")) {
            image = new UMImage(context, thumb);
        } else {
            image = new UMImage(context, R.mipmap.ic_launcher);
        }
        image.compressStyle = DEFAULT_COMPRESS_STYLE;
        return image;
    }
    // ========================== 分享小程序 ==========================

    /**
     * 分享微信小程序（仅支持微信好友）
     *
     * @param context     当前上下文（Activity/Fragment）
     * @param url         兼容低版本的网页链接
     * @param title       标题
     * @param description 描述
     * @param path        小程序页面路径（如"pages/index/index"）
     * @param userName    小程序原始ID（微信公众平台获取）
     * @param thumb       缩略图（可为null）
     * @param listener    当前分享的回调
     */
    public void shareWechatMini(Activity context, String url, String title, String description, String path, String userName, String thumb, ResultListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context不能为null，需传入当前Activity/Fragment");
        }
        UMMin umMin = new UMMin(url);
        umMin.setThumb(getImageThumb(context, thumb));
        umMin.setTitle(title);
        umMin.setDescription(description);
        umMin.setPath(path);
        umMin.setUserName(userName);

        new ShareAction(context)
                .setPlatform(SHARE_MEDIA.WEIXIN) // 微信小程序仅支持好友
                .withMedia(umMin)
                .setCallback(getSafeListener(listener, context, SHARE_MEDIA.WEIXIN))
                .share();
    }

    /**
     * 分享QQ小程序
     *
     * @param context     当前上下文（Activity/Fragment）
     * @param url         链接
     * @param title       标题
     * @param description 描述
     * @param miniAppId   QQ小程序AppId（QQ开放平台获取）
     * @param path        小程序页面路径
     * @param thumb       缩略图（可为null）
     * @param listener    当前分享的回调
     */
    public void shareQQMini(Activity context, String url, String title, String description, String path, String miniAppId, String thumb, ResultListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context不能为null，需传入当前Activity/Fragment");
        }
        UMQQMini qqMini = new UMQQMini(url);
        qqMini.setThumb(getImageThumb(context, thumb));
        qqMini.setTitle(title);
        qqMini.setDescription(description);
        qqMini.setMiniAppId(miniAppId);
        qqMini.setPath(path);

        new ShareAction(context)
                .setPlatform(SHARE_MEDIA.QQ)
                .withMedia(qqMini)
                .setCallback(getSafeListener(listener, context, SHARE_MEDIA.QQ))
                .share();
    }

    // ========================== 分享图片 ==========================

    /**
     * 分享单张图片
     *
     * @param context  当前上下文
     * @param platform 分享平台
     * @param image    图片对象（支持网络、本地、资源等）
     * @param text     附加文本（可为null）
     * @param listener 当前分享的回调
     */
    public void shareImage(Activity context, SHARE_MEDIA platform, String image, String text,
                           ResultListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context不能为null，需传入当前Activity/Fragment");
        }
        UMImage imageThumb = getImageThumb(context, image);
        ShareAction shareAction = new ShareAction(context)
                .setPlatform(platform)
                .withMedia(imageThumb)
                .setCallback(getSafeListener(listener, context, platform));
        // 附加文本（可选）
        if (text != null && !text.isEmpty()) {
            shareAction.withText(text);
        }
        shareAction.share();
    }

    /**
     * 多图分享（支持新浪微博、QQ空间、抖音等）
     *
     * @param context  当前上下文
     * @param platform 分享平台
     * @param images   图片列表（建议不超过9张）
     * @param text     附加文本（可为null）
     * @param listener 当前分享的回调
     */
    public void shareMultiImage(Activity context, SHARE_MEDIA platform, List<String> images,
                                String text, ResultListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context不能为null，需传入当前Activity/Fragment");
        }
        List<UMImage> imgs = new ArrayList<>();
        // 处理每张图片的压缩和缩略图
        for (String image : images) {
            UMImage imageThumb = getImageThumb(context, image);
            imgs.add(imageThumb);
        }

        ShareAction shareAction = new ShareAction(context)
                .setPlatform(platform)
                .withMedias(imgs.toArray(new UMImage[0]))
                .setCallback(getSafeListener(listener, context, platform));
        if (text != null && !text.isEmpty()) {
            shareAction.withText(text);
        }
        shareAction.share();
    }

    // ========================== 分享文本 ==========================

    /**
     * 分享纯文本
     *
     * @param context  当前上下文
     * @param platform 分享平台
     * @param text     文本内容
     * @param listener 当前分享的回调
     */
    public void shareText(Activity context, SHARE_MEDIA platform, String text, ResultListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context不能为null，需传入当前Activity/Fragment");
        }
        new ShareAction(context)
                .setPlatform(platform)
                .withText(text)
                .setCallback(getSafeListener(listener, context, platform))
                .share();
    }

    // ========================== 分享视频 ==========================

    /**
     * 分享网络视频
     *
     * @param context     当前上下文
     * @param platform    分享平台
     * @param videoUrl    视频链接
     * @param title       标题
     * @param description 描述
     * @param thumbUrl    缩略图链接
     * @param listener    当前分享的回调
     */
    public void shareVideo(Activity context, SHARE_MEDIA platform, String videoUrl, String title, String description, String text,
                           String thumbUrl, ResultListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context不能为null，需传入当前Activity/Fragment");
        }
        UMVideo video = new UMVideo(videoUrl);
        video.setTitle(title);
        video.setDescription(description);
        video.setThumb(getImageThumb(context, thumbUrl)); // 视频缩略图（网络链接）

        ShareAction shareAction = new ShareAction(context)
                .setPlatform(platform)
                .withMedia(video)
                .setCallback(getSafeListener(listener, context, platform));
        if (text != null && !text.isEmpty()) {
            shareAction.withText(title);
        }
        shareAction.share();
    }

    // ========================== 分享音乐 ==========================

    /**
     * 分享网络音乐
     *
     * @param context     当前上下文
     * @param platform    分享平台
     * @param musicUrl    音乐播放链接
     * @param targetUrl   音乐跳转链接（如歌曲详情页）
     * @param title       标题
     * @param description 描述
     * @param thumbUrl    缩略图链接
     * @param listener    当前分享的回调
     */
    public void shareMusic(Activity context, SHARE_MEDIA platform, String musicUrl, String targetUrl,
                           String title, String description, String thumbUrl, ResultListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context不能为null，需传入当前Activity/Fragment");
        }
        UMusic music = new UMusic(musicUrl);
        music.setTitle(title);
        music.setDescription(description);
        music.setThumb(getImageThumb(context, thumbUrl));
        music.setmTargetUrl(targetUrl); // 跳转链接
        new ShareAction(context)
                .setPlatform(platform)
                .withMedia(music)
                .setCallback(getSafeListener(listener, context, platform))
                .share();
    }

    // ========================== 分享GIF表情 ==========================

    /**
     * 分享GIF表情（仅微信好友支持）
     *
     * @param context  当前上下文
     * @param gifUrl   GIF图片网络链接
     * @param listener 当前分享的回调
     */
    public void shareEmoji(Activity context, String gifUrl, ResultListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context不能为null，需传入当前Activity/Fragment");
        }
        UMEmoji emoji = new UMEmoji(context, gifUrl);
        emoji.setThumb(getDefaultThumb(context)); // 必须设置缩略图

        new ShareAction(context)
                .setPlatform(SHARE_MEDIA.WEIXIN)
                .withMedia(emoji)
                .setCallback(getSafeListener(listener, context, SHARE_MEDIA.WEIXIN))
                .share();
    }
    // ========================== 工具方法 ==========================

    /**
     * 获取默认缩略图（使用当前上下文）
     */
    private UMImage getDefaultThumb(Activity context) {
        return new UMImage(context, DEFAULT_THUMB_RES);
    }

    /**
     * 安全处理回调：如果listener为null，提供默认空实现（避免空指针）
     */
    private UMShareListener getSafeListener(ResultListener listener, Activity context, SHARE_MEDIA platform) {
        // 默认回调（可根据需求简化）
        return new UMShareListener() {
            @Override
            public void onStart(SHARE_MEDIA share_media) {

                listener.onStart("开始", share_media);
            }

            @Override
            public void onResult(SHARE_MEDIA share_media) {
                listener.onSuccess("分享成功", share_media);
            }

            @Override
            public void onError(SHARE_MEDIA share_media, Throwable throwable) {
                listener.onError("分享失败", share_media, throwable);
                Toast.makeText(context, platform.getName() + "分享失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel(SHARE_MEDIA share_media) {
                listener.onCancel("分享取消", share_media);
            }
        };
    }

    /**
     * 释放资源（在Activity的onDestroy中调用）
     */
    public void release(Activity context) {
        if (context != null) {
            UMShareAPI.get(context).release();
        }
    }
}