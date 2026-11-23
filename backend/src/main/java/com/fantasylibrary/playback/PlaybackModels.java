package com.fantasylibrary.playback;

import java.util.List;
import org.apache.ibatis.annotations.AutomapConstructor;

public final class PlaybackModels {
    private PlaybackModels() {
    }

    public enum Edition { EPISODE, PREVIEW }
    public enum Format { MP4, WEBM }

    public record EpisodeRow(long id, Edition edition, int number, String title, int durationSeconds,
                             long nodeId, String nodeLabel, boolean nodeListed) {
        @AutomapConstructor public EpisodeRow {}

        public EpisodeRow(long id, Edition edition, int number, String title, int durationSeconds) {
            this(id, edition, number, title, durationSeconds, 0, "", true);
        }
    }
    public record SourceRow(long episodeId, long id, String label, String quality, String url, Format format) {}
    public record Source(long id, String label, String quality, String url, Format format) {}

    public record Episode(long id, Edition edition, int number, String title, int durationSeconds,
                          List<Source> sources, long nodeId, String nodeLabel) {}
}
