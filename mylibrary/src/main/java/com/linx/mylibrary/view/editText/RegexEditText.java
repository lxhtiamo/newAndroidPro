package com.linx.mylibrary.view.editText;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatEditText;


import com.linx.mylibrary.R;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 *    desc   : 正则输入限制编辑框
 */
public class RegexEditText extends AppCompatEditText implements InputFilter {

    // ************************ 预设正则（直接编译为Pattern，提升性能） ************************
    /** 手机号（只能以 1 开头，最多11位数字） */
    public static final Pattern PATTERN_MOBILE = Pattern.compile("[1]\\d{0,10}");
    /** 中文（普通的中文字符） */
    public static final Pattern PATTERN_CHINESE = Pattern.compile("[\\u4e00-\\u9fa5]*");
    /** 英文（大写和小写的英文） */
    public static final Pattern PATTERN_ENGLISH = Pattern.compile("[a-zA-Z]*");
    /** 数字（只允许输入纯数字）*/
    public static final Pattern PATTERN_NUMBER = Pattern.compile("\\d*");
    /** 计数（非 0 开头的数字） */
    public static final Pattern PATTERN_COUNT = Pattern.compile("[1-9]\\d*");
    /** 用户名（中文、英文、数字） */
    public static final Pattern PATTERN_NAME = Pattern.compile("[[\\u4e00-\\u9fa5]|[a-zA-Z]|\\d]*");
    /** 非空格的字符（不能输入空格，允许空字符串） */
    public static final Pattern PATTERN_NONNULL = Pattern.compile("\\S*");

    /** 正则表达式规则 */
    private Pattern mPattern;
    // 标记当前过滤器是否已添加，避免重复添加
    private boolean mFilterAdded = false;

    public RegexEditText(Context context) {
        this(context, null);
    }

    public RegexEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public RegexEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    /**
     * 初始化自定义属性
     */
    private void initAttrs(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray array = null;
        try {
            array = context.obtainStyledAttributes(attrs, R.styleable.RegexEditText);

            // 优先读取自定义正则
            String customRegex = array.getString(R.styleable.RegexEditText_inputRegex);
            if (!TextUtils.isEmpty(customRegex)) {
                setInputRegex(customRegex);
            } else {
                // 读取预设正则类型
                int regexType = array.getInt(R.styleable.RegexEditText_regexType, 0);
                setInputPatternByType(regexType);
            }
        } catch (Exception e) {
            Log.e("RegexEditText", "initAttrs error: ", e);
        } finally {
            if (array != null) {
                array.recycle();
            }
        }
    }

    /**
     * 根据类型设置预设的正则Pattern
     */
    private void setInputPatternByType(int regexType) {
        switch (regexType) {
            case 0x01:
                setInputPattern(PATTERN_MOBILE);
                break;
            case 0x02:
                setInputPattern(PATTERN_CHINESE);
                break;
            case 0x03:
                setInputPattern(PATTERN_ENGLISH);
                break;
            case 0x04:
                setInputPattern(PATTERN_NUMBER);
                break;
            case 0x05:
                setInputPattern(PATTERN_COUNT);
                break;
            case 0x06:
                setInputPattern(PATTERN_NAME);
                break;
            case 0x07:
                setInputPattern(PATTERN_NONNULL);
                break;
            default:
                // 无预设类型，不设置正则
                break;
        }
    }

    /**
     * 是否有这个输入标记
     */
    public boolean hasInputType(int type) {
        return (getInputType() & type) != 0;
    }

    /**
     * 添加一个输入标记
     */
    public void addInputType(int type) {
        setInputType(getInputType() | type);
    }

    /**
     * 移除一个输入标记
     */
    public void removeInputType(int type) {
        setInputType(getInputType() & ~type);
    }

    /**
     * 设置输入正则（字符串形式）
     */
    public void setInputRegex(String regex) {
        if (TextUtils.isEmpty(regex)) {
            Log.w("RegexEditText", "input regex is empty, ignore");
            return;
        }
        setInputPattern(Pattern.compile(regex));
    }

    /**
     * 设置输入正则（Pattern形式，推荐使用，避免重复编译）
     */
    public void setInputPattern(Pattern pattern) {
        mPattern = pattern;
        // 确保过滤器只添加一次
        if (!mFilterAdded) {
            addFilters(this);
            mFilterAdded = true;
        }
    }

    /**
     * 获取输入正则
     */
    public String getInputRegex() {
        return mPattern == null ? null : mPattern.pattern();
    }

    /**
     * 添加筛选规则（优化：检查是否已存在，避免重复）
     */
    public void addFilters(InputFilter filter) {
        if (filter == null) {
            return;
        }

        InputFilter[] oldFilters = getFilters();
        // 检查过滤器是否已存在
        if (oldFilters != null) {
            for (InputFilter f : oldFilters) {
                if (f == filter) {
                    return; // 已存在，直接返回
                }
            }
        }

        // 复制并添加新过滤器
        InputFilter[] newFilters = oldFilters == null ? new InputFilter[1] : Arrays.copyOf(oldFilters, oldFilters.length + 1);
        newFilters[newFilters.length - 1] = filter;
        super.setFilters(newFilters);
    }

    /**
     * 清空筛选规则（重置标记，避免后续添加失败）
     */
    @Override
    public void setFilters(InputFilter[] filters) {
        super.setFilters(filters);
        // 重置过滤器添加标记（因为外部可能调用setFilters清空了所有过滤器）
        mFilterAdded = false;
    }

    /**
     * 清空筛选规则
     */
    public void clearFilters() {
        super.setFilters(new InputFilter[0]);
        mFilterAdded = false;
    }

    /**
     * {@link InputFilter}
     *
     * @param source        新输入的字符串（替换原内容的字符串）
     * @param start         新输入的字符串起始下标（固定为0）
     * @param end           新输入的字符串终点下标（固定为source.length()）
     * @param dest          输入之前文本框的内容（Spanned类型）
     * @param destStart     原内容中被替换的起始位置
     * @param destEnd       原内容中被替换的结束位置
     * @return              返回的字符串会替换原内容中[destStart, destEnd)的部分
     */
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int destStart, int destEnd) {
        // 无正则时，不做限制
        if (mPattern == null) {
            return source;
        }

        try {
            // 步骤1：拼接替换后的完整字符串（核心优化：正确的拼接逻辑）
            // 原内容的前缀：0 ~ destStart
            String prefix = dest.subSequence(0, destStart).toString();
            // 原内容的后缀：destEnd ~ 末尾
            String suffix = dest.subSequence(destEnd, dest.length()).toString();
            // 新输入的内容：source的[start, end)部分（通常是整个source）
            String input = source.subSequence(start, end).toString();
            // 完整的目标字符串
            String target = prefix + input + suffix;

            // 步骤2：验证正则匹配
            if (mPattern.matcher(target).matches()) {
                // 匹配成功，允许输入/删除
                return source;
            } else {
                // 匹配失败：分情况处理
                if (TextUtils.isEmpty(source)) {
                    // 情况1：source为空（删除操作），返回空字符串允许删除（即使匹配失败，也不阻止删除，避免用户无法清空）
                    return "";
                } else {
                    // 情况2：source非空（输入/替换操作），返回空字符串阻止输入
                    return "";
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // 捕获索引越界异常，避免崩溃
            Log.e("RegexEditText", "filter error: ", e);
            return source;
        }
    }
}