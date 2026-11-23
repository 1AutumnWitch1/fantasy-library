package com.fantasylibrary.media;

import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.download.GameDownloadFiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Objects;
import java.util.UUID;

@Service
public class AdminMediaService {
    private static final long MIB = 1_048_576;
    private static final long IMAGE_MAX_BYTES = 5 * MIB;
    private static final long LARGE_FILE_MAX_BYTES = 5 * 1024 * MIB;
    private static final int HEADER_BYTES = 8_192;
    private final ObjectMediaStore storage;

    public AdminMediaService(ObjectMediaStore storage) {
        this.storage = storage;
    }

    public Uploaded uploadImage(MultipartFile file) {
        return uploadMedia(Kind.IMAGE, file.getSize(), file::getInputStream);
    }

    public Uploaded uploadVideo(MultipartFile file) {
        return uploadMedia(Kind.VIDEO, file.getSize(), file::getInputStream);
    }

    public Uploaded uploadVideo(long size, Body body) {
        return uploadMedia(Kind.VIDEO, size, body);
    }

    public PackageUpload uploadGamePackage(String name, long size, Body source) {
        checkUpload(Kind.GAME, size);
        String key = GameDownloadFiles.newObjectKey(name);
        if (key == null || name.isBlank() || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
            throw new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, "安装包文件名无效或过长");
        }
        try (var body = new PushbackInputStream(source.open())) {
            int first = body.read();
            if (first == -1) {
                throw new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, "安装包不能为空");
            }
            body.unread(first);
            write(Kind.GAME, key, body, size, "application/octet-stream");
            return new PackageUpload(key);
        } catch (IOException failure) {
            throw new MediaStoreException(false, failure);
        }
    }

    private Uploaded uploadMedia(Kind kind, long size, Body source) {
        checkUpload(kind, size);
        try (var body = new PushbackInputStream(source.open(), HEADER_BYTES)) {
            byte[] header = body.readNBytes(HEADER_BYTES);
            var type = MediaFileType.detect(header);
            if (type == null || type.video() != (kind == Kind.VIDEO)) {
                throw new AccountProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, kind.formats);
            }

            body.unread(header);
            String key = kind.directory + UUID.randomUUID() + "." + type.extension();
            write(kind, key, body, size, type.contentType());
            return new Uploaded("/api/media/" + key, type.video() ? type.name() : null);
        } catch (IOException failure) {
            throw new MediaStoreException(false, failure);
        }
    }

    private void checkUpload(Kind kind, long size) {
        if (!storage.enabled()) throw new MediaStoreException(false, null);
        if (size > kind.maxBytes) throw limit(kind);
    }

    private void write(Kind kind, String key, InputStream source, long size, String contentType) throws IOException {
        var body = new LimitedUpload(source, kind.maxBytes);
        try {
            storage.put(key, body, size, contentType);
        } catch (IOException | MediaStoreException failure) {
            if (body.oversized) throw limit(kind);
            throw failure;
        }
    }

    private static final class LimitedUpload extends InputStream {
        private final InputStream source;
        private long remaining;
        private volatile boolean oversized;

        private LimitedUpload(InputStream source, long limit) {
            this.source = source;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining == 0) return checkEnd();
            int value = source.read();
            if (value != -1) remaining--;
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, buffer.length);
            if (length == 0) return 0;
            if (remaining == 0) return checkEnd();
            int read = source.read(buffer, offset, (int) Math.min(length, remaining));
            if (read > 0) remaining -= read;
            return read;
        }

        private int checkEnd() throws IOException {
            if (oversized || source.read() != -1) {
                oversized = true;
                throw new IOException("Upload size exceeds the limit");
            }
            return -1;
        }
    }

    private static AccountProblem limit(Kind kind) {
        return new AccountProblem(HttpStatus.PAYLOAD_TOO_LARGE, Code.INVALID_ARGUMENT,
                switch (kind) {
                    case VIDEO -> "视频最多 5 GiB";
                    case IMAGE -> "图片最多 5 MiB";
                    case GAME -> "安装包最多 5 GiB";
                });
    }

    @FunctionalInterface
    public interface Body {
        InputStream open() throws IOException;
    }

    public record Uploaded(String url, String format) {}
    public record PackageUpload(String fileName) {}

    private enum Kind {
        IMAGE(IMAGE_MAX_BYTES, "catalog/uploads/", "请上传有效的 JPEG、PNG、WebP、GIF 或 AVIF 图片"),
        VIDEO(LARGE_FILE_MAX_BYTES, "playback/", "请上传有效的 MP4 或 WebM 视频"),
        GAME(LARGE_FILE_MAX_BYTES, "game-downloads/", "");

        private final long maxBytes;
        private final String directory;
        private final String formats;

        Kind(long maxBytes, String directory, String formats) {
            this.maxBytes = maxBytes;
            this.directory = directory;
            this.formats = formats;
        }
    }
}
