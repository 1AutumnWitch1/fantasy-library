package com.fantasylibrary.media;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

@Component
final class MinioObjectMediaStore implements ObjectMediaStore {
    private static final long UPLOAD_PART_BYTES = 32L * 1024 * 1024;
    private final MinioClient client;
    private final String bucket;

    MinioObjectMediaStore(@Value("${library.media.enabled:false}") boolean enabled,
                          @Value("${library.media.endpoint:http://127.0.0.1:9000}") String endpoint,
                          @Value("${library.media.access-key:}") String accessKey,
                          @Value("${library.media.secret-key:}") String secretKey,
                          @Value("${library.media.bucket:fantasy-library-media}") String bucket) {
        this.bucket = bucket;
        this.client = enabled ? new MinioConnection(endpoint, accessKey, secretKey, bucket).client() : null;
    }

    @Override
    public boolean enabled() {
        return client != null;
    }

    @Override
    public void put(String key, Path file, String contentType) {
        try {
            client.uploadObject(UploadObjectArgs.builder().bucket(bucket).object(key)
                    .filename(file.toString()).contentType(contentType).build());
        } catch (Exception failure) {
            throw error(failure);
        }
    }

    @Override
    public void put(String key, InputStream body, long size, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(body, size, UPLOAD_PART_BYTES).contentType(contentType).build());
        } catch (Exception failure) {
            throw error(failure);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception failure) {
            throw error(failure);
        }
    }

    @Override
    public Resource resource(String key) {
        try {
            var info = client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            return new MediaResource(key, info.size());
        } catch (Exception failure) {
            throw error(failure);
        }
    }

    private static MediaStoreException error(Exception cause) {
        boolean missing = cause instanceof ErrorResponseException reply &&
                ("NoSuchKey".equals(reply.errorResponse().code()) || "NoSuchObject".equals(reply.errorResponse().code()));
        return new MediaStoreException(missing, cause);
    }

    private final class MediaResource extends AbstractResource {
        private final String key;
        private final long size;

        private MediaResource(String key, long size) {
            this.key = key;
            this.size = size;
        }

        @Override
        public String getDescription() {
            return "对象存储媒体 " + key;
        }

        @Override
        public String getFilename() {
            return key.substring(key.lastIndexOf('/') + 1);
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public long contentLength() {
            return size;
        }

        @Override
        public InputStream getInputStream() {
            return new ObjectInputStream(key, size);
        }
    }

    private final class ObjectInputStream extends InputStream {
        private final String key;
        private final long size;
        private long position;
        private InputStream response;
        private boolean closed;

        private ObjectInputStream(String key, long size) {
            this.key = key;
            this.size = size;
        }

        @Override
        public long skip(long count) throws IOException {
            ensureOpen();
            long step = Math.min(Math.max(count, 0), size - position);
            if (step > 0 && response != null) {
                response.close();
                response = null;
            }
            position += step;
            return step;
        }

        @Override
        public int read() throws IOException {
            ensureOpen();
            if (position >= size) return -1;
            int next = response().read();
            if (next != -1) position++;
            return next;
        }

        @Override
        public int read(byte[] buffer, int start, int count) throws IOException {
            Objects.checkFromIndexSize(start, count, buffer.length);
            ensureOpen();
            if (count == 0) return 0;
            if (position >= size) return -1;
            int read = response().read(buffer, start, (int) Math.min(count, size - position));
            if (read > 0) position += read;
            return read;
        }

        private InputStream response() {
            if (response == null) {
                try {
                    var request = GetObjectArgs.builder().bucket(bucket).object(key);
                    if (position > 0) request.offset(position);
                    response = client.getObject(request.build());
                } catch (Exception failure) {
                    throw error(failure);
                }
            }
            return response;
        }

        private void ensureOpen() throws IOException {
            if (closed) throw new IOException("Media stream is closed");
        }

        @Override
        public void close() throws IOException {
            if (closed) return;
            closed = true;
            if (response != null) response.close();
        }
    }
}
