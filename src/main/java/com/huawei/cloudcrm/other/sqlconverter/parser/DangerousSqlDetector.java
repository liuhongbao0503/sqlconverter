package com.huawei.cloudcrm.other.sqlconverter.parser;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLNumericLiteralExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.oracle.parser.OracleStatementParser;

import java.util.List;

public class DangerousSqlDetector {

    /**
     * 检测sql是否是select语句，其他语句均为危险语句
     *
     * @param sql
     * @return
     */
    public static boolean isDangerousSql(String sql) {
        OracleStatementParser parser = new OracleStatementParser(sql);
        List<SQLStatement> statements = parser.parseStatementList();

        for (SQLStatement statement : statements) {
            if (!(statement instanceof SQLSelectStatement)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查语句是否是合法的sql语句
     *
     * @param sql
     * @return
     */
    public static boolean isValidSql(String sql) {
        try {
            OracleStatementParser parser = new OracleStatementParser(sql);
            parser.parseStatement();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检测是否是合法的where条件
     *
     * @param sql
     * @return
     */
    public static boolean isInvalidCondition(String sql) {
        OracleStatementParser parser = new OracleStatementParser(sql);
        List<SQLStatement> statementList = parser.parseStatementList();
        SQLStatement statement = statementList.get(0);

        if (statement instanceof SQLSelectStatement) {
            SQLSelect select = ((SQLSelectStatement) statement).getSelect();
            SQLSelectQuery selectQuery = select.getQuery();
            if (selectQuery instanceof SQLSelectQueryBlock) {
                SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) selectQuery;
                SQLExpr where = queryBlock.getWhere();
                if (where == null) {
                    return true;
                }
                return isConditionAlwaysTrue(where);
            }
        }
        return false;
    }

    /**
     * 检测where条件是否恒成立
     *
     * @param where
     * @return
     */
    private static boolean isConditionAlwaysTrue(SQLExpr where) {
        if (where instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr binaryExpr = (SQLBinaryOpExpr) where;
            if (binaryExpr.getOperator() == SQLBinaryOperator.Equality) {
                if (binaryExpr.getLeft() instanceof SQLNumericLiteralExpr && binaryExpr.getRight() instanceof SQLNumericLiteralExpr) {
                    Number left = ((SQLNumericLiteralExpr) binaryExpr.getLeft()).getNumber();
                    Number right = ((SQLNumericLiteralExpr) binaryExpr.getRight()).getNumber();
                    return left.equals(right);
                }
            }
        }
        return false;
    }
}