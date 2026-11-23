package com.fantasylibrary.service;

import com.fantasylibrary.persistence.mapper.OracleHealthMapper;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final ObjectProvider<OracleHealthMapper> oracleMapper;

    public HealthService(ObjectProvider<OracleHealthMapper> oracleMapper) {
        this.oracleMapper = oracleMapper;
    }

    public DatabaseStatus checkDatabase() {
        OracleHealthMapper mapper = oracleMapper.getIfAvailable();
        if (mapper == null) {
            return DatabaseStatus.NOT_CONFIGURED;
        }

        try {
            return mapper.ping() == 1 ? DatabaseStatus.UP : DatabaseStatus.DOWN;
        } catch (DataAccessException | PersistenceException exception) {
            return DatabaseStatus.DOWN;
        }
    }

    public enum DatabaseStatus {
        NOT_CONFIGURED, UP, DOWN
    }
}
