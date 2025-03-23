package com.huawei.cloudcrm.other.sqlconverter.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SqlConverter {
    // 数据库连接信息
    private static final String URL = "jdbc:oracle:thin:@//localhost:1521/orcl";
    private static final String USERNAME = "scott";
    private static final String PASSWORD = "123456";

    public static void main(String[] args) {
        // 输入文件路径和输出文件路径
        String inputFilePath = "d:\\code\\input.sql";
        String outputFilePath = "d:\\code\\output.sql";

        // 读取输入文件
        List<String> sqlQueries = readSqlFile(inputFilePath);

        // 连接数据库并处理每个SQL语句
        List<String> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            for (String sql : sqlQueries) {
                List<String> insertDeleteStatements = generateInsertDeleteStatements(conn, sql);
                result.addAll(insertDeleteStatements);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 写入输出文件
        writeResultToFile(outputFilePath, result);
    }

    // 读取SQL文件
    private static List<String> readSqlFile(String filePath) {
        List<String> sqlQueries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                sqlQueries.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sqlQueries;
    }

    // 生成INSERT和DELETE语句
    private static List<String> generateInsertDeleteStatements(Connection conn, String sql) {
        List<String> result = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // 获取表名
            String tableName = extractTableName(sql);

            // 获取列名
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columns = new ArrayList<>();
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // 提取表名
    private static String extractTableName(String sql) {
        String[] parts = sql.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("from")) {
                return parts[i + 1].split("\\s+")[0].replace("`", "").replace("\"", "");
            }
        }
        return "";
    }

    // 生成DELETE语句
    private static String generateDeleteStatement(String tableName, String selectSql) {
        // 这里可以根据SELECT语句的WHERE条件生成DELETE语句
        // 简单实现：假设SELECT语句的WHERE条件可以直接用于DELETE
        return "DELETE FROM " + tableName + " WHERE " + extractWhereCondition(selectSql) + ";";
    }

    // 提取WHERE条件
    private static String extractWhereCondition(String sql) {
        String[] parts = sql.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("where")) {
                return String.join(" ", Arrays.copyOfRange(parts, i + 1, parts.length));
            }
        }
        return "";
    }

    // 格式化值
    private static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + value.toString().replace("'", "''") + "'";
        }
        if (value instanceof Date) {
            return "TO_DATE('" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value) + "', 'YYYY-MM-DD HH24:MI:SS')";
        }
        return value.toString();
    }

    // 生成INSERT语句
    private static String generateInsertStatement(String tableName, List<String> columns, List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName).append(" (");
        sb.append(String.join(", ", columns));
        sb.append(") VALUES (");
        sb.append(String.join(", ", values));
        sb.append(");");
        return sb.toString();
    }

    // 写入结果到文件
    private static void writeResultToFile(String filePath, List<String> result) {
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