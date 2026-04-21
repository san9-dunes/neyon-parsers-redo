package org.koitharu.kotatsu.parsers.site.galleryadults.all

import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.galleryadults.GalleryAdultsParser
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("DOUJINDESUUK", "DoujinDesu.uk", type = ContentType.HENTAI)
internal class DoujinDesuUk(context: MangaLoaderContext) :
	GalleryAdultsParser(context, MangaParserSource.DOUJINDESUUK, "doujindesu.tv", 25) {
	override val selectGallery = ".entry"
	override val selectGalleryLink = "a"
	override val selectGalleryTitle = ".metadata a"
	override val pathTagUrl = "/tags?page="
	override val selectTags = "#tag-container"
	override val selectTag = "div.tag-container:contains(Tags) span.tags"
	override val selectAuthor = "div.tag-container:contains(Artists) a"
	override val selectLanguageChapter = "div.tag-container:contains(Languages) a"
	override val idImg = "image-container"
    override val selectGalleryImg = "img"

	override suspend fun getFilterOptions() = super.getFilterOptions().copy(
		availableLocales = setOf(
			Locale.ENGLISH,
			Locale.JAPANESE,
			Locale.CHINESE,
		),
	)

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	override fun parseMangaList(doc: Document): List<Manga> {
		val regexBrackets = Regex("\\[[^]]+]|\\([^)]+\\)")
		val regexSpaces = Regex("\\s+")
		return doc.select(selectGallery).map { div ->
			val href = div.selectFirstOrThrow(selectGalleryLink).attrAsRelativeUrl("href")
			Manga(
				id = generateUid(href),
				title = div.select(selectGalleryTitle).text().replace(regexBrackets, "")
					.replace(regexSpaces, " ")
					.trim(),
				altTitles = emptySet(),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = if (isNsfwSource) ContentRating.ADULT else null,
				coverUrl = div.selectLastOrThrow(selectGalleryImg).src(),
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		val doc = webClient.httpGet(page.url.toAbsoluteUrl(domain)).parseHtml()
		val root = doc.body()
		return root.requireElementById(idImg).selectFirstOrThrow("img").requireSrc()
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val urlChapters = doc.selectFirst("#cover a, .cover a, .left_cover a, .g_thumb a, .gallery_left a, .gt_left a")?.attr("href") ?: manga.url
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

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val pageNumber = page + 1
		val url = buildString {
			append("https://")
			append(domain)
			when {
				!filter.query.isNullOrEmpty() -> {
					append("/search/?q=")
					append(filter.query.urlEncoded())
					append("&")
				}

				filter.tags.isNotEmpty() -> {
					filter.tags.oneOrThrowIfMany()?.let {
						append("/tag/")
						append(it.key)
					}
					append("/?")
				}

				else -> {
					append("/manga/?")
				}
			}
			append("page=")
			append(pageNumber)
		}

		return parseMangaList(webClient.httpGet(url).parseHtml())
	}

}
