package com.linx.mylibrary.http;


import com.linx.mylibrary.http.callback.JsonCallback;
import com.linx.mylibrary.http.config.HttpConfig;
import com.linx.mylibrary.http.model.BasePostEntity;
import com.linx.mylibrary.utils.RxJsonTool;
import com.linx.mylibrary.utils.json.JsonUtils;
import com.linx.mylibrary.utils.klog.KLog;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.HttpParams;
import com.lzy.okgo.request.PostRequest;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author xh
 * @Description (用一句话描述这个类的作用)
 * @date 2018/1/23 09:59
 */
public class HttpManage {
    private HttpManage() {
    }

    public static HttpManage getInstance() {
        return SingleInterest.INSTANCE;
    }

    private static class SingleInterest {
        private static final HttpManage INSTANCE = new HttpManage();
    }

    /**
     * get请求,拼接 url参数.
     *
     * @param tag
     * @param url
     * @param mapParameter 参数<key,value>
     * @param callback
     * @param <T>
     */
    public <T> void GetRequestForHttpParams(Object tag, String url, HttpParams mapParameter, JsonCallback<T> callback) {
        OkGo.<T>get(url)
                .tag(tag)
                .params(mapParameter)
                .execute(callback);
    }
 /**
     * get请求,拼接 url参数.
     *
     * @param tag
     * @param url
     * @param mapParameter 参数<key,value>
     * @param callback
     * @param <T>
     */
    public <T> void GetRequest(Object tag, String url, Map<String, String> mapParameter, JsonCallback<T> callback) {
        OkGo.<T>get(url)
                .tag(tag)
                .params(mapParameter)
                .execute(callback);
    }

    /**
     * @param url           url
     * @param tag           标识:this,无所谓,用来Activity销毁时候请求还没结束,结束请求用的
     * @param Jsonparameter 参数
     * @param callback      数据回调
     * @param <T>           泛型<LzyResponse<ServerModel>> <LzyResponse<具体的实体类>>或<LzyResponse<List<具体的实体类>>>LzyResponse是与服务器约定的基类
     */
    public <T> void postJsonRequets(Object tag, String url, String Jsonparameter, JsonCallback<T> callback) {
        KLog.d(">>>>--OkGo发送url=" + url + ";参数=" + Jsonparameter);
        OkGo.<T>post(url)
                .tag(tag)
                //.params("param1", "paramValue1")//  这里不要使用params，upJson 与 params 是互斥的，只有 upJson 的数据会被上传
                .upJson(setPostBody(Jsonparameter))//
                .execute(callback);
    }

    public <T> void postJsonRequets(Object tag, String url, Map<String, String> map, JsonCallback<T> callback) {
        String Jsonparameter = "";
        if (map != null) {
            Jsonparameter = RxJsonTool.toJson(map);
        }
        KLog.d(">>>>--OkGo发送url=" + url + ";参数=" + Jsonparameter);
        OkGo.<T>post(url)
                .tag(tag)
                //.params("param1", "paramValue1")//  这里不要使用params，upJson 与 params 是互斥的，只有 upJson 的数据会被上传
                .upJson(setPostBody(Jsonparameter))//
                .execute(callback);
    }

    /**
     * @param tag       标识
     * @param url       链接
     * @param parameter 字符串参数
     * @param callback  回调
     * @param <T>       泛型
     */
    public <T> void postStringRequets(Object tag, String url, String parameter, JsonCallback<T> callback) {
        OkGo.<T>post(url)
                .tag(tag)
                //.params("param1", "paramValue1")//  这里不要使用params，upJson 与 params 是互斥的，只有 upJson 的数据会被上传
                .upString(parameter)//
                .execute(callback);
    }

    public <T> void postBytesRequets(Object tag, String url, String parameter, JsonCallback<T> callback) {
        OkGo.<T>post(url)
                .tag(tag)
                //.params("param1", "paramValue1")//  这里不要使用params，upJson 与 params 是互斥的，只有 upJson 的数据会被上传
                .upBytes(parameter.getBytes())//
                .execute(callback);
    }

    /**
     * 上传单个文件,callback可以重载uploadProgress(Progress progress)获取上传进度
     *
     * @param tag
     * @param url
     * @param file     文件对象
     * @param callback 回调
     * @param <T>      泛型
     */
    public <T> void upFileSingle(Object tag, String url, String JsonParameter, File file, JsonCallback<T> callback) {
        OkGo.<T>post(url)
                .tag(tag)
                //.headers("header1", "headerValue1")//
                //.params("param1", "paramValue1")//
                .upJson(JsonParameter)
                .upFile(file)//
                .execute(callback);
    }

    /**
     * 上传多个文件,可以带参数 callback可以重载uploadProgress(Progress progress)获取上传进度
     *
     * @param tag
     * @param url
     * @param JsonParameter
     * @param files
     * @param callback
     * @param <T>
     */
    public <T> void upFileMulti(Object tag, String url, String JsonParameter, List<File> files, JsonCallback<T> callback) {
        OkGo.<T>post(url)
                .tag(tag)
                //.headers("header1", "headerValue1")
                .params("Param_Data", JsonParameter)
                .addFileParams("file_1", files) // 这种方式为同一个key，上传多个文件 key自己付服务端约定
                .execute(callback);
    }

    /**
     * @param tag           this
     * @param url
     * @param JsonParameter json
     * @param files         文件集合
     * @param callback
     * @param <T>
     */
    public <T> void upFileMulti2(Object tag, String url, String JsonParameter, List<File> files, JsonCallback<T> callback) {
        PostRequest<T> request = OkGo.<T>post(url)
                .tag(tag)
                .params("Param_Data", JsonParameter);
        for (int i = 0; i < files.size(); i++) {
            request.params("file_" + i + 1, files.get(i));
        }
        request.execute(callback);//这种方式为不知道数量上传多个文件，又要一个文件对应一个key,
    }


      /*---------------------------------------下载api须知↓↓↓↓------------------------------------------*/
    /*FileCallback()：空参构造
    FileCallback(String destFileName)：可以额外指定文件下载完成后的文件名
    FileCallback(String destFileDir, String destFileName)：可以额外指定文件的下载目录和下载完成后的文件名*/
    /*
        文件目录如果不指定,默认下载的目录为 sdcard/download/,文件名如果不指定,则按照以下规则命名:
      1.首先检查用户是否传入了文件名,如果传入,将以用户传入的文件名命名
      2.如果没有传入,那么将会检查服务端返回的响应头是否含有Content-Disposition=attachment;filename=FileName.txt该种形式的响应头,如果有,则按照该响应头中指定的文件名命名文件,如FileName.txt
      3.如果上述响应头不存在,则检查下载的文件url,例如:http://image.baidu.com/abc.jpg,那么将会自动以abc.jpg命名文件
      4.如果url也把文件名解析不出来,那么最终将以"unknownfile_" + System.currentTimeMillis()命名文件*/

    /**
     * 通过Url下载,get模式,网址下载,callback可以重载downloadProgress(Progress progress)方法获取下载进度
     *
     * @param tag          标识
     * @param downloadUrl  下载链接
     * @param fileCallback 下载回调监听 文件目录如果不指定,默认下载的目录为 sdcard/download/
     */
    public void fileDownloadForGetMode(Object tag, String downloadUrl, FileCallback fileCallback) {
        OkGo.<File>get(downloadUrl)
                .tag(tag)
                .execute(fileCallback);

    }
    /**
     * 通过Url下载,post模式可以传参数,callback可以重载downloadProgress(Progress progress)方法获取下载进度
     *
     * @param tag
     * @param downloadUrl
     * @param JsonParameter 参数
     * @param fileCallback  下载回调,需要在new的时候在构造方法中把FileName传进去
     */
    public void fileDownloadForPostMode(Object tag, String downloadUrl, String JsonParameter, FileCallback fileCallback) {
        OkGo.<File>post(downloadUrl)
                .tag(tag)
                //.params(params)//
                .upJson(JsonParameter)
                .execute(fileCallback);
    }

    /**
     * 取消请求
     *
     * @param tag 一般是this
     */
    public void cancelRequest(Object tag) {
        if (tag != null) {
            OkGo.getInstance().cancelTag(tag);
        } else {
            OkGo.getInstance().cancelAll();
        }

    }

    /**
     * 给参数加密
     *
     * @param postStr 需要加密的json字符串
     * @return 加密后的密文
     */
    public static String setPostBody(String postStr) {
        String params;
        if (HttpConfig.isEncode) {
            BasePostEntity basePostEntity = new BasePostEntity(postStr);
            params = JsonUtils.toJson(basePostEntity);
            return params;
        } else {
            return postStr;
        }

    }
}
