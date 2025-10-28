package com.linx.mylibrary.http.model;


/**
 * Created by Administrator on 2017/2/27.
 */

public class LoginModel {

    /**
     * app_id : 应用系统凭证
     * app_secret : 凭证密钥
     * login_type : 登录方式，0：手机号码与密码登录、1：刷脸登录、2：验证码登录
     * username : 用户名(手机号码)
     * password : 手机号码与密码登录时，为密码；刷脸登录时为认证信息；验证码登录时为验证码
     * type : 类型，0：游客、1:个人用户、2：法人用户
     */

    private String app_id;
    private String app_secret;
    private String login_type;
    private String username;
    private String password;
    private String type;

    public String getApp_id() {
        return app_id;
    }

    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }

    public String getApp_secret() {
        return app_secret;
    }

    public void setApp_secret(String app_secret) {
        this.app_secret = app_secret;
    }

    public String getLogin_type() {
        return login_type;
    }

    public void setLogin_type(String login_type) {
        this.login_type = login_type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
