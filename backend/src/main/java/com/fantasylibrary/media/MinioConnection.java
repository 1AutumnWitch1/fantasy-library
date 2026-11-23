package com.fantasylibrary.media;

import io.minio.MinioClient;
import okhttp3.OkHttpClient;

import java.net.URI;
import java.time.Duration;

record MinioConnection(String endpoint, String accessKey, String secretKey, String bucket) {
    MinioConnection {
        URI server = URI.create(endpoint);
        if (!("https".equals(server.getScheme()) || "http".equals(server.getScheme()))
                || server.getHost() == null || server.getUserInfo() != null
                || server.getQuery() != null || server.getFragment() != null
                || !server.getPath().isEmpty() && !"/".equals(server.getPath())) {
            throw new IllegalArgumentException("对象存储端点须为 HTTP(S) 服务地址");
        }
        if (accessKey.isBlank() || secretKey.isBlank() || !bucket.matches("[a-z0-9][a-z0-9-]{1,61}[a-z0-9]")) {
            throw new IllegalArgumentException("请配置对象存储访问密钥和存储桶");
        }
    }

    MinioClient client() {
        var http = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(4)).readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30)).build();
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).httpClient(http).build();
    }

    @Override
    public String toString() {
        return "MinioConnection[bucket=" + bucket + "]";
    }
}
