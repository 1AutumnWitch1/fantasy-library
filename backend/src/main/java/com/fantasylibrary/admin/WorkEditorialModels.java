package com.fantasylibrary.admin;

import com.fantasylibrary.catalog.model.Catalog.WorkType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public final class WorkEditorialModels {
    private WorkEditorialModels() {}

    public enum AiringStatus { UPCOMING, ONGOING, COMPLETED }

    public record Image(@NotBlank @Size(max = 500) String url, @NotBlank @Size(max = 180) String alt) {}
    public record Anime(@Size(max = 150) @NotBlank String studio, @NotNull AiringStatus airingStatus,
                        @Positive @Max(99999) Integer episodeCount) {}
    public record Game(@Size(max = 150) @NotBlank String developer, @Size(max = 20) String ageRating,
                       @NotEmpty @Size(max = 32) List<@NotNull @Positive Long> platformIds) {}
    public record Draft(@NotNull WorkType type,
                        @NotBlank @Size(max = 90) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
                        @NotBlank @Size(max = 180) String title, @NotBlank @Size(max = 900) String summary,
                        LocalDate releaseDate, @Positive long categoryId,
                        @NotNull @Size(max = 100) List<@NotNull @Positive Long> tagIds,
                        @Valid Anime anime, @Valid Game game, @Valid Image cover,
                        @NotNull @Size(max = 8) List<@NotNull @Valid Image> gallery) {}
    public record Entry(long id, WorkType type, String slug, String title, String summary, LocalDate releaseDate,
                        long categoryId, boolean listed) {}
    public record GameRow(String developer, String ageRating) {}
    public record Archive(Entry entry, List<Long> tagIds, Anime anime, Game game, Image cover, List<Image> gallery) {}
    public record Option(long id, String name) {}
    public record Options(List<Option> categories, List<Option> tags, List<Option> platforms) {}
    public record WorkRemoval(int queuedFiles, int sharedFiles) {}

    public record Created(long id) {}
}
