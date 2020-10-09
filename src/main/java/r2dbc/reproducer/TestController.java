package r2dbc.reproducer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
public class TestController {
    private final DatabaseClient databaseClient;

    public TestController(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Transactional
    @GetMapping("/test")
    public Mono<ResponseEntity<String>> simulate() {
        final var id = UUID.randomUUID().toString();
        return
            Mono.defer(() -> {log.info("before insert data (R2DBC 0.8.4)"); return Mono.empty();})
                .then(this.insert(id))
                .doOnNext(it -> log.info("after the insertion"))
                .map(r -> ResponseEntity.ok("ok " + id))
                .doOnNext(it -> log.info("after creating the response entity"));
    }

    public Mono<String> insert(String id) {
        return databaseClient.
                insert()
                .into("applied_message")
                .value("id", id)
                .then()
                .thenReturn(id);
    }
}
