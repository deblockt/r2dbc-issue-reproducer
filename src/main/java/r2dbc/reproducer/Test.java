package r2dbc.reproducer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Slf4j
public class Test implements ApplicationRunner {
    @Autowired
    TestRepo repo;

    @Override
    public void run(ApplicationArguments args) {
        log.info("start query");
        String id = UUID.randomUUID().toString();
        for (int i = 0; i < 20; ++i) {
                repo.fetchAndInsert(id).transformDeferred(this::cancelOnError)
                .onErrorResume((e) -> Mono.empty())
                .subscribe();
        }
    }

    public <T> Mono<T> cancelOnError(Mono<T> mono) {
        return mono.doOnEach((signal) -> {
            log.info("i have found an error, i cancel the mono");
            if (signal.hasError()) {
                signal.getSubscription().cancel();
            }
        });
    }

}
