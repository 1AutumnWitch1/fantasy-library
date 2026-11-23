package com.fantasylibrary.api;

import com.fantasylibrary.media.AdminMediaService;
import com.fantasylibrary.media.ObjectMediaStore;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminMediaEndpointTest {
    @Test
    void disabledStorageReturns503ForBothUploadKinds() throws Exception {
        var service = new AdminMediaService(new DisabledStore());
        var mvc = MockMvcBuilders.standaloneSetup(new AdminMediaController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();

        mvc.perform(multipart("/api/admin/library/media/catalog")
                        .file(new MockMultipartFile("file", "sample.png", "image/png", new byte[]{1})))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("MEDIA_STORAGE_UNAVAILABLE"));

        mvc.perform(post("/api/admin/library/media/playback").contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(new byte[]{1}))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("MEDIA_STORAGE_UNAVAILABLE"));
    }

    private static final class DisabledStore implements ObjectMediaStore {
        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public void put(String key, Path file, String contentType) {
            throw new AssertionError("A disabled store must not write files");
        }

        @Override public void put(String key, java.io.InputStream source, long size, String type) { throw new AssertionError(); }

        @Override
        public Resource resource(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String key) {
            throw new UnsupportedOperationException();
        }
    }
}
