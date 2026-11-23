package com.fantasylibrary.service;

import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.media.MediaStoreException;
import com.fantasylibrary.media.ObjectMediaStore;
import com.fantasylibrary.persistence.mapper.CatalogMapper;
import com.fantasylibrary.persistence.mapper.PlaybackMapper;
import com.fantasylibrary.playback.PlaybackModels.EpisodeRow;
import com.fantasylibrary.playback.PlaybackModels.SourceRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaybackServiceTest {
    @Test
    void acceptsAppMediaAndHttpsUrls() {
        assertThat(PlaybackService.permitted("/api/media/playback/clip-1.webm")).isTrue();
        assertThat(PlaybackService.permitted("https://media.example/clip.mp4?token=example")).isTrue();
    }

    @Test
    void excludesOtherSchemesAndPaths() {
        assertThat(PlaybackService.permitted("//media.example/clip.mp4")).isFalse();
        assertThat(PlaybackService.permitted("/api/media/playback/../clip.mp4")).isFalse();
        assertThat(PlaybackService.permitted("http://media.example/clip.mp4")).isFalse();
        assertThat(PlaybackService.permitted("https://member:secret@media.example/clip.mp4")).isFalse();
    }
    @Test
    void checksOracleVisibilityBeforeRetrievingMinioVideo() throws Exception {
        var visible = new AtomicInteger(0);
        var objects = new AtomicInteger(0);
        var beans = new StaticListableBeanFactory();
        String file = "mist-harbor-1-standard.webm";
        beans.addBean("playback", new PlaybackMapper() {
            @Override
            public List<EpisodeRow> episodes(long id) { throw new AssertionError(); }

            @Override
            public List<SourceRow> sources(long id) { throw new AssertionError(); }

            @Override
            public int listedMedia(String url) {
                assertThat(url).isEqualTo("/api/media/playback/" + file);
                return visible.get();
            }
        });
        var reader = new PlaybackService(beans.getBeanProvider(CatalogMapper.class), beans.getBeanProvider(PlaybackMapper.class),
                new ObjectMediaStore() {
                    @Override
                    public boolean enabled() { return true; }

                    @Override
                    public void put(String key, Path file, String contentType) { throw new AssertionError(); }

                    @Override public void put(String key, java.io.InputStream source, long size, String type) { throw new AssertionError(); }

                    @Override
                    public Resource resource(String key) {
                        assertThat(key).isEqualTo("playback/" + file);
                        if (objects.getAndIncrement() > 0) throw new MediaStoreException(true, null);
                        return new ByteArrayResource(new byte[]{3, 6, 9});
                    }

                    @Override
                    public void delete(String key) { throw new AssertionError(); }
                });

        assertThatThrownBy(() -> reader.media(file))
                .isInstanceOfSatisfying(CatalogProblem.class,
                        error -> assertThat(error.reason()).isEqualTo(CatalogProblem.Reason.PLAYBACK_NOT_FOUND));
        assertThat(objects.get()).isZero();

        visible.set(1);
        assertThat(reader.media(file).contentLength()).isEqualTo(3);
        assertThatThrownBy(() -> reader.media(file)).isInstanceOf(CatalogProblem.class);
    }
}
