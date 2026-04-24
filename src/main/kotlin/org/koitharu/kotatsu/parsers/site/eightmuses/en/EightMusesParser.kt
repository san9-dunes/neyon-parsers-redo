package org.koitharu.kotatsu.parsers.site.eightmuses.en

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser
import java.util.EnumSet

@MangaSourceParser("EIGHTMUSES", "8muses.io", "en", ContentType.HENTAI)
internal class EightMusesIo(context: MangaLoaderContext) :
        EightMusesParser(context, MangaParserSource.EIGHTMUSES, "8muses.io")

@MangaSourceParser("EIGHTMUSES_COM", "8muses.com", "en", ContentType.HENTAI)
internal class EightMusesCom(context: MangaLoaderContext) :
        EightMusesParser(context, MangaParserSource.EIGHTMUSES_COM, "comics.8muses.com")

@MangaSourceParser("EIGHTMUSES_XXX", "8muses.xxx", "en", ContentType.HENTAI)
internal class EightMusesXxx(context: MangaLoaderContext) :
        MadaraParser(context, MangaParserSource.EIGHTMUSES_XXX, "8muses.xxx", 18) {
        override val withoutAjax = true
}

internal abstract class EightMusesParser(
        context: MangaLoaderContext,
        source: MangaParserSource,
        defaultDomain: String,
) : PagedMangaParser(context, source, pageSize = 40) {

        override val configKeyDomain = ConfigKey.Domain(defaultDomain)

        override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY)

        override val filterCapabilities: MangaListFilterCapabilities
                get() = MangaListFilterCapabilities(isSearchSupported = true)

        override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

        override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
                val url = buildString {
                        append("https://")
                        append(domain)
                        if (!filter.query.isNullOrEmpty()) {
                                append("/search?q=")
                                append(filter.query.urlEncoded())
                        } else {
                                if (domain == "comics.8muses.com") append("/comics") else append("/")
                        }
                }
                
                val doc = webClient.httpGet(url).parseHtml()
                return parseAlbumCards(doc)
        }

        override suspend fun getDetails(manga: Manga): Manga {
                val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
                val chapterLinks = parseSubAlbums(doc, manga.url)

                val chapters = if (chapterLinks.isNotEmpty()) {
                        chapterLinks.mapIndexed { index, pair ->
                                MangaChapter(
                                        id = generateUid(pair.first),
                                        title = pair.second,
                                        number = (index + 1).toFloat(),
                                        volume = 0,
                                        url = pair.first,
                                        scanlator = null,
                                        uploadDate = 0L,
                                        branch = null,
                                        source = source,
                                )
                        }
                } else {
                        listOf(
                                MangaChapter(
                                        id = manga.id,
                                        title = manga.title,
                                        number = 1f,
                                        volume = 0,
                                        url = manga.url,
                                        scanlator = null,
                                        uploadDate = 0L,
                                        branch = null,
                                        source = source,
                                ),
                        )
                }

                val cover = doc.select("img[src]").firstOrNull { img ->
                        val src = img.attr("src")
                        src.contains("/img/data/") || src.contains("/image/th/")
                }?.src()

                return manga.copy(
                        coverUrl = cover ?: manga.coverUrl,
                        description = doc.selectFirst("meta[name=description]")?.attr("content"),
                        chapters = chapters,
                )
        }

        override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
                val chapterUrl = chapter.url.toAbsoluteUrl(domain)
                val doc = webClient.httpGet(chapterUrl).parseHtml()

                val pictureLinks = doc.select("a[href*=/picture/], a.c-tile:has(img)")
                        .mapNotNull { a ->
                                val href = a.attrAsRelativeUrlOrNull("href")?.substringBefore("?") ?: return@mapNotNull null
                                if (!href.contains("/picture/")) return@mapNotNull null
                                href to a.selectFirst("img")?.src()
                        }
                        .distinctBy { it.first }

                if (pictureLinks.isNotEmpty()) {
                        return pictureLinks.map { pair ->
                                MangaPage(
                                        id = generateUid(pair.first),
                                        url = pair.first,
                                        preview = pair.second,
                                        source = source,
                                )
                        }
                }

                if (chapter.url.contains("/picture/")) {
                        return listOf(
                                MangaPage(
                                        id = generateUid(chapter.url),
                                        url = chapter.url,
                                        preview = null,
                                        source = source,
                                ),
                        )
                }

                return emptyList()
        }

        override suspend fun getPageUrl(page: MangaPage): String {
                val doc = webClient.httpGet(page.url.toAbsoluteUrl(domain)).parseHtml()
                val fullImage = doc.select("img[src]").firstOrNull { img ->
                        val src = img.attr("src")
                        src.contains("/img/data/full_") || src.contains("/img/data/") || src.contains("/image/fm/")
                }?.attr("src")
                
                if (fullImage != null) {
                        return fullImage.toAbsoluteUrl(domain)
                }
                
                // Final fallback: try to resolve by pattern if thumbnail is known
                if (page.preview != null && page.preview.contains("/image/th/")) {
                        return page.preview.replace("/image/th/", "/image/fm/")
                }
                
                return super.getPageUrl(page)
        }

        private fun parseAlbumCards(doc: Document): List<Manga> {
                val seen = HashSet<String>()
                return doc.select("a[href^=/album/], a[href^=/comics/album/]").mapNotNull { a ->
                        val href = a.attrAsRelativeUrlOrNull("href")?.substringBefore("?")?.removeSuffix("/") ?: return@mapNotNull null
                        if (href == "/album" || href == "/comics/album" || !seen.add(href)) {
                                return@mapNotNull null
                        }
                        val title = a.text().trim()
                        if (title.isEmpty()) {
                                return@mapNotNull null
                        }

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

        private fun parseSubAlbums(doc: Document, mangaUrl: String): List<Pair<String, String>> {
                val current = mangaUrl.removeSuffix("/")
                val prefix = "$current/"
                return doc.select("a[href^=/album/], a[href^=/comics/album/]")
                        .mapNotNull { a ->
                                val href = a.attrAsRelativeUrlOrNull("href")?.substringBefore("?")?.removeSuffix("/")
                                        ?: return@mapNotNull null
                                if (!href.startsWith(prefix) || href == current) {
                                        return@mapNotNull null
                                }
                                val tail = href.removePrefix(prefix)
                                if (tail.isEmpty() || tail.contains('/')) {
                                        return@mapNotNull null
                                }
                                val title = a.text().trim().ifEmpty { tail.replace('-', ' ') }
                                href to title
                        }
                        .distinctBy { it.first }
        }
}
