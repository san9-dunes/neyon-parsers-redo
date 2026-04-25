package org.koitharu.kotatsu.parsers.site.all

import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("BONDAGECOMIXXX", "BondageComiXxx.net", type = ContentType.HENTAI)
internal class BondageComiXxx(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.BONDAGECOMIXXX, pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("bondagecomixxx.net")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.NEWEST)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = true)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	init {
		setFirstPage(0)
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			if (!filter.query.isNullOrEmpty()) {
				append("/?s=")
				append(filter.query.urlEncoded())
				if (page > 0) {
					append("&paged=")
					append(page + 1)
				}
			} else {
				append("/feed/")
				if (page > 0) {
					append("?paged=")
					append(page + 1)
				}
			}
			append(if (contains("?")) "&" else "?")
			append("nonitro=1")
		}

		val doc = webClient.httpGet(url).parseHtml()
		
		// If using RSS feed
		if (url.contains("/feed/")) {
			return doc.select("item").map { item ->
				val title = item.selectFirst("title")?.text()?.trim() ?: "Unknown"
				val link = item.selectFirst("link")?.text()?.trim() ?: ""
				val relativeUrl = link.toRelativeUrl(domain)
				
				// Try to extract cover from content:encoded
				val content = item.selectFirst("content\\:encoded")?.text() ?: ""
				val coverUrl = """src="([^"]+)"""".toRegex().find(content)?.groupValues?.get(1)

				Manga(
					id = generateUid(relativeUrl),
					title = title,
					altTitles = emptySet(),
					url = relativeUrl,
					publicUrl = link,
					rating = RATING_UNKNOWN,
					contentRating = ContentRating.ADULT,
					coverUrl = coverUrl,
					tags = emptySet(),
					state = MangaState.FINISHED,
					authors = emptySet(),
					source = source,
				)
			}
		}

		// Fallback for search which is usually static in WP
		return doc.select("article, .post, .jet-listing-grid__item").mapNotNull { post ->
			val a = post.selectFirst("h2 a, .entry-title a, a") ?: return@mapNotNull null
			val href = a.attrAsRelativeUrl("href")
			val title = post.selectFirst(".elementor-heading-title")?.text()?.trim() ?: a.text().trim()
			if (title.isBlank()) return@mapNotNull null
			val coverUrl = post.selectFirst("img")?.attr("src")

			Manga(
				id = generateUid(href),
				title = title,
				altTitles = emptySet(),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = coverUrl,
				tags = emptySet(),
				state = MangaState.FINISHED,
				authors = emptySet(),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()

		val tags = doc.select(".entry-meta a, .entry-footer a, .tags a")
			.mapNotNull { it.toMangaTagOrNull() }
			.toSet()

		return manga.copy(
			tags = tags,
			description = doc.selectFirst(".entry-content p")?.text()?.trim(),
			chapters = listOf(
				MangaChapter(
					id = generateUid(manga.url),
					title = "Comic",
					number = 1f,
					volume = 0,
					url = manga.url,
					scanlator = null,
					uploadDate = 0L,
					branch = null,
					source = source,
				),
			),
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val response = webClient.httpGet(chapter.url.toAbsoluteUrl(domain).let { 
			if (it.contains("?")) "$it&nonitro=1" else "$it?nonitro=1"
		})
		val html = response.body!!.string()
		
		// Broadest possible regex for imagetwist links. 
		val imageTwistRegex = """imagetwist\.com[\\/]+[a-z0-9]+[\\/]+[^"'\s<>\\&]+""".toRegex(RegexOption.IGNORE_CASE)
		
		return imageTwistRegex.findAll(html)
			.map { it.value.replace("\\/", "/").replace("\\", "") }
			.filter { it.contains(".html", ignoreCase = true) || it.contains("/i/") || it.contains("/th/") }
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
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		if (!page.url.contains("imagetwist.com")) {
			return page.url
		}
		if (page.url.contains("imagetwist.com/i/")) {
			return page.url
		}
		val doc = webClient.httpGet(page.url).parseHtml()
		val imageUrl = doc.selectFirst("img.pic")?.requireSrc()
			?: doc.selectFirst("a[data-fancybox=gallery][href]")?.attr("href")
			?: doc.selectFirst(".pic[src]")?.requireSrc()
			?: doc.selectFirst("img[src*='/img/']")?.requireSrc()
		return imageUrl?.toAbsoluteUrl("imagetwist.com") ?: page.url
	}

	private fun Element.toMangaTagOrNull(): MangaTag? {
		val title = text().trim().ifBlank { return null }
		val href = attr("href")
		if (!href.contains("/tag/") && !href.contains("/category/")) return null
		val key = href.substringAfterLast("/", href.trim('/')).substringAfterLast("/")
		
		return MangaTag(
			title = title,
			key = key,
			source = source,
		)
	}
}
