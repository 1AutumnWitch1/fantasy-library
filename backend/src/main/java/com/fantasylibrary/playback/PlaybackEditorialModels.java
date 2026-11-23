package com.fantasylibrary.playback;

import com.fantasylibrary.playback.PlaybackModels.Edition;
import com.fantasylibrary.playback.PlaybackModels.Format;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class PlaybackEditorialModels {
    private PlaybackEditorialModels() {}

    public record NodeDraft(@NotBlank @Size(max = 100) String label, boolean listed) {}
    public record Node(long id, String label, boolean listed) {}

    public record EpisodeDraft(@NotNull Edition edition, @Positive @Max(999999) int number,
                               @NotBlank @Size(max = 180) String title,
                               @Positive @Max(9999999) int durationSeconds, @Positive Long nodeId) {}

    public record SourceDraft(@NotBlank @Size(max = 100) String label, @NotBlank @Size(max = 40) String quality,
                              @NotBlank @Size(max = 1000) String url, @NotNull Format format) {}

    public record SourceRow(long episodeId, long id, String label, String quality, String url, Format format,
                            boolean listed) {}

    public record Source(long id, String label, String quality, String url, Format format, boolean listed) {}

    public record Episode(long id, Edition edition, int number, String title, int durationSeconds,
                          List<Source> sources, long nodeId) {}
}
