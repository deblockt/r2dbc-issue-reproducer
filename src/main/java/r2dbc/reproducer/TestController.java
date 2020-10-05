package r2dbc.reproducer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import java.time.OffsetDateTime;
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
    public Mono<ResponseEntity<Object>> simulate() {
        final var id = UUID.randomUUID().toString();
        return this.insert(id)
            .map(r -> ResponseEntity.ok("ok " + id));
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
