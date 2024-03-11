package com.volcano.sync.service;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.sql.Connection;
import java.util.List;

public interface ISyncService {

    TransactionStatus getTransactionStatus();

    List<String> buildSqls(TransactionStatus status, String sql);

    Connection getConnection();

    void remove();
}
