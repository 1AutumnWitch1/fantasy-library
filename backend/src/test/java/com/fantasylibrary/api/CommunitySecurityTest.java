package com.fantasylibrary.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "library.oracle.jdbc-url=", "library.oracle.username=", "library.oracle.password="
})
@AutoConfigureMockMvc
class CommunitySecurityTest {
    @Autowired private MockMvc mvc;

    @Test
    void scopesPrivateReadsToTheSessionAndProtectsWritesWithCsrf() throws Exception {
        mvc.perform(get("/api/admin/community/reports")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me/community/posts/1/like")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/me/community/reports")).andExpect(status().isForbidden());
        mvc.perform(post("/api/me/works/1/comments")).andExpect(status().isForbidden());
        mvc.perform(put("/api/me/works/1/comments/2/like")).andExpect(status().isForbidden());
        mvc.perform(get("/api/me/works/1/comment-threads")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me/works/1/comments/2/replies")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectsLibraryManagementRoutes() throws Exception {
        for (String route : new String[]{"works", "forum", "posts", "comments", "members", "works/1/nodes", "work-options", "works/1/details", "works/1/playback"}) {
            mvc.perform(get("/api/admin/library/" + route))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
        }
        mvc.perform(put("/api/admin/library/works/1/publication")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/library/nodes/PLAYBACK/1/publication")).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/library/works")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/library/works/1/details")).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/library/media/catalog")).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/library/media/playback")).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/library/works/1/playback/episodes")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/library/forum")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/library/comments/1")).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/library/members")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/library/members/1")).andExpect(status().isForbidden());
    }

    @Test
    void validatesPublicPaginationAndFilterArgumentsBeforeQueryingOracle() throws Exception {
        mvc.perform(get("/api/community/posts").param("workId", "0").param("size", "31"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/api/community/posts").param("topic", "invalid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/api/works/1/comment-threads").param("page", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/api/works/1/comments/0/replies"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/api/works/1/comments").param("size", "31"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/api/works/1/comments")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE"));
        mvc.perform(get("/api/community/posts")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE"));
    }
}
