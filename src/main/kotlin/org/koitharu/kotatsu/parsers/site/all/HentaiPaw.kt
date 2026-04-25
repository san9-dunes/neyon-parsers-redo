package org.koitharu.kotatsu.parsers.site.all

import okhttp3.Headers
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("HENTAIPAW", "HentaiPaw", type = ContentType.HENTAI)
internal class HentaiPaw(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.valueOf("HENTAIPAW"), pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("hentaipaw.com")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("Referer", "https://$domain/")
		.build()

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.NEWEST)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = true)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (page > 0) return emptyList()

		val url = buildString {
			append("https://")
			append(domain)
			if (!filter.query.isNullOrEmpty()) {
				append("/articles/search?keyword=")
				append(filter.query.urlEncoded())
			} else {
				if (order == SortOrder.POPULARITY) {
					append("/articles/rank")
				} else {
					append("/")
				}
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		return doc.select("a[href*='/articles/']").mapNotNull { a ->
			val href = a.attrAsRelativeUrl("href")
			if (!href.startsWith("/articles/")) return@mapNotNull null
			
			val title = a.attr("title").takeIf { it.isNotEmpty() } 
				?: a.selectFirst(".title, h2, h3")?.text()?.trim() 
				?: a.text().trim()
			
			if (title.isBlank() || title.equals("Rank", ignoreCase = true) || title.contains("View More", ignoreCase = true)) {
				return@mapNotNull null
			}

			val img = a.selectFirst("img")
			val cover = (img?.attr("data-src")?.takeIf { it.isNotEmpty() } ?: img?.attr("src"))?.toAbsoluteUrl(domain)

			Manga(
				id = generateUid(href),
				title = title,
				altTitles = emptySet(),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = cover,
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}.distinctBy { it.url }
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()

		val tags = doc.select("a[href*='/tags/']").mapNotNullToSet { a ->
			val text = a.text().trim()
			if (text.isBlank()) return@mapNotNullToSet null
			MangaTag(title = text, key = text, source = source)
		}

		val artist = doc.select("a[href*='/artists/']").map { it.text().trim() }.toSet()

		return manga.copy(
			tags = tags,
			authors = artist,
			description = doc.selectFirst(".description, .content")?.text()?.trim(),
			chapters = listOf(
				MangaChapter(
					id = generateUid(manga.url),
					title = "Album",
					number = 1f,
					url = manga.url,
					source = source,
					scanlator = null,
					uploadDate = 0,
					branch = null,
					volume = 0
				)
			)
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		return doc.select("img").mapNotNull { img ->
			val url = (img.attr("data-src").takeIf { it.isNotEmpty() } ?: img.attr("src"))
			if (url.isBlank() || url.contains("data:image") || url.contains("logo") || url.contains("icon")) {
				return@mapNotNull null
			}
			
			MangaPage(
				id = generateUid(url),
				url = url.toAbsoluteUrl(domain),
				preview = null,
				source = source
			)
		}.distinctBy { it.url }
	}
}
