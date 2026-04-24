package org.koitharu.kotatsu.parsers.site.galleryadults.en

import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("WEBTOONPORN", "WebToonPorn.com", "en", type = ContentType.HENTAI)
internal class WebtoonPorn(context: MangaLoaderContext) : PagedMangaParser(context, MangaParserSource.WEBTOONPORN, pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("www.webtoonporn.com")

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("Referer", "https://$domain/")
		.build()

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.NEWEST, SortOrder.POPULARITY)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = false)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (page > 0) return emptyList()

		val url = buildString {
			append("https://")
			append(domain)
			append("/")
			when (order) {
				SortOrder.NEWEST -> append("updates.html")
				SortOrder.POPULARITY -> append("popular6.html")
				else -> {}
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		return doc.select("a").mapNotNull { a ->
			val trackingUrl = a.attr("href")
			if (!trackingUrl.contains("tracking.php?")) return@mapNotNull null
			val realUrl = trackingUrl.toAbsoluteUrl(domain).toHttpUrl().queryParameter("url") ?: return@mapNotNull null
			val relativeUrl = realUrl.toRelativeUrl(domain)
			if (!relativeUrl.startsWith("/manhwa1-")) return@mapNotNull null

			val title = a.parent()?.selectFirst("strong")?.text()?.trim() ?: a.text().trim().ifBlank { return@mapNotNull null }
			val coverUrl = a.selectFirst("img")?.requireSrc() ?: return@mapNotNull null

			Manga(
				id = generateUid(relativeUrl),
				title = title,
				altTitles = emptySet(),
				url = relativeUrl,
				publicUrl = relativeUrl.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = coverUrl,
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}.distinctBy { it.url }
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()

		val title = doc.selectFirst("h1")?.text()?.trim() ?: manga.title
		val description = doc.selectFirst(".xbookin, .xtext")?.text()?.trim()
		
		val chapters = doc.select("a").mapNotNull { a ->
			val text = a.text().uppercase()
			if (!text.contains("CHAPTER")) return@mapNotNull null
			
			val trackingUrl = a.attr("href")
			val realUrl = trackingUrl.toHttpUrl().queryParameter("url") ?: trackingUrl
			val relativeUrl = realUrl.toRelativeUrl(domain)
			
			val numberStr = text.substringAfter("CHAPTER").trim().substringBefore(" ")
			val number = numberStr.toFloatOrNull() ?: 0f

			MangaChapter(
				id = generateUid(relativeUrl),
				title = a.text().trim(),
				number = number,
				volume = 0,
				url = relativeUrl,
				scanlator = null,
				uploadDate = 0L,
				branch = null,
				source = source,
			)
		}.distinctBy { it.url }.sortedByDescending { it.number }

		return manga.copy(
			title = title,
			description = description,
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		return doc.select("img.lazy").map { img ->
			val url = img.attr("data-src").ifEmpty { img.attr("src") }.toAbsoluteUrl(domain)
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}
}
