package com.linx.mylibrary.utils;

import android.annotation.SuppressLint;
import android.os.Build;

import com.linx.mylibrary.utils.manager.AppLogMessageMgr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日期时间工具类
 * 线程安全，高性能日期时间处理工具
 * @version: 2.0.0
 */
@SuppressLint("SimpleDateFormat")
public class RxSysDateTool {

    // 线程安全的日期格式化器缓存
    private static final ConcurrentHashMap<String, ThreadLocal<SimpleDateFormat>> FORMATTER_CACHE =
            new ConcurrentHashMap<>();

    // 常用格式常量
    public static final String PATTERN_ALL = "yyyy-MM-dd HH:mm:ss:SSS";
    public static final String PATTERN_ALL_CN = "yyyy年MM月dd日 HH时mm分ss秒SSS毫秒";
    public static final String PATTERN_FULL = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_FULL_CN = "yyyy年MM月dd日 HH时mm分ss秒";
    public static final String PATTERN_MINUTE = "yyyy-MM-dd HH:mm";
    public static final String PATTERN_MINUTE_CN = "yyyy年MM月dd日 HH时mm分";
    public static final String PATTERN_DATE = "yyyy-MM-dd";
    public static final String PATTERN_DATE_CN = "yyyy年MM月dd日";
    public static final String PATTERN_TIME = "HH:mm:ss";
    public static final String PATTERN_COMPACT = "yyyyMMddHHmmss";
    public static final String PATTERN_COMPACT_FULL = "yyyyMMddHHmmssSSS";
    public static final String PATTERN_YEAR = "yyyy";
    public static final String PATTERN_MONTH = "MM";
    public static final String PATTERN_DAY = "dd";
    public static final String PATTERN_HOUR_MINUTE = "HH:mm";

    // 默认时区
    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();

    /**
     * 获取线程安全的SimpleDateFormat实例
     */
    private static SimpleDateFormat getFormatter(String pattern) {
        ThreadLocal<SimpleDateFormat> threadLocal = FORMATTER_CACHE.get(pattern);
        if (threadLocal == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                threadLocal = ThreadLocal.withInitial(() -> {
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                    sdf.setTimeZone(DEFAULT_TIME_ZONE);
                    return sdf;
                });
            }
            FORMATTER_CACHE.putIfAbsent(pattern, threadLocal);
        }
        return threadLocal.get();
    }

    /**
     * 获取当前系统时间（指定格式）
     */
    public static String getCurrentTime(String pattern) {
        try {
            return getFormatter(pattern).format(new Date());
        } catch (Exception e) {
            AppLogMessageMgr.e("RxSysDateTool", "getCurrentTime error: " + e.getMessage());
            return "";
        }
    }

    /**
     * 获取当前系统时间（完整格式）
     */
    public static String getSysDateByAll() {
        return getCurrentTime(PATTERN_ALL);
    }

    /**
     * 获取当前系统时间（完整格式，中文）
     */
    public static String getSysDateByAllFormat() {
        return getCurrentTime(PATTERN_ALL_CN);
    }

    /**
     * 获取当前系统时间（年月日时分秒）
     */
    public static String getSysDateByFull() {
        return getCurrentTime(PATTERN_FULL);
    }

    /**
     * 获取当前系统时间（年月日时分秒，中文）
     */
    public static String getSysDateByFullFormat() {
        return getCurrentTime(PATTERN_FULL_CN);
    }

    /**
     * 获取当前系统日期（年月日）
     */
    public static String getSysDate() {
        return getCurrentTime(PATTERN_DATE);
    }

    /**
     * 获取当前系统日期（年月日，中文）
     */
    public static String getSysDateFormat() {
        return getCurrentTime(PATTERN_DATE_CN);
    }

    /**
     * 格式化日期时间
     */
    public static String format(Date date, String pattern) {
        if (date == null) return "";
        try {
            return getFormatter(pattern).format(date);
        } catch (Exception e) {
            AppLogMessageMgr.e("RxSysDateTool", "format error: " + e.getMessage());
            return "";
        }
    }

    /**
     * 格式化日期时间字符串
     */
    public static String format(String dateStr, String srcPattern, String destPattern) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            Date date = parse(dateStr, srcPattern);
            return format(date, destPattern);
        } catch (Exception e) {
            AppLogMessageMgr.e("RxSysDateTool", "format string error: " + e.getMessage());
            return "";
        }
    }

    /**
     * 解析日期时间字符串
     */
    public static Date parse(String dateStr, String pattern) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return getFormatter(pattern).parse(dateStr);
        } catch (ParseException e) {
            AppLogMessageMgr.e("RxSysDateTool", "parse error: " + e.getMessage() + ", dateStr: " + dateStr);
            return null;
        }
    }

    /**
     * 获取时间戳
     */
    public static long getTimestamp(String dateStr, String pattern) {
        Date date = parse(dateStr, pattern);
        return date != null ? date.getTime() : 0;
    }

    /**
     * 获取当前时间戳
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间戳（秒）
     */
    public static long getCurrentTimestampSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 生成带随机数的时间戳字符串
     */
    public static String getTimestampWithRandom(int randomDigits) {
        StringBuilder sb = new StringBuilder(getCurrentTime(PATTERN_COMPACT));
        if (randomDigits > 0) {
            Random random = new Random();
            int max = (int) Math.pow(10, randomDigits);
            String format = "%0" + randomDigits + "d";
            sb.append(String.format(Locale.getDefault(), format, random.nextInt(max)));
        }
        return sb.toString();
    }

    /**
     * 获取日期的年份
     */
    public static String getYear(Date date) {
        return format(date, PATTERN_YEAR);
    }

    /**
     * 获取日期的月份
     */
    public static String getMonth(Date date) {
        return format(date, PATTERN_MONTH);
    }

    /**
     * 获取日期的天数
     */
    public static String getDay(Date date) {
        return format(date, PATTERN_DAY);
    }

    /**
     * 判断是否为今天
     */
    public static boolean isToday(String dateStr, String pattern) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        try {
            Date date = parse(dateStr, pattern);
            if (date == null) return false;

            Calendar target = Calendar.getInstance();
            target.setTime(date);

            Calendar today = Calendar.getInstance();

            return target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
        } catch (Exception e) {
            AppLogMessageMgr.e("RxSysDateTool", "isToday error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 判断是否为今天（默认格式）
     */
    public static boolean isToday(String dateStr) {
        return isToday(dateStr, PATTERN_DATE);
    }

    /**
     * 比较两个日期时间
     * @return -1: date1 < date2, 0: date1 = date2, 1: date1 > date2
     */
    public static int compare(String dateStr1, String dateStr2, String pattern) {
        Date date1 = parse(dateStr1, pattern);
        Date date2 = parse(dateStr2, pattern);

        if (date1 == null && date2 == null) return 0;
        if (date1 == null) return -1;
        if (date2 == null) return 1;

        return date1.compareTo(date2);
    }

    /**
     * 判断date1是否在date2之前
     */
    public static boolean isBefore(String dateStr1, String dateStr2, String pattern) {
        return compare(dateStr1, dateStr2, pattern) < 0;
    }

    /**
     * 判断date1是否在date2之后
     */
    public static boolean isAfter(String dateStr1, String dateStr2, String pattern) {
        return compare(dateStr1, dateStr2, pattern) > 0;
    }

    /**
     * 计算两个日期相差的天数
     */
    public static int daysBetween(String dateStr1, String dateStr2, String pattern) {
        Date date1 = parse(dateStr1, pattern);
        Date date2 = parse(dateStr2, pattern);

        if (date1 == null || date2 == null) return 0;

        long diff = Math.abs(date2.getTime() - date1.getTime());
        return (int) (diff / (1000 * 60 * 60 * 24));
    }

    /**
     * 计算两个日期相差的分钟数
     */
    public static long minutesBetween(String dateStr1, String dateStr2, String pattern) {
        Date date1 = parse(dateStr1, pattern);
        Date date2 = parse(dateStr2, pattern);

        if (date1 == null || date2 == null) return 0;

        long diff = Math.abs(date2.getTime() - date1.getTime());
        return diff / (1000 * 60);
    }

    /**
     * 日期加减
     */
    public static String addDays(String dateStr, String pattern, int days) {
        Date date = parse(dateStr, pattern);
        if (date == null) return "";

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, days);

        return format(calendar.getTime(), pattern);
    }

    /**
     * 获取星期几
     * @return 1-7 分别代表周一至周日
     */
    public static int getDayOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        // Calendar中周日是1，周一是2，转换为周一是1
        return day == Calendar.SUNDAY ? 7 : day - 1;
    }

    /**
     * 获取星期几（中文）
     */
    public static String getDayOfWeekCN(Date date) {
        String[] weeks = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        return weeks[day - 1];
    }

    /**
     * 获取当月第一天
     */
    public static String getFirstDayOfMonth(String pattern) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return format(calendar.getTime(), pattern);
    }

    /**
     * 获取当月最后一天
     */
    public static String getLastDayOfMonth(String pattern) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return format(calendar.getTime(), pattern);
    }

    /**
     * 验证日期字符串是否有效
     */
    public static boolean isValidDate(String dateStr, String pattern) {
        if (dateStr == null || dateStr.isEmpty()) return false;

        SimpleDateFormat sdf = getFormatter(pattern);
        sdf.setLenient(false);

        try {
            sdf.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * 获取当前时间的友好显示（刚刚、几分钟前、几小时前等）
     */
    public static String getFriendlyTime(String dateStr, String pattern) {
        Date date = parse(dateStr, pattern);
        if (date == null) return "";

        long diff = System.currentTimeMillis() - date.getTime();

        if (diff < 60000) { // 1分钟内
            return "刚刚";
        } else if (diff < 3600000) { // 1小时内
            long minutes = diff / 60000;
            return minutes + "分钟前";
        } else if (diff < 86400000) { // 24小时内
            long hours = diff / 3600000;
            return hours + "小时前";
        } else if (diff < 604800000) { // 7天内
            long days = diff / 86400000;
            return days + "天前";
        } else {
            return format(date, "MM-dd HH:mm");
        }
    }

    /**
     * 将时间戳转换为日期字符串
     */
    public static String timestampToString(long timestamp, String pattern) {
        if (timestamp <= 0) return "";
        return format(new Date(timestamp), pattern);
    }

    /**
     * 将"EEE MMM dd HH:mm:ss zzz yyyy"格式转换为指定格式
     */
    public static String convertDefaultFormat(String defaultFormatStr, String targetPattern) {
        if (defaultFormatStr == null || defaultFormatStr.isEmpty()) return "";

        try {
            SimpleDateFormat sourceFormat = new SimpleDateFormat(
                    "EEE MMM dd HH:mm:ss zzz yyyy", Locale.US
            );
            Date date = sourceFormat.parse(defaultFormatStr);
            return format(date, targetPattern);
        } catch (Exception e) {
            AppLogMessageMgr.e("RxSysDateTool", "convertDefaultFormat error: " + e.getMessage());
            return "";
        }
    }

    /**
     * 清理缓存（在内存紧张时调用）
     */
    public static void clearCache() {
        FORMATTER_CACHE.clear();
    }

    // ========== 兼容原有方法 ==========

    @Deprecated
    public static String getNowTime() {
        return getCurrentTime(PATTERN_COMPACT);
    }

    @Deprecated
    public static String getStringTime() {
        return getCurrentTime(PATTERN_COMPACT);
    }

    @Deprecated
    public static String getStringTimeFull() {
        return getCurrentTime(PATTERN_COMPACT_FULL);
    }

    @Deprecated
    public static String getStringTimeRandom4() {
        return getTimestampWithRandom(4);
    }

    @Deprecated
    public static String getStringTimeRandom6() {
        return getTimestampWithRandom(6);
    }

    @Deprecated
    public static String getStringTimeRandom8() {
        return getTimestampWithRandom(8);
    }
}