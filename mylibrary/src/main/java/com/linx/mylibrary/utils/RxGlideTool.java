package com.linx.mylibrary.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.ExternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.linx.mylibrary.R;

import java.io.File;
import java.math.BigDecimal;
import java.security.MessageDigest;

/**
 * @author xh
 * @Description (Glide图片加载工具类)
 * @date 2018/1/31 13:19
 */
public class RxGlideTool {
//--------------------------------------------------说明-----------------------------------------------
    //  with(Context context). 使用Application上下文，Glide请求将不受Activity/Fragment生命周期控制。
    //  with(Activity activity).使用Activity作为上下文，Glide的请求会受到Activity生命周期控制。
    //  with(FragmentActivity activity).Glide的请求会受到FragmentActivity生命周期控制。
    //  with(android.app.Fragment fragment).Glide的请求会受到Fragment 生命周期控制。
    //  with(android.support.v4.app.Fragment fragment).Glide的请求会受到Fragment生命周期控制。
    //-----------------------------
    //  Glide基本可以load任何可以拿到的媒体资源，如：
    //  load SD卡资源：load("file://"+ Environment.getExternalStorageDirectory().getPath()+"/test.jpg")
    //  load assets资源：load("file:///android_asset/f003.gif")
    //  load raw资源：load("android.resource://com.frank.glide/raw/raw_1")或load("android.resource://com.frank.glide/raw/"+R.raw.raw_1)
    //  load drawable资源：load("android.resource://com.frank.glide/drawable/news")或load("android.resource://com.frank.glide/drawable/"+R.drawable.news)
    //  load ContentProvider资源：load("content://media/external/images/media/139469")
    //  load http资源：load("http://img.my.csdn.net/uploads/201508/05/1438760757_3588.jpg")
    //  load https资源：load("https://img.alicdn.com/tps/TB1uyhoMpXXXXcLXVXXXXXXXXXX-476-538.jpg_240x5000q50.jpg_.webp")
    //  当然，load不限于String类型，还可以：
    //  load(Uri uri)，load(File file)，load(Integer resourceId)，load(URL url)，load(byte[] model, final String id)，load(byte[] model)，load(T model)。
    //  而且可以使用自己的ModelLoader进行资源加载：
    //  using(ModelLoader<A, T> modelLoader, Class<T> dataClass)，using(final StreamModelLoader<T> modelLoader)，using(StreamByteArrayLoader modelLoader)，using(final FileDescriptorModelLoader<T> modelLoader)。
    //  返回RequestBuilder实例
    //--------------------------------------
    //  * thumbnail(float sizeMultiplier). 请求给定系数的缩略图。如果缩略图比全尺寸图先加载完，
    //        就显示缩略图，否则就不显示。系数sizeMultiplier必须在(0,1)之间，可以递归调用该方法。

    //  * sizeMultiplier(float sizeMultiplier). 在加载资源之前给Target大小设置系数。

    //  * skipMemoryCache(boolean skip). 设置是否跳过内存缓存，但不保证一定不被缓存
    //     （比如请求已经在加载资源且没设置跳过内存缓存，这个资源就会被缓存在内存中）。
    //  *  diskCacheStrategy(DiskCacheStrategy strategy).设置缓存策略。
    //     DiskCacheStrategy.SOURCE：缓存原始数据，
    //     DiskCacheStrategy.RESULT：缓存变换修改后的资源数据，
    //     DiskCacheStrategy.NONE：什么都不缓存，
    //     DiskCacheStrategy.ALL：缓存所有图片  默认
    //          默认采用DiskCacheStrategy.RESULT策略，对于download only操作要使用DiskCacheStrategy.SOURCE。

    //  * priority(Priority priority). 指定加载的优先级，优先级越高越优先加载，但不保证所有图片都按序加载。
    //       枚举Priority.IMMEDIATE，Priority.HIGH，Priority.NORMAL，Priority.LOW。默认为Priority.NORMAL。
    //  * crossFade(5000) //设置淡入淡出效果，默认300ms，可以传参
    //  * dontAnimate(). 移除所有的动画。
    //  * animate(int animationId). 在异步加载资源完成时会执行该动画。
    //  * animate(ViewPropertyAnimation.Animator animator). 在异步加载资源完成时会执行该动画。
    //  * placeholder(int resourceId). 设置资源加载过程中的占位Drawable。
    //  * placeholder(Drawable drawable). 设置资源加载过程中的占位Drawable。

    //  * fallback(int resourceId). 设置model为空时要显示的Drawable。如果没设置fallback，
    //    model为空时将显示error的Drawable，如果error的Drawable也没设置，就显示placeholder的Drawable。
    //  * fallback(Drawable drawable).设置model为空时显示的Drawable。
    //  * error(int resourceId).设置load失败时显示的Drawable。
    //  * error(Drawable drawable).设置load失败时显示的Drawable。

    //  * Glide支持两种图片缩放形式，CenterCrop 和 FitCenter
    //    CenterCrop：等比例缩放图片， 直到图片的狂高都大于等于ImageView的宽度，然后截取中间的显示。
    //    FitCenter：等比例缩放图片，宽或者是高等于ImageView的宽或者是高。

    //  * 当列表在滑动的时候，调用pauseRequests()取消请求，滑动停止时，调用resumeRequests()恢复请求

    //  * listener(RequestListener<? super ModelType, TranscodeType> requestListener).
    //        监听资源加载的请求状态，可以使用两个回调：
    //     onResourceReady(R resource, T model, Target<R> target, boolean isFromMemoryCache, boolean isFirstResource)
    //       和onException(Exception e, T model, Target&lt;R&gt; target, boolean isFirstResource)，
    //       但不要每次请求都使用新的监听器，要避免不必要的内存申请，可以使用单例进行统一的异常监听和处理。
    //  * clear() 清除掉所有的图片加载请求
    //  * override(int width, int height). 重新设置Target的宽高值（单位为pixel）。
    //  * into(Y target).设置资源将被加载到的Target。
    //  * into(ImageView view). 设置资源将被加载到的ImageView。取消该ImageView之前所有的加载并释放资源。
    //  * into(int width, int height). 后台线程加载时要加载资源的宽高值（单位为pixel）。
    //  * preload(int width, int height). 预加载resource到缓存中（单位为pixel）。
    //  * asBitmap(). 无论资源是不是gif动画，都作为Bitmap对待。如果是gif动画会停在第一帧。
    //  * asGif().把资源作为GifDrawable对待。如果资源不是gif动画将会失败，会回调.error()。
    //------------------------------------------------------------------------------------------------------

    private static RxGlideTool instance;

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
     * 加载网络图片
     *
     * @param mContext
     * @param url
     * @param imageView
     */
    public void loadImage(Context mContext, String url, ImageView imageView) {
        Glide.with(mContext)
                .load(url)
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)
                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载本地网络图片
     *
     * @param mContext
     * @param uri
     * @param imageView
     */
    public void loadImage(Context mContext, Uri uri, ImageView imageView) {
        Glide.with(mContext)
                .load(uri)
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)

                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载图片
     *
     * @param mContext
     * @param resourceId 本地资源
     * @param imageView
     */
    public void loadImage(Context mContext, Integer resourceId, ImageView imageView) {
        Glide.with(mContext)
                .load(resourceId)
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)

                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载本地图片
     *
     * @param mContext
     * @param file
     * @param imageView
     */
    public void loadImage(Context mContext, File file, ImageView imageView) {
        Glide.with(mContext)
                .load(file)
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)

                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载图片
     *
     * @param mContext
     * @param o
     * @param imageView
     */
    public void loadImage(Context mContext, Object o, ImageView imageView) {
        if (o instanceof String) {
            String url = (String) o;
            Glide.with(mContext)
                    .load(url)
                    .placeholder(R.mipmap.icon_stub)
                    .error(R.mipmap.icon_error)

                    .centerCrop()
                    .into(imageView);
        } else if (o instanceof Uri) {
            Uri uri = (Uri) o;
            Glide.with(mContext)
                    .load(uri)
                    .placeholder(R.mipmap.icon_stub)
                    .error(R.mipmap.icon_error)

                    .centerCrop()
                    .into(imageView);
        } else if (o instanceof File) {
            File file = (File) o;
            Glide.with(mContext)
                    .load(file)
                    .placeholder(R.mipmap.icon_stub)
                    .error(R.mipmap.icon_error)

                    .centerCrop()
                    .into(imageView);
        } else if (o instanceof Integer) {
            Integer resourcesID = (Integer) o;
            Glide.with(mContext)
                    .load(resourcesID)
                    .placeholder(R.mipmap.icon_stub)
                    .error(R.mipmap.icon_error)

                    .centerCrop()
                    .into(imageView);
        }
    }



    /**
     * 加载圆形图片
     *
     * @param mContext
     * @param url
     * @param imageView
     */
    public void loadCircleImage(Context mContext, String url, ImageView imageView) {
        Glide.with(mContext)
                .load(url)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .transform(new GlideCircleTransform(mContext))
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)
                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载圆形图片
     *
     * @param mContext
     * @param file
     * @param imageView
     */
    public void loadCircleImage(Context mContext, File file, ImageView imageView) {
        Glide.with(mContext)
                .load(file)
                .transform(new GlideCircleTransform(mContext))
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)

                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载圆形图片
     *
     * @param mContext
     * @param uri       本地资源
     * @param imageView
     */
    public void loadCircleImage(Context mContext, Uri uri, ImageView imageView) {
        Glide.with(mContext)
                .load(uri)
                .transform(new GlideCircleTransform(mContext))
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)

                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载圆形图片
     *
     * @param mContext
     * @param resourceId 本地资源
     * @param imageView
     */
    public void loadCircleImage(Context mContext, Integer resourceId, ImageView imageView) {
        Glide.with(mContext)
                .load(resourceId)
                .transform(new GlideCircleTransform(mContext))
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)

                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载圆形图片支持设置边框的宽度和颜色
     *
     * @param mContext
     * @param file
     * @param borderWidth 边框宽度
     * @param borderColor 边框颜色
     */
    public void loadCircleImage(Context mContext, File file, ImageView imageView, int borderWidth, int borderColor) {
        Glide.with(mContext)
                .load(file)
                .transform(new GlideCircleTransform(mContext, borderWidth, borderColor))
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)

                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载圆角图片
     *
     * @param mContext
     * @param url
     * @param imageView
     */
    public void loadRoundImage(Context mContext, String url, ImageView imageView) {
        Glide.with(mContext)
                .load(url)
                .transform(new GlideRoundTransform(mContext))
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)
                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载圆角图片
     *
     * @param mContext
     * @param file
     * @param imageView
     */
    public void loadRoundImage(Context mContext, File file, ImageView imageView) {
        Glide.with(mContext)
                .load(file)
                .transform(new GlideRoundTransform(mContext))
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)

                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载圆角图片
     *
     * @param mContext
     * @param resourceId 本地资源
     * @param imageView
     */
    public void loadRoundImage(Context mContext, Integer resourceId, ImageView imageView) {
        Glide.with(mContext)
                .load(resourceId)
                .transform(new GlideRoundTransform(mContext))
                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)
                .centerCrop()
                .into(imageView);
    }

    /**
     * 加载圆角图片
     *
     * @param mContext
     * @param uri       本地资源
     * @param imageView
     */
    public void loadRoundImage(Context mContext, Uri uri, ImageView imageView) {
        Glide.with(mContext)
                .load(uri)
                .transform(new GlideRoundTransform(mContext))

                .placeholder(R.mipmap.icon_stub)
                .error(R.mipmap.icon_error)
                .centerCrop()
                .into(imageView);
    }


    //--------------------------------------------------

    /**
     * 图片转圆形 ---->ps加载圆形图片支持设置边框的宽度和颜色
     */
    public class GlideCircleTransform extends BitmapTransformation {
        private Paint mBorderPaint;
        private float mBorderWidth;

        public GlideCircleTransform(Context context) {
            super();
        }

        /**
         * 加载圆形图片支持设置边框的宽度和颜色
         *
         * @param context
         * @param borderWidth 边框宽度
         * @param borderColor 边框颜色
         */
        public GlideCircleTransform(Context context, int borderWidth, int borderColor) {
            super();
            mBorderWidth = Resources.getSystem().getDisplayMetrics().density * borderWidth;

            mBorderPaint = new Paint();
            mBorderPaint.setDither(true);
            mBorderPaint.setAntiAlias(true);
            mBorderPaint.setColor(borderColor);
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mBorderPaint.setStrokeWidth(mBorderWidth);
        }

        @Override
        protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
            return circleCrop(pool, toTransform);
        }

        private Bitmap circleCrop(BitmapPool pool, Bitmap source) {
            if (source == null) return null;

            int size = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);

            Bitmap result = pool.get(size, size, Bitmap.Config.ARGB_4444);
            if (result == null) {
                result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_4444);
            }

            Canvas canvas = new Canvas(result);
            Paint paint = new Paint();
            paint.setShader(new BitmapShader(squared, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
            paint.setAntiAlias(true);
            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);
            if (mBorderPaint != null) {
                float borderRadius = r - mBorderWidth / 2;
                canvas.drawCircle(r, r, borderRadius, mBorderPaint);
            }
            return result;
        }


        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {

        }
    }

    //-------------------图片转换圆角图片------------------------------

    /**
     * 图片转换圆角图片
     */
    public class GlideRoundTransform extends BitmapTransformation {

        private float radius = 0f;

        public GlideRoundTransform(Context context) {
            this(context, 4);
        }

        /**
         * 自定义圆角大小
         *
         * @param context
         * @param dp
         */
        public GlideRoundTransform(Context context, int dp) {
            super();
            this.radius = Resources.getSystem().getDisplayMetrics().density * dp;
        }

        @Override
        protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
            return roundCrop(pool, toTransform);
        }

        private Bitmap roundCrop(BitmapPool pool, Bitmap source) {
            if (source == null) return null;

            Bitmap result = pool.get(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
            if (result == null) {
                result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(result);
            Paint paint = new Paint();
            paint.setShader(new BitmapShader(source, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
            paint.setAntiAlias(true);
            RectF rectF = new RectF(0f, 0f, source.getWidth(), source.getHeight());
            canvas.drawRoundRect(rectF, radius, radius, paint);
            return result;
        }


        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {

        }
    }

/*----------------------------------------------------清除glide缓存---------------------------------------*/

    /**
     * 清除图片所有缓存
     */
    public void clearImageAllCache(Context context) {
        clearImageDiskCache(context);
        clearImageMemoryCache(context);
        String ImageExternalCatchDir = context.getExternalCacheDir() + ExternalCacheDiskCacheFactory.DEFAULT_DISK_CACHE_DIR;
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
