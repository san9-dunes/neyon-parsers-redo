package org.koitharu.kotatsu.parsers.site.all

import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("EAHENTAI", "EAHentai", type = ContentType.HENTAI)
internal class EAHentai(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.EAHENTAI, pageSize = 28) {

	override val configKeyDomain = ConfigKey.Domain("eahentai.com")

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
			if (filter.query.isNullOrEmpty()) {
				append("/?page=")
				append(pageNumber)
			} else {
				append("/search?keyword=")
				append(filter.query.urlEncoded())
				append("&page=")
				append(pageNumber)
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		val seen = HashSet<String>()
		return doc.select("a[href^=/a/][aria-label]").mapNotNull { a ->
			val href = a.attrAsRelativeUrl("href")
			if (!seen.add(href)) return@mapNotNull null
			val title = a.attr("aria-label").ifBlank {
				a.selectFirst("p")?.text().orEmpty()
			}
			if (title.isBlank()) return@mapNotNull null
			val coverUrl = a.selectFirst("img")?.attr("src")
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
		val tags = parseTags(doc.body())
		val authors = tags.filter { it.key.startsWith("artist/") || it.key.startsWith("group/") }
		val pagesCount = doc.selectFirst("span:matchesOwn(^Pages:\\s*$)")
			?.nextElementSibling()
			?.text()
			?.toIntOrNull()

		return manga.copy(
			tags = tags,
			authors = authors.mapToSet { it.title },
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
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		val seen = HashSet<String>()
		return doc.select("img[src*=/thumbnail/][src$=t.jpg]").mapNotNull { img ->
			val thumb = img.attr("src")
			if (thumb.isBlank()) return@mapNotNull null
			val full = thumb
				.replace("/thumbnail/", "/")
				.replace(Regex("t\\.jpg$"), ".jpg")
			if (!seen.add(full)) return@mapNotNull null
			MangaPage(
				id = generateUid(full),
				url = full,
				preview = thumb,
				source = source,
			)
		}
	}

	private fun parseTags(root: Element): Set<MangaTag> {
		return root.select(
			"a[href^=/tag/], a[href^=/parody/], a[href^=/character/], a[href^=/artist/], a[href^=/group/]",
		).mapNotNullToSet { a ->
			val href = a.attrAsRelativeUrlOrNull("href") ?: return@mapNotNullToSet null
			val title = a.selectFirst("span")?.text().orEmpty().ifBlank { a.ownText().ifBlank { a.text() } }
			if (title.isBlank()) return@mapNotNullToSet null
			MangaTag(
				title = title.toTitleCase(),
				key = href.removePrefix("/").removeSuffix("/"),
				source = source,
			)
		}
	}
}
