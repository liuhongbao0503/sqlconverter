package com.huawei.cloudcrm.other.sqlconverter.parser;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.oracle.parser.OracleStatementParser;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DruidSqlParser {

    public static Map<TableStat.Name, TableStat> parseSql(String sql) {
        OracleStatementParser parser = new OracleStatementParser(sql);
        List<SQLStatement> statementList = parser.parseStatementList();
        SQLStatement statement = statementList.get(0);

        SchemaStatVisitor visitor = new SchemaStatVisitor();
        statement.accept(visitor);

        return visitor.getTables();
    }

    public static Map<TableStat.Name, TableStat> extractTableName(String sql) {
        OracleStatementParser parser = new OracleStatementParser(sql);
        List<SQLStatement> statementList = parser.parseStatementList();
        SQLStatement statement = statementList.get(0);

        SchemaStatVisitor visitor = new SchemaStatVisitor();
        statement.accept(visitor);

        Map<TableStat.Name, TableStat> tables = visitor.getTables();
        return tables;
    }

    public static Collection<TableStat.Column> extractColumnNames(String sql) {
        OracleStatementParser parser = new OracleStatementParser(sql);
        List<SQLStatement> statementList = parser.parseStatementList();
        SQLStatement statement = statementList.get(0);

        SchemaStatVisitor visitor = new SchemaStatVisitor();
        statement.accept(visitor);

        return visitor.getColumns();
    }

    public static String extractWhereCondition(String sql) {
        OracleStatementParser parser = new OracleStatementParser(sql);
        List<SQLStatement> statementList = parser.parseStatementList();
        SQLStatement statement = statementList.get(0);

        if (statement instanceof SQLSelectStatement) {
            SQLSelect select = ((SQLSelectStatement) statement).getSelect();
            SQLSelectQuery selectQuery = select.getQuery();
            if (selectQuery instanceof com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock) {
                com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock queryBlock = (com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock) selectQuery;
                return queryBlock.getWhere() != null ? queryBlock.getWhere().toString() : "";
            }
        }
        return "";
    }
}