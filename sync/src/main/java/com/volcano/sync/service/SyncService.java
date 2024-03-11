package com.volcano.sync.service;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class SyncService implements ISyncService {
    private static final ThreadLocal<TransactionStatus> TRANSACTION_STATUS = new ThreadLocal<>();

    private static final ConcurrentHashMap<TransactionStatus, List<String>> CONN_SQLS = new ConcurrentHashMap();

    private static final Set<TransactionStatus> ACTIVE_TRANSACTIONS = new LinkedHashSet<>();

    private static final ExecutorService SQL_EXECUTOR = new ThreadPoolExecutor(10, 10, 0, TimeUnit.SECONDS, new ArrayBlockingQueue(100_000), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    @Value("${spring.application.name}")
    private String applicationName;

    @Resource
    DataSource dataSource;

    @PostConstruct
    public void init() {
        checkActiveConnections();
    }

    @Override
    public TransactionStatus getTransactionStatus() {
        TransactionStatus status = TRANSACTION_STATUS.get();
        if (status == null) {
            try {
                status = TransactionAspectSupport.currentTransactionStatus();
                TRANSACTION_STATUS.set(status);
            } catch (NoTransactionException e) {
                return null;
            }
        }

        return status;
    }

    @Override
    public List<String> buildSqls(TransactionStatus status, String sql) {
        if (status == null) {
            if (!StringUtils.isEmpty(sql)) {
                commit(sql);
            }

            return Collections.emptyList();
        }

        List<String> sqls = CONN_SQLS.get(status);
        if (sql == null) {
            return sqls;
        }

        if (CollectionUtils.isEmpty(sqls)) {
            sqls = new ArrayList<>();
            CONN_SQLS.put(status, sqls);
        }

        if (sql != null) {
            sqls.add(sql);
        }

        ACTIVE_TRANSACTIONS.add(status);
        return sqls;
    }

    @Override
    public Connection getConnection() {
        return null;
    }

    private void checkActiveConnections() {
        new Thread("Check Transaction Status Thread") {
            @SneakyThrows
            @Override
            public void run() {
                for (; ; ) {
                    if (!ACTIVE_TRANSACTIONS.isEmpty()) {
                        try {
                            Iterator<TransactionStatus> iterator = ACTIVE_TRANSACTIONS.iterator();
                            while (iterator != null && iterator.hasNext()) {
                                TransactionStatus status = iterator.next();
                                if (status.isCompleted()) {
                                    commit(status);
                                    CONN_SQLS.remove(status);
                                    iterator.remove();
                                } else if (status.isRollbackOnly()) {
                                    //回滚了
                                    CONN_SQLS.remove(status);
                                    iterator.remove();
                                }
                                //else {
                                //    log.info("else status : {}", status);
                                //}
                            }
                        } catch (Exception e) {
                            log.error("处理活跃事务异常", e);
                        }
                    } else {
                        Thread.sleep(100);
                    }
                }
            }
        }.start();
    }

    private void commit(TransactionStatus status) {
        List<String> sqls = buildSqls(status, null);
        sendSql2DB(sqls);
    }

    private void commit(String sql) {
        List<String> sqls = Arrays.asList(sql);
        sendSql2DB(sqls);
    }

    private void sendSql2DB(List<String> sqls) {
        if (CollectionUtils.isEmpty(sqls)) {
            return;
        }

        SQL_EXECUTOR.execute(() -> {
            String json = JSON.toJSONString(sqls);
            json = json.replaceAll("'", "~~~");
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(true);
                Statement statement = connection.createStatement();
                StringBuilder sb = new StringBuilder("INSERT INTO SYNC_SQL_LOG(SQLS,CREATED_AT,DELETED) values(");
                sb.append(toClob(json, 2000)).append(",sysdate,0)");
                String sql = sb.toString();
                log.debug(" sending sqls to db : {}", sql);
                statement.execute(sb.toString());
            } catch (SQLException e) {
                log.error("存储sql异常", e);
            }
        });
    }

    private static String toClob(String json, int length) {
        StringBuilder sb = new StringBuilder();
        if (json.length() > length) {
            int start = 0;
            int end = length;
            boolean flag = true;
            for (int i = 0; flag; i++) {
                if (end > json.length()) {
                    end = json.length();
                    flag = false;
                }

                if (i > 0) {
                    sb.append("||");
                }
                //System.out.println("substring  s : " + start + " ,e : " + end);
                sb.append("TO_CLOB('").append(json.substring(start, end)).append("')");
                start = (i + 1) * length;
                end = (i + 1) * length + length;
            }
        } else {
            sb.append("'").append(json).append("'");
        }

        return sb.toString();
    }

    @Override
    public void remove() {
        TRANSACTION_STATUS.remove();
    }
}
