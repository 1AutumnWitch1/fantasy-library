package com.fantasylibrary.api;

import com.fantasylibrary.persistence.mapper.CatalogMapper;
import com.fantasylibrary.support.CatalogStub;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "library.oracle.jdbc-url=",
        "library.oracle.username=",
        "library.oracle.password="
})
@AutoConfigureMockMvc
@Import(CatalogEndpointTest.MapperConfiguration.class)
class CatalogEndpointTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsCategoriesAndAppliesPaginationAndSearchBinding() throws Exception {
        mvc.perform(get("/api/tags").param("type", "ANIME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("mystery"));
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].slug").value("fantasy"))
                .andExpect(jsonPath("$.errors").doesNotExist());
        mvc.perform(get("/api/anime").param("page", "2").param("size", "1").param("category", "fantasy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items[0].work.releaseDate").value("2026-06-21"))
                .andExpect(jsonPath("$.data.items[0].work.title").value("星庭遗录"));
        mvc.perform(get("/api/anime").param("q", "  雾港  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].work.title").value("雾港来信"))
                .andExpect(jsonPath("$.data.items[0].tags[0].slug").value("mystery"));
    }

    @Test
    void requiresAllSelectedTagsAndAcceptsTheLegacySingleTag() throws Exception {
        mvc.perform(get("/api/anime").param("tag", "mystery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
        mvc.perform(get("/api/anime").param("tags", "mystery", "detective"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].work.id").value(11));
        mvc.perform(get("/api/anime").param("tag", "mystery").param("tags", "mystery", "detective", "detective"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/anime").param("tags", "mystery", "unlisted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/anime").param("tags", "mystery", "-bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void returnsTypedGameAndAnimeDetailsWithRelatedResources() throws Exception {
        mvc.perform(get("/api/games").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].game.platforms[1].slug").value("ps5"));
        mvc.perform(get("/api/works/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.work.type").value("ANIME"))
                .andExpect(jsonPath("$.data.anime.episodeCount").value(12))
                .andExpect(jsonPath("$.data.game").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.data.tags[0].slug").value("mystery"))
                .andExpect(jsonPath("$.data.images[0].kind").value("COVER"));
        mvc.perform(get("/api/works/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.work.type").value("GAME"))
                .andExpect(jsonPath("$.data.anime").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.data.game.developer").value("游戏工坊"))
                .andExpect(jsonPath("$.data.game.platforms[0].slug").value("pc"));
    }

    @Test
    void reportsPaginationFiltersAndIdValidationInTheSameEnvelope() throws Exception {
        mvc.perform(get("/api/anime").param("page", "0").param("size", "101")
                        .param("category", "Invalid slug").param("tag", "-wrong").param("q", "字".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("page", "size", "category", "tag", "q")));
        mvc.perform(get("/api/games").param("page", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));
        for (String id : new String[]{"0", "-1", "not-an-id", "999999999999999999999"}) {
            mvc.perform(get("/api/works/{id}", id))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                    .andExpect(jsonPath("$.errors[0].field").value("id"));
        }
    }

    @Test
    void keepsHttpStatusAndDoesNotExposeInternalErrorDetails() throws Exception {
        mvc.perform(get("/api/works/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORK_NOT_FOUND"));
        mvc.perform(get("/api/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"));
        mvc.perform(post("/api/games"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mvc.perform(get("/api/works/503"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE"))
                .andExpect(content().string(not(containsString("private"))));
        mvc.perform(get("/api/works/942"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CATALOG_NOT_INITIALIZED"))
                .andExpect(content().string(not(containsString("private"))));
        mvc.perform(get("/api/works/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("private"))));
    }

    @Test
    void servesOpenApiContractAndLocalArtwork() throws Exception {
        var result = mvc.perform(get("/api/openapi.yaml")).andExpect(status().isOk()).andReturn();
        var yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<?, ?> document = yaml.load(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(document.get("openapi")).isEqualTo("3.0.3");
        Map<?, ?> paths = (Map<?, ?>) document.get("paths");
        assertThat(Set.copyOf(paths.keySet())).isEqualTo(Set.of(
                "/api/categories", "/api/tags", "/api/anime", "/api/games", "/api/works/{id}",
                "/api/auth/csrf", "/api/auth/register", "/api/auth/login", "/api/auth/logout",
                "/api/admin/auth/csrf", "/api/admin/auth/login", "/api/admin/auth/logout", "/api/admin/auth/profile",
                "/api/me/profile", "/api/me/collection", "/api/me/collection/{workId}",
                "/api/community/posts",
                "/api/community/posts/{id}",
                "/api/community/posts/{id}/comments", "/api/works/{id}/comments", "/api/me/works/{id}/comments",
                "/api/works/{id}/comment-threads", "/api/me/works/{id}/comment-threads",
                "/api/works/{id}/comments/{root}/replies", "/api/me/works/{id}/comments/{root}/replies",
                "/api/me/works/{id}/comments/{comment}/like",
                "/api/community/media/{key}",
                "/api/me/community/posts",
                "/api/me/community/posts/{id}",
                "/api/me/community/posts/{id}/comments",
                "/api/me/community/posts/{id}/like",
                "/api/me/community/reports",
                "/api/admin/community/reports",
                "/api/admin/community/reports/{id}", "/api/avatars/{key}", "/api/me/avatar",
                "/api/recommendations", "/api/me/recommendations", "/api/me/reading-history", "/api/me/inbox",
                "/api/me/inbox/{id}/read", "/api/me/inbox/read", "/api/works/{id}/episodes", "/api/works/{id}/downloads", "/api/game-downloads/{id}/file",
                "/api/media/catalog/{suffix}",
                "/api/media/site/{suffix}",
                "/api/admin/library/media/catalog",
                "/api/admin/library/media/playback",
                "/api/admin/library/works/{workId}/playback",
                "/api/admin/library/works/{workId}/playback/episodes",
                "/api/admin/library/works/{workId}/playback/episodes/{episodeId}",
                "/api/admin/library/works/{workId}/playback/episodes/{episodeId}/sources",
                "/api/admin/library/works/{workId}/playback/episodes/{episodeId}/sources/{sourceId}",
                "/api/media/playback/{fileName}", "/api/admin/library/works", "/api/admin/library/works/{id}/nodes",
                "/api/admin/library/works/{id}/publication", "/api/admin/library/nodes/{kind}/{id}/publication",
                "/api/admin/library/work-options", "/api/admin/library/works/{id}/details", "/api/admin/library/works/{id}",
                "/api/admin/library/forum", "/api/admin/library/posts", "/api/admin/library/posts/{id}/publication",
                "/api/admin/library/comments", "/api/admin/library/comments/{id}",
                "/api/admin/library/members", "/api/admin/library/members/{id}"));
        checkReferences(document, document);

        var image = mvc.perform(get("/api/media/catalog/mist-harbor.webp"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("image/webp")))
                .andReturn();
        assertThat(image.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    private static void checkReferences(Object node, Map<?, ?> document) {
        if (node instanceof Map<?, ?> map) {
            if (map.get("$ref") instanceof String reference) {
                Object target = document;
                assertThat(reference).startsWith("#/");
                for (String name : reference.substring(2).split("/")) {
                    target = ((Map<?, ?>) target).get(name);
                    assertThat(target).as(reference).isNotNull();
                }
            }
            map.values().forEach(value -> checkReferences(value, document));
        } else if (node instanceof Iterable<?> list) {
            list.forEach(value -> checkReferences(value, document));
        }
    }

    @TestConfiguration
    static class MapperConfiguration {
        @Bean
        CatalogMapper catalogMapper() {
            return new CatalogStub();
        }
    }
}
