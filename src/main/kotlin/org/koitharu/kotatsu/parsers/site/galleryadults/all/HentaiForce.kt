package org.koitharu.kotatsu.parsers.site.galleryadults.all

import org.jsoup.internal.StringUtil
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.galleryadults.GalleryAdultsParser
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("HENTAIFORCE", "HentaiForce", type = ContentType.HENTAI)
internal class HentaiForce(context: MangaLoaderContext) :
	HentaiForceParser(context, MangaParserSource.HENTAIFORCE, "hentaiforce.net")

@MangaSourceParser("FHENTAI", "FHentai", type = ContentType.HENTAI)
internal class FHentaiParser(
	context: MangaLoaderContext,
) : PagedMangaParser(context, MangaParserSource.FHENTAI, pageSize = 30) {

	override val configKeyDomain = ConfigKey.Domain("fhentai.net")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = true)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = when {
			!filter.query.isNullOrEmpty() -> "https://$domain/search?q=${filter.query.urlEncoded()}"
			page > 0 -> "https://$domain/?page=${page + 1}"
			else -> "https://$domain/"
		}
		val doc = webClient.httpGet(url).parseHtml()
		return parseList(doc)
	}

	private fun parseList(doc: org.jsoup.nodes.Document): List<Manga> {
		val seen = HashSet<String>()
		return doc.select("a[href^=/f/]").mapNotNull { a ->
			val href = a.attrAsRelativeUrlOrNull("href")?.substringBefore('#') ?: return@mapNotNull null
			if (!seen.add(href)) return@mapNotNull null
			val title = a.attr("title").ifBlank { a.text().trim() }
			if (title.isBlank()) return@mapNotNull null

			Manga(
				id = generateUid(href),
				title = title,
				altTitles = emptySet(),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = null,
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val readUrl = doc.selectFirst("a[href^=/read/]")?.attrAsRelativeUrlOrNull("href")
			?: "/read/${manga.url.substringAfterLast('/')}"

		val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
			?.substringBefore(" [")
			?.takeIf { it.isNotBlank() }
			?: manga.title

		val description = doc.selectFirst("meta[name=description]")?.attr("content")

		val tags = doc.select("a[href^=/tags/], a[href^=/artists/], a[href^=/series/]").mapNotNullToSet { a ->
			val href = a.attrAsRelativeUrlOrNull("href") ?: return@mapNotNullToSet null
			val key = href.removeSuffix('/').substringAfterLast('/')
			if (key.isBlank()) return@mapNotNullToSet null
			val name = a.text().trim()
			if (name.isBlank()) return@mapNotNullToSet null
			MangaTag(
				key = key,
				title = name,
				source = source,
			)
		}

		val chapter = MangaChapter(
			id = generateUid(readUrl),
			title = null,
			number = 1f,
			volume = 0,
			url = readUrl,
			scanlator = null,
			uploadDate = 0,
			branch = null,
			source = source,
		)

		return manga.copy(
			title = title,
			description = description,
			tags = tags,
			chapters = listOf(chapter),
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		val pages = doc.select("#gallery-preview-grid img[src], img[src*='/api/v1/images/']")
			.mapNotNull { img -> img.src()?.takeIf { it.isNotBlank() } }
			.distinct()
		return pages.map { imgUrl ->
			val relative = imgUrl.toRelativeUrl(domain)
			MangaPage(
				id = generateUid(relative),
				url = relative,
				preview = null,
				source = source,
			)
		}
	}

	override suspend fun getPageUrl(page: MangaPage): String = page.url.orEmpty().toAbsoluteUrl(domain)
}

internal abstract class HentaiForceParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : GalleryAdultsParser(context, source, defaultDomain, pageSize = 30) {
	override val configKeyDomain = ConfigKey.Domain(defaultDomain)

	override val selectGallery = ".gallery"
	override val selectGalleryLink = "a.gallery-thumb"
	override val pathTagUrl = "/tags/popular/"
	override val selectTags = ".tag-listing"
	override val selectUrlChapter = "#gallery-main-cover a"
	override val selectTag = "div.tag-container:contains(Tags:)"
	override val selectAuthor = "div.tag-container:contains(Artists:) a"
	override val selectLanguageChapter = "div.tag-container:contains(Languages:) a"
	override val idImg = ".gallery-reader-img-wrapper img"

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isMultipleTagsSupported = true,
		)

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY)

	override suspend fun getFilterOptions(): MangaListFilterOptions {
		return super.getFilterOptions().copy(
			availableLocales = setOf(
				Locale.ENGLISH,
				Locale.FRENCH,
				Locale.JAPANESE,
				Locale.CHINESE,
				Locale("es"),
				Locale("ru"),
				Locale("ko"),
				Locale.GERMAN,
				Locale("id"),
				Locale.ITALIAN,
				Locale("pt"),
				Locale("th"),
				Locale("vi"),
			),
		)
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		val doc = webClient.httpGet(page.url.toAbsoluteUrl(domain)).parseHtml()
		return doc.selectFirstOrThrow(idImg).requireSrc()
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			when {
				!filter.query.isNullOrEmpty() -> {
					append("/search?q=")
					append(filter.query.urlEncoded())
					append("&page=")
				}

				else -> {
					if (filter.tags.size > 1 || (filter.tags.isNotEmpty() && filter.locale != null)) {
						append("/search?q=")
						append(buildQuery(filter.tags, filter.locale))
						if (order == SortOrder.POPULARITY) {
							append("&sort=popular")
						}
						append("&page=")
					} else if (filter.tags.isNotEmpty()) {
						filter.tags.oneOrThrowIfMany()?.let {
							append("/tag/")
							append(it.key)
						}
						append("/")

						if (order == SortOrder.POPULARITY) {
							append("popular/")
						}
						append("?")
					} else if (filter.locale != null) {
						append("/language/")
						append(filter.locale.toLanguagePath())
						append("/")

						if (order == SortOrder.POPULARITY) {
							append("popular/")
						}
						append("?")
					} else {
						append("/page/")
					}
				}
			}
			append(page.toString())
		}

		return parseMangaList(webClient.httpGet(url).parseHtml())
	}

	private fun buildQuery(tags: Collection<MangaTag>, language: Locale?): String {
		val joiner = StringUtil.StringJoiner(" ")
		tags.forEach { tag ->
			joiner.add("tag:\"")
			joiner.append(tag.key)
			joiner.append("\"")
		}
		language?.let { lc ->
			joiner.add("language:\"")
			joiner.append(lc.toLanguagePath())
			joiner.append("\"")
		}
		return joiner.complete()
	}
}
