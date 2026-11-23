package com.fantasylibrary.download;

import java.util.UUID;
import java.util.regex.Pattern;

public final class GameDownloadFiles {
    private static final String PREFIX = "game-downloads/";
    private static final Pattern LEGACY_FILE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,179}");
    private static final Pattern OBJECT_KEY = Pattern.compile(
            PREFIX + "[0-9a-f-]{36}/[\\p{L}\\p{N}][\\p{L}\\p{N}\\p{M} ._()\\[\\]-]{0,119}");

    private GameDownloadFiles() {}

    public static boolean isLegacyFile(String value) {
        return value != null && LEGACY_FILE.matcher(value).matches();
    }

    public static boolean isObjectKey(String value) {
        return value != null && OBJECT_KEY.matcher(value).matches();
    }

    public static String newObjectKey(String fileName) {
        String safeName = fileName.strip().replaceAll("[^\\p{L}\\p{N}\\p{M} ._()\\[\\]-]", "_");
        if (safeName.isEmpty() || !Character.isLetterOrDigit(safeName.codePointAt(0))) {
            safeName = "download" + safeName;
        }
        if (safeName.length() > 120 || safeName.endsWith(".")) return null;
        return PREFIX + UUID.randomUUID() + "/" + safeName;
    }

    public static String attachmentName(String key) {
        return key.substring(key.lastIndexOf('/') + 1);
    }
}
