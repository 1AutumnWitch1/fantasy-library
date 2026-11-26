package com.fantasylibrary.playback;

import com.fantasylibrary.playback.PlaybackModels.Edition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;

public final class ViewingModels {
    private ViewingModels() {}

    public static final long MAX_DURATION = 86_400_000;
    public record Start(@Positive long sourceId, @Min(0) @Max(MAX_DURATION) long positionMs) {}
    public record Sample(@Min(0) @Max(MAX_DURATION) long positionMs,
                         @Positive @Max(MAX_DURATION) long durationMs, boolean playing, boolean discontinuity) {}
    public record Source(long workId, long episodeId, Edition edition, int episodeNo, int durationSeconds, String url) {}
    public record Session(String id, long sourceId, long lastPositionMs, OffsetDateTime lastSampleAt,
                          long watchedMs, boolean settled) {}
    public record Started(String id) {}
    public record Stats(long followers, long plays) {}
    public record SavedEpisode(int episodeNo, long positionMs, long durationMs, boolean completed, OffsetDateTime updatedAt) {}
    public record Progress(SavedEpisode latest, int completedEpisodes) {}
    public record Recording(String coverage, boolean completed) {}
}
