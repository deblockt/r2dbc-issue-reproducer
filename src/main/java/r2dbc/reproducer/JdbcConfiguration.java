package r2dbc.reproducer;

import liquibase.integration.spring.SpringLiquibase;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class JdbcConfiguration {

    @Bean
    public DataSource dataSource(@Value("${spring.r2dbc.url}") String url,
                                 @Value("${spring.r2dbc.username}") String user,
                                 @Value("${spring.r2dbc.password}") String password,
                                 @Value("${spring.r2dbc.properties.schema:public}") String schema) {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(url.replace("r2dbc:", "jdbc:"));
        ds.setUser(user);
        ds.setPassword(password);
        ds.setCurrentSchema(schema);
        return ds;
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource, @Value("${spring.liquibase.change-log}") String changeLog) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog(changeLog);
        liquibase.setDataSource(dataSource);
        return liquibase;
    }

}
