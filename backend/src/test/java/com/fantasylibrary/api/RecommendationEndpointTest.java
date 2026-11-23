package com.fantasylibrary.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "library.oracle.jdbc-url=", "library.oracle.username=", "library.oracle.password="
})
@AutoConfigureMockMvc
class RecommendationEndpointTest {
    @Autowired private MockMvc mvc;

    @Test
    void rejectsUnsupportedTypesAndReportsAnUnavailableDatabase() throws Exception {
        mvc.perform(get("/api/recommendations").param("type", "invalid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/api/recommendations").param("type", "GAME"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE"));
    }

    @Test
    void playbackCatalogValidatesWorkIdAndReportsDatabaseUnavailability() throws Exception {
        mvc.perform(get("/api/works/0/episodes")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/api/works/1/episodes")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE"));
    }

    @Test
    void personalizedFeedRequiresAnAuthenticatedSession() throws Exception {
        mvc.perform(get("/api/me/recommendations").param("userId", "42"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
    }
}
