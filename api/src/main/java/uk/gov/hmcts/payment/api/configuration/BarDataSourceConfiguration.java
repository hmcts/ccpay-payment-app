package uk.gov.hmcts.payment.api.configuration;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "bar.datasource.enabled", havingValue = "true")
public class BarDataSourceConfiguration {

    /**
     * Keep the payment DB as the primary DataSource. Declaring {@code barDataSource}
     * otherwise suppresses Spring Boot's auto-configured DataSource, so JPA and Liquibase
     * bind to the BAR pool.
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /**
     * Keep JPA transactions on the payment DB. {@code barTransactionManager} would otherwise
     * satisfy {@code @ConditionalOnMissingBean(TransactionManager.class)} and replace it.
     */
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "barDataSource")
    @ConfigurationProperties(prefix = "bar.datasource")
    public DataSource barDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "barJdbcTemplate")
    public JdbcTemplate barJdbcTemplate(@Qualifier("barDataSource") DataSource barDataSource) {
        return new JdbcTemplate(barDataSource);
    }

    @Bean(name = "barTransactionManager")
    public PlatformTransactionManager barTransactionManager(
        @Qualifier("barDataSource") DataSource barDataSource) {
        return new DataSourceTransactionManager(barDataSource);
    }
}
