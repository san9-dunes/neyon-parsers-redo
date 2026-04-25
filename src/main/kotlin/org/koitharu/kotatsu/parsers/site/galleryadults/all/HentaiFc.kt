package org.koitharu.kotatsu.parsers.site.galleryadults.all

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.galleryadults.GalleryAdultsParser
import org.koitharu.kotatsu.parsers.util.*
import java.util.EnumSet

@MangaSourceParser("HENTAIFC", "HentaiFC", type = ContentType.HENTAI)
internal class HentaiFc(
    context: MangaLoaderContext
) : GalleryAdultsParser(context, MangaParserSource.HENTAIFC, "hentaifc.com", pageSize = 30) {

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        super.onCreateConfig(keys)
        keys.add(userAgentKey)
        keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
    }

    override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY)
    
    override val filterCapabilities: MangaListFilterCapabilities
        get() = super.filterCapabilities.copy(
            isMultipleTagsSupported = true,
        )

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = buildString {
            append("https://")
            append(domain)
            
            if (!filter.query.isNullOrEmpty()) {
                if (page > 1) {
                    append("/page/")
                    append(page)
                } else {
                    append("/")
                }
                append("?search=")
                append(filter.query.urlEncoded())
                if (order == SortOrder.POPULARITY) {
                    append("&sort=popular")
                }
            } else if (filter.tags.isNotEmpty()) {
                append("/tag-search/")
                if (page > 1) {
                    append("page/")
                    append(page)
                }
                append("?include=")
                append(filter.tags.joinToString(",") { it.key }.urlEncoded())
                append("&filter=1")
                if (order == SortOrder.POPULARITY) {
                    append("&sort=popular")
                }
            } else {
                if (page > 1) {
                    append("/page/")
                    append(page)
                } else {
                    append("/")
                }
                if (order == SortOrder.POPULARITY) {
                    append("?sort=popular")
                }
            }
        }
        
        return parseMangaList(webClient.httpGet(url).parseHtml())
    }

    override fun parseMangaList(doc: Document): List<Manga> {
        return doc.select(".wrap_item").mapNotNull { el: Element ->
            val titleNode = el.selectFirst("h3.title a") ?: return@mapNotNull null
            val imgNode = el.selectFirst(".wrap_img img") ?: return@mapNotNull null
            
            Manga(
                id = generateUid(titleNode.attr("href")),
                title = titleNode.text(),
                altTitles = emptySet(),
                url = titleNode.attr("href"),
                publicUrl = titleNode.attr("href").toAbsoluteUrl(domain),
                rating = RATING_UNKNOWN,
                contentRating = ContentRating.ADULT,
                coverUrl = imgNode.attr("data-src").takeIf { !it.isNullOrEmpty() } ?: imgNode.attr("src"),
                tags = el.select(".genres a").map { it: Element -> MangaTag(it.attr("href").substringAfterLast("/"), it.text(), source = source) }.toSet(),
                state = MangaState.FINISHED,
                authors = emptySet(),
                source = source,
            )
        }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
        
        val authorNodes = doc.select(".authors a")
        val tagNodes = doc.select(".genres a[href*=/tag/]")

        return manga.copy(
            description = doc.selectFirst(".summary, .description")?.text(),
            authors = authorNodes.map { it: Element -> it.text() }.toSet().takeIf { it.isNotEmpty() } ?: emptySet(),
            tags = tagNodes.map { it: Element -> MangaTag(it.attr("href").substringAfterLast("/"), it.text(), source = source) }.toSet(),
            contentRating = ContentRating.ADULT,
            state = MangaState.FINISHED,
            chapters = listOf(
                MangaChapter(
                    id = manga.id,
                    title = manga.title,
                    number = 1f,
                    volume = 0,
                    url = manga.url,
                    uploadDate = 0,
                    source = source,
                    scanlator = null,
                    branch = null,
                )
            )
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
        return doc.select(".thumbs .wrap_item .wrap_img a img").map { img: Element ->
            val src = img.attr("data-src").takeIf { it.isNotEmpty() } ?: img.attr("src")
            MangaPage(
                id = generateUid(src),
                url = src,
                preview = null,
                source = source,
            )
        }
    }
}
