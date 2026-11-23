package com.fantasylibrary.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration(proxyBeanMethods = false)
@Conditional(OracleDataSourceConfiguration.OracleConfigured.class)
@EnableConfigurationProperties(OracleConnectionProperties.class)
class OracleDataSourceConfiguration {

    @Bean
    HikariDataSource oracleDataSource(OracleConnectionProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.jdbcUrl());
        dataSource.setUsername(properties.username());
        dataSource.setPassword(properties.password());
        dataSource.setMaximumPoolSize(4);
        dataSource.setMinimumIdle(0);
        dataSource.setConnectionTimeout(3_000);
        dataSource.setValidationTimeout(2_000);
        dataSource.setInitializationFailTimeout(-1);
        dataSource.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", "2000");
        dataSource.addDataSourceProperty("oracle.jdbc.ReadTimeout", "2000");
        return dataSource;
    }

    static class OracleConfigured implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return Binder.get(context.getEnvironment())
                    .bind(OracleConnectionProperties.PREFIX, OracleConnectionProperties.class)
                    .map(OracleConnectionProperties::isComplete)
                    .orElse(false);
        }
    }
}
