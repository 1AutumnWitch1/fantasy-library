package com.fantasylibrary.catalog.model;

import java.util.regex.Pattern;

public final class CatalogImagePaths {
    private static final String CATALOG_PREFIX = "/api/media/catalog/";
    private static final String SITE_PREFIX = "/api/media/site/";
    private static final String IMAGE_FILE = "[A-Za-z0-9_-]+\\.(?:webp|jpe?g|png|avif|gif)";
    private static final Pattern CATALOG_PATH = Pattern.compile("(?:(?:editions|uploads)/)?" + IMAGE_FILE);
    private static final Pattern SITE_PATH = Pattern.compile("(?:account/|shelf/|reader/|catalog/(?:editions/)?)" + IMAGE_FILE);

    private CatalogImagePaths() {}

    public static String objectKey(String path) {
        return objectKey(path, CATALOG_PREFIX, CATALOG_PATH, "catalog/");
    }

    public static String siteObjectKey(String path) {
        return objectKey(path, SITE_PREFIX, SITE_PATH, "site/");
    }

    private static String objectKey(String path, String prefix, Pattern allowed, String storagePrefix) {
        if (path == null || !path.startsWith(prefix)) return null;
        String suffix = path.substring(prefix.length());
        return allowed.matcher(suffix).matches() ? storagePrefix + suffix : null;
    }
}
