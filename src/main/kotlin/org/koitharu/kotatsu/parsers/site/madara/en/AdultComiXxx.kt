package org.koitharu.kotatsu.parsers.site.madara.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("ADULTCOMIXXX", "AdultComiXxx", "en", type = ContentType.HENTAI)
internal class AdultComiXxx(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.valueOf("ADULTCOMIXXX"), pageSize = 10) {

	override val configKeyDomain = ConfigKey.Domain("adultcomixxx.com")

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
		
		if (url.contains("/feed/")) {
			return doc.select("item").map { item ->
				val title = item.selectFirst("title")?.text()?.trim() ?: "Unknown"
				val link = item.selectFirst("link")?.text()?.trim() ?: ""
				val relativeUrl = link.toRelativeUrl(domain)
				
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
					state = null,
					authors = emptySet(),
					source = source,
				)
			}
		}

		return doc.select(".postitem, article, .post").mapNotNull { post ->
			val a = post.selectFirst("a") ?: return@mapNotNull null
			val href = a.attrAsRelativeUrl("href")
			val title = post.selectFirst("h2, h3, .title")?.text()?.trim() ?: a.text().trim()
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
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val tags = doc.select(".entry-meta a, .entry-footer a, .tags a, .xtags a")
			.map { tag ->
				MangaTag(
					title = tag.text().trim().removePrefix("📚").trim(),
					key = tag.attr("href").substringAfterLast("/").substringBefore(".html"),
					source = source,
				)
			}.toSet()

		val chapters = doc.select(".btn-chapter, a[href*='/chapter-']").mapNotNull { a ->
			val href = a.attrAsRelativeUrl("href")
			if (!href.contains("/chapter-")) return@mapNotNull null
			
			val title = a.text().trim()
			val number = title.substringAfter("Chapter").trim().toFloatOrNull() ?: 1f

			MangaChapter(
				id = generateUid(href),
				title = title,
				number = number,
				volume = 0,
				url = href,
				scanlator = null,
				uploadDate = 0L,
				branch = null,
				source = source,
			)
		}.distinctBy { it.url }.sortedByDescending { it.number }

		return manga.copy(
			tags = tags,
			description = doc.selectFirst(".entry-content p, .xbookin, .xtext")?.text()?.trim(),
			chapters = if (chapters.isNotEmpty()) chapters else listOf(
				MangaChapter(
					id = generateUid(manga.url),
					title = "Chapter 1",
					number = 1f,
					volume = 0,
					url = manga.url,
					scanlator = null,
					uploadDate = 0L,
					branch = null,
					source = source,
				)
			)
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		
		return doc.select("img").mapNotNull { img ->
			val url = img.attr("data-lazy-src").ifEmpty { img.attr("data-src") }.ifEmpty { img.attr("src") }
			if (url.isBlank() || url.contains("data:image") || url.contains("logo") || url.contains("icon")) {
				return@mapNotNull null
			}
			
			MangaPage(
				id = generateUid(url),
				url = url.toAbsoluteUrl(domain),
				preview = null,
				source = source,
			)
		}.distinctBy { it.url }
	}

	override suspend fun getPageUrl(page: MangaPage): String = page.url.orEmpty()
}
