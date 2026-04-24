package org.koitharu.kotatsu.parsers.site.all

import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType
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
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.requireElementById
import org.koitharu.kotatsu.parsers.util.requireSrc
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import org.koitharu.kotatsu.parsers.util.urlEncoded
import java.util.EnumSet
import java.util.LinkedHashSet

@MangaSourceParser("SVSCOMICS", "SVSComics", type = ContentType.HENTAI)
internal class SvsComicsParser(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.SVSCOMICS, pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain(
		"svscomics.com",
		"svscomics.org",
	)

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val pageNumber = page.coerceAtLeast(1)
		val url = buildString {
			append("https://")
			append(domain)
			append("/")
			if (!filter.query.isNullOrEmpty()) {
				append("?s=")
				append(filter.query.urlEncoded())
				if (pageNumber > 1) {
					append("&paged=")
					append(pageNumber)
				}
			} else {
				if (pageNumber > 1) {
					append("page/")
					append(pageNumber)
				}
			}
		}
		val doc = webClient.httpGet(url).parseHtml()
		return doc.select("article.post").mapNotNull { post ->
			val href = post.selectFirst("a[href*=/download/]")?.attrAsRelativeUrl("href") ?: return@mapNotNull null
			val title = post.selectFirst(".header-title")?.text().orEmpty().ifBlank {
				post.selectFirst("a[href*=/download/]")?.attr("title").orEmpty()
			}
			if (title.isBlank()) return@mapNotNull null
			Manga(
				id = generateUid(href),
				title = title,
				altTitles = emptySet(),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = post.selectFirst("figure img")?.attr("src"),
				tags = post.select(".tags a").mapNotNullToSet { it.toMangaTagOrNull() },
				state = MangaState.FINISHED,
				authors = post.select("li.author a").mapToSet { it.text() },
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val pagesCount = PAGES_COUNT_REGEX.find(
			doc.selectFirst("li.pages")?.text().orEmpty(),
		)?.groupValues?.getOrNull(1)?.toIntOrNull()
		val chapterTitle = if (pagesCount != null) {
			"Oneshot ($pagesCount pages)"
		} else {
			"Oneshot"
		}
		return manga.copy(
			title = doc.selectFirst(".headerh1full h1")?.text().orEmpty().ifBlank { manga.title },
			coverUrl = doc.selectFirst(".figurefull img")?.attr("src") ?: manga.coverUrl,
			description = doc.selectFirst("meta[name=description]")?.attr("content"),
			tags = doc.select(".tagzfull a, .tags a").mapNotNullToSet { it.toMangaTagOrNull() },
			authors = doc.select("li.author a").mapToSet { it.text() },
			state = MangaState.FINISHED,
			chapters = listOf(
				MangaChapter(
					id = generateUid(manga.url),
					title = chapterTitle,
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
		val seen = LinkedHashSet<String>()
		return doc.select(".prevgallery .preview a[href]").mapNotNull { a ->
			val href = a.attr("href")
			if (href.isBlank() || !seen.add(href)) return@mapNotNull null
			MangaPage(
				id = generateUid(href),
				url = href,
				preview = a.selectFirst("img")?.attr("src"),
				source = source,
			)
		}
	}

	override suspend fun getPageUrl(page: MangaPage): String {
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
		val relativeUrl = runCatching { attrAsRelativeUrl("href") }.getOrNull().orEmpty()
		val key = relativeUrl
			.trim('/')
			.substringAfter("category/", relativeUrl.trim('/'))
			.ifBlank { title }
		return MangaTag(
			title = title,
			key = key,
			source = source,
		)
	}

	private companion object {
		val PAGES_COUNT_REGEX = Regex("(\\d+)\\s+pages?", RegexOption.IGNORE_CASE)
	}
}
