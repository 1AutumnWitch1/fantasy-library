package com.fantasylibrary.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "library.oracle.jdbc-url=", "library.oracle.username=", "library.oracle.password=",
        "library.auth.secure-cookie=true"
})
@AutoConfigureMockMvc
class AccountSecurityTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void privateReadsRequireAuthenticationEvenWithAnInvalidCookie() throws Exception {
        mvc.perform(get("/api/me/profile")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
        mvc.perform(get("/api/me/collection").cookie(new Cookie("fantasy_library_session", "invalid")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void privateLibraryRequiresAnAccountAndProtectsReadReceipts() throws Exception {
        mvc.perform(get("/api/me/reading-history").param("userId", "1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me/inbox")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/me/inbox/1/read")).andExpect(status().isForbidden());
        mvc.perform(put("/api/me/inbox/read")).andExpect(status().isForbidden());
    }

    @Test
    void administratorCsrfUsesItsOwnCookieAndRejectsPublicProof() throws Exception {
        var publicResponse = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andReturn().getResponse();
        var adminResponse = mvc.perform(get("/api/admin/auth/csrf")).andExpect(status().isOk())
                .andReturn().getResponse();
        var publicProof = json.readTree(publicResponse.getContentAsString()).path("data");
        var adminProof = json.readTree(adminResponse.getContentAsString()).path("data");
        var adminCookie = adminResponse.getCookie("FANTASY-ADMIN-XSRF");
        assertThat(adminCookie).isNotNull();
        assertThat(adminCookie.getPath()).isEqualTo("/api/admin");
        assertThat(adminCookie.isHttpOnly()).isTrue();
        assertThat(adminCookie.getSecure()).isTrue();
        assertThat(adminResponse.getCookie("XSRF-TOKEN")).isNull();
        assertThat(adminProof.path("headerName").asText()).isEqualTo("X-Admin-CSRF-Token");

        mvc.perform(post("/api/admin/auth/login")
                        .cookie(publicResponse.getCookie("XSRF-TOKEN"))
                        .header(publicProof.path("headerName").asText(), publicProof.path("token").asText()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/auth/profile")
                        .cookie(new Cookie("fantasy_library_session", "a".repeat(43))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unsafeRequestsRequireCsrfAndCsrfDoesNotGrantIdentity() throws Exception {
        mvc.perform(post("/api/auth/login"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        var response = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andReturn().getResponse();
        var proof = json.readTree(response.getContentAsString()).path("data");
        var cookie = response.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(proof.path("token").asText()).isNotBlank().isNotEqualTo(cookie.getValue());
        mvc.perform(delete("/api/me/collection/1")
                        .cookie(cookie).header(proof.path("headerName").asText(), proof.path("token").asText()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
    }
}
