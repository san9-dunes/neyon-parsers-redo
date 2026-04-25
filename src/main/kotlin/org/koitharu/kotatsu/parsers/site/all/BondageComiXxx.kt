package org.koitharu.kotatsu.parsers.site.all

import okhttp3.Headers
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("BONDAGECOMIXXX", "BondageComiXxx.net", type = ContentType.HENTAI)
internal class BondageComiXxx(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.BONDAGECOMIXXX, pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("bondagecomixxx.net")

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
		}

		val doc = webClient.httpGet(url).parseHtml()
		return doc.select("item").mapNotNull { item ->
			val title = item.selectFirst("title")?.text()?.trim() ?: return@mapNotNull null
			val link = item.selectFirst("link")?.nextSibling()?.toString()?.trim() 
				?: item.selectFirst("link")?.text()?.trim() 
				?: return@mapNotNull null
			
			val guid = item.selectFirst("guid")?.text() ?: link
			
			Manga(
				id = generateUid(guid),
				title = title,
				altTitles = emptySet(),
				url = guid, 
				publicUrl = link,
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
		val feedUrl = "https://$domain/feed/"
		val doc = webClient.httpGet(feedUrl).parseHtml()
		val item = doc.select("item").find { 
			it.selectFirst("guid")?.text() == manga.url || it.selectFirst("link")?.text() == manga.publicUrl 
		}

		val tags = item?.select("category")?.map { tag ->
			MangaTag(title = tag.text(), key = tag.text(), source = source)
		}?.toSet() ?: emptySet()

		return manga.copy(
			tags = tags,
			chapters = listOf(
				MangaChapter(
					id = manga.id,
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
		val response = webClient.httpGet(chapter.url.toAbsoluteUrl(domain).let { 
			if (it.contains("?")) "$it&nonitro=1" else "$it?nonitro=1"
		})
		val html = response.body!!.string()
		
		// Broadest possible regex for imagetwist links. 
		// We look for patterns like "imagetwist.com/..." and "nitro-lazy-src=...imagetwist.com/..."
		val imageTwistRegex = """imagetwist\.com[\\/]+[a-z0-9]+[\\/]+[^"'\s<>\\&]+""".toRegex(RegexOption.IGNORE_CASE)
		
		return imageTwistRegex.findAll(html)
			.map { it.value.replace("\\/", "/").replace("\\", "") }
			.filter { it.contains(".html", ignoreCase = true) || it.contains("/i/", ignoreCase = true) || it.contains("/th/", ignoreCase = true) }
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
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		if (page.url.contains("imagetwist.com/i/")) return page.url
		
		val doc = webClient.httpGet(page.url).parseHtml()
		val imageUrl = doc.selectFirst("img.pic")?.requireSrc()
			?: doc.selectFirst(".pic[src]")?.requireSrc()
			?: doc.selectFirst("img[src*='/img/']")?.requireSrc()
		
		return imageUrl?.toAbsoluteUrl("imagetwist.com") ?: page.url
	}
}
