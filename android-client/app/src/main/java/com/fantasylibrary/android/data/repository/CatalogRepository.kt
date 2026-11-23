package com.fantasylibrary.android.data.repository

import com.fantasylibrary.android.data.network.AnimeItemDto
import com.fantasylibrary.android.data.network.ApiClient
import com.fantasylibrary.android.data.network.CategoryDto
import com.fantasylibrary.android.data.network.EpisodeDto
import com.fantasylibrary.android.data.network.GameItemDto
import com.fantasylibrary.android.data.network.Outcome
import com.fantasylibrary.android.data.network.PageDto
import com.fantasylibrary.android.data.network.WorkDetailDto

/**
 * 真实馆藏数据：全部来自现有 Spring Boot + Oracle，通过 Retrofit 调用。
 * 不做本地缓存或假数据，保证测试版看到的就是数据库里的内容。
 */
class CatalogRepository(private val client: ApiClient) {

    suspend fun animePage(
        baseUrl: String,
        page: Int,
        size: Int,
        query: String? = null,
        category: String? = null,
    ): Outcome<PageDto<AnimeItemDto>> = callApi("读取动漫列表") {
        client.api(baseUrl).anime(page = page, size = size, query = query, category = category)
    }

    suspend fun gamesPage(
        baseUrl: String,
        page: Int,
        size: Int,
        query: String? = null,
        category: String? = null,
    ): Outcome<PageDto<GameItemDto>> = callApi("读取游戏列表") {
        client.api(baseUrl).games(page = page, size = size, query = query, category = category)
    }

    suspend fun categories(baseUrl: String): Outcome<List<CategoryDto>> = callApi("读取分类") {
        client.api(baseUrl).categories()
    }

    suspend fun workDetail(baseUrl: String, workId: Long): Outcome<WorkDetailDto> = callApi("读取作品详情") {
        client.api(baseUrl).work(workId)
    }

    suspend fun episodes(baseUrl: String, workId: Long): Outcome<List<EpisodeDto>> = callApi("读取分集") {
        client.api(baseUrl).episodes(workId, allNodes = true)
    }
}
