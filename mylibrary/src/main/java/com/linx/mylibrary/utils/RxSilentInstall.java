package com.linx.mylibrary.utils;

import com.linx.mylibrary.utils.klog.KLog;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * root权限下静默安装
 */
public class RxSilentInstall {

    private RxSilentInstall() {
    }

    private static class SingletonInstance {
        private static final RxSilentInstall INSTANCE = new RxSilentInstall();
    }

    public static RxSilentInstall getInstance() {
        return SingletonInstance.INSTANCE;
    }

    public static boolean install(String apkPath) {
        boolean result = false;
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        try {
            Process process = Runtime.getRuntime().exec("su");//申请root权限
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            String command = "pm install -r " + apkPath + "\n";//拼接 pm install 命令，执行。-r表示若存在则覆盖安装
            dataOutputStream.write(command.getBytes(Charset.forName("utf-8")));
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            process.waitFor();//安装过程是同步的，安装完成后再读取结果
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String message = "";
            String line;
            while ((line = errorStream.readLine()) != null) {
                message += line;
            }
            // 如果执行结果中包含Failure字样就认为是安装失败，否则就认为安装成功
            KLog.e("silentInstall", message);
            if (!message.contains("Failure")) {
                result = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }
}