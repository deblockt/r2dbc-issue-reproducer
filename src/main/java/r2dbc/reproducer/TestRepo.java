package r2dbc.reproducer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TestRepo {
    @Autowired
    DatabaseClient databaseClient;

    @Transactional
    public Mono<Void> insert(String id) {
        return
                databaseClient.
                    insert()
                    .into("applied_message")
                    .value("id", id)
                    .then()
            ;
    }

    @Transactional
    public Flux<String> all() {
        return databaseClient
            .select()
            .from("applied_message")
            .project("id")
            .map(row -> row.get("id", String.class))
            .all();
    }

    @Transactional
    public Mono<Void> fetchAndInsert(String id) {
        return Mono.when(
            this.all(),
            this.insert(id)
        );
    }
}
