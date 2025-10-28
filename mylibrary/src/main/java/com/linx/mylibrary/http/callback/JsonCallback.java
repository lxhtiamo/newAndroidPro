/*
 * Copyright 2016 jeasonlzy(廖子尧)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linx.mylibrary.http.callback;

import android.text.TextUtils;

import com.google.gson.JsonParseException;
import com.linx.mylibrary.http.exception.MyException;
import com.linx.mylibrary.http.model.LzyResponse;
import com.linx.mylibrary.utils.klog.KLog;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.exception.HttpException;
import com.lzy.okgo.exception.StorageException;
import com.lzy.okgo.request.base.Request;

import org.json.JSONException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import okhttp3.Response;

/**
 * ================================================
 * 作    者：jeasonlzy（廖子尧）Github地址：https://github.com/jeasonlzy
 * 版    本：1.0
 * 创建日期：2016/1/14
 * 描    述：默认将返回的数据解析成需要的Bean,可以是 BaseBean，String，List，Map
 * 修订历史：
 * ================================================
 */
public abstract class JsonCallback<T> extends AbsCallback<T> {

    private Type type;
    private Class<T> clazz;

    public JsonCallback() {
    }

    public JsonCallback(Type type) {
        this.type = type;
    }

    public JsonCallback(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void onStart(Request<T, ? extends Request> request) {
        super.onStart(request);
        // 主要用于在所有请求之前添加公共的请求头或请求参数
        // 例如登录授权的 token
        // 使用的设备信息
        // 可以随意添加,也可以什么都不传
        // 加密代码，根据自己的业务需求和服务器的配合，
     /*   request.headers("header1", "HeaderValue1")//
                .params("params1", "ParamsValue1")//
                .params("token", "3215sdf13ad1f65asd4f3ads1f");*/
    }


    /**
     * 该方法是子线程处理，不能做ui相关的工作
     * 主要作用是解析网络返回的 response 对象,生产onSuccess回调中需要的数据对象
     * 这里的解析工作不同的业务逻辑基本都不一样,所以需要自己实现,以下给出的时模板代码,实际使用根据需要修改
     */
    @Override
    public T convertResponse(Response response) throws Throwable {

        // 重要的事情说三遍，不同的业务，这里的代码逻辑都不一样，如果你不修改，那么基本不可用
        // 重要的事情说三遍，不同的业务，这里的代码逻辑都不一样，如果你不修改，那么基本不可用
        // 重要的事情说三遍，不同的业务，这里的代码逻辑都不一样，如果你不修改，那么基本不可用

        //详细自定义的原理和文档，看这里： https://github.com/jeasonlzy/okhttp-OkGo/wiki/JsonCallback

        if (type == null) {
            if (clazz == null) {
                Type genType = getClass().getGenericSuperclass();
                type = ((ParameterizedType) genType).getActualTypeArguments()[0];
            } else {
                JsonConvert<T> convert = new JsonConvert<>(clazz);
                return convert.convertResponse(response);
            }
        }

        JsonConvert<T> convert = new JsonConvert<>(type);
        return convert.convertResponse(response);
    }

    @Override
    public void onError(com.lzy.okgo.model.Response<T> response) {
        super.onError(response);
        if (response == null) return;

        Throwable exception = response.getException();
        if (exception != null) exception.printStackTrace();

        String err = "";
        int code = response.code();
        if (code == 404) {
            err = "404";

        }
        if (code == 500) {
            err = "500";
        }
        if (exception instanceof SocketTimeoutException) {
            err = "请求超时";
        } else if (exception instanceof SocketException) {
            err = "服务器忙";
        } else if (exception instanceof ConnectException || exception instanceof UnknownHostException) {
            err = "网络中断，请检查您的网络状态";
        } else if (exception instanceof HttpException) {
            err = "服务器忙"; //不是400就是500
        } else if (exception instanceof JsonParseException || exception instanceof JSONException) {
            err = "JSON解析异常";
        } else if (exception instanceof StorageException) {
            err = "SD卡不存在或没有读写权限";
        } else if (exception instanceof MyException) {
            LzyResponse errorBean = ((MyException) exception).getErrorBean();
            if (errorBean != null) {
                String msg = errorBean.msg;
                if (!TextUtils.isEmpty(msg)) {
                    err = msg;
                }
            }
            // String error = ErrorCodeEnum.INSTANCE.getError(result);

            /*switch (((MyException) exception).getErrorBean().code) {
                case "107": //约定的错误码
                    //约定的错误码信息
                    break;
            }*/
        }
        Response rawResponse = response.getRawResponse();
        if (rawResponse!=null){
            okhttp3.Request request = rawResponse.request();
            if (request!=null){
                String url = request.url().toString();
                KLog.d("<<<<--OkGo返回错误url=" + url + ";[错误]=" + err);
            }
        }
        onFail(err);
    }

    protected abstract void onFail(String err);

}
