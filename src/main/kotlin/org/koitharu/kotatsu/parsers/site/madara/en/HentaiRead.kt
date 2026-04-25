package org.koitharu.kotatsu.parsers.site.madara.en

import okhttp3.Headers
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNull
import java.util.*

@MangaSourceParser("HENTAIREAD", "HentaiRead", "en", ContentType.HENTAI)
internal class HentaiRead(context: MangaLoaderContext) :
        PagedMangaParser(context, MangaParserSource.HENTAIREAD, pageSize = 30) {

        override val configKeyDomain = ConfigKey.Domain("hentairead.com")

        override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
                super.onCreateConfig(keys)
                keys.add(userAgentKey)
                keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
        }

        override fun getRequestHeaders(): Headers = Headers.Builder()
                .add("Referer", "https://$domain/")
                .build()

        override val availableSortOrders: Set<SortOrder> = EnumSet.of(
                SortOrder.UPDATED,
                SortOrder.NEWEST,
                SortOrder.POPULARITY
        )

        override val filterCapabilities: MangaListFilterCapabilities
                get() = MangaListFilterCapabilities(isSearchSupported = true)

        override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

        override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
                val url = buildString {
                        append("https://")
                        append(domain)
                        append("/")
                        if (!filter.query.isNullOrEmpty()) {
                                append("?s=")
                                append(filter.query.urlEncoded())
                                if (page > 0) {
                                        append("&page=")
                                        append(page + 1)
                                }
                        } else {
                                append("hentai/")
                                when (order) {
                                        SortOrder.POPULARITY -> append("hentai-list/all/any/all/most-popular/")
                                        SortOrder.NEWEST -> {} // default is newest
                                        else -> {}
                                }
                                if (page > 0) {
                                        append("page/")
                                        append(page + 1)
                                        append("/")
                                }
                        }
                }

                val doc = webClient.httpGet(url).parseHtml()
                return doc.select(".manga-item").mapNotNull { item ->
                        val a = item.selectFirst("a[href*='/hentai/']") ?: return@mapNotNull null
                        val href = a.attrAsRelativeUrl("href")
                        val title = item.selectFirst(".manga-item__name, .title")?.text()?.trim() 
                                ?: a.attr("title").ifEmpty { item.select("img").attr("alt") }

                        Manga(
                                id = generateUid(href),
                                title = title,
                                altTitles = emptySet(),
                                url = href,
                                publicUrl = href.toAbsoluteUrl(domain),
                                rating = RATING_UNKNOWN,
                                contentRating = ContentRating.ADULT,
                                coverUrl = item.selectFirst("img")?.requireSrc(),
                                tags = emptySet(),
                                state = null,
                                authors = emptySet(),
                                source = source,
                        )
                }
        }

        override suspend fun getDetails(manga: Manga): Manga {
                val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()

                val tags = doc.select("a[href*='/tag/']").map { tag ->
                        val text = tag.text().substringBefore("\n").trim()
                        MangaTag(
                                title = text,
                                key = tag.attr("href").substringAfter("/tag/").substringBefore("/"),
                                source = source,
                        )
                }.toSet()

                val authors = doc.select("a[href*='/artist/']").map { it.text().trim() }.toSet()

                val readerUrl = doc.selectFirst("a:contains(Read Now)")?.attrAsRelativeUrl("href")
                        ?: "${manga.url.removeSuffix("/")}/english/p/1/"

                return manga.copy(
                        tags = tags,
                        authors = authors,
                        description = doc.selectFirst(".manga-excerpt")?.text()?.trim(),
                        chapters = listOf(
                                MangaChapter(
                                        id = generateUid(readerUrl),
                                        title = "Comic",
                                        number = 1f,
                                        volume = 0,
                                        url = readerUrl,
                                        scanlator = null,
                                        uploadDate = 0L,
                                        branch = null,
                                        source = source,
                                ),
                        ),
                )
        }

        override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
                val response = webClient.httpGet(chapter.url.toAbsoluteUrl(domain))
                val html = response.body!!.string()
                
                // Find the base64 string in any global variable assignment
                // Pattern matches window.variableName = "base64..." or var variableName = "base64..."
                val base64Regex = """(?:window\.|var\s+)([a-zA-Z0-9_$]+)\s*=\s*['"]([a-zA-Z0-9+/=\s]{1000,})['"]""".toRegex()
                
                val match = base64Regex.findAll(html).lastOrNull() 
                        ?: throw ParseException("Could not find image data in script tags.", chapter.url)
                
                val base64Json = match.groupValues[2].replace(Regex("\\s"), "")
                val jsonStr = String(Base64.getDecoder().decode(base64Json))
                val json = JSONObject(jsonStr)
                
                // Extract images from collection[0].chapter.images
                val collection = json.optJSONArray("collection") ?: return emptyList()
                val firstItem = collection.optJSONObject(0) ?: return emptyList()
                val images = firstItem.optJSONObject("chapter")?.optJSONArray("images") ?: return emptyList()
                
                return images.mapJSONNotNull { img ->
                        val src = img.getString("src")
                        // Base URL usually comes from chapterExtraData.baseUrl in JS, defaulting to henread.xyz
                        val url = "https://henread.xyz/$src"
                        MangaPage(
                                id = generateUid(url),
                                url = url,
                                preview = null,
                                source = source,
                        )
                }
        }
}
