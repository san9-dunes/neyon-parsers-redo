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

@MangaSourceParser("HENTAICLAP", "HentaiClap", type = ContentType.HENTAI)
internal class HentaiClap(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAICLAP, "hentaiclap.com") {
	override val selectUrlChapter = ".gt_btm a"
}

@Broken("WIP: Search not finished yet / WIP")
@MangaSourceParser("HENTAIHAND", "HentaiHand", type = ContentType.HENTAI)
internal class HentaiHand(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAIHAND, "hentaihand.com")

@MangaSourceParser("HENTAIHERE", "HentaiHere", type = ContentType.HENTAI)
internal class HentaiHere(context: MangaLoaderContext) :
        HentaiEraParser(context, MangaParserSource.HENTAIHERE, "hentaihere.com") {
	override val selectGallery = ".item"
	override val selectGalleryLink = "a"
	override val selectGalleryTitle = ".pos-rlt img"

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val html = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml().html()
		val imagesJson = html.substringAfter("var rff_imageList = ").substringBefore(";")
		if (imagesJson.isEmpty() || !imagesJson.startsWith("[")) {
			return super.getPages(chapter)
		}
		
		val images = org.json.JSONArray(imagesJson)
		return (0 until images.length()).map { i ->
			val path = images.getString(i)
			val url = "https://hentaicdn.com/hentai$path"
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}
}

@MangaSourceParser("HENTAIZAP", "HentaiZap", type = ContentType.HENTAI)internal class HentaiZap(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAIZAP, "hentaizap.com") {
	override val selectUrlChapter = ".gp_read a, .gt_btm a"
}

@MangaSourceParser("HENTAIHUG", "HentaiHug", type = ContentType.HENTAI)
internal class HentaiHug(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAIHUG, "hentaihug.com") {
	override val selectGallery = ".thumb"
	override val selectGalleryLink = "a"
	override val selectGalleryTitle = "a"
	override val selectGalleryImg = "img"

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val urlChapters = doc.selectFirst("#cover a, .cover a, .left_cover a, .gt_btm a")?.attr("href") ?: manga.url
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

@MangaSourceParser("EIGHTEENCOMIX", "18comix", type = ContentType.HENTAI)
internal class EighteenComix(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.valueOf("EIGHTEENCOMIX"), "18comix.org") {
	override val selectGallery = ".post, .article"
}

@MangaSourceParser("HDPORNCOMICS", "HDPornComics", type = ContentType.HENTAI)
internal class HDPornComics(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.valueOf("HDPORNCOMICS"), "hdporncomics.com")

@MangaSourceParser("CARTOONPORN_TO", "CartoonPorn.to", type = ContentType.HENTAI)
internal class CartoonPornTo(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.valueOf("CARTOONPORN_TO"), "cartoonporn.to") {
	override val selectGallery = "article"
}

@Broken
@MangaSourceParser("MHENTAI", "MHentai", type = ContentType.HENTAI)
internal class Mhentai(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.MHENTAI, "m-hentai.net")

@Broken
@MangaSourceParser("HENTAILAND", "HentaiLand", type = ContentType.HENTAI)
internal class HentaiLand(context: MangaLoaderContext) :
	HentaiEraParser(context, MangaParserSource.HENTAILAND, "hentailand.com")

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

internal abstract class HentaiEraParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : GalleryAdultsParser(context, source, defaultDomain, 25) {
	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
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
		"18comix.org",
		"hdporncomics.com",
		"cartoonporn.to",
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
		val urlChapters = doc.selectFirstOrThrow("#cover a, .cover a, .left_cover a, .gt_btm a").attr("href")
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

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val response = webClient.httpGet(chapter.url.toAbsoluteUrl(domain))
		val html = response.body!!.string()
		
		val imageTwistRegex = """imagetwist\.com[\\/]+[a-z0-9]+[\\/]+[^"'\s<>\\&]+""".toRegex(RegexOption.IGNORE_CASE)
		
		val pages = imageTwistRegex.findAll(html)
			.map { it.value.replace("\\/", "/").replace("\\", "") }
			.filter { it.contains(".html", ignoreCase = true) || it.contains("/i/") || it.contains("/th/", ignoreCase = true) }
			.distinct()
			.map { path ->
				val url = if (path.startsWith("http")) path else "https://$path"
				MangaPage(
					id = generateUid(url),
					url = url,
					preview = null,
					source = source,
				)
			}.toList()

		if (pages.isNotEmpty()) return pages

		// Fallback for native hosting
		val doc = org.jsoup.Jsoup.parse(html)
		return doc.select("img").mapNotNull { img ->
			val src = (img.attr("data-src").takeIf { it.isNotEmpty() } ?: img.attr("src"))
			if (src.isBlank() || src.contains("data:image") || src.contains("logo") || src.contains("icon")) {
				return@mapNotNull null
			}
			
			MangaPage(
				id = generateUid(src),
				url = src.toAbsoluteUrl(domain),
				preview = null,
				source = source
			)
		}.distinctBy { it.url }
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		if (!page.url.contains("imagetwist.com")) return page.url
		if (page.url.contains("imagetwist.com/i/")) return page.url
		
		val doc = webClient.httpGet(page.url).parseHtml()
		val imageUrl = doc.selectFirst("img.pic")?.requireSrc()
			?: doc.selectFirst(".pic[src]")?.requireSrc()
			?: doc.selectFirst("img[src*='/img/']")?.requireSrc()
		
		return imageUrl?.toAbsoluteUrl("imagetwist.com") ?: page.url
	}
}
