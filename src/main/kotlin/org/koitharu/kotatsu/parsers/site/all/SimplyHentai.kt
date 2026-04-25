package org.koitharu.kotatsu.parsers.site.all

import okhttp3.Headers
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNull
import org.koitharu.kotatsu.parsers.util.json.mapJSONToSet
import java.text.SimpleDateFormat
import java.util.*

@MangaSourceParser("SIMPLYHENTAI", "Simply-Hentai", type = ContentType.HENTAI)
internal class SimplyHentai(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.valueOf("SIMPLYHENTAI"), pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("simply-hentai.com")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("Referer", "https://www.simply-hentai.com/")
		.build()

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.NEWEST, SortOrder.POPULARITY)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = true)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val apiUrl = if (!filter.query.isNullOrEmpty()) {
			"https://api.simply-hentai.com/v3/search/complex?query=${filter.query.urlEncoded()}&page=${page + 1}"
		} else {
			val sort = if (order == SortOrder.NEWEST) "newest" else "popular"
			"https://api.simply-hentai.com/v3/tag/english?type=language&page=${page + 1}&sort=$sort"
		}

		val json = webClient.httpGet(apiUrl).parseJson()
		val data = json.optJSONObject("data") ?: json
		val albums = data.optJSONArray("albums") ?: data.optJSONArray("data") ?: return emptyList()

		return albums.mapJSONNotNull { item ->
			val obj = item.optJSONObject("object") ?: item
			val path = obj.getString("path")
			val slug = path.substringAfterLast("/")
			
			Manga(
				id = generateUid(slug),
				title = obj.getString("title"),
				altTitles = emptySet(),
				url = slug,
				publicUrl = "https://www.simply-hentai.com$path",
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = obj.optJSONObject("preview")?.optJSONObject("sizes")?.getStringOrNull("thumb"),
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val apiUrl = "https://api.simply-hentai.com/v3/manga/${manga.url}"
		val response = webClient.httpGet(apiUrl).parseJson()
		val album = response.getJSONObject("data")

		val tags = album.optJSONArray("tags")?.mapJSONToSet {
			MangaTag(title = it.getString("title"), key = it.getString("slug"), source = source)
		} ?: emptySet()

		val authors = album.optJSONArray("artists")?.mapJSONNotNull {
			it.getStringOrNull("title")
		}?.toSet() ?: emptySet()

		val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ROOT)

		return manga.copy(
			tags = tags,
			authors = authors,
			description = album.getStringOrNull("description"),
			chapters = listOf(
				MangaChapter(
					id = generateUid(manga.url),
					title = "Chapter",
					number = 1f,
					url = manga.url,
					uploadDate = dateFormat.parseSafe(album.getStringOrNull("created_at")),
					source = source,
					scanlator = null,
					branch = null,
					volume = 0
				)
			)
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val apiUrl = "https://api.simply-hentai.com/v3/manga/${chapter.url}/pages"
		val response = webClient.httpGet(apiUrl).parseJson()
		val pages = response.getJSONObject("data").getJSONArray("pages")

		return pages.mapJSONNotNull { item ->
			val url = item.getJSONObject("sizes").getString("full")
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = item.getJSONObject("sizes").getStringOrNull("thumb"),
				source = source
			)
		}
	}
}
