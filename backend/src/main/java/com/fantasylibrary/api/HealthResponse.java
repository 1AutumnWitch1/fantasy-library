package com.fantasylibrary.api;

import com.fantasylibrary.service.HealthService.DatabaseStatus;

public record HealthResponse(String application, DatabaseStatus database) {
}
