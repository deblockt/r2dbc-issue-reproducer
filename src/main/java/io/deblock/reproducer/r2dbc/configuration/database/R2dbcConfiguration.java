package io.deblock.reproducer.r2dbc.configuration.database;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryBuilder;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.boot.autoconfigure.r2dbc.EmbeddedDatabaseConnection;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class R2dbcConfiguration {

    /**
     * this is a copy/past of org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryConfiguration.ConnectionPoolConnectionFactoryConfiguration
     * waiting an r2dbc zipkin implementation provided by Spring (see https://github.com/spring-cloud/spring-cloud-sleuth/issues/1524)
     */
    protected static ConnectionFactory createConnectionFactory(R2dbcProperties properties, ClassLoader classLoader,
                                                               List<ConnectionFactoryOptionsBuilderCustomizer> optionsCustomizers) {
        return ConnectionFactoryBuilder.of(properties, () -> EmbeddedDatabaseConnection.get(classLoader))
            .configure((options) -> {
                for (ConnectionFactoryOptionsBuilderCustomizer optionsCustomizer : optionsCustomizers) {
                    optionsCustomizer.customize(options);
                }
            }).build();
    }

    @Bean(destroyMethod = "dispose")
    public ConnectionPool connectionPool(R2dbcProperties properties, ResourceLoader resourceLoader,
                                     ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers) {
        ConnectionFactory connectionFactory = createConnectionFactory(properties, resourceLoader.getClassLoader(),
            customizers.orderedStream().collect(Collectors.toList()));
        // this is a custom code to allow tracing requests on zipkin
        ConnectionFactory connectionWithTracing = ProxyConnectionFactory.builder(connectionFactory)
            .listener(new TracingLifeCycleListener())
            .listener(new TracingExecutionListener())
            .build();
        R2dbcProperties.Pool pool = properties.getPool();
        ConnectionPoolConfiguration.Builder builder = ConnectionPoolConfiguration.builder(connectionWithTracing)
            .maxSize(pool.getMaxSize()).initialSize(pool.getInitialSize()).maxIdleTime(pool.getMaxIdleTime());
        if (StringUtils.hasText(pool.getValidationQuery())) {
            builder.validationQuery(pool.getValidationQuery());
        }
        return new ConnectionPool(builder.build());
    }

    /**
     * This bean is used to manage r2dbc retrocompatibility. new devs should not use it
     */
    @Bean
    public DatabaseClient databaseClient(ConnectionFactory connectionFactory) {
        return DatabaseClient.create(connectionFactory);
    }
}
