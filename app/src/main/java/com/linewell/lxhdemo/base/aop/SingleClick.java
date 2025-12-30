package com.linewell.lxhdemo.base.aop;

import com.flyjingfish.android_aop_annotation.anno.AndroidAopPointCut;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *    desc   : 防重复点击注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@AndroidAopPointCut(SingleClickCut.class)
public @interface SingleClick {

    /**
     * 快速点击的间隔
     */
    long value() default 1000;
}