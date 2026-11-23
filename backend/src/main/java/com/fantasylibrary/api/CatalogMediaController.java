package com.fantasylibrary.api;

import com.fantasylibrary.catalog.model.CatalogImagePaths;
import com.fantasylibrary.media.MediaStoreException;
import com.fantasylibrary.media.ObjectMediaStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "library.media.enabled", havingValue = "true")
public class CatalogMediaController {
    private final ObjectMediaStore storage;

    public CatalogMediaController(ObjectMediaStore storage) {
        this.storage = storage;
    }

    @GetMapping("/api/media/catalog/{*suffix}")
    public ResponseEntity<Resource> catalog(@PathVariable String suffix) {
        return image(CatalogImagePaths.objectKey("/api/media/catalog" + suffix));
    }

    @GetMapping("/api/media/site/{*suffix}")
    public ResponseEntity<Resource> site(@PathVariable String suffix) {
        return image(CatalogImagePaths.siteObjectKey("/api/media/site" + suffix));
    }

    private ResponseEntity<Resource> image(String key) {
        if (key == null) throw new MediaStoreException(true, null);
        String extension = key.substring(key.lastIndexOf('.') + 1);
        String contentType = "image/" + ("jpg".equals(extension) ? "jpeg" : extension);
        return ResponseEntity.ok().cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(contentType))
                .header("X-Content-Type-Options", "nosniff")
                .body(storage.resource(key));
    }
}
