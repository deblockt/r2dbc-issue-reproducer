package io.deblock.reproducer.r2dbc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.ConnectionAccessor;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class Repository {
    private final DatabaseClient databaseClient;
    private final ConnectionAccessor accessor;

    public Repository(
        DatabaseClient databaseClient,
        ConnectionAccessor accessor
    ) {
        this.databaseClient = databaseClient;
        this.accessor = accessor;
    }

    public Mono<String> getWithBindById() {
        return Mono.fromRunnable(() -> log.info("before executing query mono"))
            .then(
                databaseClient.execute("select pg_sleep(:duration), '1' as data")
                    .bind("duration", 3)
                    .map(row -> row.get("data", String.class))
                    .one()
            ).doOnNext($ -> log.info("after execute the query mono"));
    }


    public Mono<Void> runWithoutMapResult() {
        return Mono.fromRunnable(() -> log.info("before executing query mono"))
                .then(this.accessor.inConnection(connection -> {
                    return Flux.from(connection.createStatement("select pg_sleep(3), '1' as data")
                            .execute()).then().thenReturn("something");
                }))
                .doOnNext($ -> log.info("after execute the query mono"))
                .then();
    }

    /**
     * in this function the query is not really executed, because the result of Mono<Result> is ignored
     */
    public Mono<Void> runWithoutMapResultAndBinding() {
        return Mono.fromRunnable(() -> log.info("before executing query mono"))
                .then(this.accessor.inConnection(connection -> {
                    return Flux.from(connection.createStatement("select pg_sleep($1), '1' as data")
                            .bind(0, 3)
                            .execute()).then().thenReturn("something");
                }))
                .doOnNext($ -> log.info("after execute the query mono"))
                .then();
    }

    public Mono<String> getWithoutBindingById() {
        return Mono.fromRunnable(() -> log.info("before executing query mono"))
                .then(
                        databaseClient.execute("select pg_sleep(3), '1' as data;")
                                .map(row -> row.get("data", String.class))
                                .one()
                ).doOnNext($ -> log.info("after execute the query mono"));
    }

    public Mono<String> getUsingR2DBC() {
        return Mono.fromRunnable(() -> log.info("before executing query mono"))
        .then(this.accessor.inConnectionMany(connection -> {
            return Flux.from(connection.createStatement("select pg_sleep($1), '1' as data")
                    .bind(0, 3)
                    .add()
                    .bind(0, 1)
                    .execute())
                    .flatMap(result -> {
                        log.info("mapping result");
                        return result.map((row, it) -> row.get("data", String.class));
                    });
        }).collectList().map(it -> it.get(0)))
        .doOnNext($ -> log.info("after execute the query mono"));
    }
}
