package com.linx.mylibrary.http.model;

import java.io.Serializable;
import java.util.List;

/**
 * 描述说明:
 * 作者: lxh
 * 创建日期: 2018/7/2 17:47
 */
public class HistoryBean implements Serializable {

    /**
     * nowOffice : 15
     * nowConsignee : 15
     * thirtyOffice : 100
     * thirtyConsignee : 100
     * sum : 230
     * week : [{"day":"7.1","office":"10","consignee":"10"},{"day":"7.2","office":"10","consignee":"10"},{"day":"7.3","office":"10","consignee":"10"},{"day":"7.4","office":"10","consignee":"10"},{"day":"7.5","office":"10","consignee":"10"},{"day":"7.6","office":"10","consignee":"10"},{"day":"7.7","office":"10","consignee":"10"}]
     */

    private int nowOffice;
    private int nowConsignee;
    private int thirtyOffice;
    private int thirtyConsignee;
    private int sum;
    private List<WeekBean> week;

    public int getNowOffice() {
        return nowOffice;
    }

    public void setNowOffice(int nowOffice) {
        this.nowOffice = nowOffice;
    }

    public int getNowConsignee() {
        return nowConsignee;
    }

    public void setNowConsignee(int nowConsignee) {
        this.nowConsignee = nowConsignee;
    }

    public int getThirtyOffice() {
        return thirtyOffice;
    }

    public void setThirtyOffice(int thirtyOffice) {
        this.thirtyOffice = thirtyOffice;
    }

    public int getThirtyConsignee() {
        return thirtyConsignee;
    }

    public void setThirtyConsignee(int thirtyConsignee) {
        this.thirtyConsignee = thirtyConsignee;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

    public List<WeekBean> getWeek() {
        return week;
    }

    public void setWeek(List<WeekBean> week) {
        this.week = week;
    }

    public static class WeekBean implements Serializable {
        /**
         * day : 7.1
         * office : 10
         * consignee : 10
         */

        private String day;
        private int office;
        private int consignee;

        public String getDay() {
            return day;
        }

        public void setDay(String day) {
            this.day = day;
        }

        public int getOffice() {
            return office;
        }

        public void setOffice(int office) {
            this.office = office;
        }

        public int getConsignee() {
            return consignee;
        }

        public void setConsignee(int consignee) {
            this.consignee = consignee;
        }
    }
}
