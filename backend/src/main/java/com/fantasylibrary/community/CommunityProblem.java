package com.fantasylibrary.community;

import com.fantasylibrary.api.ApiResponse.Code;
import org.springframework.http.HttpStatus;

public final class CommunityProblem extends RuntimeException {
    private final HttpStatus status;
    private final Code code;

    private CommunityProblem(HttpStatus status, Code code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus status() { return status; }
    public Code code() { return code; }

    public static CommunityProblem invalid(String message) {
        return new CommunityProblem(HttpStatus.BAD_REQUEST, Code.INVALID_ARGUMENT, message);
    }
    public static CommunityProblem notFound() {
        return new CommunityProblem(HttpStatus.NOT_FOUND, Code.COMMUNITY_NOT_FOUND, "该帖子、评论或图片已不可见");
    }
    public static CommunityProblem forbidden() {
        return new CommunityProblem(HttpStatus.FORBIDDEN, Code.FORBIDDEN, "只有作者可以编辑或删除自己的帖子");
    }
    public static CommunityProblem unavailable() {
        return new CommunityProblem(HttpStatus.SERVICE_UNAVAILABLE, Code.DATABASE_UNAVAILABLE, "社区数据库暂不可用");
    }
    public static CommunityProblem closed() {
        return new CommunityProblem(HttpStatus.SERVICE_UNAVAILABLE, Code.COMMUNITY_CLOSED, "社区论坛暂停开放");
    }
    public static CommunityProblem imageRuntime() {
        return new CommunityProblem(HttpStatus.SERVICE_UNAVAILABLE, Code.COMMUNITY_STORAGE_UNAVAILABLE,
                "后端缺少图片处理组件，请使用完整的 JDK 21");
    }
    public static CommunityProblem storage() {
        return new CommunityProblem(HttpStatus.SERVICE_UNAVAILABLE, Code.COMMUNITY_STORAGE_UNAVAILABLE, "图片存储暂不可用");
    }
}
