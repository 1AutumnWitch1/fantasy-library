package com.fantasylibrary.media;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface ObjectMediaStore {
    boolean enabled();
    void put(String key, Path file, String contentType);
    void put(String key, InputStream body, long size, String contentType) throws IOException;
    Resource resource(String key);
    void delete(String key);
}
