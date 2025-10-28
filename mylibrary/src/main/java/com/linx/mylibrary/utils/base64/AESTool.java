package com.linx.mylibrary.utils.base64;


import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Administrator on 2017/2/25.
 * 1.3.3.	加密、解密类
 */

public class AESTool {
    private static String ivParameter = "0231345874954435";// 偏移量,可自行修改
    private static AESTool instance = null;

    private AESTool() {
    }

    public static AESTool getInstance() {
        if (instance == null)
            instance = new AESTool();
        return instance;
    }

    public static String Encrypt(String encData, String secretKey, String vector) throws Exception {

        if (secretKey == null) {
            return null;
        }
        if (secretKey.length() != 16) {
            return null;
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] raw = secretKey.getBytes();
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        IvParameterSpec iv = new IvParameterSpec(vector.getBytes());// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        byte[] encrypted = cipher.doFinal(encData.getBytes("utf-8"));
        return new BASE64Encoder().encode(encrypted);// 此处使用BASE64做转码。
    }

    /**
     * 加密
     *
     * @param sSrc           加密内容
     * @param encodingAESKey 加解密密钥
     * @return
     * @throws Exception
     */
    public static String encrypt(String sSrc, String encodingAESKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] raw = encodingAESKey.getBytes();
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        byte[] encrypted = cipher.doFinal(sSrc.getBytes("utf-8"));
        return new BASE64Encoder().encode(encrypted);// 此处使用BASE64做转码。
    }

    /**
     * 解密
     *
     * @param sSrc           解密内容
     * @param encodingAESKey 加解密密钥
     * @return
     * @throws Exception
     */
    public static String decrypt(String sSrc, String encodingAESKey) throws Exception {
        try {
            byte[] raw = encodingAESKey.getBytes("ASCII");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] encrypted1 = new BASE64Decoder().decodeBuffer(sSrc);// 先用base64解密
            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original, "utf-8");
            return originalString;
        } catch (Exception ex) {
            return null;
        }
    }

    public String decrypt(String sSrc, String key, String ivs) throws Exception {
        try {
            byte[] raw = key.getBytes("ASCII");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(ivs.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] encrypted1 = new BASE64Decoder().decodeBuffer(sSrc);// 先用base64解密
            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original, "utf-8");
            return originalString;
        } catch (Exception ex) {
            return null;
        }
    }

    public static String encodeBytes(byte[] bytes) {
        StringBuffer strBuf = new StringBuffer();

        for (int i = 0; i < bytes.length; i++) {
            strBuf.append((char) (((bytes[i] >> 4) & 0xF) + ((int) 'a')));
            strBuf.append((char) (((bytes[i]) & 0xF) + ((int) 'a')));
        }

        return strBuf.toString();
    }

    public static void main(String[] args) throws Exception {
        // 需要加密的字串
        String cSrc = "{\"access_token\":\"应用系统接入认证有效令牌\",\"user_token\":\"登录用户有效令牌\",\"unid\":\"主键\",\"user_name\":\"姓名\",\"password\":\"密码\"\"user_login_name\":\"登录名\",\"user_type\":\"用户类型，01个人，02法人\",\"user\":{\"name\":\"姓名\",\"sex\":\"性别,1男，0女\",\"mobilePhone\":\"手机号码\",\"certificateNumber\":\"证件号码\",\"phone\":\"电话号码\",\"email\":\"邮箱地址\",\"address\":\"详细地址\"},\"umcEnterpriseUser\":{\"jbr_name\":\"姓名\",\"jbr_sex\":\"性别,1男，0女\",\"jbr_mobilePhone\":\"手机号码\",\"jbr_certificateType \":\"证件类型\",\"jbr_certificateNumber\":\"证件号码\",\"jbr_phone\":\"电话号码\",\"jbr_email\":\"邮箱地址\",\"jbr_address\":\"详细地址\",\"jbr_postcode\":\"邮编\",\"orgName\":\"机构名称\",\"org_type\":\"机构类型\",\"orgCodee\":\"组织机构代码\",\"org_address\":\"组织机构代码\",\"org_law_name\":\"法人代表姓名\",\"org_law_sex\":\"法人代表性别\",\"org_law_idcard\":\"法人代表身份证\",\"org_code_photo\":\"法人代表图片\",\"org_law_mobile\":\"法人代表手机号码\"}";
        // 加密用的Key 可以用26个字母和数字组成 此处使用AES-128-CBC加密模式，key需要为16位。
        String encodingAESKey = "20160226!#xm@837";// key，可自行修改

        // 加密
        long lStart = System.currentTimeMillis();
        String enString = AESTool.getInstance().encrypt(cSrc, encodingAESKey);
        System.out.println("加密后的字串是：" + enString);

        long lUseTime = System.currentTimeMillis() - lStart;
        System.out.println("加密耗时：" + lUseTime + "毫秒");
        // 解密
        lStart = System.currentTimeMillis();
        String DeString = AESTool.getInstance().decrypt(enString, encodingAESKey);
        lUseTime = System.currentTimeMillis() - lStart;
        System.out.println("解密耗时：" + lUseTime + "毫秒");
    }
}
