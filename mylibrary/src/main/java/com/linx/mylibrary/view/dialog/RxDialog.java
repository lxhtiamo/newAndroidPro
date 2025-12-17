package com.linx.mylibrary.view.dialog;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDialog;

import com.linx.mylibrary.R;

/**
 * 通用对话框基类：覆盖安卓APP开发绝大部分对话框场景
 * 核心功能：基础样式配置、沉浸式/虚拟导航栏处理、窗口参数灵活配置、系统级悬浮窗、权限检查、参数动态更新
 * 兼容：Android 4.4+（KITKAT）至最新版本，适配各品牌定制系统（华为、小米、OPPO等）
 */
public class RxDialog extends AppCompatDialog {
    private static final String TAG = "RxDialog";
    // 窗口对象（私有，通过getter访问，符合封装原则）
    private Window mWindow;
    // 窗口布局参数（私有，通过getter访问）
    private WindowManager.LayoutParams mLayoutParams;

    // ======================== 构造方法（覆盖基础初始化场景） ========================
    public RxDialog(@NonNull Context context) {
        super(context);
        initCommonView();
    }

    public RxDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
        initCommonView();
    }

    public RxDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        initCommonView();
    }

    /**
     * 自定义构造方法：支持初始化时设置透明度和重力方向
     * @param context 上下文
     * @param alpha 窗口透明度（0.0f~1.0f，0全透，1不透明）
     * @param gravity 窗口显示方向（如Gravity.CENTER、Gravity.BOTTOM、Gravity.TOP）
     */
    public RxDialog(Context context, float alpha, int gravity) {
        super(context);
        initCommonView();
        // 参数合法性校验，避免无效值导致显示异常
        if (mLayoutParams != null) {
            mLayoutParams.alpha = Math.max(0.0f, Math.min(1.0f, alpha)); // 限制透明度范围
            mLayoutParams.gravity = (gravity == 0) ? Gravity.CENTER : gravity; // 无效值默认居中
            mWindow.setAttributes(mLayoutParams);
        }
    }

    // ======================== 初始化公共配置（核心基础逻辑） ========================
    /**
     * 初始化所有对话框的通用配置：无标题、透明背景、默认参数
     * 该方法在所有构造方法中调用，确保配置优先执行
     */
    private void initCommonView() {
        // 1. 移除对话框标题（必须在setContentView之前调用，否则无效）
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 2. 获取窗口对象（防御性编程，避免空指针）
        mWindow = getWindow();
        if (mWindow == null) {
            Log.w(TAG, "获取Window对象失败，部分配置将无法生效");
            return;
        }

        // 3. 设置窗口透明背景（消除默认的白色背景和圆角）
        mWindow.setBackgroundDrawableResource(R.drawable.transparent_bg);

        // 4. 初始化窗口布局参数
        mLayoutParams = mWindow.getAttributes();
        if (mLayoutParams == null) {
            mLayoutParams = new WindowManager.LayoutParams();
        }

        // 5. 设置默认参数（适配绝大部分场景，用户可通过方法动态修改）
        mLayoutParams.alpha = 1.0f; // 默认不透明
        mLayoutParams.gravity = Gravity.CENTER; // 默认居中显示
        mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT; // 默认宽度自适应内容
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT; // 默认高度自适应内容
        mWindow.setAttributes(mLayoutParams);

        // 6. 默认设置：点击外部可关闭（可通过方法修改）
        setCanceledOnTouchOutside(true);
        // 7. 默认设置：可取消（返回键/代码调用dismiss）
        setCancelable(true);
    }

    // ======================== 沉浸式/虚拟导航栏处理（适配定制系统） ========================
    /**
     * 沉浸式处理：解决虚拟导航栏显示问题，适配华为、小米等定制系统
     * @param view 目标View（通常为Window的DecorView）
     * @param hideNavigation 是否隐藏虚拟导航栏（建议大部分场景设为false，仅布局适配）
     */
    protected void fullScreenImmersive(View view, boolean hideNavigation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 基础UI_FLAG：仅做布局适配，不强制隐藏（避免部分机型导航栏复现）
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

            // 如果需要强制隐藏导航栏/状态栏，添加额外FLAG
            if (hideNavigation) {
                uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY; // 粘性沉浸式，触摸后不持久显示
            }

            view.setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * 重写show方法：在显示时执行沉浸式逻辑，确保沉浸式效果生效
     */
    @Override
    public void show() {
        if (mWindow == null) {
            super.show();
            return;
        }

        // 步骤1：设置临时FLAG，避免沉浸式过程中焦点丢失问题
        mWindow.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        // 步骤2：显示对话框
        super.show();
        // 步骤3：执行沉浸式处理（默认仅布局适配，不强制隐藏导航栏，可根据需求修改第二个参数）
        fullScreenImmersive(mWindow.getDecorView(), false);
        // 步骤4：清除临时FLAG，恢复焦点（确保对话框可交互）
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    // ======================== 窗口参数配置（覆盖绝大部分开发场景） ========================
    /**
     * 隐藏状态栏（全屏显示，仅隐藏状态栏，保留导航栏）
     */
    public void hideStatusBar() {
        if (mWindow == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        mWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * 设置对话框全屏显示（宽高均为MATCH_PARENT，占满整个屏幕）
     */
    public void setFullScreen() {
        setWindowSize(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    }

    /**
     * 设置对话框宽度全屏（宽度MATCH_PARENT，高度WRAP_CONTENT）
     */
    public void setFullWidth() {
        setWindowSize(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    /**
     * 设置对话框高度全屏（高度MATCH_PARENT，宽度WRAP_CONTENT）
     */
    public void setFullHeight() {
        setWindowSize(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.MATCH_PARENT);
    }

    /**
     * 自定义设置对话框宽高（支持具体像素值或MATCH_PARENT/WRAP_CONTENT）
     * @param width 宽度（px）或布局常量
     * @param height 高度（px）或布局常量
     */
    public void setWindowSize(int width, int height) {
        if (mWindow == null || mLayoutParams == null) {
            return;
        }
        // 清除内边距，避免内容被裁剪
        mWindow.getDecorView().setPadding(0, 0, 0, 0);
        mLayoutParams.width = width;
        mLayoutParams.height = height;
        updateLayoutParams(); // 实时更新参数
    }

    /**
     * 设置对话框边距（距离屏幕四边的距离，支持dp转px，适配不同分辨率）
     * @param left 左边距（px）
     * @param top 上边距（px）
     * @param right 右边距（px）
     * @param bottom 下边距（px）
     */
    public void setWindowMargin(int left, int top, int right, int bottom) {
        if (mWindow == null) {
            return;
        }
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mWindow.getDecorView().getLayoutParams();
        layoutParams.setMargins(left, top, right, bottom);
        mWindow.getDecorView().setLayoutParams(layoutParams);
    }

    /**
     * 设置窗口透明度（支持动态修改，对话框显示时也能实时生效）
     * @param alpha 透明度（0.0f~1.0f）
     */
    public void setWindowAlpha(float alpha) {
        if (mLayoutParams == null) {
            return;
        }
        mLayoutParams.alpha = Math.max(0.0f, Math.min(1.0f, alpha)); // 限制范围
        updateLayoutParams(); // 实时更新参数
    }

    /**
     * 设置窗口显示的重力方向（如Gravity.BOTTOM、Gravity.TOP、Gravity.LEFT）
     * @param gravity 重力方向常量
     */
    public void setWindowGravity(int gravity) {
        if (mLayoutParams == null) {
            return;
        }
        mLayoutParams.gravity = (gravity == 0) ? Gravity.CENTER : gravity; // 校验有效性
        updateLayoutParams(); // 实时更新参数
    }

    /**
     * 设置对话框为系统级悬浮窗（可悬浮在所有应用之上）
     * 注意：1. 需要在Manifest中声明SYSTEM_ALERT_WINDOW权限；2. Android 6.0+需动态申请该权限
     */
    public void setSystemAlertType() {
        if (mWindow == null) {
            return;
        }
        // 检查是否有悬浮窗权限（Android 6.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(getContext())) {
            Log.w(TAG, "请先申请SYSTEM_ALERT_WINDOW权限！路径：设置→应用→权限管理→悬浮窗");
            return;
        }
        // 版本适配：Android 8.0+使用TYPE_APPLICATION_OVERLAY替代TYPE_SYSTEM_ALERT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mWindow.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            mWindow.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
    }

    // ======================== 封装setContentView：避免配置失效 ========================
    @Override
    public void setContentView(int layoutResID) {
        // 再次确认移除标题（防止子类重写构造方法跳过initCommonView）
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        super.setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        super.setContentView(view);
    }

    // ======================== 工具方法：实时更新参数 ========================
    /**
     * 内部工具方法：更新窗口布局参数，确保修改后的参数实时生效
     */
    private void updateLayoutParams() {
        if (mWindow != null && mLayoutParams != null) {
            if (isShowing()) {
                mWindow.setAttributes(mLayoutParams); // 显示中，直接更新
            } else {
                mWindow.setAttributes(mLayoutParams); // 未显示，提前设置
            }
        }
    }

    // ======================== 对外提供的getter方法（封装性） ========================
    /**
     * 获取Window对象（供子类扩展使用）
     */
    public Window getDialogWindow() {
        return mWindow;
    }

    /**
     * 获取窗口布局参数（供子类扩展使用）
     */
    public WindowManager.LayoutParams getDialogLayoutParams() {
        return mLayoutParams;
    }
}