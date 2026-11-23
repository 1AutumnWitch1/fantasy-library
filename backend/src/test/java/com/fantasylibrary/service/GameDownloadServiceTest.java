package com.fantasylibrary.service;

import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.catalog.model.CatalogProblem.Reason;
import com.fantasylibrary.download.GameDownload;
import com.fantasylibrary.persistence.mapper.CatalogMapper;
import com.fantasylibrary.media.ObjectMediaStore;
import com.fantasylibrary.persistence.mapper.GameDownloadMapper;
import com.fantasylibrary.persistence.mapper.GameDownloadMapper.Node;
import com.fantasylibrary.support.CatalogStub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GameDownloadServiceTest {
    @TempDir Path downloads;

    @Test
    void listsOnlyAvailableFilesAndValidHttpsNodes() throws IOException {
        Files.writeString(downloads.resolve("game.zip"), "test data");
        var service = service(List.of(
                new Node(1, "馆内节点", "PC", null, "game.zip"),
                new Node(2, "备用节点", "PC", "https://downloads.example/game", null),
                new Node(3, "无文件", "PC", null, "missing.zip"),
                new Node(4, "目录越界", "PC", null, "../game.zip"),
                new Node(5, "非 HTTPS 节点", "PC", "http://downloads.example/game", null),
                new Node(6, "包含凭据", "PC", "https://user:token@downloads.example/game", null)));
        assertThat(service.nodes(21)).containsExactly(
                new GameDownload(1, "馆内节点", "PC", "/api/game-downloads/1/file", false),
                new GameDownload(2, "备用节点", "PC", "https://downloads.example/game", true));
        assertThat(Files.readString(service.file(1).resource().getFile().toPath())).isEqualTo("test data");
        assertThatExceptionOfType(CatalogProblem.class).isThrownBy(() -> service.file(4))
                .satisfies(error -> assertThat(error.reason()).isEqualTo(Reason.DOWNLOAD_NOT_FOUND));
        assertThatExceptionOfType(CatalogProblem.class).isThrownBy(() -> service.file(2))
                .satisfies(error -> assertThat(error.reason()).isEqualTo(Reason.DOWNLOAD_NOT_FOUND));
    }

    @Test
    void returnsEmptyForAGameWithoutSourcesAndRejectsAnime() {
        var service = service(List.of());
        assertThat(service.nodes(21)).isEmpty();
        assertThatExceptionOfType(CatalogProblem.class).isThrownBy(() -> service.nodes(11))
                .satisfies(error -> assertThat(error.reason()).isEqualTo(Reason.WORK_NOT_FOUND));
    }

    private GameDownloadService service(List<Node> nodes) {
        var providers = new StaticListableBeanFactory();
        providers.addBean("catalog", new CatalogStub());
        providers.addBean("downloads", new GameDownloadMapper() {
            @Override public List<Node> nodes(long workId) { return nodes; }

            @Override public String fileName(long id) {
                return nodes.stream().filter(node -> node.id() == id).findFirst().map(Node::fileName).orElse(null);
            }
        });
        var storage = new ObjectMediaStore() {
            @Override public boolean enabled() { return false; }
            @Override public void put(String key, Path file, String contentType) { throw new AssertionError(); }
            @Override public void put(String key, java.io.InputStream source, long size, String type) { throw new AssertionError(); }

            @Override public Resource resource(String key) { throw new AssertionError(); }
            @Override public void delete(String key) { throw new AssertionError(); }
        };
        return new GameDownloadService(providers.getBeanProvider(CatalogMapper.class),
                providers.getBeanProvider(GameDownloadMapper.class), downloads.toString(), storage);
    }
}
