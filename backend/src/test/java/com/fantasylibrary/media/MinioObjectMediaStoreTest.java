package com.fantasylibrary.media;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MinioObjectMediaStoreTest {
    private static final byte[] BYTES = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    @Test
    void springStyleSkipReadsOnlyTheRequestedRangeAndMissingKeysAre404() throws IOException {
        HttpServer peer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> range = new AtomicReference<>();
        peer.createContext("/", exchange -> handle(exchange, range));
        peer.start();
        try {
            var store = new MinioObjectMediaStore(true, "http://127.0.0.1:" + peer.getAddress().getPort(),
                    "test-id", "test-local-fake-password", "test-media");
            var resource = store.resource("playback/clip.webm");
            assertThat(resource.contentLength()).isEqualTo(BYTES.length);
            assertThat(resource.getFilename()).isEqualTo("clip.webm");

            try (var stream = resource.getInputStream()) {
                assertThat(stream.skip(9)).isEqualTo(9);
                assertThat(range.get()).isNull();
                assertThat(stream.readAllBytes()).containsExactly("9ABCDEF".getBytes(StandardCharsets.US_ASCII));
            }
            assertThat(range.get()).isEqualTo("bytes=9-");

            assertThatThrownBy(() -> store.resource("playback/missing.webm"))
                    .isInstanceOfSatisfying(MediaStoreException.class, error -> assertThat(error.isMissing()).isTrue());
        } finally {
            peer.stop(0);
        }
    }

    private static void handle(HttpExchange exchange, AtomicReference<String> range) throws IOException {
        try (exchange) {
            var path = exchange.getRequestURI().getPath();
            if ("/test-media".equals(path) && "location=".equals(exchange.getRequestURI().getQuery())) {
                byte[] xml = "<LocationConstraint>us-east-1</LocationConstraint>".getBytes(StandardCharsets.US_ASCII);
                exchange.sendResponseHeaders(200, xml.length);
                exchange.getResponseBody().write(xml);
                return;
            }
            if (!"/test-media/playback/clip.webm".equals(path)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            var headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "video/webm");
            headers.set("ETag", "\"sample\"");
            headers.set("Last-Modified", "Tue, 20 Oct 2026 12:30:00 GMT");
            if ("HEAD".equals(exchange.getRequestMethod())) {
                headers.set("Content-Length", Integer.toString(BYTES.length));
                exchange.sendResponseHeaders(200, -1);
            } else {
                range.set(exchange.getRequestHeaders().getFirst("Range"));
                int start = range.get() == null ? 0 : Integer.parseInt(range.get().substring(6, range.get().length() - 1));
                if (start > 0) headers.set("Content-Range", "bytes " + start + "-" + (BYTES.length - 1) + "/" + BYTES.length);
                exchange.sendResponseHeaders(start == 0 ? 200 : 206, BYTES.length - start);
                exchange.getResponseBody().write(BYTES, start, BYTES.length - start);
            }
        }
    }
}
