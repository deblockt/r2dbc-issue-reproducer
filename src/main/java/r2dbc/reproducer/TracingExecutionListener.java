package r2dbc.reproducer;

import brave.Span;
import brave.Tracer;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.LifeCycleListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.joining;

public class TracingExecutionListener implements LifeCycleListener {

    private static final String TAG_THREAD_ID = "threadId";
    private static final String TAG_THREAD_NAME = "threadName";
    private static final String TAG_QUERIES = "queries";
    private static final String TAG_CONNECTION_ID = "connectionId";
    private static final String TAG_TRANSACTION_SAVEPOINT = "savepoint";

    private Tracer tracer;
    private Map<String, Span> connectionSpans = new ConcurrentHashMap<>();
    private Map<String, Span> transactionSpans = new ConcurrentHashMap<>();

    public TracingExecutionListener(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void beforeCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
        final var connectionSpan = this.tracer.nextSpan()
            .name("r2dbc:connection")
            .kind(Span.Kind.CLIENT)
            .start();

        methodExecutionInfo.getValueStore().put("connectionSpan", connectionSpan);
    }

    @Override
    public void afterCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
        final var connectionSpan = methodExecutionInfo.getValueStore().get("connectionSpan", Span.class);

        final var thrown = methodExecutionInfo.getThrown();
        if (thrown != null) {
            connectionSpan
                .error(thrown)
                .finish();
            return;
        }

        if (methodExecutionInfo.getConnectionInfo() == null) {
            connectionSpan.finish();
            return;
        }

        final var connectionId = methodExecutionInfo.getConnectionInfo().getConnectionId();

        connectionSpan
            .tag(TAG_THREAD_ID, String.valueOf(methodExecutionInfo.getThreadId()))
            .tag(TAG_THREAD_NAME, methodExecutionInfo.getThreadName())
            .annotate("Connection Acquired");

        this.connectionSpans.put(connectionId, connectionSpan);
    }

    @Override
    public void afterCloseOnConnection(MethodExecutionInfo methodExecutionInfo) {
        final var connectionInfo = methodExecutionInfo.getConnectionInfo();
        final var connectionId = connectionInfo.getConnectionId();
        final var connectionSpan = this.connectionSpans.remove(connectionId);
        if (connectionSpan != null) {
            final var thrown = methodExecutionInfo.getThrown();
            if (thrown != null) {
                connectionSpan.error(thrown);
            }
            connectionSpan.finish();
        }
    }

    @Override
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

        final var querySpan = this.tracer
            .nextSpan()
            .name("r2dbc:query")
            .kind(Span.Kind.CLIENT)
            .tag(TAG_QUERIES, queries)
            .start();

        queryExecutionInfo.getValueStore().put("querySpan", querySpan);
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
        final var querySpan = queryExecutionInfo.getValueStore().get("querySpan", Span.class);

        querySpan
            .tag(TAG_THREAD_ID, String.valueOf(queryExecutionInfo.getThreadId()))
            .tag(TAG_THREAD_NAME, queryExecutionInfo.getThreadName());

        final var thrown = queryExecutionInfo.getThrowable();
        if (thrown != null) {
            querySpan.error(thrown);
        }
        querySpan.finish();
    }

    @Override
    public void beforeBeginTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
        final var connectionId = methodExecutionInfo.getConnectionInfo().getConnectionId();
        final var transactionSpan = this.tracer.nextSpan()
            .name("r2dbc:transaction")
            .start();

        this.transactionSpans.put(connectionId, transactionSpan);
    }

    @Override
    public void afterCommitTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
        final var connectionId = methodExecutionInfo.getConnectionInfo().getConnectionId();

        final var transactionSpan = this.transactionSpans.remove(connectionId);
        if (transactionSpan != null) {
            transactionSpan
                .annotate("commit")
                .tag(TAG_CONNECTION_ID, connectionId)
                .tag(TAG_THREAD_ID, String.valueOf(methodExecutionInfo.getThreadId()))
                .tag(TAG_THREAD_NAME, methodExecutionInfo.getThreadName())
                .finish();
        }

        if (this.connectionSpans.containsKey(connectionId)) {
            final var connectionSpan = this.connectionSpans.get(connectionId);
            connectionSpan.annotate("Transaction commit");
        }
    }

    @Override
    public void afterRollbackTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
        final var connectionId = methodExecutionInfo.getConnectionInfo().getConnectionId();

        final var transactionSpan = this.transactionSpans.remove(connectionId);
        if (transactionSpan != null) {
            transactionSpan
                .annotate("rollback")
                .tag(TAG_CONNECTION_ID, connectionId)
                .tag(TAG_THREAD_ID, String.valueOf(methodExecutionInfo.getThreadId()))
                .tag(TAG_THREAD_NAME, methodExecutionInfo.getThreadName())
                .finish();
        }

        if (this.connectionSpans.containsKey(connectionId)) {
            final var connectionSpan = this.connectionSpans.get(connectionId);
            connectionSpan.annotate("Transaction rollback");
        }
    }

    @Override
    public void afterRollbackTransactionToSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
        final var connectionId = methodExecutionInfo.getConnectionInfo().getConnectionId();
        final var savepoint = (String) methodExecutionInfo.getMethodArgs()[0];

        final var transactionSpan = this.transactionSpans.remove(connectionId);
        if (transactionSpan != null) {
            transactionSpan
                .annotate("rollback to savepoint")
                .tag(TAG_TRANSACTION_SAVEPOINT, savepoint)
                .tag(TAG_CONNECTION_ID, connectionId)
                .tag(TAG_THREAD_ID, String.valueOf(methodExecutionInfo.getThreadId()))
                .tag(TAG_THREAD_NAME, methodExecutionInfo.getThreadName())
                .finish();
        }

        if (this.connectionSpans.containsKey(connectionId)) {
            final var connectionSpan = this.connectionSpans.get(connectionId);
            connectionSpan.annotate("Transaction rollback to savepoint");
        }
    }
}
