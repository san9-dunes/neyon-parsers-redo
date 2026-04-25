package org.koitharu.kotatsu.parsers.site.all

import okhttp3.Headers
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("ILIKECOMIX", "ILikeComix", type = ContentType.HENTAI)
internal class ILikeComix(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.valueOf("ILIKECOMIX"), pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("ilikecomix.com")

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
				append("/?s=")
				append(filter.query.urlEncoded())
			} else {
				append("/en-comics/")
			}
			if (page > 0) {
				append("page/")
				append(page + 1)
				append("/")
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		return doc.select("article, .post-item, .entry-header").mapNotNull { el ->
			val a = el.selectFirst("a") ?: return@mapNotNull null
			val href = a.attrAsRelativeUrl("href")
			if (href == "/" || href.contains("/category/")) return@mapNotNull null
			
			val title = el.selectFirst(".entry-title, .title")?.text()?.trim() ?: a.text().trim()
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

		val tags = doc.select(".entry-meta a, .tags a, .xtags a").mapNotNullToSet { a ->
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
		val response = webClient.httpGet(chapter.url.toAbsoluteUrl(domain))
		val html = response.body!!.string()
		
		val imageTwistRegex = """imagetwist\.com[\\/]+[a-z0-9]+[\\/]+[^"'\s<>\\&]+""".toRegex(RegexOption.IGNORE_CASE)
		
		val pages = imageTwistRegex.findAll(html)
			.map { it.value.replace("\\/", "/").replace("\\", "") }
			.filter { it.contains(".html", ignoreCase = true) || it.contains("/i/") || it.contains("/th/", ignoreCase = true) }
			.distinct()
			.map { path ->
				val url = if (path.startsWith("http")) path else "https://$path"
				MangaPage(
					id = generateUid(url),
					url = url,
					preview = null,
					source = source,
				)
			}.toList()

		if (pages.isNotEmpty()) return pages

		// Fallback for native hosting
		val doc = org.jsoup.Jsoup.parse(html)
		return doc.select("img").mapNotNull { img ->
			val src = (img.attr("data-src").takeIf { it.isNotEmpty() } ?: img.attr("src"))
			if (src.isBlank() || src.contains("data:image") || src.contains("logo") || src.contains("icon")) {
				return@mapNotNull null
			}
			
			MangaPage(
				id = generateUid(src),
				url = src.toAbsoluteUrl(domain),
				preview = null,
				source = source
			)
		}.distinctBy { it.url }
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		if (!page.url.contains("imagetwist.com")) return page.url
		if (page.url.contains("imagetwist.com/i/")) return page.url
		
		val doc = webClient.httpGet(page.url).parseHtml()
		val imageUrl = doc.selectFirst("img.pic")?.requireSrc()
			?: doc.selectFirst(".pic[src]")?.requireSrc()
			?: doc.selectFirst("img[src*='/img/']")?.requireSrc()
		
		return imageUrl?.toAbsoluteUrl("imagetwist.com") ?: page.url
	}
}
