package org.koitharu.kotatsu.parsers.site.galleryadults.en

import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("FREECOMICS_XXX", "FreeComics.xxx", "en", ContentType.HENTAI)
internal class FreeComicsXxx(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.FREECOMICS_XXX, pageSize = 30) {

	override val configKeyDomain = ConfigKey.Domain("www.freecomics.xxx")

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("Referer", "https://$domain/")
		.build()

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.NEWEST)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = true, isMultipleTagsSupported = false)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions(
		availableTags = setOf(
			MangaTag("milf", "genre-milf", source),
			MangaTag("hentai", "genre-hentai", source),
			MangaTag("family", "genre-family", source),
			MangaTag("gangbang", "genre-gangbang", source),
			MangaTag("anime", "genre-anime", source),
			MangaTag("sister", "genre-sister", source),
			MangaTag("aunt", "genre-aunt", source),
			MangaTag("teacher", "genre-teacher", source),
			MangaTag("full color", "genre-full-color", source),
			MangaTag("futanari", "genre-futanari", source),
			MangaTag("cheating", "genre-cheating", source),
			MangaTag("cartoon", "genre-cartoon", source),
			MangaTag("webtoon", "genre-webtoon", source),
			MangaTag("ai gen", "genre-ai-gen", source),
			MangaTag("3d", "genre-3d", source),
			MangaTag("old man", "genre-old-man", source),
			MangaTag("western", "genre-western", source),
			MangaTag("indian", "genre-indian", source),
			MangaTag("big penis", "genre-big-penis", source),
			MangaTag("nun", "genre-nun", source),
			MangaTag("schoolgirl", "genre-schoolgirl", source),
			MangaTag("monster", "genre-monster", source),
			MangaTag("pregnant", "genre-pregnant", source),
			MangaTag("slave", "genre-slave", source),
			MangaTag("shemale", "genre-shemale", source),
			MangaTag("netorare", "genre-netorare", source),
			MangaTag("grandmother", "genre-grandmother", source),
			MangaTag("superhero", "genre-superhero", source),
			MangaTag("interracial", "genre-interracial", source),
			MangaTag("cousin", "genre-cousin", source),
			MangaTag("maid", "genre-maid", source),
			MangaTag("sleeping", "genre-sleeping", source),
			MangaTag("bdsm", "genre-bdsm", source),
			MangaTag("orgy", "genre-orgy", source),
			MangaTag("game", "genre-game", source),
			MangaTag("the incredibles", "genre-the-incredibles", source),
			MangaTag("ffm threesome", "genre-ffm-threesome", source),
			MangaTag("dilf", "genre-dilf", source),
			MangaTag("naruto", "genre-naruto", source),
			MangaTag("big ass", "genre-big-ass", source),
			MangaTag("defloration", "genre-defloration", source),
			MangaTag("pokemon", "genre-pokemon", source),
			MangaTag("anal", "genre-anal", source),
			MangaTag("mmf threesome", "genre-mmf-threesome", source),
			MangaTag("big breasts", "genre-big-breasts", source),
			MangaTag("retro", "genre-retro", source),
			MangaTag("hijab", "genre-hijab", source),
			MangaTag("femdom", "genre-femdom", source),
			MangaTag("impregnation", "genre-impregnation", source),
			MangaTag("orc", "genre-orc", source),
			MangaTag("tights", "genre-tights", source),
			MangaTag("harem", "genre-harem", source),
			MangaTag("yuri", "genre-yuri", source),
			MangaTag("hairy", "genre-hairy", source),
			MangaTag("lactation", "genre-lactation", source),
			MangaTag("the flintstones", "genre-the-flintstones", source),
			MangaTag("drunk", "genre-drunk", source),
			MangaTag("sex toys", "genre-sex-toys", source),
			MangaTag("bikini", "genre-bikini", source),
			MangaTag("muscle", "genre-muscle", source),
			MangaTag("policewoman", "genre-policewoman", source),
			MangaTag("giantess", "genre-giantess", source),
			MangaTag("tentacles", "genre-tentacles", source),
			MangaTag("dark skin", "genre-dark-skin", source),
			MangaTag("furry", "genre-furry", source),
			MangaTag("voyeurism", "genre-voyeurism", source),
			MangaTag("gravity falls", "genre-gravity-falls", source),
			MangaTag("x-ray", "genre-x-ray", source),
			MangaTag("ahegao", "genre-ahegao", source),
			MangaTag("nakadashi", "genre-nakadashi", source),
			MangaTag("blowjob", "genre-blowjob", source),
			MangaTag("bisexual", "genre-bisexual", source),
			MangaTag("cunnilingus", "genre-cunnilingus", source),
			MangaTag("spider-man", "genre-spider-man", source),
			MangaTag("stockings", "genre-stockings", source),
			MangaTag("kim possible", "genre-kim-possible", source),
			MangaTag("humiliation", "genre-humiliation", source),
			MangaTag("small breasts", "genre-small-breasts", source),
			MangaTag("superheroine", "genre-superheroine", source),
			MangaTag("bondage", "genre-bondage", source),
			MangaTag("masturbation", "genre-masturbation", source),
			MangaTag("bukkake", "genre-bukkake", source),
			MangaTag("nurse", "genre-nurse", source),
			MangaTag("overwatch", "genre-overwatch", source),
			MangaTag("swimsuit", "genre-swimsuit", source),
			MangaTag("tall girl", "genre-tall-girl", source),
			MangaTag("star wars", "genre-star-wars", source),
			MangaTag("group", "genre-group", source),
			MangaTag("paizuri", "genre-paizuri", source),
			MangaTag("spanking", "genre-spanking", source),
			MangaTag("glasses", "genre-glasses", source),
			MangaTag("strap-on", "genre-strap-on", source),
			MangaTag("witch", "genre-witch", source),
			MangaTag("footjob", "genre-footjob", source),
			MangaTag("foot licking", "genre-foot-licking", source),
			MangaTag("blood", "genre-blood", source),
			MangaTag("robot", "genre-robot", source),
			MangaTag("novel", "genre-novel", source),
			MangaTag("bald", "genre-bald", source),
			MangaTag("piercing", "genre-piercing", source),
			MangaTag("eyemask", "genre-eyemask", source),
			MangaTag("fisting", "genre-fisting", source),
			MangaTag("cosplaying", "genre-cosplaying", source),
			MangaTag("alien", "genre-alien", source),
			MangaTag("elf", "genre-elf", source),
			MangaTag("monster girl", "genre-monster-girl", source),
			MangaTag("pantyhose", "genre-pantyhose", source),
			MangaTag("pirate", "genre-pirate", source),
			MangaTag("artbook", "genre-artbook", source),
			MangaTag("midget", "genre-midget", source),
			MangaTag("comedy", "genre-comedy", source),
			MangaTag("succubus", "genre-succubus", source),
			MangaTag("tail", "genre-tail", source),
			MangaTag("garter belt", "genre-garter-belt", source),
		)
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			append("/")
			if (!filter.query.isNullOrEmpty()) {
				append("?search=")
				append(filter.query.urlEncoded())
				if (page > 0) {
					append("&page=")
					append(page + 1)
				}
			} else if (filter.tags.isNotEmpty()) {
				append(filter.tags.first().key)
				append("-page-")
				append(page + 1)
				append(".html")
			} else {
				// Default to hentai genre as "latest"
				append("genre-hentai-page-")
				append(page + 1)
				append(".html")
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		return parseMangaList(doc)
	}

	private fun parseMangaList(doc: Document): List<Manga> {
		return doc.select(".xcpreview").map { item ->
			val a = item.selectFirstOrThrow("a")
			val trackingUrl = a.attr("href")
			val realUrl = trackingUrl.toHttpUrl().queryParameter("url")
				?: trackingUrl.substringAfter("url=", trackingUrl)

			val relativeUrl = realUrl.toAbsoluteUrl(domain).toRelativeUrl(domain)
			val title = a.attr("title").ifEmpty { item.select(".xcpin").text() }.trim()
			val coverUrl = item.selectFirstOrThrow("img").requireSrc()

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
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()

		val tags = doc.select(".xtags a, .tags a, .bookinfo a[href*='genre-']")
			.mapNotNull { it.toMangaTagOrNull() }
			.toSet()

		val authors = doc.select(".bookinfo a[href*='artist-']")
			.map { it.text().trim() }
			.toSet()

		return manga.copy(
			tags = tags,
			authors = authors,
			description = doc.selectFirst(".xbookin")?.text()?.trim(),
			chapters = listOf(
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
				),
			),
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		return doc.select("img.lazy").map { img ->
			val url = img.attr("data-src").ifEmpty { img.attr("src") }.toAbsoluteUrl(domain)
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}

	private fun org.jsoup.nodes.Element.toMangaTagOrNull(): MangaTag? {
		val title = text().trim().removePrefix("📚").trim().ifBlank { return null }
		val href = attr("href")
		val key = href.toHttpUrl().pathSegments.lastOrNull()?.removeSuffix(".html")?.substringBefore("-page-")
			?: title.lowercase().replace(" ", "-")
		
		return MangaTag(
			title = title,
			key = key,
			source = source,
		)
	}
}
