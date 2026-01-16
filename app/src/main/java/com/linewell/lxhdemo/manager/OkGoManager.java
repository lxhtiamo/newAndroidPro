package com.linewell.lxhdemo.manager;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import com.linewell.lxhdemo.app.AppConfig;
import com.linewell.lxhdemo.app.MyApplication;
import com.linx.mylibrary.utils.klog.KLog;
import com.linx.mylibrary.utils.manager.LoginHelper;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheEntity;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.cookie.CookieJarImpl;
import com.lzy.okgo.cookie.store.DBCookieStore;
import com.lzy.okgo.https.HttpsUtils;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import com.lzy.okgo.model.HttpHeaders;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * OkGo 网络请求管理类
 * 统一封装 OkGo 的初始化、配置、全局参数设置等逻辑
 * 避免耦合在 Application 中导致代码凌乱
 */
public class OkGoManager {
    private static volatile OkGoManager instance;
    private static final int DEFAULT_TIMEOUT = 30000; // 默认连接超时时间
    private final Context mContext;
    private String mAccessToken; // 全局 Token

    // 私有构造方法，避免外部实例化
    private OkGoManager(Context context) {
        this.mContext = context.getApplicationContext(); // 使用Application Context，避免内存泄漏
    }

    /**
     * 获取单例实例
     *
     * @param context 上下文（建议传入 Application Context）
     * @return OkGoManager 单例
     */
    public static OkGoManager getInstance(Context context) {
        if (instance == null) {
            synchronized (OkGoManager.class) {
                if (instance == null) {
                    instance = new OkGoManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化 OkGo 配置（核心方法）
     */
    public void init() {
        //----------------不需要就不传------------
        //HttpHeaders headers = new HttpHeaders(); // 全局公共头
        // headers.put("commonHeaderKey1", "commonHeaderValue1"); //header不支持中文，不允许有特殊字符
        //headers.put("commonHeaderKey2", "commonHeaderValue2");
        //HttpParams params = new HttpParams();//全局公共参数
        // params.put("commonParamsKey1", "commonParamsValue1"); //param支持中文,直接传,不要自己编码
        // params.put("commonParamsKey2", "这里支持中文参数");
        //----------------------------------------------------------------------------------------//

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        //log相关
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);        //log打印级别，决定了log显示的详细程度
        loggingInterceptor.setColorLevel(Level.INFO);                               //log颜色级别，决定了log在控制台显示的颜色
        builder.addInterceptor(loggingInterceptor);                                 //添加OkGo默认debug日志
        //第三方的开源库，使用通知显示当前请求的log，不过在做文件下载的时候，这个库好像有问题，对文件判断不准确
        //builder.addInterceptor(new ChuckInterceptor(this));

        //超时时间设置，默认60秒
        builder.readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);      //全局的读取超时时间
        builder.writeTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);     //全局的写入超时时间
        builder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);   //全局的连接超时时间

        //自动管理cookie（或者叫session的保持），以下几种任选其一就行
        //builder.cookieJar(new CookieJarImpl(new SPCookieStore(this)));            //使用sp保持cookie，如果cookie不过期，则一直有效
        builder.cookieJar(new CookieJarImpl(new DBCookieStore(mContext)));              //使用数据库保持cookie，如果cookie不过期，则一直有效
        //builder.cookieJar(new CookieJarImpl(new MemoryCookieStore()));            //使用内存保持cookie，app退出后，cookie消失

        //https相关设置，以下几种方案根据需要自己设置
        //方法一：信任所有证书,不安全有风险
        HttpsUtils.SSLParams sslParams1 = HttpsUtils.getSslSocketFactory();
        //方法二：自定义信任规则，校验服务端证书
        HttpsUtils.SSLParams sslParams2 = HttpsUtils.getSslSocketFactory(new SafeTrustManager());
        //方法三：使用预埋证书，校验服务端证书（自签名证书）
        //HttpsUtils.SSLParams sslParams3 = HttpsUtils.getSslSocketFactory(getAssets().open("srca.cer"));
        //方法四：使用bks证书和密码管理客户端证书（双向认证），使用预埋证书，校验服务端证书（自签名证书）
        //HttpsUtils.SSLParams sslParams4 = HttpsUtils.getSslSocketFactory(getAssets().open("xxx.bks"), "123456", getAssets().open("yyy.cer"));
        builder.sslSocketFactory(sslParams1.sSLSocketFactory, sslParams1.trustManager);
        //配置https的域名匹配规则，详细看demo的初始化介绍，不需要就不要加入，使用不当会导致https握手失败
        //builder.hostnameVerifier(new SafeHostnameVerifier());

        // 其他统一的配置
        // 详细说明看GitHub文档：https://github.com/jeasonlzy/
        OkGo.getInstance().init((Application) mContext)                           //必须调用初始化
                .setOkHttpClient(builder.build())               //建议设置OkHttpClient，不设置会使用默认的
                .setCacheMode(CacheMode.NO_CACHE)               //全局统一缓存模式，默认不使用缓存，可以不传
                .setCacheTime(CacheEntity.CACHE_NEVER_EXPIRE)   //全局统一缓存时间，默认永不过期，可以不传
                .setRetryCount(1);                            //全局统一超时重连次数，默认为三次，那么最差的情况会请求4次(一次原始请求，三次重连请求)，不需要可以设置为0
        //.addCommonHeaders(headers)                      //全局公共头
        //.addCommonParams(params);                       //全局公共参数

        // ==========初始化时自动读取 SP 中的 Token 并设置 ==========
        getTokenFromSP();
    }

    /**
     * 自动从 SP 读取 Token，并设置为 OkGo 全局 Header
     */
    private void getTokenFromSP() {
        // 从 SP 读取上次保存的 Token
        LoginHelper loginHelper = LoginHelper.getInstance(mContext);
        String userToken = loginHelper.getUserToken();
        if (!TextUtils.isEmpty(userToken)) {
            setAccessToken(userToken);
        } else {
            KLog.d("OkGoManager: SP 中无保存的 Token");
        }
    }

    /**
     * 设置全局 AccessToken 请求头
     *
     * @param accessToken Token 值
     */
    public void setAccessToken(String accessToken) {
        if (!TextUtils.isEmpty(accessToken)) {
            this.mAccessToken = accessToken;
            HttpHeaders headers = new HttpHeaders();
            headers.put(AppConfig.Access_Token, accessToken);
            OkGo.getInstance().addCommonHeaders(headers);
            KLog.d("OkGoManager: 全局 AccessToken 已设置 = " + accessToken);
        }
    }

    /**
     * 获取当前的全局 Token
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    // ======================== HTTPS 相关内部类 ========================

    /**
     * 这里只是我谁便写的认证规则，具体每个业务是否需要验证，以及验证规则是什么，请与服务端或者leader确定
     * 重要的事情说三遍，以下代码不要直接使用
     * 自定义信任管理器（根据业务需求调整验证规则）
     */
    private class SafeTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                for (X509Certificate certificate : chain) {
                    certificate.checkValidity(); // 检查证书有效期、签名等
                }
            } catch (Exception e) {
                throw new CertificateException(e);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * 这里只是我谁便写的认证规则，具体每个业务是否需要验证，以及验证规则是什么，请与服务端或者leader确定
     * * 重要的事情说三遍，以下代码不要直接使用
     * 自定义主机名验证器（根据业务需求调整验证规则）
     */
    private class SafeHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            // 建议根据实际域名做验证，示例中直接返回true（信任所有域名）
            //验证主机名是否匹配
            //return hostname.equals("server.jeasonlzy.com");
            return true;
        }
    }
}