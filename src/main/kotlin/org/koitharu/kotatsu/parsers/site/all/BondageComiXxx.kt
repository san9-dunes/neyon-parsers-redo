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
	PagedMangaParser(context, MangaParserSource.BONDAGECOMIXXX, pageSize = 10) {

	override val configKeyDomain = ConfigKey.Domain("bondagecomixxx.net")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.NEWEST)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = true)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			append("/")
			if (!filter.query.isNullOrEmpty()) {
				append("?s=")
				append(filter.query.urlEncoded())
				if (page > 0) {
					append("&paged=")
					append(page + 1)
				}
			} else {
				if (page > 0) {
					append("page/")
					append(page + 1)
					append("/")
				}
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		return doc.select(".jet-listing-grid__item").mapNotNull { post ->
			val a = post.selectFirst("a") ?: return@mapNotNull null
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
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		return doc.select("a[href*='imagetwist.com']").map { a ->
			val url = a.attr("href")
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = a.selectFirst("img")?.attr("src"),
				source = source,
			)
		}
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
			?: doc.requireElementById("main-image").requireSrc()
		return imageUrl.toAbsoluteUrl("imagetwist.com")
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
