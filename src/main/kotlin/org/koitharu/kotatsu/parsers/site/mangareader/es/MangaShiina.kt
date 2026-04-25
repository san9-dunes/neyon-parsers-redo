package org.koitharu.kotatsu.parsers.site.mangareader.es

import okhttp3.Headers
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.asTypedList
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNull
import java.util.*

@Broken
@MangaSourceParser("MANGASHIINA", "MangaMukai.com", "es")
internal class MangaShiina(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.valueOf("MANGASHIINA"), pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("mangamukai.com")

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("Referer", "https://$domain/")
		.build()

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = false)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	init {
		setFirstPage(0)
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (page > 0) return emptyList()

		val url = "https://$domain/wp-json/mangamukai/v1/catalog"
		val response = webClient.httpGet(url).parseJson()
		val mangas = response.optJSONArray("mangas") ?: return emptyList()

		return mangas.mapJSONNotNull { item ->
		        val id = item.optInt("id", 0).takeIf { it != 0 }?.toString() ?: return@mapJSONNotNull null
		        val title = item.getStringOrNull("titulo") ?: return@mapJSONNotNull null
		        if (title.isBlank() || title.contains("DUMMY", ignoreCase = true)) return@mapJSONNotNull null
		        
		        val relativeUrl = "/manga/$id"
			Manga(
				id = generateUid(relativeUrl),
				title = title,
				altTitles = emptySet(),
				url = id, // Use numeric ID as internal URL
				publicUrl = relativeUrl.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = null,
				coverUrl = item.getStringOrNull("portada"),
				tags = item.optJSONArray("genres")?.asTypedList<String>()?.mapToSet { tag ->
					MangaTag(title = tag, key = tag, source = source)
				} ?: emptySet(),
				state = if (item.getStringOrNull("status") == "Ongoing") MangaState.ONGOING else MangaState.FINISHED,
				authors = emptySet(),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val url = "https://$domain/wp-json/mangamukai/v1/manga/${manga.url}"
		val item = webClient.httpGet(url).parseJson()

		val chaptersUrl = "https://$domain/wp-json/mangamukai/v1/series/${manga.url}/chapters"
		val chaptersResponse = webClient.httpGet(chaptersUrl).parseJson()
		val chaptersJson = chaptersResponse.optJSONArray("chapters") ?: return manga

		var chapters = chaptersJson.mapJSONNotNull { ch ->
			val id = ch.optInt("id", 0).takeIf { it != 0 } ?: return@mapJSONNotNull null
			val num = ch.optDouble("chapter_number", 0.0).toFloat()
			val relativeUrl = "/manga/${manga.url}/chapter/$id"

			MangaChapter(
				id = generateUid(relativeUrl),
				title = ch.getStringOrNull("title") ?: "Chapter $num",
				number = num,
				volume = 0,
				url = relativeUrl,
				scanlator = null,
				uploadDate = 0L,
				branch = null,
				source = source,
			)
		}.sortedByDescending { it.number }
		
		return manga.copy(
			description = item.getStringOrNull("descripcion"),
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val html = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml().html()
		
		// Search for images in script tags (common in MangaMukai React layout)
		val imagesRegex = """["'](https?://[^"']+/wp-content/uploads/[^"']+)["']""".toRegex()
		val matches = imagesRegex.findAll(html)
			.map { it.groupValues[1] }
			.filter { it.contains("/manga/") || it.contains("/uploads/") }
			.distinct()
			.toList()

		if (matches.isNotEmpty()) {
			return matches.map { url ->
				MangaPage(
					id = generateUid(url),
					url = url,
					preview = null,
					source = source,
				)
			}
		}

		// Fallback to direct selection
		val doc = org.jsoup.Jsoup.parse(html)
		return doc.select("img[src*='wp-content/uploads']").map { img ->
			val url = img.requireSrc()
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}
}
