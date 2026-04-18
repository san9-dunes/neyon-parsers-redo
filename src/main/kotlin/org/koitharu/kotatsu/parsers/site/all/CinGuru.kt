package org.koitharu.kotatsu.parsers.site.all

import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.json.mapJSONToSet
import org.koitharu.kotatsu.parsers.util.nullIfEmpty
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.requireElementById
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy
import java.util.EnumSet

@MangaSourceParser("CINGURU", "Cin.guru", type = ContentType.HENTAI)
internal class CinGuru(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.CINGURU, pageSize = 25) {

	override val configKeyDomain = ConfigKey.Domain("cin.guru")

	private val buildId = suspendLazy(initializer = ::fetchBuildId)

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (page > 1) return emptyList()
		val data = fetchHomeData()
		val listKey = if (order == SortOrder.POPULARITY) "popular" else "all"
		val sourceArray = data.optJSONArray(listKey) ?: return emptyList()
		val query = filter.query?.trim()?.lowercase().orEmpty()
		return buildList(sourceArray.length()) {
			for (i in 0 until sourceArray.length()) {
				val jo = sourceArray.optJSONObject(i) ?: continue
				val manga = parseListItem(jo)
				if (query.isNotEmpty()) {
					val title = manga.title.lowercase()
					if (!title.contains(query)) continue
				}
				add(manga)
			}
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val id = manga.url.toLongOrNull() ?: return manga
		val data = fetchDetailsData(id)
		if (!data.optBoolean("ok", false)) return manga

		val uploadTime = data.optLong("upload_time")
		val uploadDate = when {
			uploadTime <= 0L -> 0L
			uploadTime >= 1_000_000_000_000L -> uploadTime
			else -> uploadTime * 1000L
		}

		val tags = data.optJSONArray("tags")?.mapJSONToSet { tag ->
			val key = tag.optString("slug").nullIfEmpty() ?: tag.optString("id")
			MangaTag(
				key = key,
				title = tag.optString("name").ifEmpty { key },
				source = source,
			)
		}.orEmpty()

		val authors = data.optJSONArray("tags")?.mapJSONToSetNotNull { tag ->
			when (tag.optString("type")) {
				"artist", "group", "circle" -> tag.optString("name").nullIfEmpty()
				else -> null
			}
		}.orEmpty()

		val title = data.optJSONObject("title")
			?.optString("pretty")
			?.nullIfEmpty() ?: manga.title

		val chapter = MangaChapter(
			id = generateUid(id),
			title = title,
			number = 1f,
			volume = 0,
			url = id.toString(),
			scanlator = data.optString("scanlator").nullIfEmpty(),
			uploadDate = uploadDate,
			branch = data.optString("lang").nullIfEmpty(),
			source = source,
		)

		return manga.copy(
			title = title,
			authors = authors,
			tags = tags,
			chapters = listOf(chapter),
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val id = chapter.url.toLongOrNull() ?: return emptyList()
		val data = fetchDetailsData(id)
		val images = data.optJSONArray("images") ?: return emptyList()
		return buildList(images.length()) {
			for (i in 0 until images.length()) {
				val url = images.optString(i).nullIfEmpty() ?: continue
				add(
					MangaPage(
						id = generateUid("$id:$i"),
						url = url,
						preview = null,
						source = source,
					),
				)
			}
		}
	}

	private suspend fun fetchBuildId(): String {
		val doc = webClient.httpGet("https://$domain/").parseHtml()
		val raw = doc.requireElementById("__NEXT_DATA__").data()
		return JSONObject(raw).getString("buildId")
	}

	private suspend fun fetchHomeData(): JSONObject {
		val url = "https://$domain/_next/data/${buildId.get()}/index.json"
		return webClient.httpGet(url).parseJson().getJSONObject("pageProps").getJSONObject("data")
	}

	private suspend fun fetchDetailsData(id: Long): JSONObject {
		val url = "https://$domain/_next/data/${buildId.get()}/v/$id.json"
		return webClient.httpGet(url).parseJson().getJSONObject("pageProps").getJSONObject("data")
	}

	private fun parseListItem(jo: JSONObject): Manga {
		val id = jo.optLong("id")
		val titleObj = jo.optJSONObject("title")
		val title = titleObj?.optString("pretty")?.nullIfEmpty()
			?: titleObj?.optString("english")?.nullIfEmpty()
			?: titleObj?.optString("japanese")?.nullIfEmpty()
			?: id.toString()
		val cover = jo.optJSONObject("cover")?.optString("t")?.nullIfEmpty()
		return Manga(
			id = generateUid(id),
			title = title,
			altTitles = emptySet(),
			url = id.toString(),
			publicUrl = "https://$domain/v/$id",
			rating = RATING_UNKNOWN,
			contentRating = ContentRating.ADULT,
			coverUrl = cover,
			largeCoverUrl = cover,
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			source = source,
		)
	}
}

private inline fun <T> org.json.JSONArray.mapJSONToSetNotNull(transform: (org.json.JSONObject) -> T?): Set<T> {
	val result = LinkedHashSet<T>(length())
	for (i in 0 until length()) {
		val jo = optJSONObject(i) ?: continue
		transform(jo)?.let(result::add)
	}
	return result
}
