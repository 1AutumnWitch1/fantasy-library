package com.fantasylibrary.android.ui.screens.catalog

import com.fantasylibrary.android.data.network.AnimeItemDto
import com.fantasylibrary.android.data.network.GameItemDto
import com.fantasylibrary.android.data.network.MediaUrls

enum class CatalogKind(val route: String, val title: String, val subtitle: String) {
    ANIME("anime", "动漫资料库", "Oracle 中的真实动漫馆藏"),
    GAME("games", "游戏资料库", "Oracle 中的真实游戏档案"),
}

/** 列表统一的展示模型，动漫与游戏共用一套卡片样式。 */
data class CatalogCard(
    val id: Long,
    val title: String,
    val summary: String,
    val category: String,
    val cover: MediaUrls.Check,
    val tags: List<String>,
    val badge: String,
    val metaLine: String,
    val kind: CatalogKind,
)

object CatalogMapper {

    fun airingLabel(raw: String?): String = when (raw?.uppercase()) {
        "ONGOING" -> "连载中"
        "COMPLETED" -> "已完结"
        "UPCOMING" -> "待播出"
        else -> ""
    }

    fun animeCard(dto: AnimeItemDto, baseUrl: String, allowCleartext: Boolean): CatalogCard {
        val work = dto.work
        val anime = dto.anime
        val meta = buildList {
            anime?.studio?.takeIf { it.isNotBlank() }?.let { add(it) }
            anime?.episodeCount?.let { add("全 $it 集") }
            work.releaseDate?.take(4)?.let { add(it) }
        }.joinToString(" · ")
        return CatalogCard(
            id = work.id,
            title = work.title.ifBlank { "未命名作品 #${work.id}" },
            summary = work.summary.orEmpty().replace("\r", ""),
            category = work.category?.name.orEmpty(),
            cover = MediaUrls.resolve(work.cover?.url, baseUrl, allowCleartext),
            tags = dto.tags.map { it.name },
            badge = airingLabel(anime?.airingStatus),
            metaLine = meta,
            kind = CatalogKind.ANIME,
        )
    }

    fun gameCard(dto: GameItemDto, baseUrl: String, allowCleartext: Boolean): CatalogCard {
        val work = dto.work
        val game = dto.game
        val platforms = game?.platforms?.map { it.name }?.filter { it.isNotBlank() }.orEmpty()
        val meta = buildList {
            game?.developer?.takeIf { it.isNotBlank() }?.let { add(it) }
            if (platforms.isNotEmpty()) add(platforms.joinToString(" / "))
            work.releaseDate?.take(4)?.let { add(it) }
        }.joinToString(" · ")
        return CatalogCard(
            id = work.id,
            title = work.title.ifBlank { "未命名作品 #${work.id}" },
            summary = work.summary.orEmpty().replace("\r", ""),
            category = work.category?.name.orEmpty(),
            cover = MediaUrls.resolve(work.cover?.url, baseUrl, allowCleartext),
            tags = dto.tags.map { it.name },
            badge = game?.ageRating.orEmpty(),
            metaLine = meta,
            kind = CatalogKind.GAME,
        )
    }
}
