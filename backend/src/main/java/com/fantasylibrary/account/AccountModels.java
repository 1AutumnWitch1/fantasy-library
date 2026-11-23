package com.fantasylibrary.account;

import com.fantasylibrary.catalog.model.Catalog.WorkType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AccountModels {
    private AccountModels() {
    }

    public record Registration(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,40}", message = "账号名须为 3–40 位字母、数字或下划线") String username,
            @NotBlank @Size(min = 8, max = 72, message = "密码至少需要 8 个字符") String password,
            @NotBlank @Size(max = 60, message = "昵称最多 60 个字符") String displayName) {
    }

    public record Credentials(
            @NotBlank @Size(max = 40) String username,
            @NotBlank @Size(max = 72) String password) {
    }

    public record Profile(long id, String username, String displayName, String bio, String accountRole, String avatarKey) {
    }

    public record ProfileUpdate(
            @NotBlank @Size(max = 60, message = "昵称最多 60 个字符") String displayName,
            @Size(max = 240, message = "个人简介最多 240 个字符") String bio) {
    }

    public enum WatchStatus { PLAN_TO_WATCH, WATCHING, COMPLETED, ON_HOLD }

    public record SaveWork(
            @NotNull Boolean favorite, @NotNull Boolean wishlist, WatchStatus watchStatus,
            @NotNull @Min(value = 0, message = "观看集数不能小于 0")
            @Max(value = 99999, message = "观看集数最多 99999") Integer watchedEpisodes) {
    }

    public record SavedWork(long workId, WorkType workType, String title, String coverUrl, String coverAlt,
                            Integer episodeCount, int favorite, int wishlist, WatchStatus watchStatus,
                            int watchedEpisodes) {
    }

    public record Entry(SavedWork item) {
    }

    public record CsrfProof(String headerName, String token) {
    }
}
