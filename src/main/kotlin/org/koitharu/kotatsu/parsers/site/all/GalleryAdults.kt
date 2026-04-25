package org.koitharu.kotatsu.parsers.site.all

import okhttp3.Headers
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("PORNCOMICS_CLOUD", "PornComics.cloud", type = ContentType.HENTAI)
internal class PornComicsCloud(context: MangaLoaderContext) :
	GalleryAdultsCustomParser(context, MangaParserSource.valueOf("PORNCOMICS_CLOUD"), "porncomics.cloud")

@MangaSourceParser("PORNCOMICS_PICS", "PornComics.pics", type = ContentType.HENTAI)
internal class PornComicsPics(context: MangaLoaderContext) :
	GalleryAdultsCustomParser(context, MangaParserSource.valueOf("PORNCOMICS_PICS"), "porncomics.pics")

@MangaSourceParser("EGGPORNCOMICS", "EggPornComics", type = ContentType.HENTAI)
internal class EggPornComics(context: MangaLoaderContext) :
	GalleryAdultsCustomParser(context, MangaParserSource.valueOf("EGGPORNCOMICS"), "eggporncomics.com")

internal abstract class GalleryAdultsCustomParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : PagedMangaParser(context, source, pageSize = 30) {

	override val configKeyDomain = ConfigKey.Domain(defaultDomain)

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
		val url = buildString {
			append("https://")
			append(domain)
			if (!filter.query.isNullOrEmpty()) {
				append("/search/")
				append(filter.query.replace(" ", "-").urlEncoded())
			} else {
				append("/latest-comics")
			}
			if (page > 0) {
				append("?page=")
				append(page + 1)
			}
		}

		val doc = webClient.httpGet(url).parseHtml()
		return doc.select("div.preview, div.comic-item").mapNotNull { el ->
			val a = el.selectFirst("a") ?: return@mapNotNull null
			val href = a.attrAsRelativeUrl("href")
			
			val title = el.selectFirst(".name, .title, h2")?.text()?.trim() ?: a.attr("title").trim()
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
		}.distinctBy { it.url }
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()

		val tags = doc.select("a[href*='/category/'], a[href*='/tag/']").mapNotNullToSet { a ->
			val text = a.text().trim()
			if (text.isBlank()) return@mapNotNullToSet null
			MangaTag(title = text, key = text, source = source)
		}

		return manga.copy(
			tags = tags,
			description = doc.selectFirst(".description, .entry-content")?.text()?.trim(),
			chapters = listOf(
				MangaChapter(
					id = generateUid(manga.url),
					title = "Comic",
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
		return doc.select("div.grid div.image img, .chapter-container img").map { img ->
			val src = (img.attr("data-src").takeIf { it.isNotEmpty() } ?: img.attr("src"))
			// Transform thumbnail to original: remove thumb300_ or similar prefixes
			val url = src.replace(Regex("""thumb\d+_"""), "").toAbsoluteUrl(domain)
			
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source
			)
		}
	}
}
