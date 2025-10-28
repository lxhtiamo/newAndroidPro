package com.linx.mylibrary.http.exception;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.linx.mylibrary.http.model.LzyResponse;

/**
 * @author xh
 * @Description (用一句话描述这个类的作用)
 * @date 2018/1/23 13:52
 */
public class MyException extends IllegalStateException {

    private LzyResponse errorBean;

    public MyException(String s) {
        super(s);
        try {
            errorBean = new Gson().fromJson(s, LzyResponse.class);
        } catch (JsonParseException e) {
            errorBean = new LzyResponse();
            errorBean.msg = s;
        }
    }

    public LzyResponse getErrorBean() {
        return errorBean;
    }

}
