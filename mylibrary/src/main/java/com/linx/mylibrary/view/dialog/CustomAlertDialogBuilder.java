package com.linx.mylibrary.view.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import com.linx.mylibrary.R;

/**
 * 自定义AlertDialogBuilder：解决弹窗虚拟导航栏显示问题，适配各品牌定制系统
 * 核心优化：修复沉浸式逻辑顺序、添加空指针防御、增强兼容性、提升扩展性
 */
public class CustomAlertDialogBuilder extends AlertDialog.Builder {
    // 沉浸式是否强制隐藏导航栏（默认false，仅布局适配，避免定制系统复现）
    private boolean mHideNavigation = false;

    // ======================== 构造方法 ========================
    /**
     * 默认构造方法：使用R.style.CustomAlertDialog作为主题，适配对话框基础样式
     * @param context 上下文
     */
    public CustomAlertDialogBuilder(@NonNull Context context) {
        super(context, R.style.CustomAlertDialog);
    }

    /**
     * 自定义主题构造方法
     * @param context 上下文
     * @param themeResId 主题资源ID
     */
    public CustomAlertDialogBuilder(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    // ======================== 对外配置方法 ========================
    /**
     * 设置是否强制隐藏虚拟导航栏（建议大部分场景使用默认false）
     * @param hideNavigation true-强制隐藏，false-仅布局适配
     * @return Builder自身，支持链式调用
     */
    public CustomAlertDialogBuilder setHideNavigation(boolean hideNavigation) {
        mHideNavigation = hideNavigation;
        return this;
    }

    // ======================== 重写show方法：修复逻辑顺序，确保沉浸式生效 ========================
    @Override
    public AlertDialog show() {
        AlertDialog dialog = create();
        // 防御性判断：dialog或window为空时，直接返回（避免空指针）
        if (dialog == null || dialog.getWindow() == null) {
            return dialog;
        }

        Window window = dialog.getWindow();
        // 1. 设置透明背景（优先使用资源文件，保持与BaseDialog一致）
        setWindowBackground(window);
        // 2. 设置临时FLAG，避免沉浸式过程中焦点丢失
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        // 3. 显示对话框（关键：必须先show，再处理沉浸式，确保DecorView初始化完成）
        dialog.show();
        // 4. 执行沉浸式处理，解决虚拟导航栏问题
        fullScreenImmersive(window.getDecorView(), mHideNavigation);
        // 5. 清除临时FLAG，恢复对话框交互能力
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        return dialog;
    }

    // ======================== 沉浸式处理：适配定制系统，增强兼容性 ========================
    /**
     * 解决弹窗虚拟导航栏显示问题，适配华为、小米等定制系统
     * @param view 目标View（通常为Window的DecorView）
     * @param hideNavigation 是否强制隐藏导航栏
     */
    protected void fullScreenImmersive(View view, boolean hideNavigation) {
        if (view == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }

        // 基础UI_FLAG：仅布局适配，不强制隐藏（核心：避免定制系统导航栏复现）
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

        // 如果需要强制隐藏导航栏/状态栏，添加额外FLAG（谨慎使用）
        if (hideNavigation) {
            uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY; // 粘性沉浸式，提升体验
        }

        view.setSystemUiVisibility(uiOptions);
    }

    // ======================== 内部工具方法：设置窗口背景 ========================
    /**
     * 设置窗口透明背景：优先使用资源文件R.drawable.transparent_bg，若不存在则使用ColorDrawable
     */
    private void setWindowBackground(Window window) {
        try {
            // 优先使用项目中的透明背景资源（与BaseDialog保持一致）
            window.setBackgroundDrawableResource(R.drawable.transparent_bg);
        } catch (Exception e) {
            // 若资源不存在，降级使用ColorDrawable透明背景
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    // 【删除冗余的create方法】：原重写的create方法无意义，直接移除
}