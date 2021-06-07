package io.deblock.reproducer.r2dbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@Slf4j
@RestController
@RequiredArgsConstructor
public class Controller {
    private final Repository repository;

    @Transactional
    @GetMapping("/test-with-binding")
    public Mono<?> list() {
        return repository.getWithBindById();
    }

    @Transactional
    @GetMapping("/test-without-binding")
    public Mono<?> list2() {
        return repository.getWithoutBindingById();
    }

    @Transactional
    @GetMapping("/test-using-r2dbc")
    public Mono<?> list3() {
        return repository.getUsingR2DBC();
    }

    @Transactional
    @GetMapping("/test-using-then")
    public Mono<?> empty() {
        return repository.runWithoutMapResult();
    }

    @Transactional
    @GetMapping("/test-using-then-and-binding")
    public Mono<?> emptyWithBinding() {
        return repository.runWithoutMapResultAndBinding();
    }
}
