package com.fantasylibrary.catalog.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogImagePathsTest {
    @Test
    void mapsCatalogImagesToTheirPrivateObjectKeys() {
        assertThat(CatalogImagePaths.objectKey("/api/media/catalog/mist-harbor.webp"))
                .isEqualTo("catalog/mist-harbor.webp");
        assertThat(CatalogImagePaths.objectKey("/api/media/catalog/editions/modern-1.webp"))
                .isEqualTo("catalog/editions/modern-1.webp");
        assertThat(CatalogImagePaths.objectKey("/api/media/catalog/uploads/saved-cover.png"))
                .isEqualTo("catalog/uploads/saved-cover.png");
    }

    @Test
    void mapsOnlyAllowedFrontendAssetDirectories() {
        assertThat(CatalogImagePaths.siteObjectKey("/api/media/site/shelf/walnut-grain.webp"))
                .isEqualTo("site/shelf/walnut-grain.webp");
        assertThat(CatalogImagePaths.siteObjectKey("/api/media/site/catalog/editions/modern-1.webp"))
                .isEqualTo("site/catalog/editions/modern-1.webp");
        assertThat(CatalogImagePaths.siteObjectKey("/api/media/site/community/forbidden.png")).isNull();
        assertThat(CatalogImagePaths.siteObjectKey("/api/media/site/account/../reader/secret.png")).isNull();
    }

    @Test
    void rejectsTraversalAndNonImageObjects() {
        assertThat(CatalogImagePaths.objectKey("/api/media/catalog/editions/../../secret.png")).isNull();
        assertThat(CatalogImagePaths.objectKey("/api/media/catalog/%2e%2e/secret.png")).isNull();
        assertThat(CatalogImagePaths.objectKey("/api/media/catalog/uploads/page.svg")).isNull();
        assertThat(CatalogImagePaths.objectKey("/api/media/catalog/game.png?version=1")).isNull();
    }
}
