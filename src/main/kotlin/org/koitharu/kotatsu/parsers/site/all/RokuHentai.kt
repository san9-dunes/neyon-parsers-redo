package org.koitharu.kotatsu.parsers.site.all

import okhttp3.Headers
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("ROKUHENTAI", "Roku Hentai", type = ContentType.HENTAI)
internal class RokuHentai(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.valueOf("ROKUHENTAI"), pageSize = 24) {

	override val configKeyDomain = ConfigKey.Domain("rokuhentai.com")

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
		if (page > 1) return emptyList() // Stateful pagination not supported yet

		val url = buildString {
			append("https://")
			append(domain)
			if (!filter.query.isNullOrEmpty()) {
				append("/?q=")
				append(filter.query.urlEncoded())
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		
		return doc.select(".mdc-card").mapNotNull { el ->
			val a = el.selectFirst("a") ?: return@mapNotNull null
			val href = a.attrAsRelativeUrl("href").substringBeforeLast("/")
			val title = el.selectFirst(".site-manga-card__title--primary")?.text()?.trim() ?: return@mapNotNull null
			
			val style = el.selectFirst(".mdc-card__media")?.attr("style") ?: ""
			val cover = IMG_REGEX.find(style)?.groupValues?.get(1)?.toAbsoluteUrl(domain)

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
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		
		val tags = doc.select(".mdc-chip").mapNotNullToSet { chip ->
			val text = chip.text().trim()
			if (text.isBlank()) return@mapNotNullToSet null
			MangaTag(title = text, key = text, source = source)
		}

		val artist = tags.find { it.title.startsWith("artist:", ignoreCase = true) }?.title?.substringAfter(":")?.trim()
		
		val infoText = doc.selectFirst(".mdc-typography--caption:contains(images)")?.text() ?: ""
		val pageCount = infoText.substringBefore(" images").trim().toIntOrNull() ?: 1
		
		val chapterUrl = "${manga.url}#$pageCount"

		return manga.copy(
			tags = tags,
			authors = setOfNotNull(artist),
			description = doc.select(".site-manga-info__info h6").getOrNull(1)?.text()?.trim(),
			chapters = listOf(
				MangaChapter(
					id = generateUid(chapterUrl),
					title = "Comic",
					number = 1f,
					url = chapterUrl,
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
		val id = chapter.url.substringBefore("#").substringAfterLast("/")
		val count = chapter.url.substringAfter("#").toIntOrNull() ?: 0
		
		return (0 until count).map { i ->
			val url = "https://$domain/_images/pages/$id/$i.jpg"
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source
			)
		}
	}

	companion object {
		private val IMG_REGEX = Regex("""background-image: url\("(.+?)"\);""")
	}
}
