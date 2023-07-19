package com.volcano.range.filter;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLDbTypedObject;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import com.volcano.range.mapping.Mapping;
import com.volcano.range.mapping.SourceFromInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * druidDataSource 拦截器，拦截sql,添加租户ID条件
 *
 * @author wuya
 */
@Slf4j
public class MysqlFilter {

    private static final String MYSQL_STRING = "mysql";


    private List<IRangeFilter> filters = new ArrayList<>();

    public void addFilter(IRangeFilter filter) {
        filters.add(filter);
    }


    public void setFilters(List<IRangeFilter> filters) {
        this.filters = filters;
    }

    public void addAllFilters(List<IRangeFilter> filters) {
        this.filters.addAll(filters);
    }

    public String filter(String sql) {
        if (CollectionUtils.isEmpty(filters)) {
            log.debug("filter is empty,sql not filter!");
            return sql;
        }
        String oldSql = sql;
        try {
            // 解析sql
            MySqlStatementParser parser = new MySqlStatementParser(sql);
            SQLStatement stmt = parser.parseStatement();
            if (stmt instanceof SQLSelectStatement) {
                SQLSelect sqlSelect = ((SQLSelectStatement) stmt).getSelect();
                if (sqlSelect.getQuery() instanceof SQLUnionQuery) {
                    SQLUnionQuery unionQuery = (SQLUnionQuery) sqlSelect.getQuery();
                    sql = doUnionSelect(unionQuery);
                } else {
                    sql = doSelectSql(sql, (MySqlSelectQueryBlock) sqlSelect.getQueryBlock());
                }
            } else if (stmt instanceof MySqlUpdateStatement) {
                MySqlUpdateStatement update = (MySqlUpdateStatement) stmt;
                sql = doUpdateSql(sql, update);
            } else if (stmt instanceof MySqlDeleteStatement) {
                MySqlDeleteStatement delete = (MySqlDeleteStatement) stmt;
                sql = doDeleteSql(sql, delete);
            }
        } catch (Exception e) {
            log.error("deal self filter sql error {}\n{}", sql, e);
            sql = oldSql;
        }
        log.debug("new sql = {}", sql);
        return sql;
    }

    /**
     * 处理union查询语句
     *
     * @param unionQuery 语句
     * @return 处理结果
     */
    private String doUnionSelect(SQLUnionQuery unionQuery) {
        SQLSelectQuery left = unionQuery.getLeft();
        SQLSelectQuery right = unionQuery.getRight();
        if (left instanceof SQLUnionQuery) {
            doUnionSelect((SQLUnionQuery) left);
        } else {
            doSelectSql(String.valueOf(left), (MySqlSelectQueryBlock) left);
        }
        if (right instanceof SQLUnionQuery) {
            doUnionSelect((SQLUnionQuery) right);
        } else {
            doSelectSql(String.valueOf(right), (MySqlSelectQueryBlock) right);
        }
        return String.valueOf(unionQuery);
    }

    /**
     * 处理查询语句
     *
     * @param sql SQL
     * @return 处理后的SQL
     */
    private String doSelectSql(String sql, MySqlSelectQueryBlock select) {
        // 获取where对象
        SQLExpr where = select.getWhere();
        List<SQLSelectItem> selectList = select.getSelectList();
        // 遍历查询的字段，如果查询字段中有子查询 则加上租户ID查询条件
        selectList.forEach(e -> {
            if (e.getExpr() instanceof SQLQueryExpr) {
                SQLQueryExpr expr = (SQLQueryExpr) e.getExpr();
                String newFieldSql = doSelectSql(String.valueOf(expr), (MySqlSelectQueryBlock) expr.getSubQuery().getQueryBlock());
                SQLExpr subSelect = SQLUtils.toMySqlExpr(newFieldSql);
                e.setExpr(subSelect);
                log.debug("sql select field have subQuery = {}", newFieldSql);
            }
        });
        // 获取所查询的表
        SQLTableSource from = select.getFrom();
        // 如果from语句是子查询
        if (from instanceof SQLSubqueryTableSource) {
            String fromString = String.valueOf(from);
            SQLSubqueryTableSource subqueryTableSource = (SQLSubqueryTableSource) from;
            String subQuery = doSelectSql(fromString, (MySqlSelectQueryBlock) subqueryTableSource.getSelect().getQueryBlock());
            log.debug("sql from have subQuery = {}", subQuery);
            SQLSelect sqlSelectBySql = getSqlSelectBySql(subQuery);
            ((SQLSubqueryTableSource) from).setSelect(sqlSelectBySql);
            select.setWhere(getNewWhereCondition(select, where, sql, from));
        }
        // 如果from语句是关联查询
        if (from instanceof SQLJoinTableSource) {
            SQLJoinTableSource joinFrom = (SQLJoinTableSource) from;
            SQLTableSource left = joinFrom.getLeft();
            SQLTableSource right = joinFrom.getRight();
            setTableSourceNewSql(left);
            setTableSourceNewSql(right);
        }
        select.setWhere(getNewWhereCondition(select, where, sql, from));
        try {
            return select.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return sql;
        }
    }

    /**
     * 处理更新语句
     *
     * @param sql  sql语句
     * @param stmt 解析的语句
     * @return 修改的后的sql
     */
    private String doUpdateSql(String sql, SQLStatement stmt) {
        MySqlUpdateStatement update = (MySqlUpdateStatement) stmt;
        SQLExpr where = update.getWhere();
        // 拼接where条件
        update.setWhere(getNewWhereCondition(null, where, sql, update.getTableSource()));
        return update.toString();
    }

    /**
     * 处理delete语句
     *
     * @param sql  sql语句
     * @param stmt 解析的语句
     * @return 修改的后的sql
     */
    private String doDeleteSql(String sql, SQLStatement stmt) {
        MySqlDeleteStatement delete = (MySqlDeleteStatement) stmt;
        SQLExpr where = delete.getWhere();
        // 拼接where条件
        delete.setWhere(getNewWhereCondition(null, where, sql, delete.getTableSource()));
        return delete.toString();
    }

    /**
     * 添加where条件
     *
     * @param where where语句
     * @return 修改后的where条件
     */
    private SQLExpr getNewWhereCondition(MySqlSelectQueryBlock select, SQLExpr where, String sql,
                                         SQLTableSource tableSource) {
        // 如果where中包含子查询
        if (where instanceof SQLInSubQueryExpr) {
            SQLSelect subSelect = ((SQLInSubQueryExpr) where).subQuery;
            // 获取子查询语句
            String subQuery = String.valueOf(subSelect);
            // 处理子查询语句
            String newSubQuery = doSelectSql(subQuery, (MySqlSelectQueryBlock) subSelect.getQueryBlock());
            SQLSelect sqlSelectBySql = getSqlSelectBySql(newSubQuery);
            ((SQLInSubQueryExpr) where).setSubQuery(sqlSelectBySql);
        }
        SQLBinaryOpExpr binaryOpExprWhere = new SQLBinaryOpExpr(MYSQL_STRING);
        List<SourceFromInfo> tableNameList = new ArrayList();
        getTableNames(select, tableSource, tableNameList);
        if (CollectionUtils.isEmpty(tableNameList)) {
            return where;
        }
        // 根据多个表名获取拼接条件
        SQLExpr conditionByTableName =  getWhereConditionByTableList(tableNameList);
        log.debug("get tableInfos = {}", JSON.toJSONString(tableNameList));
        // 没有需要添加的条件，直接返回
        if (ObjectUtils.isEmpty(conditionByTableName)) {
            return where;
        }
        // 没有where条件时 则返回需要添加的条件
        if (where == null) {
            return conditionByTableName;
        }
        binaryOpExprWhere.setLeft(conditionByTableName);
        binaryOpExprWhere.setOperator(SQLBinaryOperator.BooleanAnd);
        binaryOpExprWhere.setRight(where.clone());
        if (isFilterAndOrCondition(where)) {
            log.debug("the sql contains or condition by tenant_id, sql = {}", sql);
        }
        return binaryOpExprWhere;
    }

    /**
     * 根据from语句得到的表名拼接条件
     *
     * @param tableNameList 表名列表
     * @return 拼接后的条件
     */
    private SQLExpr getWhereConditionByTableList( List<SourceFromInfo> tableNameList) {
        // 先过滤掉不需要添加条件的
        //tableNameList =
        //        tableNameList.stream().filter(fromInfo -> fromInfo.isNeedAddCondition()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tableNameList)) {
            return null;
        }
        MySqlSelectQueryBlock selectQueryBlock=new MySqlSelectQueryBlock();

        for (int i = 0; i < tableNameList.size(); i++) {
            for (IRangeFilter filter : filters) {
                SourceFromInfo tableNameInfo = tableNameList.get(i);

                SQLExpr newWhere = filter.getFilterCondition(tableNameInfo);

                if (newWhere == null)
                    continue;
                whereAdd(selectQueryBlock,newWhere);

            }
        }
        return selectQueryBlock.getWhere();
    }

    public void whereAdd(SQLSelectQueryBlock sqlSelectQueryBlock, SQLExpr sqlExpr) {
        if (sqlSelectQueryBlock.getWhere() != null) {
            SQLExpr sqlExprWhere = sqlSelectQueryBlock.getWhere();
            List<SQLObject> sqlList = sqlExprWhere.getChildren();
            List<SQLObject> sqlObjectList = sqlExpr.getChildren();
            if (sqlList != null && sqlList.size() > 1) {
                // 原有的where 如果是or 则不拆
                if (sqlExprWhere instanceof SQLBinaryOpExpr) {
                    if (!((SQLBinaryOpExpr) sqlExprWhere).getOperator().equals(SQLBinaryOperator.BooleanOr)) {
                        // 原有where 和需要拼接的where
                        if (sqlObjectList != null && sqlObjectList.size() > 1) {
                            if (!((SQLBinaryOpExpr) sqlExpr).getOperator().equals(SQLBinaryOperator.BooleanOr)) {
                                sqlObjectList.forEach(sqlObject -> {
                                    if (!sqlList.contains(sqlObject)) {
                                        sqlSelectQueryBlock.addWhere((SQLExpr) sqlObject);
                                    }
                                });
                            } else {
                                if (!sqlList.contains(sqlExpr)) {
                                    sqlSelectQueryBlock.addWhere(sqlExpr);
                                }
                            }
                        } else {
                            if (!sqlList.contains(sqlExpr)) {
                                sqlSelectQueryBlock.addWhere(sqlExpr);
                            }
                        }
                    } else {
                        sqlSelectQueryBlock.addWhere(sqlExpr);
                    }
                } else if (sqlExprWhere instanceof SQLInSubQueryExpr) {
                    sqlSelectQueryBlock.addWhere(sqlExpr);
                } else {
                    sqlSelectQueryBlock.addWhere(sqlExpr);
                }
            } else {
                sqlSelectQueryBlock.addWhere(sqlExpr);
            }
        } else {
            sqlSelectQueryBlock.addWhere(sqlExpr);
        }

    }

    /**
     * 拼接and条件
     *
     * @param left  左侧条件
     * @param right 右侧条件
     * @return 拼接后的条件
     */
    private SQLBinaryOpExpr getAndCondition(SQLExpr left, SQLExpr right) {
        SQLBinaryOpExpr condition = new SQLBinaryOpExpr(MYSQL_STRING);
        condition.setLeft(left);
        condition.setOperator(SQLBinaryOperator.BooleanAnd);
        condition.setRight(right);
        return condition;
    }

    /**
     * 根据表信息拼接tenantId 条件
     *
     * @param mapping 表信息
     * @return 拼接后的条件
     */
    private SQLBinaryOpExpr getFilterCondition(Mapping mapping) {
        SQLBinaryOpExpr where = new SQLBinaryOpExpr(MYSQL_STRING);
        if (StringUtils.isEmpty(mapping.getAlias())) {
            // 拼接新的条件 带别名的
            where.setLeft(new SQLIdentifierExpr(mapping.getField()));
        } else {
            where.setLeft(new SQLPropertyExpr(mapping.getAlias(), mapping.getField()));
        }

        where.setOperator(SQLBinaryOperator.Equality);

        //设置 条件的值
        //where.setRight(new SQLIntegerExpr(tenantId));
        return where;
    }

    /**
     * 根据表信息拼接tenantId 条件
     *
     * @param tableNameInfo 表信息
     * @return 拼接后的条件
     */
    private SQLExpr getFilterCondition(SourceFromInfo tableNameInfo) {
//        SQLBinaryOpExpr where = new SQLBinaryOpExpr(MYSQL_STRING);
//        if (StringUtils.isEmpty(mapping.getAlias())) {
//            // 拼接新的条件 带别名的
//            where.setLeft(new SQLIdentifierExpr(mapping.getField()));
//        } else {
//            where.setLeft(new SQLPropertyExpr(mapping.getAlias(), mapping.getField()));
//        }
//
//        where.setOperator(SQLBinaryOperator.Equality);
//
//        // 设置 条件的值
//        //where.setRight(new SQLIntegerExpr(tenantId));
        return null;
    }

    /**
     * 查询所有的表信息
     *
     * @param select        from语句对应的select语句
     * @param tableSource   from语句
     * @param tableNameList sql中from语句中所有表信息
     */
    private void getTableNames(MySqlSelectQueryBlock select, SQLTableSource tableSource,
                               List<SourceFromInfo> tableNameList) {
        // 子查询
        if (tableSource instanceof SQLSubqueryTableSource) {
            SourceFromInfo fromInfo = new SourceFromInfo();
            fromInfo.setSubQuery(true);
            SQLSubqueryTableSource subqueryTableSource = (SQLSubqueryTableSource) tableSource;
            //fromInfo.setTableName(tableSource.ge);
            // 设置别名
            fromInfo.setAlias(subqueryTableSource.getAlias());
            List<SQLSelectItem> selectList = select.getSelectList();
            Optional.ofNullable(selectList).filter(list -> !CollectionUtils.isEmpty(selectList)).map(list -> {
                list.forEach(item -> {
                    String itemString = String.valueOf(item).toLowerCase();

                    // 这里和mapping比较 如果语句中包含，则设置需要添加条件
                    // 如果查询字段中有tenant_id 字段则需要加条件 否则不用加

                });
                return list;
            });
            tableNameList.add(fromInfo);
        }
        // 连接查询
        if (tableSource instanceof SQLJoinTableSource) {
            SQLJoinTableSource joinSource = (SQLJoinTableSource) tableSource;
            SQLTableSource left = joinSource.getLeft();
            SQLTableSource right = joinSource.getRight();
            // 子查询则递归获取
            if (left instanceof SQLSubqueryTableSource) {
                getTableNames((MySqlSelectQueryBlock) ((SQLSubqueryTableSource) left).getSelect().getQuery(), left,
                        tableNameList);
            }
            // 子查询则递归获取
            if (right instanceof SQLSubqueryTableSource) {
                getTableNames((MySqlSelectQueryBlock) ((SQLSubqueryTableSource) right).getSelect().getQuery(), right,
                        tableNameList);
            }
            // 连接查询 左边是单表
            if (left instanceof SQLExprTableSource) {
                addOnlyTable(left, tableNameList);
            }
            // 连接查询 右边是单表
            if (right instanceof SQLExprTableSource) {
                addOnlyTable(right, tableNameList);
            }
            // 连接查询 左边还是连接查询 则递归继续获取表名
            if (left instanceof SQLJoinTableSource) {
                getTableNames(null, left, tableNameList);
            }
            // 连接查询 右边还是连接查询 则递归继续获取表名
            if (right instanceof SQLJoinTableSource) {
                getTableNames(null, right, tableNameList);
            }
        }
        // 普通表查询
        if (tableSource instanceof SQLExprTableSource) {
            addOnlyTable(tableSource, tableNameList);
        }
    }


    /**
     * 如果当前from语句只有单表，则添加到list中
     *
     * @param tableSource   from语句
     * @param tableNameList 表信息list
     */
    private void addOnlyTable(SQLTableSource tableSource, List<SourceFromInfo> tableNameList) {
        SourceFromInfo fromInfo = new SourceFromInfo();
        // 普通表查询
        String tableName = String.valueOf(tableSource);
        fromInfo.setTableName(tableName);
        fromInfo.setAlias(tableSource.getAlias());

        tableNameList.add(fromInfo);
    }

    /**
     * 条件中是否为 and or 表达式
     *
     * @param where sql中where条件语句
     * @return 判断结果
     */
    private boolean isContainsCondition(SQLExpr where) {
        if (!(where instanceof SQLBinaryOpExpr)) {
            return false;
        }
        SQLBinaryOpExpr binaryOpExpr = (SQLBinaryOpExpr) where;
        SQLExpr left = binaryOpExpr.getLeft();
        SQLExpr right = binaryOpExpr.getRight();
        // 是否包含映射表中的字段 为查询条件
//        List<Mapping> mappings=tableMapping.getMappings();
//        for(Mapping mapping : mappings){
//            if (!(left instanceof SQLBinaryOpExpr) && !(right instanceof SQLBinaryOpExpr)
//                    && (mapping.getField().equals(String.valueOf(left))
//                    || mapping.getField().equals(String.valueOf(right)))) {
//                return true;
//            }
//        }
        return false;
    }

    /**
     * 是否包括 or tenant_id = xx的条件
     *
     * @param where sql中where条件语句
     * @return 判断结果
     */
    private boolean isFilterAndOrCondition(SQLExpr where) {
        if (!(where instanceof SQLBinaryOpExpr)) {
            return false;
        }
        SQLBinaryOpExpr binaryOpExpr = (SQLBinaryOpExpr) where;
        if ((isContainsCondition(binaryOpExpr.getLeft())
                || isContainsCondition(binaryOpExpr.getRight()))
                && "BooleanOr".equals(String.valueOf(binaryOpExpr.getOperator()))) {
            return true;
        }
        return isFilterAndOrCondition(binaryOpExpr.getLeft()) || isFilterAndOrCondition(binaryOpExpr.getRight());
    }

    /**
     * from语句是子查询的 处理子查询 并更新from语句
     *
     * @param tableSource from语句
     */
    private void setTableSourceNewSql(SQLTableSource tableSource) {
        if (!(tableSource instanceof SQLSubqueryTableSource)) {
            return;
        }
        SQLSubqueryTableSource subqueryTableSource = (SQLSubqueryTableSource) tableSource;
        String leftSubQueryString = String.valueOf(subqueryTableSource.getSelect());
        String newLeftSubQueryString = doSelectSql(leftSubQueryString, (MySqlSelectQueryBlock) subqueryTableSource.getSelect().getQueryBlock());
        SQLSelect sqlselect = getSqlSelectBySql(newLeftSubQueryString);
        subqueryTableSource.setSelect(sqlselect);
    }

    /**
     * 将String类型select sql语句转化为SQLSelect对象
     *
     * @param sql 查询SQL语句
     * @return 转化后的对象实体
     */
    private SQLSelect getSqlSelectBySql(String sql) {
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, MYSQL_STRING);
        List<SQLStatement> parseStatementList = parser.parseStatementList();
        if (CollectionUtils.isEmpty(parseStatementList)) {
            return null;
        }
        SQLSelectStatement sstmt = (SQLSelectStatement) parseStatementList.get(0);
        SQLSelect sqlselect = sstmt.getSelect();
        return sqlselect;
    }
}