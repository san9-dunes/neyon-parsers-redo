package org.koitharu.kotatsu.parsers.site.id

import okhttp3.Headers
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.text.SimpleDateFormat
import java.util.*

@MangaSourceParser("DOUJINDESU", "DoujinDesu (.tv)", "id", type = ContentType.HENTAI)
internal class DoujinDesuParser(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.DOUJINDESU, pageSize = 18) {

	override val configKeyDomain: ConfigKey.Domain
		get() = ConfigKey.Domain("doujindesu.tv")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	override val availableSortOrders: Set<SortOrder>
		get() = EnumSet.of(SortOrder.UPDATED, SortOrder.NEWEST, SortOrder.POPULARITY)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions()

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("X-Requested-With", "XMLHttpRequest")
		.add("Referer", "https://$domain/")
		.build()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			if (!filter.query.isNullOrEmpty()) {
				append("/?s=")
				append(filter.query.urlEncoded())
			} else {
				when (order) {
					SortOrder.POPULARITY -> append("/manhwa")
					SortOrder.NEWEST -> append("/doujin")
					else -> append("/manga")
				}
			}
			if (page > 0) {
				append("/page/")
				append(page + 1)
				append("/")
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		return doc.select("#archives div.entries article, .animposx, .manga-item").mapNotNull { el ->
			val a = el.selectFirst("a") ?: return@mapNotNull null
			val href = a.attrAsRelativeUrl("href")
			val title = el.selectFirst("h3.title, .title, .manga-name")?.text()?.trim() ?: a.attr("title").trim()
			
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
		val info = doc.selectFirst("section.metadata") ?: doc.selectFirst(".metadata")!!
		
		val author = info.selectFirst("td:contains(Author) ~ td")?.text()
		val artist = info.selectFirst("td:contains(Artist) ~ td")?.text()
		
		val tags = doc.select(".tags a, .genre-info a").mapNotNullToSet { a ->
			val text = a.text().trim()
			if (text.isBlank()) return@mapNotNullToSet null
			MangaTag(title = text, key = text, source = source)
		}

		val chapterDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id"))
		val chapters = doc.select("#chapter_list li").mapChapters(reversed = true) { index, el ->
			val a = el.selectFirst(".lchx a") ?: el.selectFirst("a")!!
			val href = a.attrAsRelativeUrl("href")
			val dateText = el.selectFirst(".date")?.text()
			MangaChapter(
				id = generateUid(href),
				title = a.text().trim(),
				number = index + 1f,
				url = href,
				uploadDate = chapterDateFormat.parseSafe(dateText),
				source = source,
				scanlator = null,
				branch = null,
				volume = 0
			)
		}

		return manga.copy(
			authors = setOfNotNull(author, artist),
			tags = tags,
			description = doc.select(".description").text().trim(),
			chapters = chapters
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		val id = doc.select("#reader").attr("data-id").takeIf { it.isNotEmpty() }
		
		if (id != null) {
			val pagesDoc = webClient.httpPost("/themes/ajax/ch.php".toAbsoluteUrl(domain), "id=$id").parseHtml()
			return pagesDoc.select("img").map { img ->
				val url = img.requireSrc()
				MangaPage(
					id = generateUid(url),
					url = url,
					preview = null,
					source = source
				)
			}
		}

		// Fallback
		return doc.select("div#viewer img").map { img ->
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
