package r2dbc.reproducer;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class StartAndEndLogWebFilter implements WebFilter {

    private final static List<String> pathToIgnore = List.of("/actuator", "/info");

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange,
                             WebFilterChain webFilterChain) {
        return webFilterChain
                .filter(serverWebExchange)
                .transformDeferred(mono -> addExecutionLog(serverWebExchange, mono));
    }

    private static Publisher<Void> addExecutionLog(ServerWebExchange exchange, Mono<Void> httpRequest) {
        final var start = System.currentTimeMillis();
        final var path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (!pathShouldBeIgnored(path)) {
            log.info("INTERNAL start request to {}", path);
            return httpRequest
                .doOnSuccess((value) -> log.info("INTERNAL end request to {} with status code {}. duration: {}ms", path, status(exchange), duration(start)))
                .doOnCancel(() -> log.error("the request to {} has been canceled by client. this can happen due to a client timeout. duration: {}ms", path, duration(start)))
                .doOnError((cause) -> log.info("INTERNAL end request to {} with error. duration: {}ms", path, duration(start)));
        } else {
            return httpRequest
                .doOnError((cause) -> log.info("INTERNAL end request to {} with error. duration: {}ms", path, duration(start), cause));
        }
    }

    private static boolean pathShouldBeIgnored(String path) {
        return pathToIgnore.stream().anyMatch(path::startsWith);
    }

    private static HttpStatus status(ServerWebExchange serverWebExchange) {
        final var statusCode = serverWebExchange.getResponse().getStatusCode();
        if (statusCode == null) {
            return HttpStatus.OK;
        }
        return statusCode;
    }

    private static long duration(long startOnMilli) {
        return System.currentTimeMillis() - startOnMilli;
    }
}
