package io.deblock.reproducer.r2dbc.configuration.database;

import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.LifeCycleListener;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.joining;

@Slf4j
public class TracingLifeCycleListener implements LifeCycleListener {
    public void beforeExecuteOnBatch(QueryExecutionInfo queryExecutionInfo) {
        beforeExecuteQuery(queryExecutionInfo);
    }

    @Override
    public void beforeExecuteOnStatement(QueryExecutionInfo queryExecutionInfo) {
        beforeExecuteQuery(queryExecutionInfo);
    }

    private void beforeExecuteQuery(QueryExecutionInfo queryExecutionInfo) {
        final var queries = queryExecutionInfo.getQueries().stream()
            .map(QueryInfo::getQuery)
            .collect(joining(", "));
        log.info("beforeExecuteQuery > start the query " + queries);
    }

    @Override
    public void afterExecuteOnBatch(QueryExecutionInfo queryExecutionInfo) {
        afterExecuteQuery(queryExecutionInfo);
    }

    @Override
    public void afterExecuteOnStatement(QueryExecutionInfo queryExecutionInfo) {
        afterExecuteQuery(queryExecutionInfo);
    }

    private void afterExecuteQuery(QueryExecutionInfo queryExecutionInfo) {
        final var queries = queryExecutionInfo.getQueries().stream()
            .map(QueryInfo::getQuery)
            .collect(joining(", "));
        log.info("afterExecuteQuery > end the query " + queries);
    }
}
