import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.volcano.range.filter.AbstructRangeFilter;
import com.volcano.range.filter.IRangeFilter;
import com.volcano.range.filter.MysqlFilter;
import com.volcano.range.mapping.ITableMapping;
import com.volcano.range.mapping.Mapping;
import com.volcano.test.config.range.AdminRangeFilter;
import com.volcano.test.config.range.UserRangeFilter;
import com.volcano.test.config.range.UserRangeRangeData;
import com.volcano.test.config.range.UserTableMapping;
import org.junit.Test;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TestSQL {

    @Test
    public void testDruidParserSql() {

        UserRangeRangeData userRangeRangeData = new UserRangeRangeData();
        userRangeRangeData.set(1L);
        MysqlFilter filter=new MysqlFilter();
        IRangeFilter iRangeFilter = new UserRangeFilter(userRangeRangeData);
        List<Mapping> mappings = new ArrayList<>();
        //mappings.add(new Mapping("排除得表",""));
        ITableMapping exclude=new UserTableMapping();
        exclude.setMappings(mappings);
        ((AbstructRangeFilter) iRangeFilter).setExclude(exclude);
        filter.addFilter(iRangeFilter);

        Long userId = userRangeRangeData.getUserId();
        System.out.println(userId);
        AdminRangeFilter adminRangeFilter = new AdminRangeFilter(userRangeRangeData);
        List<Mapping> mappings1 = new ArrayList<>();
        mappings1.add(new Mapping("sys_user","user_id"));
        UserTableMapping mappingaa=new UserTableMapping();
        mappingaa.setMappings(mappings1);
        ((AbstructRangeFilter) adminRangeFilter).setMapping(mappingaa);
        filter.addFilter(adminRangeFilter);

        String sql = "select * from sys_user a left join b c on a.xxx=b.yyy left join c d on d.aaa=a.zzz ";

        String newSql = filter.filter(sql);
        System.out.println(sql);
        System.out.println(newSql);

    }

    /**
     * @param exp
     */
    public void sqlExpr(SQLExpr exp) {
        if (exp != null && exp instanceof SQLQueryExpr) {
            sqlSelect(((SQLQueryExpr) exp).getSubQuery());
        } else if (exp instanceof SQLInSubQueryExpr) {
            sqlSelect(((SQLInSubQueryExpr) exp).getSubQuery());
        } else if (exp instanceof SQLBinaryOpExpr) {
            sqlExpr(((SQLBinaryOpExpr) exp).getRight());
            sqlExpr(((SQLBinaryOpExpr) exp).getRight());
        } else if (exp instanceof SQLInListExpr
                || exp instanceof SQLIntegerExpr
                || exp instanceof SQLCharExpr
                || exp instanceof SQLPropertyExpr
        ) {
            return;
        } else {
            System.out.println(exp.getClass() + ":遇到无法解析的对象:" + Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    /**
     * sql select
     *
     * @param sqlSelect
     */
    private void sqlSelect(SQLSelect sqlSelect) {
        sqlSelectQuery(sqlSelect.getQuery());
    }

    /**
     * @param sqlSelectQuery
     */
    private void sqlSelectQuery(SQLSelectQuery sqlSelectQuery) {
        if (sqlSelectQuery instanceof SQLSelectQueryBlock) {
            if (((SQLSelectQueryBlock) sqlSelectQuery).getSelectList().size() > 1) {
                List<SQLSelectItem> sqlSelectItems = ((SQLSelectQueryBlock) sqlSelectQuery).getSelectList();
                sqlSelectItems.forEach(sqlSelectItem -> sqlExpr(sqlSelectItem.getExpr()));
            }
            sqlTableSource(((SQLSelectQueryBlock) sqlSelectQuery).getFrom());
        } else if (sqlSelectQuery instanceof SQLUnionQuery) {
            sqlSelectQuery(((SQLUnionQuery) sqlSelectQuery).getLeft());
            sqlSelectQuery(((SQLUnionQuery) sqlSelectQuery).getRight());
        } else {
            System.out.println(sqlSelectQuery.getClass() + ":遇到无法解析的对象:" + Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    /**
     * @param sqlTableSource
     */
    private void sqlTableSource(SQLTableSource sqlTableSource) {
        // 右边是连接查询
        if (sqlTableSource instanceof SQLJoinTableSource) {
            sqlTableSource(((SQLJoinTableSource) sqlTableSource).getRight());
            sqlTableSource(((SQLJoinTableSource) sqlTableSource).getLeft());
            // 右边是子查询
        } else if (sqlTableSource instanceof SQLSubqueryTableSource) {
            sqlSelect(((SQLSubqueryTableSource) sqlTableSource).getSelect());
            // 右边是 union
        } else if (sqlTableSource instanceof SQLUnionQueryTableSource) {
            sqlSelectQuery(((SQLUnionQueryTableSource) sqlTableSource).getUnion());
            //  右边是table
        } else if (sqlTableSource instanceof SQLExprTableSource) { // 当tablesource 为 SQLExprTableSource 时候即可拼接sql
            String alias = getAlias(sqlTableSource);
            System.out.println("table:" + sqlTableSource + "> alias:" + alias);
            SQLSelectQueryBlock sqlSelectQueryBlock = getSelectQueryBlock(sqlTableSource);
            if (sqlSelectQueryBlock != null) {
                sqlSelectQueryBlock(sqlSelectQueryBlock, alias);
            }
        } else {
            System.out.println(sqlTableSource.getClass() + ":遇到无法解析的对象:" + Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    /**
     * 通过迭代获取 MySqlSelectQueryBlock
     *
     * @param sqlObject
     * @return
     */
    private SQLSelectQueryBlock getSelectQueryBlock(SQLObject sqlObject) {
        if (sqlObject.getParent() instanceof SQLSelectQueryBlock) {
            return (SQLSelectQueryBlock) sqlObject.getParent();
        } else {
            return getSelectQueryBlock(sqlObject.getParent());
        }
    }

    /**
     * sql 拼接
     *
     * @param sqlSelectQueryBlock
     * @param alias
     */
    private void sqlSelectQueryBlock(SQLSelectQueryBlock sqlSelectQueryBlock, String alias) {
        if (sqlSelectQueryBlock.getWhere() != null) {
            sqlExpr(sqlSelectQueryBlock.getWhere());
        }

        //SQLBinaryOpExpr sqlBinaryOpExpr = new SQLBinaryOpExpr(new SQLPropertyExpr(alias, "is_delete"),
        //        new SQLIntegerExpr(0), SQLBinaryOperator.Equality);
        //SQLBinaryOpExpr sqlBinaryOpExpr = new SQLBinaryOpExpr();
        // 第一个条件
        //whereAdd(sqlSelectQueryBlock, sqlBinaryOpExpr);

        SQLInListExpr sqlInListExpr = new SQLInListExpr();
        sqlInListExpr.setExpr(new SQLPropertyExpr(alias, "name"));
        List<SQLExpr> list = new ArrayList();
        list.add(new SQLCharExpr("lisr"));
        list.add(new SQLCharExpr("gqq"));
        sqlInListExpr.setTargetList(list);
        // 第二个条件
        // whereAdd(sqlSelectQueryBlock, sqlInListExpr);

        //SQLBinaryOpExpr whereExpr = new SQLBinaryOpExpr(sqlBinaryOpExpr, SQLBinaryOperator.BooleanOr, sqlInListExpr);
        whereAdd(sqlSelectQueryBlock, sqlInListExpr);

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
     * 获取别名
     *
     * @param tableSource
     * @return
     */
    private String getAlias(SQLTableSource tableSource) {
        if (StringUtils.isEmpty(tableSource.getAlias())) {
            if (((SQLExprTableSource) tableSource).getExpr() instanceof SQLIdentifierExpr) {
                return ((SQLIdentifierExpr) ((SQLExprTableSource) tableSource).getExpr()).getName();
            }
        }
        return tableSource.getAlias();
    }
}
