package org.koitharu.kotatsu.parsers.site.galleryadults.all

import okhttp3.Headers
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("YIFFER", "Yiffer.xyz", type = ContentType.HENTAI)
internal class Yiffer(context: MangaLoaderContext) : PagedMangaParser(context, MangaParserSource.YIFFER, pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("yiffer.xyz")

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("Referer", "https://$domain/")
		.build()

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.NEWEST)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = false)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (page > 0) return emptyList()

		val html = webClient.httpGet("https://$domain/browse").parseHtml().html()
		
		// Extremely broad regex to find ANY numeric ID followed by an escaped title string
		val comicDataRegex = """(\d+),\\"([^\\"]+)"""".toRegex()
		
		return comicDataRegex.findAll(html).map { match ->
			val id = match.groupValues[1]
			val title = match.groupValues[2]
			val relativeUrl = "/c/${title.replace(" ", "%20")}"
			val coverUrl = "https://pics.yiffer.xyz/comics/$id/thumbnail-2x.webp"

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
		}.distinctBy { it.url }.toList()
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()

		val authors = doc.select("a[href*='/artist/']")
			.map { it.text().trim() }
			.toSet()

		val tags = doc.select("a[href*='/tag/']")
			.map { tag ->
				MangaTag(
					title = tag.text().trim(),
					key = tag.attr("href").substringAfter("/tag/"),
					source = source,
				)
			}.toSet()

		return manga.copy(
			authors = authors.ifEmpty { manga.authors },
			tags = tags,
			description = doc.selectFirst("div.mt-4.text-sm")?.text()?.trim(),
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
		return doc.select("img[src*='/comics/']").map { img ->
			val url = img.requireSrc()
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}
}
