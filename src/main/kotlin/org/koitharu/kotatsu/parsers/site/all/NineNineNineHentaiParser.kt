package org.koitharu.kotatsu.parsers.site.all

import org.koitharu.kotatsu.parsers.Broken

import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSONToSet
import java.util.*

@MangaSourceParser("NINENINENINEHENTAI", "9Hentai (.so)", type = ContentType.HENTAI)
internal class NineNineNineHentaiParser(context: MangaLoaderContext) :
	NineNineNineHentaiBaseParser(context, MangaParserSource.NINENINENINEHENTAI, "9hentai.so")

@Broken
@MangaSourceParser("SRC_9HENTAI", "9Hentai", type = ContentType.HENTAI)
internal class Src9HentaiParser(context: MangaLoaderContext) :
	NineNineNineHentaiBaseParser(context, MangaParserSource.SRC_9HENTAI, "9hentai.com")

internal abstract class NineNineNineHentaiBaseParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : PagedMangaParser(context, source, PAGE_SIZE) {

	override val configKeyDomain = ConfigKey.Domain(defaultDomain)

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.NEWEST,
		SortOrder.RATING,
		SortOrder.ALPHABETICAL,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	init {
		paginator.firstPage = 0
		searchPaginator.firstPage = 0
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val payload = buildSearchPayload(page, order, filter.query)
		val results = apiCall(API_GET_BOOK, payload).optJSONArray("results") ?: return emptyList()
		return buildList(results.length()) {
			for (i in 0 until results.length()) {
				val jo = results.optJSONObject(i) ?: continue
				add(parseManga(jo))
			}
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val id = manga.url.toLongOrNull() ?: return manga
		val details = apiCall(API_GET_BOOK_BY_ID, JSONObject().put("id", id)).optJSONObject("results")
		val rawTitle = details?.getStringOrNull("title")?.nullIfEmpty()
		val resolvedRawTitle = rawTitle ?: manga.altTitles.firstOrNull() ?: manga.title
		val parsedTitle = normalizeTitle(resolvedRawTitle)
		val coverBase = details?.getStringOrNull("image_server")?.nullIfEmpty() ?: DEFAULT_IMAGE_HOST
		val galleryUrl = "/g/$id/"
		val htmlInfo = runCatching {
			val doc = webClient.httpGet(galleryUrl.toAbsoluteUrl(domain)).parseHtml()
			parseHtmlTags(doc) to parseDescription(doc)
		}.getOrNull()
		val tags = (htmlInfo?.first ?: manga.tags).ifEmpty { manga.tags }
		val totalPages = details?.optInt("total_page") ?: 0
		return manga.copy(
			title = parsedTitle,
			altTitles = setOfNotNull(rawTitle, manga.altTitles.firstOrNull()).filter { it != parsedTitle }.toSet(),
			coverUrl = "$coverBase$id/cover.jpg",
			largeCoverUrl = "$coverBase$id/cover.jpg",
			authors = setOfNotNull(extractAuthor(resolvedRawTitle)),
			tags = tags,
			description = htmlInfo?.second ?: manga.description,
			contentRating = ContentRating.ADULT,
			chapters = listOf(
				MangaChapter(
					id = generateUid(id),
					title = parsedTitle,
					number = 1f,
					volume = 0,
					url = id.toString(),
					uploadDate = 0L,
					branch = null,
					scanlator = totalPages.takeIf { it > 0 }?.let { "$it pages" },
					source = source,
				),
			),
		)
	}

	override suspend fun getRelatedManga(seed: Manga): List<Manga> {
		val id = seed.url.toLongOrNull() ?: return emptyList()
		val related = apiCall(API_GET_BOOK_RELATED, JSONObject().put("id", id)).optJSONArray("results") ?: return emptyList()
		return buildList(related.length()) {
			for (i in 0 until related.length()) {
				val jo = related.optJSONObject(i) ?: continue
				add(parseManga(jo))
			}
		}
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val id = chapter.url.toLongOrNull() ?: return emptyList()
		val details = apiCall(API_GET_BOOK_BY_ID, JSONObject().put("id", id)).optJSONObject("results") ?: return emptyList()
		val totalPages = details.optInt("total_page")
		if (totalPages <= 0) return emptyList()
		val imageHost = details.getStringOrNull("image_server")?.nullIfEmpty() ?: DEFAULT_IMAGE_HOST
		return (1..totalPages).map { pageNum ->
			MangaPage(
				id = generateUid("$id/$pageNum"),
				url = "$imageHost$id/$pageNum.jpg",
				preview = "$imageHost$id/preview/${pageNum}t.jpg",
				source = source,
			)
		}
	}

	private fun buildSearchPayload(page: Int, order: SortOrder, query: String?): JSONObject {
		val tagItems = JSONObject()
			.put("included", org.json.JSONArray())
			.put("excluded", org.json.JSONArray())
		val tag = JSONObject()
			.put("text", "")
			.put("type", 1)
			.put("tags", org.json.JSONArray())
			.put("items", tagItems)
		val pages = JSONObject().put("range", org.json.JSONArray().put(0).put(2000))
		val search = JSONObject()
			.put("text", query.orEmpty())
			.put("page", page)
			.put("sort", sortToCode(order))
			.put("pages", pages)
			.put("tag", tag)
		return JSONObject().put("search", search)
	}

	private fun sortToCode(order: SortOrder): Int = when (order) {
		SortOrder.NEWEST -> 0
		SortOrder.POPULARITY -> 1
		SortOrder.RATING -> 2
		SortOrder.UPDATED -> 3
		SortOrder.ALPHABETICAL -> 4
		else -> 0
	}

	private fun parseManga(entry: JSONObject): Manga {
		val id = entry.optLong("id")
		val rawTitle = entry.getStringOrNull("title").orEmpty()
		val title = normalizeTitle(rawTitle)
		val coverHost = entry.getStringOrNull("image_server")?.nullIfEmpty() ?: DEFAULT_IMAGE_HOST
		val tags = entry.optJSONArray("tags")?.mapJSONToSet {
			MangaTag(
				title = it.getString("name").toCamelCase(),
				key = it.opt("id").toString(),
				source = source,
			)
		}.orEmpty()
		val altTitle = entry.getStringOrNull("alt_title")?.nullIfEmpty()
		return Manga(
			id = generateUid(id),
			title = title,
			altTitles = setOfNotNull(altTitle, rawTitle.takeIf { it.isNotBlank() && it != title }),
			url = id.toString(),
			publicUrl = "/g/$id/".toAbsoluteUrl(domain),
			rating = RATING_UNKNOWN,
			contentRating = ContentRating.ADULT,
			coverUrl = "$coverHost$id/cover-small.jpg",
			largeCoverUrl = "$coverHost$id/cover.jpg",
			tags = tags,
			state = null,
			authors = setOfNotNull(extractAuthor(rawTitle)),
			description = null,
			chapters = null,
			source = source,
		)
	}

	private fun normalizeTitle(rawTitle: String): String {
		if (rawTitle.isBlank()) return rawTitle
		val normalized = rawTitle.replace(authorPrefixRegex, "").trim()
		return normalized.ifBlank { rawTitle.trim() }
	}

	private fun extractAuthor(rawTitle: String): String? {
		return authorRegex.find(rawTitle)?.groupValues?.getOrNull(1)?.trim()?.nullIfEmpty()
	}

	private fun parseDescription(doc: org.jsoup.nodes.Document): String? {
		return doc.selectFirst("meta[property=og:description]")?.attr("content")?.nullIfEmpty()
	}

	private fun parseHtmlTags(doc: org.jsoup.nodes.Document): Set<MangaTag> {
		return doc.select("a[href*='/t/']").mapNotNullToSet { a ->
			val href = a.attr("href")
			val key = href.substringAfter("/t/").substringBefore('/').trim()
			val title = a.text().trim().nullIfEmpty()
			if (key.isEmpty() || title == null) {
				null
			} else {
				MangaTag(
					title = title.toCamelCase(),
					key = key,
					source = source,
				)
			}
		}
	}

	private suspend fun apiCall(endpoint: String, body: JSONObject): JSONObject {
		val url = "https://$domain/api/$endpoint"
		val response = webClient.httpPost(url, body).parseJson()
		if (!response.optBoolean("status", false)) {
			throw ParseException("API request failed: $endpoint", url)
		}
		return response
	}

	companion object {
		private const val PAGE_SIZE = 20
		private const val API_GET_BOOK = "getBook"
		private const val API_GET_BOOK_BY_ID = "getBookByID"
		private const val API_GET_BOOK_RELATED = "getBookRelated"
		private const val DEFAULT_IMAGE_HOST = "https://i.9hentai.so/images/"
		private val authorRegex = Regex("""^\s*\[([^]]+)]""")
		private val authorPrefixRegex = Regex("""^\s*\[[^]]+]\s*[-–:]?\s*""")
	}
}
