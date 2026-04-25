package org.koitharu.kotatsu.parsers.site.all

import okhttp3.Headers
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("BESTPORNCOMIX", "BestPornComix.com", type = ContentType.HENTAI)
internal class BestPornComix(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.valueOf("BESTPORNCOMIX"), pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("bestporncomix.com")

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
		val url = buildString {
			append("https://")
			append(domain)
			if (!filter.query.isNullOrEmpty()) {
				append("/gallery/search?keyword=")
				append(filter.query.urlEncoded())
			} else {
				append("/")
			}
			if (page > 0) {
				append(if (contains("?")) "&" else "?")
				append("page=")
				append(page + 1)
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		return doc.select(".thumbnail-doujin, .post, .article").mapNotNull { el ->
			val a = el.selectFirst("a[href*='/gallery/']") ?: return@mapNotNull null
			val href = a.attrAsRelativeUrl("href")
			if (!href.startsWith("/gallery/")) return@mapNotNull null
			
			val title = el.selectFirst(".title, h2, h3")?.text()?.trim() ?: a.text().trim()
			if (title.isBlank()) return@mapNotNull null

			val img = el.selectFirst("img")
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

		val tags = doc.select("a[href*='/gallery/category/'], a[href*='/tag/']").mapNotNullToSet { a ->
			val text = a.text().trim()
			if (text.isBlank()) return@mapNotNullToSet null
			MangaTag(title = text, key = text, source = source)
		}

		return manga.copy(
			tags = tags,
			description = doc.selectFirst(".description, .entry-content p")?.text()?.trim(),
			chapters = listOf(
				MangaChapter(
					id = generateUid(manga.url),
					title = "Comic",
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
			val src = (img.attr("data-src").takeIf { it.isNotEmpty() } ?: img.attr("src"))
			if (src.isBlank() || src.contains("data:image") || !src.contains("/content/")) {
				return@mapNotNull null
			}
			
			// Transform thumbnail to original: remove -WxH suffix before extension
			val url = src.replace(Regex("""-\d+x\d+(?=\.(jpg|png|webp|jpeg))"""), "").toAbsoluteUrl(domain)
			
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source
			)
		}.distinctBy { it.url }
	}
}
