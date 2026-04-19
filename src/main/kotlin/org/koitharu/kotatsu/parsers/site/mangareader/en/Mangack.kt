package org.koitharu.kotatsu.parsers.site.mangareader.en

import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser
import org.koitharu.kotatsu.parsers.util.*

@MangaSourceParser("MANGACK", "Mangack", "en")
internal class Mangack(context: MangaLoaderContext) :
        MangaReaderParser(context, MangaParserSource.MANGACK, "mangack.com", pageSize = 20, searchPageSize = 20) {
        override val configKeyDomain = ConfigKey.Domain("mangack.com")

        override val selectMangaList = "table.table.table-striped"
        
        override fun parseMangaList(docs: Document): List<Manga> {
                return docs.select(selectMangaList).mapNotNull { table ->
                        val a = table.selectFirst("a") ?: return@mapNotNull null
                        val relativeUrl = a.attrAsRelativeUrl("href")
                        
                        Manga(
                                id = generateUid(relativeUrl),
                                url = relativeUrl,
                                title = table.selectFirst(".latest-title a")?.text() ?: a.attr("title"),
                                altTitles = emptySet(),
                                publicUrl = a.attrAsAbsoluteUrl("href"),
                                rating = RATING_UNKNOWN,
                                contentRating = if (isNsfwSource) ContentRating.ADULT else null,
                                coverUrl = table.selectFirst("img")?.src(),
                                tags = emptySet(),
                                state = null,
                                authors = emptySet(),
                                source = source,
                        )
                }
        }
}
