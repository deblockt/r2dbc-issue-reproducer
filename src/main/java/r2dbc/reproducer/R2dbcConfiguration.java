package r2dbc.reproducer;

import brave.Tracer;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryBuilder;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import zipkin2.internal.Trace;

import java.util.List;

@Configuration
public class R2dbcConfiguration {

    private final Tracer tracer;

    public R2dbcConfiguration(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * this is a copy/past of org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryConfiguration.ConnectionPoolConnectionFactoryConfiguration
     * waiting an r2dbc zipkin implementation provided by Spring (see https://github.com/spring-projects-experimental/spring-boot-r2dbc/issues/71)
     */
    @Bean(destroyMethod = "dispose")
    public ConnectionPool withTracing(R2dbcProperties properties,
                                  List<ConnectionFactoryOptionsBuilderCustomizer> customizers) {
        ConnectionFactory connectionFactory = ConnectionFactoryBuilder.create(properties).customize(customizers)
            .build();
        R2dbcProperties.Pool pool = properties.getPool();
        ConnectionPoolConfiguration.Builder builder = ConnectionPoolConfiguration.builder(connectionFactory)
            .maxSize(pool.getMaxSize()).initialSize(pool.getInitialSize()).maxIdleTime(pool.getMaxIdleTime());
        if (StringUtils.hasText(pool.getValidationQuery())) {
            builder.validationQuery(pool.getValidationQuery());
        }
        return new ConnectionPool(builder.build());
    }

    @Primary
    @Bean
    public ConnectionFactory connectionFactoryWithTracing(ConnectionPool original) {
        return ProxyConnectionFactory.builder(original)
            .listener(new TracingExecutionListener(tracer))
            .build();
    }

}
