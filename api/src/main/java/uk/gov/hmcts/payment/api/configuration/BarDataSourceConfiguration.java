package uk.gov.hmcts.payment.api.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "bar.datasource.enabled", havingValue = "true")
public class BarDataSourceConfiguration {

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
