package com.linx.mylibrary.http.model;


import com.linx.mylibrary.http.config.HttpConfig;
import com.linx.mylibrary.utils.base64.AESTool;
import com.linx.mylibrary.utils.json.JsonUtils;

/**
 * Created by Administrator on 2017/2/27.
 */

public class BasePostEntity {
    private String access_token;
    private String user_token;
    private String signature;
    private String timestamp;
    private String params;

    public BasePostEntity(String cls) {
        boolean iscrypt = true;
        String param = "";
        if (iscrypt) {
            try {

                 param = AESTool.encrypt(cls, HttpConfig.encryptNumbers);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            param=cls;
        }
        this.access_token =  "D30F355A9E4A88CBD850340B87099C0120170426062527";
        this.user_token ="";
        this.signature ="";
        this.timestamp ="";
        this.params = param;
    }

    public String getAESString(BasePostEntity basePostEntity) {
        //String testparams = GsonUtil.GsonString(basePostEntity);
        String testparams = JsonUtils.toJson(basePostEntity);
        return testparams;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getUser_token() {
        return user_token;
    }

    public void setUser_token(String user_token) {
        this.user_token = user_token;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

}
