package com.fantasylibrary.media;

import com.fantasylibrary.account.AccountProblem;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminMediaServiceTest {
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aV9sAAAAASUVORK5CYII=");
    private static final byte[] MP4_HEADER = {
            0, 0, 0, 20, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0, 'm', 'p', '4', '2'
    };

    @Test
    void determinesMimeTypeAndPathFromFileBytes() {
        var storage = new RecordingStore(true);
        var media = new AdminMediaService(storage);
        var file = new MockMultipartFile("file", "deliberately-incorrect.jpg", "image/jpeg", PNG);

        var result = media.uploadImage(file);

        assertThat(result.url()).matches("/api/media/catalog/uploads/[a-f0-9-]{36}\\.png");
        assertThat(result.format()).isNull();
        assertThat(storage.contentType).isEqualTo("image/png");
        assertThat(storage.key).isEqualTo(result.url().substring("/api/media/".length()));
        assertThat(storage.bytes).containsExactly(PNG);
        assertThat(storage.size).isEqualTo(PNG.length);
    }

    @Test
    void recordsVideoFormatAndRejectsIncorrectContentOrOversizedRequests() {
        var storage = new RecordingStore(true);
        var media = new AdminMediaService(storage);
        var result = media.uploadVideo(-1, () -> new ByteArrayInputStream(MP4_HEADER));

        assertThat(result.url()).matches("/api/media/playback/[a-f0-9-]{36}\\.mp4");
        assertThat(result.format()).isEqualTo("MP4");
        assertThat(storage.contentType).isEqualTo("video/mp4");
        assertThatThrownBy(() -> media.uploadVideo(-1, () -> new ByteArrayInputStream(PNG)))
                .isInstanceOfSatisfying(AccountProblem.class, error -> assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> media.uploadVideo(5L * 1024 * 1_048_576 + 1, () -> {
            throw new AssertionError("An oversized request must not be read");
        })).isInstanceOfSatisfying(AccountProblem.class, error -> assertThat(error.status()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void disabledStorageRejectsUploadsBeforeReadingThem() {
        var media = new AdminMediaService(new RecordingStore(false));
        assertThatThrownBy(() -> media.uploadVideo(-1, () -> {
            throw new AssertionError("A disabled store must not read uploads");
        })).isInstanceOfSatisfying(MediaStoreException.class, error -> assertThat(error.isMissing()).isFalse());
    }

    @Test
    void preservesLargeContentLengthAndEnforcesLimitOnUnknownLength() {
        var storage = new RecordingStore(true);
        var media = new AdminMediaService(storage);
        long fiveGiB = 5L * 1024 * 1024 * 1024;
        media.uploadVideo(fiveGiB, () -> new ByteArrayInputStream(MP4_HEADER));
        assertThat(storage.size).isEqualTo(fiveGiB);
        assertThat(storage.bytes).containsExactly(MP4_HEADER);
        var game = media.uploadGamePackage("game.zip", fiveGiB, () -> new ByteArrayInputStream(new byte[]{42}));
        assertThat(game.fileName()).startsWith("game-downloads/").endsWith("/game.zip");
        assertThat(storage.bytes).containsExactly(42);
        assertThat(storage.size).isEqualTo(fiveGiB);
        assertThatThrownBy(() -> media.uploadGamePackage("game.zip", fiveGiB + 1, () -> {
            throw new AssertionError("An oversized package must not be read");
        })).isInstanceOfSatisfying(AccountProblem.class, error -> assertThat(error.status()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));

        var oversizedImage = Arrays.copyOf(PNG, 5 * 1024 * 1024 + 1);
        var raw = new MockMultipartFile("file", "large.png", "image/png", oversizedImage) {
            @Override public long getSize() { return -1; }
        };
        assertThatThrownBy(() -> media.uploadImage(raw))
                .isInstanceOfSatisfying(AccountProblem.class, error -> assertThat(error.status()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    private static final class RecordingStore implements ObjectMediaStore {
        private final boolean enabled;
        private String key;
        private String contentType;
        private byte[] bytes;
        private long size;

        private RecordingStore(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public void put(String path, Path source, String type) {
            throw new AssertionError("Admin uploads must stream");
        }

        @Override
        public void put(String path, InputStream source, long size, String type) throws IOException {
            key = path;
            contentType = type;
            this.size = size;
            bytes = source.readAllBytes();
        }

        @Override
        public Resource resource(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String path) {
            throw new UnsupportedOperationException();
        }
    }
}
