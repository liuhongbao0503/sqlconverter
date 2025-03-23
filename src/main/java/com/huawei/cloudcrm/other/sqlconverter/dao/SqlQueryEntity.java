package com.huawei.cloudcrm.other.sqlconverter.dao;

import java.util.ArrayList;
import java.util.List;

public class SqlQueryEntity {
    private String lineNum;

    private String sql;

    private List<String> converterResult = new ArrayList<>();

    private String errMsg;

    public SqlQueryEntity(String lineNum, String sql) {
        this.lineNum = lineNum;
        this.sql = sql;
    }

    public String getLineNum() {
        return lineNum;
    }

    public void setLineNum(String lineNum) {
        this.lineNum = lineNum;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<String> getConverterResult() {
        return converterResult;
    }

    public void setConverterResult(List<String> converterResult) {
        this.converterResult = converterResult;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
}
