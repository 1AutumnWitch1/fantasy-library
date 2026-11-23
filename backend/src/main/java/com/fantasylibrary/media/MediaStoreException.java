package com.fantasylibrary.media;

public final class MediaStoreException extends RuntimeException {
    private final boolean missing;

    public MediaStoreException(boolean missing, Throwable cause) {
        super(missing ? "对象存储中没有该资源" : "媒体存储未启用或暂不可用", cause);
        this.missing = missing;
    }

    public boolean isMissing() {
        return missing;
    }
}
