package com.fantasylibrary.catalog.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

public final class Catalog {

    private Catalog() {
    }

    public enum WorkType { ANIME, GAME }

    public enum AiringStatus { UPCOMING, ONGOING, COMPLETED }

    public enum ImageKind { COVER, GALLERY }

    public record Category(long id, String slug, String name, int sortOrder) {
    }

    public record Tag(long id, String slug, String name) {
    }

    public record Image(long id, ImageKind kind, String url, String altText, int sortOrder) {
    }

    public record Platform(long id, String slug, String name) {
    }

    public record Work(long id, WorkType type, String slug, String title, String summary,
                       LocalDate releaseDate, Category category, Image cover) {
    }

    public record AnimeInfo(Integer episodeCount, String studio, AiringStatus airingStatus) {
    }

    public record GameInfo(String developer, String ageRating, List<Platform> platforms) {
    }

    public record AnimeItem(Work work, AnimeInfo anime, List<Tag> tags) {
    }

    public record GameItem(Work work, GameInfo game, List<Tag> tags) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkDetail(Work work, List<Tag> tags, List<Image> images,
                             AnimeInfo anime, GameInfo game) {
    }

    public record Page<T>(List<T> items, int page, int size, long total, long totalPages) {
        public static <T> Page<T> of(List<T> items, int page, int size, long total) {
            return new Page<>(items, page, size, total, Math.ceilDiv(total, size));
        }
    }
}
