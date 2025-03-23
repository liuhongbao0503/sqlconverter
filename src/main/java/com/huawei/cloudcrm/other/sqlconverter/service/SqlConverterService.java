package com.huawei.cloudcrm.other.sqlconverter.service;

import com.alibaba.druid.stat.TableStat;
import com.huawei.cloudcrm.other.sqlconverter.parser.DangerousSqlDetector;
import com.huawei.cloudcrm.other.sqlconverter.parser.DruidSqlParser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SqlConverterService {

    private final JdbcTemplate jdbcTemplate;

    public SqlConverterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void convertSqlFile(String inputFilePath, String outputFilePath) {
        List<String> sqlQueries = readSqlFile(inputFilePath);
        List<String> result = new ArrayList<>();
        for (String sql : sqlQueries) {
            // 检测SQL语句是否有效
            if (!DangerousSqlDetector.isValidSql(sql)) {
                System.err.println("检测到无效SQL语句，跳过执行: " + sql);
                result.add("检测到无效SQL语句，跳过执行: " + sql);
                continue;
            }
            // 检测是否为危险SQL语句
            if (DangerousSqlDetector.isDangerousSql(sql)) {
                System.err.println("检测到危险SQL语句，跳过执行: " + sql);
                result.add("检测到危险SQL语句，跳过执行: " + sql);
                continue;
            }
            // 检测WHERE条件是否无效
            if (DangerousSqlDetector.isInvalidCondition(sql)) {
                System.err.println("检测到无效的WHERE条件，跳过执行: " + sql);
                result.add("检测到无效的WHERE条件，跳过执行: " + sql);
                continue;
            }
            List<String> insertDeleteStatements = generateInsertDeleteStatements(sql);
            result.addAll(insertDeleteStatements);
        }

        writeResultToFile(outputFilePath, result);
    }

    private List<String> readSqlFile(String filePath) {
        List<String> sqlQueries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String sqlStr = line.trim();
                if (sqlStr.endsWith(";")) {
                    sqlStr = sqlStr.substring(0, sqlStr.length() - 1);
                }
                sqlQueries.add(sqlStr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sqlQueries;
    }

    private List<String> generateInsertDeleteStatements(String sql) {
        List<String> result = new ArrayList<>();
        Map<TableStat.Name, TableStat> nameTableStatMap = DruidSqlParser.extractTableName(sql);
        Set<TableStat.Name> names = nameTableStatMap.keySet();
        String tableName = names.iterator().next().getName();

        SqlRowSet rs = jdbcTemplate.queryForRowSet(sql);

        // 获取列名
        List<String> columns = new ArrayList<>();
        SqlRowSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnName(i));
        }

        // 生成DELETE语句
        String deleteSql = generateDeleteStatement(tableName, sql);
        result.add(deleteSql);

        // 处理查询结果并生成INSERT语句
        while (rs.next()) {
            List<String> values = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
                values.add(formatValue(value));
            }
            String insertSql = generateInsertStatement(tableName, columns, values);
            result.add(insertSql);
        }

        return result;
    }

    private String generateDeleteStatement(String tableName, String selectSql) {
        String whereCondition = DruidSqlParser.extractWhereCondition(selectSql);
        if (!StringUtils.hasText(whereCondition)) {
            return "DELETE 语句生成失败，没有正确解析出where条件。";
        }
        return "DELETE FROM " + tableName + " WHERE " + whereCondition + ";";
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }

        if (value instanceof String) {
            // 处理字符串，转义单引号
            return "'" + value.toString().replace("'", "''") + "'";
        } else if (value instanceof java.util.Date) {
            // 处理日期类型，格式化为Oracle的TO_DATE格式
            return "TO_DATE('" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value) + "', 'YYYY-MM-DD HH24:MI:SS')";
        } else if (value instanceof java.sql.Timestamp) {
            // 处理时间戳类型，格式化为Oracle的TO_TIMESTAMP格式
            return "TO_TIMESTAMP('" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(value) + "', 'YYYY-MM-DD HH24:MI:SS.FF')";
        } else if (value instanceof Number) {
            // 处理数字类型，直接转换为字符串
            return value.toString();
        } else if (value instanceof java.math.BigDecimal) {
            // 处理BigDecimal类型，直接转换为字符串
            return value.toString();
        } else if (value instanceof java.sql.Blob) {
            // 处理BLOB类型，转换为字节数组
            try {
                java.sql.Blob blob = (java.sql.Blob) value;
                byte[] bytes = blob.getBytes(1, (int) blob.length());
                return "UTL_RAW.CAST_TO_VARCHAR(" + new String(bytes) + ")";
            } catch (SQLException e) {
                e.printStackTrace();
                return "NULL";
            }
        } else if (value instanceof java.sql.Clob) {
            // 处理CLOB类型，转换为字符串
            try {
                java.sql.Clob clob = (java.sql.Clob) value;
                Reader reader = clob.getCharacterStream();
                char[] buffer = new char[(int) clob.length()];
                reader.read(buffer);
                return "'" + new String(buffer).replace("'", "''") + "'";
            } catch (Exception e) {
                e.printStackTrace();
                return "NULL";
            }
        } else {
            // 其他类型，尝试转换为字符串
            return "'" + value.toString().replace("'", "''") + "'";
        }
    }

    private String generateInsertStatement(String tableName, List<String> columns, List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName).append(" (");
        sb.append(String.join(", ", columns));
        sb.append(") VALUES (");
        sb.append(String.join(", ", values));
        sb.append(");");
        return sb.toString();
    }

    private void writeResultToFile(String filePath, List<String> result) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : result) {
                bw.write(line);
                bw.newLine();
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}