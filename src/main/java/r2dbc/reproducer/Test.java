package r2dbc.reproducer;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
@Slf4j
public class Test implements ApplicationRunner {
    @Autowired
    TestRepo repo;

    @Override
    public void run(ApplicationArguments args) {
        log.info("start query");
        String id = UUID.randomUUID().toString();
        List<DurationComputer> listOfDuration = new ArrayList<>();
        List<Mono<?>> monos = new ArrayList<>();
        for (int i = 0; i < 1; ++i) {
            final var bench = new DurationComputer();
            listOfDuration.add(bench);
            final var mono = repo.insert(UUID.randomUUID().toString())
                .flatMap(x -> repo.insert(UUID.randomUUID().toString()))
                .flatMap(x -> repo.insert(UUID.randomUUID().toString()))
                .flatMap(x -> repo.insert(UUID.randomUUID().toString()))
                .flatMap(x -> repo.insert(UUID.randomUUID().toString()))
                .transform(bench);
            monos.add(mono);
        }

         Mono.when(monos)
            .then(Mono.defer(() -> {
                final var average = listOfDuration.stream()
                        .mapToDouble(durationComputer -> durationComputer.duration)
                        .average()
                        .getAsDouble();
                System.out.println("duration average : " + average + " ms");
                return Mono.empty();
            }))
            .subscribe();
    }

    public class DurationComputer implements Function<Mono<String>, Publisher<String>> {
        private long startTime;
        private long duration;

        @Override
        public Publisher<String> apply(Mono<String> mono) {
            return mono.doOnSubscribe((subscription) -> startTime = System.currentTimeMillis())
                        .doOnTerminate(() -> {
                            duration = System.currentTimeMillis() - startTime;
                            System.out.println("duration = " + duration + " ms");
                        });
        }
    }

}
