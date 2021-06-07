package io.deblock.reproducer.r2dbc.configuration.database;

import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.ProxyMethodExecutionListener;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.joining;

@Slf4j
public class TracingExecutionListener implements ProxyMethodExecutionListener {

    @Override
    public void beforeQuery(QueryExecutionInfo execInfo) {
        final var queries = execInfo.getQueries().stream()
            .map(QueryInfo::getQuery)
            .collect(joining(", "));
        log.info("TracingExecutionListener beforeQuery > start the query {}", queries);
    }

    @Override
    public void eachQueryResult(QueryExecutionInfo execInfo) {
        final var queries = execInfo.getQueries().stream()
            .map(QueryInfo::getQuery)
            .collect(joining(", "));
        log.info("TracingExecutionListener eachQueryResult > a result on the query {} {}", queries, execInfo.getExecuteDuration().toMillis());
    }

    @Override
    public void afterQuery(QueryExecutionInfo execInfo) {
        final var queries = execInfo.getQueries().stream()
        .map(QueryInfo::getQuery)
        .collect(joining(", "));

        log.info("TracingExecutionListener afterQuery > end the query {} {}ms execInfo.isSuccess = {}", queries, execInfo.getExecuteDuration().toMillis(), execInfo.isSuccess());
    }
}
