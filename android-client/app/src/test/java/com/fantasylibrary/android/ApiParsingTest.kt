package com.fantasylibrary.android

import com.fantasylibrary.android.data.network.AnimeItemDto
import com.fantasylibrary.android.data.network.ApiEnvelope
import com.fantasylibrary.android.data.network.EpisodeDto
import com.fantasylibrary.android.data.network.GameItemDto
import com.fantasylibrary.android.data.network.HealthDto
import com.fantasylibrary.android.data.network.PageDto
import com.fantasylibrary.android.data.network.WorkDetailDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 用「真实抓取下来的响应」验证 DTO 与后端的字段契约，
 * 这样即使 Android 端不连服务器也能立刻发现字段名对不上。
 * 期望字段全部来自 backend 源码与真实响应，不是猜的。
 */
class ApiParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun `health 不是 ApiResponse 包装`() {
        val dto = json.decodeFromString(HealthDto.serializer(), """{"application":"UP","database":"UP"}""")
        assertEquals("UP", dto.application)
        assertEquals("UP", dto.database)
    }

    @Test
    fun `anime 分页响应可解析`() {
        val payload = """
            {"code":"OK","message":"请求成功","data":{"items":[
              {"work":{"id":110,"type":"ANIME","slug":"overlord","title":"OVERLORD",
                "summary":"简介\n\n第二段","releaseDate":"2026-11-05",
                "category":{"id":16,"slug":"story","name":"叙事冒险","sortOrder":110},
                "cover":{"id":130,"kind":"COVER","url":"/api/media/catalog/uploads/665374d6-561b-435a-8c58-8be74428f614.jpg","altText":"1","sortOrder":0}},
               "anime":{"episodeCount":1,"studio":"KADOKAWA","airingStatus":"COMPLETED"},
               "tags":[{"id":2,"slug":"adventure","name":"冒险"},{"id":24,"slug":"magic","name":"魔法"}]},
              {"work":{"id":83,"type":"ANIME","slug":"djr","title":"电锯人","summary":"x","releaseDate":"2026-11-04",
                "category":{"id":16,"slug":"story","name":"叙事冒险","sortOrder":110},
                "cover":{"id":85,"kind":"COVER","url":"/api/media/catalog/uploads/3cd16862-db55-49e4-b5e0-bf6e4d61d6e3.png","altText":"1","sortOrder":0}},
               "anime":{"episodeCount":null,"studio":"未知","airingStatus":"COMPLETED"},
               "tags":[{"id":38,"slug":"action","name":"动作"}]}
            ],"page":1,"size":3,"total":5,"totalPages":2},"errors":[]}
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<PageDto<AnimeItemDto>>>(payload)
        assertTrue(envelope.isOk)
        val page = envelope.data!!
        assertEquals(5L, page.total)
        assertEquals(2L, page.totalPages)
        assertEquals(2, page.items.size)
        assertEquals(110L, page.items[0].work.id)
        assertEquals("叙事冒险", page.items[0].work.category?.name)
        assertEquals("/api/media/catalog/uploads/665374d6-561b-435a-8c58-8be74428f614.jpg", page.items[0].work.cover?.url)
        assertEquals(1, page.items[0].anime?.episodeCount)
        assertNull(page.items[1].anime?.episodeCount)
        assertEquals(listOf("冒险", "魔法"), page.items[0].tags.map { it.name })
    }

    @Test
    fun `games 响应包含平台信息`() {
        val payload = """
            {"code":"OK","message":"请求成功","data":{"items":[
              {"work":{"id":108,"type":"GAME","slug":"milkquest","title":"勇者闯魔穴",
                "summary":"简介","releaseDate":"2026-11-22",
                "category":{"id":16,"slug":"story","name":"叙事冒险","sortOrder":110},
                "cover":{"id":109,"kind":"COVER","url":"/api/media/catalog/uploads/861cbae6.jpg","altText":"1","sortOrder":0}},
               "game":{"developer":"未知","ageRating":"18+","platforms":[{"id":2,"slug":"pc","name":"PC"}]},
               "tags":[{"id":2,"slug":"adventure","name":"冒险"}]}
            ],"page":1,"size":2,"total":4,"totalPages":2}}
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<PageDto<GameItemDto>>>(payload)
        val game = envelope.data!!.items.single()
        assertEquals("18+", game.game?.ageRating)
        assertEquals("未知", game.game?.developer)
        assertEquals(listOf("PC"), game.game?.platforms?.map { it.name })
    }

    @Test
    fun `作品详情区分 anime 与 game 字段`() {
        val payload = """
            {"code":"OK","message":"请求成功","data":{
              "work":{"id":110,"type":"ANIME","slug":"overlord","title":"OVERLORD","summary":"简介",
                "releaseDate":"2026-11-05","category":{"id":16,"slug":"story","name":"叙事冒险","sortOrder":110},
                "cover":{"id":130,"kind":"COVER","url":"/api/media/catalog/uploads/a.jpg","altText":"1","sortOrder":0}},
              "tags":[{"id":2,"slug":"adventure","name":"冒险"}],
              "images":[{"id":130,"kind":"COVER","url":"/api/media/catalog/uploads/a.jpg","altText":"1","sortOrder":0}],
              "anime":{"episodeCount":1,"studio":"KADOKAWA","airingStatus":"COMPLETED"}}}
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<WorkDetailDto>>(payload)
        val detail = envelope.data!!
        assertEquals("KADOKAWA", detail.anime?.studio)
        assertNull(detail.game)
        assertEquals(1, detail.images.size)
    }

    @Test
    fun `分集响应带真实播放源`() {
        val payload = """
            {"code":"OK","message":"请求成功","data":[
              {"id":114,"edition":"EPISODE","number":1,"title":"1","durationSeconds":60,
               "sources":[{"id":116,"label":"1","quality":"1080p",
                 "url":"/api/media/playback/0290e37e-a88f-444f-89c9-b5333448fa6a.mp4","format":"MP4"}],
               "nodeId":104,"nodeLabel":"本地"}]}
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<List<EpisodeDto>>>(payload)
        val episode = envelope.data!!.single()
        assertEquals(1, episode.number)
        assertEquals(60, episode.durationSeconds)
        assertEquals("本地", episode.nodeLabel)
        assertEquals("/api/media/playback/0290e37e-a88f-444f-89c9-b5333448fa6a.mp4", episode.sources.single().url)
        assertEquals("1080p", episode.sources.single().quality)
    }

    @Test
    fun `错误响应能取到 code 与 message`() {
        val payload = """{"code":"LOGIN_REQUIRED","message":"请先登录","errors":[]}"""
        val envelope = json.decodeFromString<ApiEnvelope<WorkDetailDto>>(payload)
        assertEquals(false, envelope.isOk)
        assertEquals("LOGIN_REQUIRED", envelope.code)
        assertEquals("请先登录", envelope.message)
        assertNull(envelope.data)
    }

    @Test
    fun `未知字段不会导致解析失败`() {
        val payload = """{"application":"UP","database":"UP","futureField":{"a":1}}"""
        val dto = json.decodeFromString(HealthDto.serializer(), payload)
        assertNotNull(dto.application)
    }
}
