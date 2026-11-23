package com.fantasylibrary.download;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class DownloadEditorialModels {
    private DownloadEditorialModels() {}

    public record Draft(@Positive long platformId, @NotBlank @Size(max = 100) String label,
                        @Size(max = 1000) String externalUrl, @Size(max = 180) String fileName,
                        boolean listed) {}
    public record Platform(long id, String name) {}
    public record Entry(long id, long platformId, String label, String externalUrl, String fileName, boolean listed) {}
    public record Archive(List<Platform> platforms, List<Entry> nodes) {}
}
