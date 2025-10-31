package com.linewell.lxhdemo.thirdAppUtil;

import com.linewell.lxhdemo.R;

public class UMConfig {

    // 必传参数
    public static final String umAppKey = ""; // 替换为你的友盟AppKey（从友盟后台获取）
    // 平台可选参数
    public static final String wechatAppId = ""; // 替换为你的微信AppId
    public static final String qqAppId = ""; // 替换为你的QQ AppId
    public static final String qqAppKey = ""; // 替换为你的QQ AppKey
    public static final String sinaAppKey = ""; // 替换为你的微博AppKey
    public static final String sinaAppSecret = ""; // 替换为你的微博AppSecret
    public static final String sinaRedirectUrl = ""; // 替换为你的微博回调页（需与微博开放平台一致）
    public static final String douyinClientKey = ""; // 替换为你的抖音ClientKey
    public static final String dingtalkAppId = ""; // 替换为你的钉钉AppId
    // 图标资源（需在res/drawable中添加对应图标）
    public static final int wechatIcon = R.drawable.share_wechat_ic;
    public static final int wechatCircleIcon = R.drawable.share_moment_ic;
    public static final int qqIcon = R.drawable.share_qq_ic;
    public static final int qzoneIcon = R.drawable.share_qzone_ic;
    public static final int sinaIcon = R.drawable.share_webo_ic;
    public static final int douyinIcon = R.drawable.share_douyin_ic;
    public static final int dingtalkIcon = R.drawable.share_ding_ic;
    public static final int copyLinkIcon = R.drawable.share_link_ic;
}
