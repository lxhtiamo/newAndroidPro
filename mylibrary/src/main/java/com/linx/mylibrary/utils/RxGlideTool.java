package com.linx.mylibrary.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.linx.mylibrary.R;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * @author xh
 * @Description (Glide图片加载工具类)
 */
public class RxGlideTool {

    private static volatile RxGlideTool instance;

    public static RxGlideTool getInstance() {
        if (instance == null) {
            synchronized (RxGlideTool.class) {
                if (instance == null) {
                    instance = new RxGlideTool();
                }
            }
        }
        return instance;
    }

    /**
     * 加载图片
     */
    public void loadImage(Context mContext, Object url, ImageView imageView) {
        loadImage(mContext, url, imageView, R.mipmap.icon_stub, R.mipmap.icon_error);
    }

    /**
     * 加载图片 设置默认缓冲图 错误图
     */
    public void loadImage(Context mContext, Object url, ImageView imageView, int placeholder, int error) {
        Glide.with(mContext)
                .load(url)
                .placeholder(placeholder)
                .error(error)
                .into(imageView);
    }

    /**
     * 加载图片 设置默认展位图 失败图
     */
    public void loadImage(Context mContext, Object url, ImageView imageView, Drawable placeholder, Drawable error) {
        Glide.with(mContext)
                .load(url)
                .placeholder(placeholder)
                .error(error)
                .into(imageView);
    }


    /**
     * 加载圆形图片 默认展位图 失败图
     */
    public void loadCircleImage(Context mContext, Object url, ImageView imageView) {
        loadCircleImage(mContext, url, imageView, R.mipmap.icon_stub, R.mipmap.icon_error);
    }

    /**
     * 加载圆形图片 设置圆形图片 默认展位图 失败图
     */
    public void loadCircleImage(Context mContext, Object url, ImageView imageView, int placeholder, int error) {
        Glide.with(mContext)
                .load(url)
                .transform(new CircleCrop())
                .placeholder(placeholder)
                .error(error)
                .into(imageView);
    }

    /**
     * 加载圆形图片2 默认圆形图片 默认展位图 失败图
     */
    public void loadCircleImage2(Context mContext, Object url, ImageView imageView) {
        Glide.with(mContext)
                .load(url)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)
                .into(imageView);
    }

    /**
     * 加载圆角图片 自定义的圆角  默认圆形图片 默认展位图 失败图
     */
    public void loadRoundedImage(Context mContext, Object resourceId, ImageView imageView, int roundedCorner) {
        loadRoundedImage(mContext, resourceId, imageView, roundedCorner, R.mipmap.icon_stub, R.mipmap.icon_error);
    }

    /**
     * 加载圆角图片 自定义的圆角 设置圆形图片 默认展位图 失败图
     */
    public void loadRoundedImage(Context mContext, Object resourceId, ImageView imageView, int roundedCorner, int placeholder, int error) {
        Glide.with(mContext)
                .load(resourceId)
                .transform(new GlideRoundTransform(mContext, roundedCorner))
                .placeholder(placeholder)
                .error(error)
                .into(imageView);
    }

    /**
     * 加载圆环图片支持设置边框的宽度和颜色
     * @param mContext
     * @param borderWidth 边框宽度
     * @param borderColor 边框颜色
     */
    public void loadCircleRingImage(Context mContext, Object file, ImageView imageView, int borderWidth, int borderColor) {
        Glide.with(mContext)
                .load(file)
                .transform(new GlideCircleTransform(mContext, borderWidth, borderColor))
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)
                .into(imageView);
    }

    /*----------------------------图片转圆环图片------------------------------*/
    /**
     * 自定义加载圆环图片 ----Glide圆形图片转换器（支持边框宽度和颜色）加载圆形图片支持设置边框的宽度和颜色
     */
    public class GlideCircleTransform extends BitmapTransformation {
        private static final String ID = "com.example.GlideCircleTransform";
        private  final byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);

        private Paint mBorderPaint;
        private float mBorderWidth; // 边框宽度（像素值）

        // 无参构造（无边框）
        public GlideCircleTransform(Context context) {
            super();
        }

        /**
         * 加载圆形图片支持设置边框的宽度和颜色
         *
         * @param context     上下文
         * @param borderWidth 边框宽度（单位：dp）
         * @param borderColor 边框颜色
         */
        public GlideCircleTransform(Context context, int borderWidth, int borderColor) {
            super();
            // 更准确的dp转像素（适配不同屏幕密度）
            mBorderWidth = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    borderWidth,
                    context.getResources().getDisplayMetrics()
            );

            mBorderPaint = new Paint();
            mBorderPaint.setDither(true); // 防抖动，增强色彩过渡平滑度
            mBorderPaint.setAntiAlias(true); // 抗锯齿
            mBorderPaint.setColor(borderColor);
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mBorderPaint.setStrokeWidth(mBorderWidth);
            // 边框笔触圆角处理，避免边框转角生硬
            mBorderPaint.setStrokeCap(Paint.Cap.ROUND);
            mBorderPaint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected Bitmap transform(@NonNull BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
            return circleCrop(pool, toTransform);
        }

        private Bitmap circleCrop(BitmapPool pool, Bitmap source) {
            if (source == null) return null;

            // 裁剪出正方形（取原图最小边为边长，确保圆形比例正确）
            int size = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;
            // 裁剪时保留边缘像素，避免裁剪导致的细节丢失
            Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);

            // 从缓存池获取目标Bitmap，减少内存分配
            Bitmap result = pool.get(size, size, Bitmap.Config.ARGB_8888);
            if (result == null) {
                result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(result);
            // 清除画布为透明（关键：避免默认黑色背景影响边缘）
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // 绘制内部圆形的画笔（核心优化）
            Paint paint = new Paint();
            paint.setDither(true); // 防抖动，让图片色彩过渡更平滑
            paint.setAntiAlias(true); // 抗锯齿，减少边缘锯齿
            // 设置图片Shader，确保图片拉伸无变形
            paint.setShader(new BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

            // 计算中心坐标（浮点数，提高精度）
            float centerX = size / 2f;
            float centerY = size / 2f;

            // 计算内部圆形半径（核心优化：避开像素边界）
            float innerRadius;
            if (mBorderPaint != null && mBorderWidth > 0) {
                // 有边框时：内部半径 = (总尺寸 - 边框宽度*2)/2 - 0.5f（避开像素边界）
                innerRadius = (size - 2 * mBorderWidth) / 2f - 0.5f;
                // 防止边框过宽导致内部半径为负数
                if (innerRadius < 0) {
                    innerRadius = size / 2f - 0.5f;
                    mBorderWidth = 0;
                    mBorderPaint = null;
                }
            } else {
                // 无边框时：半径 = 尺寸/2 - 0.5f（落在像素中心，减少锯齿）
                innerRadius = size / 2f - 0.5f;
            }

            // 绘制内部圆形（边缘更平滑）
            canvas.drawCircle(centerX, centerY, innerRadius, paint);

            // 绘制边框（若有）
            if (mBorderPaint != null && mBorderWidth > 0) {
                // 边框半径 = 内部半径 + 边框宽度/2（确保边框在内部圆形外侧，无缝衔接）
                float borderRadius = innerRadius + mBorderWidth / 2f;
                canvas.drawCircle(centerX, centerY, borderRadius, mBorderPaint);
            }

            // 回收裁剪的临时Bitmap（减少内存占用）
            if (squared != source) {
                squared.recycle();
            }

            return result;
        }

        // 完善缓存逻辑（避免不同参数的图片缓存冲突）
        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
            messageDigest.update(ID_BYTES);
            // 加入边框参数（宽度和颜色），确保缓存唯一
            String params = mBorderWidth + "," + (mBorderPaint != null ? mBorderPaint.getColor() : 0);
            messageDigest.update(params.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GlideCircleTransform that = (GlideCircleTransform) o;
            return Float.compare(that.mBorderWidth, mBorderWidth) == 0 &&
                    (mBorderPaint != null ? mBorderPaint.getColor() == that.mBorderPaint.getColor() : that.mBorderPaint == null);
        }

        @Override
        public int hashCode() {
            int result = Float.hashCode(mBorderWidth);
            result = 31 * result + (mBorderPaint != null ? mBorderPaint.getColor() : 0);
            return result;
        }
    }

    //-------------------图片转换圆角图片------------------------------

    /**
     * 图片转换圆角图片
     */
    public class GlideRoundTransform extends BitmapTransformation {
        private float radius = 4f;
        private static final int defaultRadius = 4;
        private static final String TRANSFORM_ID = "com.yourpackage.GlideRoundTransform";
        // 用于标识变换的哈希值（固定，避免频繁计算）
        private final byte[] TRANSFORM_ID_BYTES = TRANSFORM_ID.getBytes(StandardCharsets.UTF_8);

        public GlideRoundTransform(Context context) {
            this(context, defaultRadius);
        }

        public GlideRoundTransform(Context context, int dp) {
            // 使用应用上下文的资源获取密度，避免系统资源与应用资源差异
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            this.radius = metrics.density * dp; // dp 转 px，确保不同设备圆角一致
        }

        @Override
        protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform,
                                   int outWidth, int outHeight) {
            // 关键：使用 Glide 计算的目标尺寸（outWidth/outHeight）绘制，而非原图尺寸
            return roundCrop(pool, toTransform, outWidth, outHeight);
        }

        private Bitmap roundCrop(BitmapPool pool, Bitmap source, int targetWidth, int targetHeight) {
            if (source == null) return null;

            // 1. 计算原图到目标尺寸的缩放比例，避免绘制时拉伸导致的锯齿
            float scaleX = (float) targetWidth / source.getWidth();
            float scaleY = (float) targetHeight / source.getHeight();

            // 2. 从缓存池获取合适的 Bitmap，优先复用，减少内存占用
            Bitmap result = pool.get(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            if (result == null) {
                result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            }

            // 3. 配置画布和画笔，增强抗锯齿和平滑度
            Canvas canvas = new Canvas(result);
            Paint paint = getPaint(source, scaleX, scaleY);
            // 5. 绘制圆角矩形（使用目标尺寸的 RectF）
            RectF rectF = new RectF(0f, 0f, targetWidth, targetHeight);
            canvas.drawRoundRect(rectF, radius, radius, paint);
            return result;
        }

        private Paint getPaint(Bitmap source, float scaleX, float scaleY) {
            Paint paint = new Paint();
            paint.setAntiAlias(true); // 基础抗锯齿
            paint.setFilterBitmap(true); // 缩放时启用滤波，减少锯齿
            paint.setDither(true); // 抖动处理，提升色彩过渡平滑度
            // 4. 为 Shader 设置缩放矩阵，确保图片适配目标尺寸，避免拉伸导致的边缘粗糙
            BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            Matrix matrix = new Matrix();
            matrix.setScale(scaleX, scaleY); // 按目标尺寸缩放原图
            shader.setLocalMatrix(matrix);
            paint.setShader(shader);
            return paint;
        }

        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
            // 加入目标尺寸和圆角半径，确保不同参数的变换缓存唯一
            messageDigest.update(TRANSFORM_ID_BYTES);
            messageDigest.update(String.valueOf(radius).getBytes(StandardCharsets.UTF_8));
        }
    }

    /*----------------------------------------------------清除glide缓存---------------------------------------*/

    /**
     * 清除图片所有缓存
     */
    public void clearImageAllCache(Context context) {
        clearImageDiskCache(context);
        clearImageMemoryCache(context);
       // String ImageExternalCatchDir = context.getExternalCacheDir() + ExternalCacheDiskCacheFactory.DEFAULT_DISK_CACHE_DIR;
        String ImageExternalCatchDir = context.getExternalCacheDir() + DiskCache.Factory.DEFAULT_DISK_CACHE_DIR;
        deleteFolderFile(ImageExternalCatchDir, true);
    }

    /**
     * 清除图片内存缓存
     */
    public boolean clearImageMemoryCache(Context context) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) { //只能在主线程执行
                Glide.get(context).clearMemory();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return false;
    }

    /**
     * 清除图片磁盘缓存
     */
    public boolean clearImageDiskCache(final Context context) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.get(context).clearDiskCache();
                    }
                }).start();
            } else {
                Glide.get(context).clearDiskCache();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取Glide造成的缓存大小
     *
     * @return CacheSize
     */
    public String getCacheSize(Context context) {
        try {
            return getFormatSize(getFolderSize(new File(context.getCacheDir() + "/" + InternalCacheDiskCacheFactory.DEFAULT_DISK_CACHE_DIR)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 格式化单位
     *
     * @param size size
     * @return size
     */
    private static String getFormatSize(double size) {

        double kiloByte = size / 1024;
        if (kiloByte < 1) {
            return size + "Byte";
        }

        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB";
        }

        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);

        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";
    }

    /**
     * 获取指定文件夹内所有文件大小的和
     *
     * @param file file
     * @return size
     * @throws Exception
     */
    private long getFolderSize(File file) throws Exception {
        long size = 0;
        try {
            File[] fileList = file.listFiles();
            for (File aFileList : fileList) {
                if (aFileList.isDirectory()) {
                    size = size + getFolderSize(aFileList);
                } else {
                    size = size + aFileList.length();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * 删除指定目录下的文件，这里用于缓存的删除
     *
     * @param filePath       filePath
     * @param deleteThisPath deleteThisPath
     */
    private boolean deleteFolderFile(String filePath, boolean deleteThisPath) {
        if (!TextUtils.isEmpty(filePath)) {
            try {
                File file = new File(filePath);
                if (file.isDirectory()) {
                    File files[] = file.listFiles();
                    for (File file1 : files) {
                        deleteFolderFile(file1.getAbsolutePath(), true);
                    }
                }
                if (deleteThisPath) {
                    if (!file.isDirectory()) {
                        file.delete();
                    } else {
                        if (file.listFiles().length == 0) {
                            file.delete();
                        }
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}
