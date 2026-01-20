package com.linewell.lxhdemo.view.dialog;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;


import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.linewell.lxhdemo.R;
import com.linewell.lxhdemo.base.BaseDialog;
import com.linewell.lxhdemo.base.aop.SingleClick;
import com.linx.mylibrary.utils.rvItemDecoration.PickerLayoutManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 *    desc   : 日期选择对话框
 */
public final class DateDialog {

    public static final class Builder
            extends StyleDialog.Builder<Builder>
            implements Runnable,
            PickerLayoutManager.OnPickerListener {

        private final int mStartYear;

        @NonNull
        private final RecyclerView mYearView;
        @NonNull
        private final RecyclerView mMonthView;
        @NonNull
        private final RecyclerView mDayView;

        @NonNull
        private final PickerLayoutManager mYearManager;
        @NonNull
        private final PickerLayoutManager mMonthManager;
        @NonNull
        private final PickerLayoutManager mDayManager;

        @NonNull
        private final PickerAdapter mYearAdapter;
        @NonNull
        private final PickerAdapter mMonthAdapter;
        @NonNull
        private final PickerAdapter mDayAdapter;

        @Nullable
        private OnListener mListener;

        public Builder(@NonNull Context context) {
            this(context, Calendar.getInstance(Locale.CHINA).get(Calendar.YEAR) - 100);
        }

        public Builder(@NonNull Context context, int startYear) {
            this(context, startYear, Calendar.getInstance(Locale.CHINA).get(Calendar.YEAR));
        }

        public Builder(@NonNull Context context, int startYear, int endYear) {
            super(context);
            mStartYear = startYear;

            setCustomView(R.layout.dialog_date);
            setTitle(R.string.time_title);

            mYearView = findViewById(R.id.rv_date_year);
            mMonthView = findViewById(R.id.rv_date_month);
            mDayView = findViewById(R.id.rv_date_day);

            mYearAdapter = new PickerAdapter(context);
            mMonthAdapter = new PickerAdapter(context);
            mDayAdapter = new PickerAdapter(context);

            // 生产年份
            ArrayList<String> yearData = new ArrayList<>(10);
            for (int i = mStartYear; i <= endYear; i++) {
                yearData.add(i + " " + getString(R.string.common_year));
            }

            // 生产月份
            ArrayList<String> monthData = new ArrayList<>(12);
            for (int i = 1; i <= 12; i++) {
                monthData.add(i + " " + getString(R.string.common_month));
            }

            Calendar calendar = Calendar.getInstance(Locale.CHINA);

            int day = calendar.getActualMaximum(Calendar.DATE);
            // 生产天数
            ArrayList<String> dayData = new ArrayList<>(day);
            for (int i = 1; i <= day; i++) {
                dayData.add(i + " " + getString(R.string.common_day));
            }

            mYearAdapter.setNewData(yearData);
            mMonthAdapter.setNewData(monthData);
            mDayAdapter.setNewData(dayData);

            mYearManager = new PickerLayoutManager.Builder(context)
                    .build();
            mMonthManager = new PickerLayoutManager.Builder(context)
                    .build();
            mDayManager = new PickerLayoutManager.Builder(context)
                    .build();

            mYearView.setLayoutManager(mYearManager);
            mMonthView.setLayoutManager(mMonthManager);
            mDayView.setLayoutManager(mDayManager);

            mYearView.setAdapter(mYearAdapter);
            mMonthView.setAdapter(mMonthAdapter);
            mDayView.setAdapter(mDayAdapter);
            
            setYear(calendar.get(Calendar.YEAR));
            setMonth(calendar.get(Calendar.MONTH) + 1);
            setDay(calendar.get(Calendar.DAY_OF_MONTH));

            mYearManager.setOnPickerListener(this);
            mMonthManager.setOnPickerListener(this);
        }

        public Builder setListener(@Nullable OnListener listener) {
            mListener = listener;
            return this;
        }

        /**
         * 不选择天数
         */
        public Builder setIgnoreDay() {
            mDayView.setVisibility(View.GONE);
            return this;
        }

        public Builder setDate(long date) {
            if (date > 0) {
                setDate(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date(date)));
            }
            return this;
        }

        public Builder setDate(String date) {
            if (date.matches("\\d{8}")) {
                // 20190519
                setYear(date.substring(0, 4));
                setMonth(date.substring(4, 6));
                setDay(date.substring(6, 8));
            } else if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                // 2019-05-19
                setYear(date.substring(0, 4));
                setMonth(date.substring(5, 7));
                setDay(date.substring(8, 10));
            }
            return this;
        }

        public Builder setYear(String year) {
            return setYear(Integer.parseInt(year));
        }

        public Builder setYear(int year) {
            int index = year - mStartYear;
            if (index < 0) {
                index = 0;
            } else if (index > mYearAdapter.getItemCount() - 1) {
                index = mYearAdapter.getItemCount() - 1;
            }
            mYearView.scrollToPosition(index);
            postRefreshMonthMaximumDay();
            return this;
        }

        public Builder setMonth(String month) {
            return setMonth(Integer.parseInt(month));
        }

        public Builder setMonth(int month) {
            int index = month - 1;
            if (index < 0) {
                index = 0;
            } else if (index > mMonthAdapter.getItemCount() - 1) {
                index = mMonthAdapter.getItemCount() - 1;
            }
            mMonthView.scrollToPosition(index);
            postRefreshMonthMaximumDay();
            return this;
        }

        public Builder setDay(String day) {
            return setDay(Integer.parseInt(day));
        }

        public Builder setDay(int day) {
            refreshMonthMaximumDay();
            int index = day - 1;
            if (index < 0) {
                index = 0;
            } else if (index > mDayAdapter.getItemCount() - 1) {
                index = mDayAdapter.getItemCount() - 1;
            }
            mDayView.scrollToPosition(index);
            return this;
        }

        @SingleClick
        @Override
        public void onClick(@NonNull View view) {
            int viewId = view.getId();
            if (viewId == R.id.tv_ui_confirm) {
                performClickDismiss();
                if (mListener == null) {
                    return;
                }
                mListener.onSelected(getDialog(),
                        mStartYear + mYearManager.getPickedPosition(),
                        mMonthManager.getPickedPosition() + 1,
                        mDayManager.getPickedPosition() + 1);
            } else if (viewId == R.id.tv_ui_cancel) {
                performClickDismiss();
                if (mListener == null) {
                    return;
                }
                mListener.onCancel(getDialog());
            }
        }

        /**
         * {@link PickerLayoutManager.OnPickerListener}
         *
         * @param recyclerView              RecyclerView 对象
         * @param position                  当前滚动的位置
         */
        @Override
        public void onPicked(@NonNull RecyclerView recyclerView, int position) {
            postRefreshMonthMaximumDay();
        }

        @Override
        public void run() {
            refreshMonthMaximumDay();
        }

        /**
         * 刷新每个月天最大天数
         */
        private void refreshMonthMaximumDay() {
            // 获取这个月最多有多少天
            Calendar calendar = Calendar.getInstance(Locale.CHINA);
            calendar.set(mStartYear + mYearManager.getPickedPosition(), mMonthManager.getPickedPosition(), 1);

            int day = calendar.getActualMaximum(Calendar.DATE);
            if (mDayAdapter.getItemCount() == day) {
                return;
            }

            ArrayList<String> dayData = new ArrayList<>(day);
            for (int i = 1; i <= day; i++) {
                dayData.add(i + " " + getString(R.string.common_day));
            }
            mDayAdapter.setNewData(dayData);
        }

        private void postRefreshMonthMaximumDay() {
            mYearView.removeCallbacks(this);
            mYearView.post(this);
        }

        private static final class PickerAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

            private PickerAdapter(@NonNull Context context) {
                super(R.layout.picker_item);
            }

            @Override
            protected void convert(BaseViewHolder helper, String item) {
                
                helper.setText(R.id.tv_picker_name, item);
            }
        }
    }

    public interface OnListener {

        /**
         * 选择完日期后回调
         *
         * @param year              年
         * @param month             月
         * @param day               日
         */
        void onSelected(@NonNull BaseDialog dialog, int year, int month, int day);

        /**
         * 点击取消时回调
         */
        default void onCancel(@NonNull BaseDialog dialog) {
            // default implementation ignored
        }
    }
}