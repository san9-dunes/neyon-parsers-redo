package org.koitharu.kotatsu.parsers.site.galleryadults.all

import org.koitharu.kotatsu.parsers.Broken

import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.galleryadults.GalleryAdultsParser
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("HENTAIERA", "HentaiEra", type = ContentType.HENTAI)
internal class HentaiEra(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAIERA, "hentaiera.com")

@Broken("WIP: Search not finished yet / WIP")
@MangaSourceParser("HENTAIHAND", "HentaiHand", type = ContentType.HENTAI)
internal class HentaiHand(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAIHAND, "hentaihand.com")

@MangaSourceParser("HENTAIHERE", "HentaiHere", type = ContentType.HENTAI)
internal class HentaiHere(context: MangaLoaderContext) :
        HentaiEraParser(context, MangaParserSource.HENTAIHERE, "hentaihere.com") {
	override val selectGallery = ".seriesBlock"
	override val selectGalleryLink = ".padder-v-top a"
	override val selectGalleryTitle = ".padder-v-top a"
}

@MangaSourceParser("HENTAIZAP", "HentaiZap", type = ContentType.HENTAI)internal class HentaiZap(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAIZAP, "hentaizap.com")

@Broken
@MangaSourceParser("SIMPLYHENTAI", "SimplyHentai", type = ContentType.HENTAI)
internal class SimplyHentai(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.SIMPLYHENTAI, "simplyhentai.com")

@Broken
@MangaSourceParser("MHENTAI", "MHentai", type = ContentType.HENTAI)
internal class Mhentai(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.MHENTAI, "m-hentai.net")

@Broken
@MangaSourceParser("HENTAILAND", "HentaiLand", type = ContentType.HENTAI)
internal class HentaiLand(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAILAND, "hentailand.com")

@MangaSourceParser("HENTAIHUG", "HentaiHug", type = ContentType.HENTAI)
internal class HentaiHug(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAIHUG, "hentaihug.com") {
	override val selectGallery = ".thumb"
	override val selectGalleryLink = "a"

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val urlChapters = doc.selectFirst("#cover a, .cover a, .left_cover a")?.attr("href") ?: manga.url
		val tag = doc.selectFirst(selectTag)?.parseTags()
		val branch = doc.select(selectLanguageChapter).joinToString(separator = " / ") {
			it.text()
		}
		val author = doc.selectFirst(selectAuthor)?.text()
		return manga.copy(
			tags = tag.orEmpty(),
			authors = setOfNotNull(author),
			chapters = listOf(
				MangaChapter(
					id = manga.id,
					title = manga.title,
					number = 1f,
					volume = 0,
					url = urlChapters,
					scanlator = null,
					uploadDate = 0,
					branch = branch,
					source = source,
				),
			),
		)
	}
}

@MangaSourceParser("HENTAINAME", "Hentai.name", type = ContentType.HENTAI)
internal class HentaiName(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAINAME, "hentai.name") {
	override val selectGallery = ".gallery"
	override val selectGalleryLink = "a"
	override val selectGalleryTitle = ".caption"

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (filter.query.isNullOrEmpty() && filter.tags.isEmpty() && filter.locale == null) {
			val pageNumber = page + 1
			val url = "https://$domain/new/?p=$pageNumber"
			return parseMangaList(webClient.httpGet(url).parseHtml())
		}
		return super.getListPage(page, order, filter)
	}
}

@Broken("WIP: Search not finished yet / WIP")
@MangaSourceParser("HENTAIKISU", "HentaiKisu", type = ContentType.HENTAI)
internal class HentaiKisu(context: MangaLoaderContext) :
	HentaiKisuParser(context)

@Broken("WIP: Search not finished yet / WIP")
@MangaSourceParser("HENTAILOOP", "HentaiLoop", type = ContentType.HENTAI)
internal class HentaiLoop(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAILOOP, "hentailoop.com")

internal abstract class HentaiEraParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : GalleryAdultsParser(context, source, defaultDomain, 25) {
	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	override val configKeyDomain = ConfigKey.Domain(
		defaultDomain,
		"hentaiera.com",
		"hentaiclap.com",
		"hentaihand.com",
		"hentaihere.com",
		"hentaizap.com",
		"simplyhentai.com",
		"simply-hentai.com",
		"m-hentai.net",
		"hentailand.com",
		"hentaipaw.com",
		"hentaihug.com",
		"hentai.name",
		"hentaikisu.com",
		"hentailoop.com",
	)

	override val selectTags = ".tags_section"
	override val selectTag = ".galleries_info li:contains(Tags) div.info_tags"
	override val selectAuthor = ".galleries_info li:contains(Artists) span.item_name"
	override val selectLanguageChapter = ".galleries_info li:contains(Languages) div.info_tags .item_name"

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isMultipleTagsSupported = true,
		)

	override suspend fun getFilterOptions() = super.getFilterOptions().copy(
		availableLocales = setOf(
			Locale.ENGLISH,
			Locale.FRENCH,
			Locale.JAPANESE,
			Locale("es"),
			Locale("ru"),
			Locale("ko"),
			Locale.GERMAN,
		),
	)

	override fun Element.parseTags() = select("a.tag, .gallery_title a").mapToSet {
		val key = it.attr("href").removeSuffix('/').substringAfterLast('/')
		val name = it.selectFirst(".item_name")?.text() ?: it.text()
		MangaTag(
			key = key,
			title = name,
			source = source,
		)
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val pageNumber = page + 1
		val url = buildString {
			append("https://")
			append(domain)
			when {

				!filter.query.isNullOrEmpty() -> {
					append("/search/?key=")
					append(filter.query.urlEncoded())
					append("&")
				}

				else -> {
					if (filter.tags.size > 1 || (filter.tags.isNotEmpty() && filter.locale != null)) {
						append("/search/?key=")
						if (order == SortOrder.POPULARITY) {
							append(
								buildQuery(filter.tags, filter.locale)
									.replace("&lt=1&dl=0&pp=0&tr=0", "&lt=0&dl=0&pp=1&tr=0"),
							)
						} else {
							append(buildQuery(filter.tags, filter.locale))
						}
						append("&")
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
						append("/?")
					}
				}
			}
			append("page=")
			append(pageNumber.toString())
		}

		return parseMangaList(webClient.httpGet(url).parseHtml())
	}

	private fun buildQuery(tags: Collection<MangaTag>, locale: Locale?): String {
		val queryDefault =
			"&search=&mg=1&dj=1&ws=1&is=1&ac=1&gc=1&en=0&jp=0&es=0&fr=0&kr=0&de=0&ru=0&lt=1&dl=0&pp=0&tr=0"
		val tag = tags.joinToString(" ", postfix = " ") { it.key }
		val queryMod = when (val lp = locale?.toLanguagePath()) {
			"english" -> queryDefault.replace("en=0", "en=1")
			"japanese" -> queryDefault.replace("jp=0", "jp=1")
			"spanish" -> queryDefault.replace("es=0", "es=1")
			"french" -> queryDefault.replace("fr=0", "fr=1")
			"korean" -> queryDefault.replace("kr=0", "kr=1")
			"russian" -> queryDefault.replace("ru=0", "ru=1")
			"german" -> queryDefault.replace("de=0", "de=1")
			null -> "&search=&mg=1&dj=1&ws=1&is=1&ac=1&gc=1&en=1&jp=1&es=1&fr=1&kr=1&de=1&ru=1&lt=1&dl=0&pp=0&tr=0"
			else -> throw IllegalArgumentException("Unsupported locale: $lp")
		}
		return tag + queryMod
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val urlChapters = doc.selectFirstOrThrow("#cover a, .cover a, .left_cover a").attr("href")
		val tag = doc.selectFirst(selectTag)?.parseTags()
		val branch = doc.select(selectLanguageChapter).joinToString(separator = " / ") {
			it.text()
		}
		val author = doc.selectFirst(selectAuthor)?.text()
		return manga.copy(
			tags = tag.orEmpty(),
			authors = setOfNotNull(author),
			chapters = listOf(
				MangaChapter(
					id = manga.id,
					title = manga.title,
					number = 1f,
					volume = 0,
					url = urlChapters,
					scanlator = null,
					uploadDate = 0,
					branch = branch,
					source = source,
				),
			),
		)
	}
}

internal open class HentaiKisuParser(
	context: MangaLoaderContext,
) : PagedMangaParser(context, MangaParserSource.HENTAIKISU, pageSize = 24) {

	override val configKeyDomain = ConfigKey.Domain("hentaikisu.com")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = true)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val pageNumber = page + 1
		val url = when {
			!filter.query.isNullOrEmpty() -> "https://$domain/search?s=${filter.query.urlEncoded()}"
			page > 0 -> "https://$domain/?page=$pageNumber"
			else -> "https://$domain/"
		}
		val doc = webClient.httpGet(url).parseHtml()
		return parseList(doc)
	}

	private fun parseList(doc: org.jsoup.nodes.Document): List<Manga> {
		val seen = HashSet<String>()
		return doc.select("#ipage .book-list a[href], #ipage .book-list a[href^=/g/], #ipage .book-list a[href^=g/]")
			.mapNotNull { a ->
				val href = a.attrAsRelativeUrlOrNull("href")?.substringBefore('#') ?: return@mapNotNull null
				if (!href.contains("/g/") && !href.startsWith("g/")) return@mapNotNull null
				if (!seen.add(href)) return@mapNotNull null

				val title = a.selectFirst(".book-description p")?.text()?.trim()
					?: a.attr("title").substringBefore(" online hentai").substringAfter("read ").trim()
				if (title.isBlank()) return@mapNotNull null

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

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val title = doc.selectFirst("#info h1")?.text()?.trim().orEmpty().ifBlank { manga.title }
		val author = doc.select(".tag-container:contains(Artist:) a").firstOrNull()?.text()?.trim()
		val chapter = MangaChapter(
			id = manga.id,
			title = null,
			number = 1f,
			volume = 0,
			url = manga.url,
			scanlator = null,
			uploadDate = 0,
			branch = null,
			source = source,
		)

		return manga.copy(
			title = title,
			description = doc.selectFirst("meta[name=description]")?.attr("content"),
			authors = setOfNotNull(author),
			tags = doc.select("section#tags a.tag").mapNotNullToSet { tag ->
				val href = tag.attrAsRelativeUrlOrNull("href") ?: return@mapNotNullToSet null
				val key = href.substringAfterLast('/').substringAfterLast('=').trim()
				if (key.isBlank()) return@mapNotNullToSet null
				val name = tag.text().trim()
				if (name.isBlank()) return@mapNotNullToSet null
				MangaTag(
					key = key,
					title = name,
					source = source,
				)
			},
			chapters = listOf(chapter),
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		val pages = doc.select("img.thum_pre[data-src], img.thum_pre[src]")
			.mapNotNull { img -> img.src()?.takeIf { it.isNotBlank() } }
			.distinct()
		return pages.map { imageUrl ->
			val relative = imageUrl.toRelativeUrl(domain)
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
