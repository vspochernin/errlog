package ru.vspochernin.errapi.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class DataSourcesConfig {

    // DataSourceProperties умеет автоматически маппить spring.datasource.url в jdbcUrl для Hikari.
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties postgresDataSourceProperties() {
        return new DataSourceProperties();
    }

    // Postgres datasource - основной (для JPA + Flyway).
    @Bean
    @Primary
    @FlywayDataSource
    public DataSource postgresDataSource(DataSourceProperties props) {
        return props
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    // ClickHouse datasource - для общения с ClickHouse вручную.
    @Bean(name = "clickhouseDataSource")
    public DataSource clickhouseDataSource(
            @Value("${CLICKHOUSE_JDBC_URL}") String jdbcUrl,
            @Value("${CLICKHOUSE_USER}") String user,
            @Value("${CLICKHOUSE_PASSWORD}") String password)
    {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);

        // Драйвер ClickHouse.
        config.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");

        // Пул соединений до ClickHouse.
        config.setPoolName("clickhouse-pool");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0); // Чтобы при отсутствии трафика пул уменьшался до нуля.

        config.setConnectionTimeout(10000); // Время ожидания подключения от пула.
        config.setValidationTimeout(5000); // Время проверки активности соединения (защищает от мертвы коннектов).

        return new HikariDataSource(config);
    }

    @Bean(name = "clickhouseJdbcTemplate")
    public NamedParameterJdbcTemplate clickhouseJdbcTemplate(@Qualifier("clickhouseDataSource") DataSource dataSource) {
        // Поддерживает именованные параметры.
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
