package org.koitharu.kotatsu.parsers.site.en

import okhttp3.Headers
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*
import java.util.concurrent.TimeUnit

@MangaSourceParser("DOUJINS_COM", "Doujins.com", "en", type = ContentType.HENTAI)
internal class DoujinsCom(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.valueOf("DOUJINS_COM"), pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("doujins.com")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(ConfigKey.UserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"))
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("Referer", "https://$domain/")
		.build()

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.NEWEST,
		SortOrder.POPULARITY
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = true)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			if (!filter.query.isNullOrEmpty()) {
				append("/searches?words=")
				append(filter.query.urlEncoded())
				if (page > 0) {
					append("&page=")
					append(page + 1)
				}
			} else {
				if (order == SortOrder.POPULARITY) {
					append("/top")
				} else {
					// Time-window pagination for Latest (3-day chunks)
					val calendar = Calendar.getInstance()
					calendar.add(Calendar.DAY_OF_MONTH, -(page * 3))
					val end = TimeUnit.MILLISECONDS.toSeconds(calendar.timeInMillis)
					calendar.add(Calendar.DAY_OF_MONTH, -3)
					val start = TimeUnit.MILLISECONDS.toSeconds(calendar.timeInMillis)
					append("/folders?start=$start&end=$end")
				}
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		return doc.select(".thumbnail-doujin, .thumbnail").mapNotNull { el ->
			val a = el.selectFirst("a") ?: return@mapNotNull null
			val href = a.attrAsRelativeUrl("href")
			val title = a.text().trim()
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
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()

		val tags = doc.select("a[href*='/tag/']").mapNotNullToSet { a ->
			val text = a.text().trim()
			if (text.isBlank()) return@mapNotNullToSet null
			MangaTag(
				title = text,
				key = a.attr("href").substringAfterLast("/").nullIfEmpty() ?: text,
				source = source
			)
		}

		val authors = doc.select("a[href*='/artist/']").map { it.text().trim() }.toSet()

		return manga.copy(
			tags = tags,
			authors = authors,
			description = doc.selectFirst(".description")?.text()?.trim(),
			chapters = listOf(
				MangaChapter(
					id = generateUid(manga.url),
					title = "Doujin",
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
		return doc.select("img.swiper-lazy").map { img ->
			val url = (img.attr("data-src").takeIf { it.isNotEmpty() } ?: img.attr("src")).toAbsoluteUrl(domain)
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source
			)
		}
	}
}
