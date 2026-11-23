package com.fantasylibrary.download;

public record GameDownload(long id, String label, String platform, String url, boolean external) {
}
