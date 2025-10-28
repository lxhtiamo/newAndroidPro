package com.linx.mylibrary.utils;

import androidx.annotation.NonNull;

import com.linx.mylibrary.utils.klog.KLog;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * rx java定时器
 */
public class RxTimerUtil {

    private Disposable mdDisposable;

    private RxTimerUtil() {
    }

    private static class SingletonInstance {
        private static final RxTimerUtil INSTANCE = new RxTimerUtil();
    }

    public static RxTimerUtil getInstance() {

        return SingletonInstance.INSTANCE;

    }

    /**
     * 毫秒后执行next操作
     * @param minute
     * @param next
     */
    public void timer(long minute, final IRxNext next) {
        Observable.timer(minute, TimeUnit.MINUTES)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {
                        mdDisposable = disposable;
                    }

                    @Override
                    public void onNext(@NonNull Long number) {
                        if (next != null) {
                            next.doNext(number);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        //取消订阅
                        cancel();
                    }

                    @Override
                    public void onComplete() {
                        //取消订阅
                        cancel();
                    }
                });


    }

    public void timedelay() {
        //从0开始发射11个数字为：0-10依次输出，延时0s执行，每1s发射一次。
        Observable.intervalRange(0, 11, 0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mdDisposable = d;
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**轮询
     * @param minute 多少分钟
     * @param next
     */
    public void timerLoop(long minute, final IRxNext next) {
        Observable.interval(minute, TimeUnit.MINUTES)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mdDisposable = d;
                    }

                    @Override
                    public void onNext(Long aLong) {
                        if (next != null) {
                            next.doNext(aLong);
                        }

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    /**
     * 取消订阅
     */
    public void cancel() {
        if(mdDisposable!=null&&!mdDisposable.isDisposed()){
            mdDisposable.dispose();
            KLog.d("====定时器取消======");
        }
    }

    public interface IRxNext {
        void doNext(long type);
    }

}