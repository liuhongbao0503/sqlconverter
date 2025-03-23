package com.huawei.cloudcrm.other.sqlconverter.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SqlConverterServiceSimple {

    private final JdbcTemplate jdbcTemplate;

    public SqlConverterServiceSimple(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void convertSqlFile(String inputFilePath, String outputFilePath) {
        List<String> sqlQueries = readSqlFile(inputFilePath);
        List<String> result = new ArrayList<>();

        for (String sql : sqlQueries) {
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
        String tableName = extractTableName(sql);

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

    private String extractTableName(String sql) {
        String[] parts = sql.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("from")) {
                return parts[i + 1].split("\\s+")[0].replace("`", "").replace("\"", "");
            }
        }
        return "";
    }

    private String generateDeleteStatement(String tableName, String selectSql) {
        return "DELETE FROM " + tableName + " WHERE " + extractWhereCondition(selectSql) + ";";
    }

    private String extractWhereCondition(String sql) {
        String[] parts = sql.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("where")) {
                return String.join(" ", Arrays.copyOfRange(parts, i + 1, parts.length));
            }
        }
        return "";
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + value.toString().replace("'", "''") + "'";
        }
        if (value instanceof java.util.Date) {
            return "TO_DATE('" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value) + "', 'YYYY-MM-DD HH24:MI:SS')";
        }
        return value.toString();
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}