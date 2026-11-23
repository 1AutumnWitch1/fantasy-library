package com.fantasylibrary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(OracleConnectionProperties.PREFIX)
record OracleConnectionProperties(String jdbcUrl, String username, String password) {

    static final String PREFIX = "library.oracle";

    boolean isComplete() {
        return StringUtils.hasText(jdbcUrl)
                && StringUtils.hasText(username)
                && StringUtils.hasText(password);
    }

    @Override
    public String toString() {
        return "OracleConnectionProperties[configured=" + isComplete() + "]";
    }
}
