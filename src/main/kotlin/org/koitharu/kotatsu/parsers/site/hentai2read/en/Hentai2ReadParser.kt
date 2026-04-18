package org.koitharu.kotatsu.parsers.site.hentai2read.en

import org.json.JSONArray
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.attrAsRelativeUrl
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.mapNotNullToSet
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.parsers.util.urlEncoded
import java.util.EnumSet

@MangaSourceParser("HENTAI2READ", "Hentai2Read", "en", ContentType.HENTAI)
internal class Hentai2ReadParser(context: MangaLoaderContext) :
	Hentai2ReadBaseParser(context, MangaParserSource.HENTAI2READ, "hentai2read.com")

@MangaSourceParser("HENTAI2NET", "Hentai2", "en", ContentType.HENTAI)
internal class Hentai2NetParser(context: MangaLoaderContext) :
	Hentai2ReadBaseParser(context, MangaParserSource.HENTAI2NET, "hentai2.net")

internal abstract class Hentai2ReadBaseParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : PagedMangaParser(context, source, pageSize = 40, searchPageSize = 36) {

	override val configKeyDomain = ConfigKey.Domain(defaultDomain, "hentai2read.com", "hentai2.net")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.RATING,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val pageNumber = page.coerceAtLeast(1)
		val url = buildListUrl(pageNumber, order, filter)
		return parseMangaList(webClient.httpGet(url).parseHtml())
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()

		val title = parseTitle(doc, manga.title)
		val tags = parseTags(doc)
		val authors = parseAuthors(doc)
		val state = parseState(doc)
		val chapters = parseChapters(doc)

		return manga.copy(
			title = title,
			coverUrl = doc.selectFirst("img.img-responsive.border-black-op")?.attr("src") ?: manga.coverUrl,
			description = doc.selectFirst("meta[name=description]")?.attr("content"),
			tags = tags,
			authors = authors,
			state = state,
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val chapterUrl = chapter.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(chapterUrl).parseHtml()
		val html = doc.html()

		val imagesRaw = imagesRegex.find(html)?.groupValues?.get(1)
			?.replace("\\/", "/")
			?: return emptyList()

		val images = runCatching { JSONArray(imagesRaw) }.getOrNull() ?: return emptyList()

		val firstImagePath = if (images.length() > 0) images.getString(0).replace("\\/", "/") else null
		val firstFullImageUrl = doc.select("img[src]")
			.map { it.attr("src") }
			.firstOrNull { it.contains("/hentai/") }

		val imageHostPrefix = when {
			firstImagePath == null -> defaultImageHost
			!firstFullImageUrl.isNullOrEmpty() && firstFullImageUrl.contains(firstImagePath) -> {
				firstFullImageUrl.substringBefore(firstImagePath)
			}

			else -> defaultImageHost
		}

		return buildList(images.length()) {
			for (i in 0 until images.length()) {
				val imagePath = images.getString(i).replace("\\/", "/")
				val url = if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
					imagePath
				} else {
					"$imageHostPrefix$imagePath"
				}

				add(
					MangaPage(
						id = generateUid(url),
						url = url,
						preview = null,
						source = source,
					),
				)
			}
		}
	}

	private fun buildListUrl(page: Int, order: SortOrder, filter: MangaListFilter): String {
		return buildString {
			append("https://")
			append(domain)

			if (filter.query.isNullOrEmpty()) {
				when (order) {
					SortOrder.POPULARITY -> append("/hentai-list/all/any/all/most-popular?page=$page")
					SortOrder.RATING -> append("/hentai-list/all/any/all/top-rating?page=$page")
					else -> append("/?order=update&page=$page")
				}
			} else {
				append("/?s=")
				append(filter.query.urlEncoded())
				append("&page=")
				append(page)
			}
		}
	}

	private fun parseMangaList(doc: Document): List<Manga> {
		val seen = HashSet<String>()
		return doc.select("a.link-effect.mangaPopover[href]").mapNotNull { a ->
			val relativeUrl = runCatching { a.attrAsRelativeUrl("href") }.getOrNull() ?: return@mapNotNull null
			if (!seen.add(relativeUrl)) return@mapNotNull null

			val title = a.text().trim().ifBlank { return@mapNotNull null }
			val cover = a.selectFirst("img")?.attr("src")

			Manga(
				id = generateUid(relativeUrl),
				title = title,
				altTitles = emptySet(),
				url = relativeUrl,
				publicUrl = relativeUrl.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = cover,
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	private fun parseTitle(doc: Document, fallback: String): String {
		val fromHeader = doc.selectFirst("#availableChapters .block-title")
			?.text()
			?.substringBeforeLast(" Chapters")
			?.trim()

		if (!fromHeader.isNullOrEmpty()) {
			return fromHeader
		}

		val fromMeta = doc.selectFirst("title")?.text()?.substringBefore(" hentai by")?.trim()
		return fromMeta?.takeIf { it.isNotEmpty() } ?: fallback
	}

	private fun parseChapters(doc: Document): List<MangaChapter> {
		return doc.select("ul.nav-chapters li a.pull-left[href]").mapIndexedNotNull { index, a ->
			val relativeUrl = runCatching { a.attrAsRelativeUrl("href") }.getOrNull() ?: return@mapIndexedNotNull null
			val text = a.text().lineSequence().firstOrNull()?.trim().orEmpty()
			if (text.isBlank()) return@mapIndexedNotNull null

			val number = chapterNumberRegex.find(text)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: (index + 1).toFloat()
			MangaChapter(
				id = generateUid(relativeUrl),
				title = text,
				number = number,
				volume = 0,
				url = relativeUrl,
				uploadDate = 0L,
				scanlator = null,
				branch = null,
				source = source,
			)
		}
	}

	private fun parseTags(doc: Document): Set<MangaTag> {
		return doc.select("a[href*='/hentai-list/category/']").mapNotNullToSet { a ->
			val href = a.attr("href")
			val key = href.substringAfter("/hentai-list/category/").trim('/').nullIfBlank() ?: return@mapNotNullToSet null
			val title = a.text().trim().nullIfBlank() ?: return@mapNotNullToSet null
			MangaTag(
				title = title.toTitleCase(),
				key = key,
				source = source,
			)
		}
	}

	private fun parseAuthors(doc: Document): Set<String> {
		return doc.select("a[href*='/hentai-list/author/'], a[href*='/hentai-list/artist/']")
			.mapNotNullToSet { it.text().substringBefore("[").trim().nullIfBlank() }
	}

	private fun parseState(doc: Document): MangaState? {
		val statusHref = doc.selectFirst("a[href*='/hentai-list/status/']")?.attr("href") ?: return null
		return when {
			statusHref.contains("/Completed/", ignoreCase = true) -> MangaState.FINISHED
			statusHref.contains("/Ongoing/", ignoreCase = true) -> MangaState.ONGOING
			else -> null
		}
	}

	private fun String.nullIfBlank(): String? = if (isBlank()) null else this

	private companion object {
		val imagesRegex = Regex("""['\"]images['\"]\s*:\s*(\[[^\]]*])""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
		val chapterNumberRegex = Regex("""^(\d+(?:\.\d+)?)\s*-""")
		const val defaultImageHost = "https://static.hentai.direct/hentai"
	}
}
