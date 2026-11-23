package com.fantasylibrary.community;

import com.fantasylibrary.media.MediaStoreException;
import com.fantasylibrary.media.ObjectMediaStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunityImagesTest {
    @TempDir Path directory;

    @Test
    void reencodesRasterDataUnderGeneratedFileNames() throws Exception {
        var store = new CommunityImages(directory.toString(), new TestStore(false));
        var encoded = png(24, 10);
        var key = store.save(List.of(new MockMultipartFile("images", "../untrusted.png", "image/png", encoded))).getFirst();
        assertThat(key).endsWith(".png").doesNotContain("untrusted", "..", "/");
        try (var input = store.load(key).getInputStream()) {
            var normalized = ImageIO.read(input);
            assertThat(normalized.getWidth()).isEqualTo(24);
            assertThat(normalized.getHeight()).isEqualTo(10);
        }
        store.remove(List.of(key));
        assertThat(Files.exists(directory.resolve(key))).isFalse();
    }

    @Test
    void rejectsInvalidDataAndRollsBackPreviouslyWrittenFilesInTheBatch() throws Exception {
        var store = new CommunityImages(directory.toString(), new TestStore(false));
        var valid = new MockMultipartFile("images", "valid.png", "image/png", png(4, 4));
        var disguised = new MockMultipartFile("images", "fake.png", "image/png",
                "<svg/>".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> store.save(List.of(valid, disguised))).isInstanceOf(CommunityProblem.class);
        try (var files = Files.list(directory)) { assertThat(files).isEmpty(); }
        assertThatThrownBy(() -> store.load("../outside.png")).isInstanceOf(CommunityProblem.class);
    }

    @Test
    void enforcesDimensionsBeforeAllocatingTheDecodedRaster() throws Exception {
        var store = new CommunityImages(directory.toString(), new TestStore(false));
        var tooWide = new MockMultipartFile("images", "wide.png", "image/png", png(4097, 1));
        assertThatThrownBy(() -> store.save(List.of(tooWide))).isInstanceOf(CommunityProblem.class)
                .hasMessageContaining("4096");
    }

    @Test
    void storesForumImagesAndAvatarsInDistinctPrivatePrefixes() throws Exception {
        var remote = new TestStore(true);
        var store = new CommunityImages(directory.toString(), remote);
        var png = new MockMultipartFile("images", "pic.png", "image/png", png(24, 10));

        String image = store.save(List.of(png)).getFirst();
        String avatar = store.saveAvatar(png);
        assertThat(remote.objects).containsOnlyKeys("community/" + image, "avatars/" + avatar);
        try (var input = store.load(image).getInputStream()) {
            assertThat(ImageIO.read(input).getWidth()).isEqualTo(24);
        }
        try (var input = store.loadAvatar(avatar).getInputStream()) {
            assertThat(ImageIO.read(input).getWidth()).isEqualTo(24);
        }
        assertThatThrownBy(() -> store.load(avatar)).isInstanceOf(CommunityProblem.class);
        try (var entries = Files.list(directory)) {
            assertThat(entries).isEmpty();
        }

        var invalid = new MockMultipartFile("images", "fake.png", "image/png", new byte[]{0, 1});
        assertThatThrownBy(() -> store.save(List.of(png, invalid))).isInstanceOf(CommunityProblem.class);
        assertThat(remote.objects).containsOnlyKeys("community/" + image, "avatars/" + avatar);

        store.remove(List.of(image));
        store.removeAvatar(avatar);
        assertThat(remote.objects).isEmpty();
    }

    private record TestStore(boolean enabled, Map<String, byte[]> objects) implements ObjectMediaStore {
        TestStore(boolean enabled) {
            this(enabled, new HashMap<>());
        }

        @Override
        public void put(String key, Path file, String contentType) {
            try {
                objects.put(key, Files.readAllBytes(file));
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
        }

        @Override public void put(String key, java.io.InputStream source, long size, String type) { throw new AssertionError(); }

        @Override
        public Resource resource(String key) {
            var value = objects.get(key);
            if (value == null) throw new MediaStoreException(true, null);
            return new ByteArrayResource(value);
        }

        @Override
        public void delete(String key) {
            objects.remove(key);
        }
    }

    private static byte[] png(int width, int height) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }
}
