package org.koitharu.kotatsu.parsers.site.all

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("HENTAIRUN", "HentaiRun", type = ContentType.HENTAI)
internal class HentaiRun(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.HENTAIRUN, pageSize = 28) {

	override val configKeyDomain = ConfigKey.Domain("hentairun.com")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = false,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (!filter.query.isNullOrEmpty()) return emptyList()
		val pageNumber = page.coerceAtLeast(1)
		val url = if (pageNumber <= 1) {
			"https://$domain/"
		} else {
			"https://$domain/page/$pageNumber/"
		}
		val doc = webClient.httpGet(url).parseHtml()
		val seen = HashSet<String>()
		return doc.select("a[href^=/gallery/][aria-label]").mapNotNull { a ->
			val href = a.attrAsRelativeUrl("href")
			if (!seen.add(href)) return@mapNotNull null
			val title = a.attr("aria-label").ifBlank { a.selectFirst("p")?.text().orEmpty() }
			if (title.isBlank()) return@mapNotNull null
			val coverUrl = a.selectFirst("img[src*=thumb.jpg]")?.attr("src")
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
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val tags = doc.select(
			"a[href^=/tag/], a[href^=/parody/], a[href^=/character/], a[href^=/group/], a[href^=/artist/]",
		).mapToSet { a ->
			val href = a.attrAsRelativeUrl("href")
			MangaTag(
				title = a.selectFirst("span")?.text().orEmpty().ifBlank { a.ownText().ifBlank { a.text() } }.toTitleCase(),
				key = href.removePrefix("/").removeSuffix("/"),
				source = source,
			)
		}
		val pagesCount = doc.selectFirst("span:matchesOwn(^Pages:\\s*$)")
			?.nextElementSibling()
			?.text()
			?.toIntOrNull()

		return manga.copy(
			tags = tags,
			description = doc.selectFirst("meta[name=description]")?.attr("content"),
			state = MangaState.FINISHED,
			chapters = listOf(
				MangaChapter(
					id = generateUid(manga.url),
					title = if (pagesCount != null) "Oneshot ($pagesCount pages)" else "Oneshot",
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
		val raw = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseRaw()
		val match = DETAIL_DATA_REGEX.find(raw)
		val server = match?.groupValues?.get(1)?.toIntOrNull()
		val imgDir = match?.groupValues?.get(2)
		val galleryId = match?.groupValues?.get(3)
		val pages = match?.groupValues?.get(4)?.toIntOrNull()
		if (server == null || imgDir.isNullOrEmpty() || galleryId.isNullOrEmpty() || pages == null || pages <= 0) {
			return emptyList()
		}
		val base = "https://m$server.hentairun.com/$imgDir/$galleryId"
		return (1..pages).map { index ->
			val url = "$base/$index.webp"
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}

	private companion object {
		val DETAIL_DATA_REGEX = Regex(
			"\\\"id\\\":\\\"\\\\d+\\\",\\\"server\\\":(\\\\d+),\\\"img_dir\\\":\\\"([^\\\"]+)\\\",\\\"gallery_id\\\":\\\"([^\\\"]+)\\\",\\\"pages\\\":(\\\\d+)",
		)
	}
}
