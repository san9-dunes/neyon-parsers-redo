package org.koitharu.kotatsu.parsers.site.eightmuses.en

import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.SinglePageMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("EIGHTMUSES", "8muses", "en", ContentType.HENTAI)
internal class EightMuses(context: MangaLoaderContext) : EightMusesParser(context, MangaParserSource.EIGHTMUSES, "comics.8muses.com")

@MangaSourceParser("EIGHTMUSES_COM", "8muses.com", "en", ContentType.HENTAI)
internal class EightMusesCom(context: MangaLoaderContext) : EightMusesParser(context, MangaParserSource.EIGHTMUSES_COM, "8muses.com")

@MangaSourceParser("EIGHTMUSES_XXX", "8muses.xxx", "en", ContentType.HENTAI)
internal class EightMusesXxx(context: MangaLoaderContext) : EightMusesParser(context, MangaParserSource.EIGHTMUSES_XXX, "8muses.xxx")

internal abstract class EightMusesParser(
        context: MangaLoaderContext,
        source: MangaParserSource,
        defaultDomain: String,
) : SinglePageMangaParser(context, source) {

        override val configKeyDomain = ConfigKey.Domain(defaultDomain)

        override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
                super.onCreateConfig(keys)
                keys.add(userAgentKey)
        }

        override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.NEWEST)

        override val filterCapabilities: MangaListFilterCapabilities
                get() = MangaListFilterCapabilities(isSearchSupported = true)

        override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

        override suspend fun getList(order: SortOrder, filter: MangaListFilter): List<Manga> {
                val url = buildString {
                        append("https://")
                        append(domain)
                        if (!filter.query.isNullOrEmpty()) {
                                append("/search?q=")
                                append(filter.query.urlEncoded())
                        } else {
                                if (domain.contains("8muses.com")) append("/comics") else append("/")
                        }
                }
                
                val doc = webClient.httpGet(url).parseHtml()
                return parseAlbumCards(doc)
        }

        private fun parseAlbumCards(doc: Document): List<Manga> {
                return doc.select(".gallery .album, .gallery .image, .album-card, .gallery a[href*='/album/']").mapNotNull { card ->
                        val a = if (card.tagName() == "a") card else card.selectFirst("a")
                        if (a == null) return@mapNotNull null
                        
                        val href = a.attrAsRelativeUrl("href")
                        if (!href.contains("/album/")) return@mapNotNull null
                        
                        val title = card.selectFirst(".title, .album-title")?.text()?.trim() 
                                ?: a.text().trim().ifEmpty { href.substringAfterLast('/').replace('-', ' ') }

                        Manga(
                                id = generateUid(href),
                                title = title,
                                altTitles = emptySet(),
                                url = href,
                                publicUrl = href.toAbsoluteUrl(domain),
                                rating = RATING_UNKNOWN,
                                contentRating = ContentRating.ADULT,
                                coverUrl = card.selectFirst("img")?.requireSrc(),
                                tags = emptySet(),
                                state = null,
                                authors = emptySet(),
                                source = source,
                        )
                }.distinctBy { it.url }
        }

        override suspend fun getDetails(manga: Manga): Manga {
                val mangaUrl = manga.url.toAbsoluteUrl(domain)
                val doc = webClient.httpGet(mangaUrl).parseHtml()
                val chapterLinks = parseSubAlbums(doc, manga.url)

                if (chapterLinks.isEmpty()) {
                        // Single chapter
                        return manga.copy(
                                chapters = listOf(
                                        MangaChapter(
                                                id = generateUid(manga.url),
                                                title = manga.title,
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

                val chapters = chapterLinks.mapIndexed { index, (href, title) ->
                        MangaChapter(
                                id = generateUid(href),
                                title = title,
                                number = (index + 1).toFloat(),
                                volume = 0,
                                url = href,
                                scanlator = null,
                                uploadDate = 0L,
                                branch = null,
                                source = source,
                        )
                }

                return manga.copy(chapters = chapters)
        }

        private fun parseSubAlbums(doc: Document, parentUrl: String): List<Pair<String, String>> {
                return doc.select(".gallery .album, .album-card").mapNotNull { card ->
                        val a = card.selectFirst("a") ?: return@mapNotNull null
                        val href = a.attrAsRelativeUrl("href")
                        if (href == parentUrl) return@mapNotNull null
                        val title = card.selectFirst(".title, .album-title")?.text()?.trim() ?: a.text().trim()
                        href to title
                }
        }

        override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
                val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
                
                // If it's a viewer page (single image)
                if (chapter.url.contains("/picture/")) {
                         val img = doc.selectFirst("img.main-image, .image img, img[src*='/image/']")
                         val src = img?.attr("data-src")?.takeIf { it.isNotEmpty() } ?: img?.attr("src")
                         return if (src != null) {
                                 val fullSrc = src.toFullImageSize().toAbsoluteUrl(domain)
                                 listOf(MangaPage(id = generateUid(fullSrc), url = fullSrc, preview = null, source = source))
                         } else emptyList()
                }

                // If it's an album page, find all items
                return doc.select(".gallery .image, .gallery .album, .album-card, .image-container").mapNotNull { el ->
                        val a = el.selectFirst("a")
                        val img = el.selectFirst("img")
                        val src = img?.attr("data-src")?.takeIf { it.isNotEmpty() } ?: img?.attr("src")
                        
                        if (a != null) {
                            val href = a.attrAsRelativeUrl("href")
                            MangaPage(
                                id = generateUid(href),
                                url = href,
                                preview = src?.toAbsoluteUrl(domain),
                                source = source,
                            )
                        } else if (src != null) {
                            // Direct image link (thumbnail to full)
                            val fullSrc = src.toFullImageSize().toAbsoluteUrl(domain)
                            MangaPage(
                                id = generateUid(fullSrc),
                                url = fullSrc,
                                preview = src.toAbsoluteUrl(domain),
                                source = source,
                            )
                        } else null
                }
        }

        override suspend fun getPageUrl(page: MangaPage): String {
                if (page.url.contains("/image/fl/") || page.url.contains("/image/pb/") || page.url.contains("/image/images/")) return page.url
                
                val doc = webClient.httpGet(page.url.toAbsoluteUrl(domain)).parseHtml()
                val img = doc.selectFirst("img.main-image, .image img, #image, img[src*='/image/']")
                val src = img?.attr("data-src")?.takeIf { it.isNotEmpty() } ?: img?.attr("src")
                
                return src?.toFullImageSize()?.toAbsoluteUrl(domain) ?: page.url
        }

        private fun String.toFullImageSize(): String {
                return replace("/th/", "/fl/")
                        .replace("/as/", "/fl/")
                        .replace("/thumbnails/", "/images/")
                        .replace("/fm/", "/fl/")
        }
}
