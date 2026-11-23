package com.fantasylibrary.service;

import com.fantasylibrary.persistence.mapper.OracleHealthMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import java.util.Map;

import static com.fantasylibrary.service.HealthService.DatabaseStatus.DOWN;
import static com.fantasylibrary.service.HealthService.DatabaseStatus.UP;
import static org.assertj.core.api.Assertions.assertThat;

class HealthServiceTest {

    @Test
    void reportsConnectedWhenMapperReturnsOne() {
        assertThat(serviceWithMapper(() -> 1).checkDatabase()).isEqualTo(UP);
    }

    @Test
    void reportsConnectionFailureWhenMapperCannotConnect() {
        HealthService service = serviceWithMapper(() -> {
            throw new CannotGetJdbcConnectionException("Test connection failure");
        });

        assertThat(service.checkDatabase()).isEqualTo(DOWN);
    }

    private HealthService serviceWithMapper(OracleHealthMapper mapper) {
        var beans = new StaticListableBeanFactory(Map.of("oracleHealthMapper", mapper));
        return new HealthService(beans.getBeanProvider(OracleHealthMapper.class));
    }
}
