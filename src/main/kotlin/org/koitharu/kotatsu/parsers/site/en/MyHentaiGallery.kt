package org.koitharu.kotatsu.parsers.site.en

import okhttp3.Headers
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("MYHENTAIGALLERY", "MyHentaiGallery", "en", type = ContentType.HENTAI)
internal class MyHentaiGallery(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.valueOf("MYHENTAIGALLERY"), pageSize = 24) {

	override val configKeyDomain = ConfigKey.Domain("myhentaigallery.com")

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
				append("/search?name=")
				append(filter.query.urlEncoded())
			}
			if (page > 0) {
				append(if (contains("?")) "&" else "?")
				append("page=")
				append(page + 1)
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		return doc.select(".comic-inner").mapNotNull { el ->
			val a = el.selectFirst("a[href^='/a/']") ?: return@mapNotNull null
			val href = a.attrAsRelativeUrl("href")
			
			val title = el.selectFirst(".comic-name")?.text()?.trim() ?: a.text().trim()
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

		val tags = doc.select("a[href*='/tag/']").mapNotNullToSet { a ->
			val text = a.text().trim()
			if (text.isBlank()) return@mapNotNullToSet null
			MangaTag(title = text, key = text, source = source)
		}

		val artist = doc.select("a[href*='/artist/']").map { it.text().trim() }.toSet()

		return manga.copy(
			tags = tags,
			authors = artist,
			description = doc.selectFirst(".description")?.text()?.trim(),
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
		return doc.select("div.comic-inner img").map { img ->
			val url = (img.attr("data-src").takeIf { it.isNotEmpty() } ?: img.attr("src"))
				.replace("/thumbnail/", "/original/")
				.toAbsoluteUrl(domain)
			
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source
			)
		}
	}
}
