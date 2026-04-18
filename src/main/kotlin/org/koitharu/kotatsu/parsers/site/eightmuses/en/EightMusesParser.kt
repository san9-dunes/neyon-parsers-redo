package org.koitharu.kotatsu.parsers.site.eightmuses.en

import org.jsoup.nodes.Document
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
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.attrAsRelativeUrlOrNull
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.src
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import java.util.EnumSet

@MangaSourceParser("EIGHTMUSES", "8muses", "en", ContentType.HENTAI)
internal class EightMusesParser(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.EIGHTMUSES, pageSize = 40) {

	override val configKeyDomain = ConfigKey.Domain("8muses.io", "8muses.com", "8muses.xxx")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = false)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (page > 1 || !filter.query.isNullOrEmpty()) {
			return emptyList()
		}
		val doc = webClient.httpGet("https://$domain/").parseHtml()
		return parseAlbumCards(doc)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val chapterLinks = parseSubAlbums(doc, manga.url)

		val chapters = if (chapterLinks.isNotEmpty()) {
			chapterLinks.mapIndexed { index, pair ->
				MangaChapter(
					id = generateUid(pair.first),
					title = pair.second,
					number = (index + 1).toFloat(),
					volume = 0,
					url = pair.first,
					scanlator = null,
					uploadDate = 0L,
					branch = null,
					source = source,
				)
			}
		} else {
			listOf(
				MangaChapter(
					id = manga.id,
					title = manga.title,
					number = 1f,
					volume = 0,
					url = manga.url,
					scanlator = null,
					uploadDate = 0L,
					branch = null,
					source = source,
				),
			)
		}

		val cover = doc.select("img[src]").firstOrNull { img ->
			img.attr("src").contains("/img/data/")
		}?.src()

		return manga.copy(
			coverUrl = cover ?: manga.coverUrl,
			description = doc.selectFirst("meta[name=description]")?.attr("content"),
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val chapterUrl = chapter.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(chapterUrl).parseHtml()

		val pictureLinks = doc.select("a[href^=/picture/]")
			.mapNotNull { a ->
				val href = a.attrAsRelativeUrlOrNull("href")?.substringBefore("?") ?: return@mapNotNull null
				href to a.selectFirst("img")?.src()
			}
			.distinctBy { it.first }

		if (pictureLinks.isNotEmpty()) {
			return pictureLinks.map { pair ->
				MangaPage(
					id = generateUid(pair.first),
					url = pair.first,
					preview = pair.second,
					source = source,
				)
			}
		}

		if (chapter.url.startsWith("/picture/")) {
			return listOf(
				MangaPage(
					id = generateUid(chapter.url),
					url = chapter.url,
					preview = null,
					source = source,
				),
			)
		}

		return emptyList()
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		val doc = webClient.httpGet(page.url.toAbsoluteUrl(domain)).parseHtml()
		val fullImage = doc.select("img[src]").firstOrNull { img ->
			val src = img.attr("src")
			src.contains("/img/data/full_") || src.contains("/img/data/")
		}?.attr("src")
		return fullImage?.toAbsoluteUrl(domain) ?: super.getPageUrl(page)
	}

	private fun parseAlbumCards(doc: Document): List<Manga> {
		val seen = HashSet<String>()
		return doc.select("a[href^=/album/]").mapNotNull { a ->
			val href = a.attrAsRelativeUrlOrNull("href")?.substringBefore("?")?.removeSuffix("/") ?: return@mapNotNull null
			if (href == "/album" || !seen.add(href)) {
				return@mapNotNull null
			}
			val title = a.text().trim()
			if (title.isEmpty()) {
				return@mapNotNull null
			}

			Manga(
				id = generateUid(href),
				title = title,
				altTitles = emptySet(),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = a.selectFirst("img")?.src(),
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	private fun parseSubAlbums(doc: Document, mangaUrl: String): List<Pair<String, String>> {
		val current = mangaUrl.removeSuffix("/")
		val prefix = "$current/"
		return doc.select("a[href^=/album/]")
			.mapNotNull { a ->
				val href = a.attrAsRelativeUrlOrNull("href")?.substringBefore("?")?.removeSuffix("/")
					?: return@mapNotNull null
				if (!href.startsWith(prefix) || href == current) {
					return@mapNotNull null
				}
				val tail = href.removePrefix(prefix)
				if (tail.isEmpty() || tail.contains('/')) {
					return@mapNotNull null
				}
				val title = a.text().trim().ifEmpty { tail.replace('-', ' ') }
				href to title
			}
			.distinctBy { it.first }
	}
}
