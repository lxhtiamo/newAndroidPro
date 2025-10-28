package com.linx.mylibrary.http.exception;

import android.text.TextUtils;

/**
 * @author xh
 * @Description (解析错误码)
 * @date 2018/4/27 12:01
 */
public enum ErrorCodeEnum {

    INSTANCE;

    public String getError(String code) {
        String err = "";
        if (!TextUtils.isEmpty(code)){
            switch (code) {
                case "107":
                    err = "";
                    break;
            }
        }else {
            err="系统错误";
        }
        return err;
    }
}
