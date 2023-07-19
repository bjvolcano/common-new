import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.volcano.cache.entity.SqlEntity;
import com.volcano.cache.entity.SqlParams;
import com.volcano.cache.entity.TransactionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Collectors;
@Slf4j
public class TestCache {

    @Value("${spring.application.name}")
    protected String applicationName;
    protected static final ThreadLocal<Map<SqlParams, SqlEntity>> dealSqls = new ThreadLocal();
    protected static final ThreadLocal<SqlParams> key = new ThreadLocal();
    protected static final ThreadLocal<TransactionInfo> transactionInfo = new ThreadLocal();
    protected static final ThreadLocal<Set<String>> hotKeys = new ThreadLocal() {
        @Override
        protected Object initialValue() {
            return new HashSet();
        }
    };

    public static void main(String[] args) {
        TestCache cache=new TestCache();
        String a=cache.getKey("insert into a (id,name) values ('111232','asdf')",null);
        log.info("a : {}",a);
    }

    public String getKey(String sql, Object argValues) {
        Map<SqlParams, SqlEntity> entitys = dealSqls.get();
        if (entitys != null) {
            SqlParams keyName = new SqlParams(sql);
            SqlEntity entity = entitys.get(keyName);
            if (entity != null) {
                keyName.setKey(entity.getKey());
                key.set(keyName);
                return entity.getKey();
            }
        }

        return getSqlByArgs(sql, (Collection) argValues);
    }

    private String getSqlByArgs(String sql, Collection<Object> argValues) {
        Integer tablesHashCode = this.getTablesHash(sql, argValues);
        Integer sqlHashCode = sql.hashCode();
        SqlEntity entity = new SqlEntity();
        entity.setTableHashCode(tablesHashCode);
        entity.setSqlHashCode(sqlHashCode);
        //entity.setTables(this.getHashTables().get(tablesHashCode));
        entity.setSql(sql);
        entity.setKey(this.applicationName + ":" + tablesHashCode + ":" + sqlHashCode);
        entity.setParams(argValues);
        //this.set(entity);
        key.set(new SqlParams(sql, entity.getKey()));
        //this.reduceHotKey(entity.getKey());
        return entity.getKey();
    }

    private Collection<String> getTablesJoin(String sql, Object argValues) {
        try {
            List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, "mysql");
            Collection<TableStat.Name> tableNames = null;
            Collection<String> tableNamesList = new ArrayList();
            MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
            Map<TableStat.Name, TableStat> tables = null;

            for (int i = 0; i < stmtList.size(); ++i) {
                SQLStatement stmt = stmtList.get(i);
                stmt.accept(visitor);
                tables = visitor.getTables();
                tableNames = tables.keySet();
                tableNamesList.addAll((Collection) tableNames.stream().map((x) -> {
                    return x.getName().replaceAll("`", "");
                }).distinct().collect(Collectors.toList()));
            }

            SqlEntity entity = new SqlEntity();
            entity.setSql(sql);
            entity.setParams(argValues);
            return tableNamesList;
        } catch (Exception var10) {
            log.error("异常sql: sql:{}", sql);
            throw new RuntimeException("异常sql:" + sql);
        }
    }

    private Integer getTablesHash(String sql, Object argValues) {
        Collection<String> tables = this.getTablesJoin(sql, argValues);
        Integer tableHash = String.join("_", tables).hashCode();
        //this.updateTableAndHash(tables, tableHash);
        return tableHash;
    }
}
