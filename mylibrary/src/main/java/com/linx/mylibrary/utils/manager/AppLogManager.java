package com.linx.mylibrary.utils.manager;
import com.linx.mylibrary.utils.klog.KLog;


/**
 * 主要功能： 系统日志输出工具类
 */
public class AppLogManager {
    //是否输出
    private static boolean isDebug = true;

    /*
     * 设置debug模式(true:打印日志  false：不打印)
     */
    public static void isEnableDebug(boolean isDebug){
        AppLogManager.isDebug = isDebug;
    }
    
    /**
     * 
     * @param tag
     * @param msg
     */
    public static void i(String tag,String msg){
        if(isDebug){
            KLog.i(tag, msg != null ? msg : "");
        }
    }
    public static void i(Object object,String msg){
        if(isDebug){
            KLog.i(object.getClass().getSimpleName(), msg != null ? msg : "");
        }
    }

    public static void i(String msg){
        if(isDebug){
            KLog.i(" [INFO] --- ", msg != null ? msg : "");
        }
    }

    /**
     * 
     * @param tag
     * @param msg
     */
    public static void d(String tag,String msg){
        if(isDebug){
            KLog.d(tag, msg != null ? msg : "");
        }
    }

    public static void d(Object object,String msg){
        if(isDebug){
            KLog.d(object.getClass().getSimpleName(), msg != null ? msg : "");
        }
    }

    public static void d(String msg){
        if(isDebug){
            KLog.d(" [DEBUG] --- ", msg != null ? msg : "");
        }
    }

    /**
     * 
     * @param tag
     * @param msg
     */
    public static void w(String tag,String msg){
        if(isDebug){
            KLog.w(tag, msg != null ? msg : "");
        }
    }

    public static void w(Object object,String msg){
        if(isDebug){
            KLog.w(object.getClass().getSimpleName(), msg != null ? msg : "");
        }
    }

    public static void w(String msg){
        if(isDebug){
            KLog.w(" [WARN] --- ", msg != null ? msg : "");
        }
    }

    /**
     * 
     * @param tag
     * @param msg
     */
    public static void e(String tag,String msg){
        if(isDebug){
            KLog.e(tag, msg !=null ? msg : "");
        }
    }

    public static void e(Object object,String msg){
        if(isDebug){
            KLog.e(object.getClass().getSimpleName(), msg !=null ? msg : "");
        }
    }

    public static void e(String msg){
        if(isDebug){
            KLog.e(" [ERROR] --- ", msg !=null ? msg : "");
        }
    }

    /**
     * 
     * @param tag
     * @param msg
     */
    public static void v(String tag, String msg){
        if(isDebug){
            KLog.v(tag, msg != null ? msg : "");
        }
    }

    public static void v(Object object, String msg){
        if(isDebug){
            KLog.v(object.getClass().getSimpleName(), msg != null ? msg : "");
        }
    }

    public static void v( String msg){
        if(isDebug){
            KLog.v(" [VERBOSE] --- ", msg != null ? msg : "");
        }
    }
}