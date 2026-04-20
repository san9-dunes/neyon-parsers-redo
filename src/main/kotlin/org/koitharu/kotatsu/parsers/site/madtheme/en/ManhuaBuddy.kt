package org.koitharu.kotatsu.parsers.site.madtheme.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.parsers.site.madtheme.MadthemeParser
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.attrAsRelativeUrl
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import org.koitharu.kotatsu.parsers.config.ConfigKey

@MangaSourceParser("MANHUABUDDY", "ManhuaBuddy", "en")
internal class ManhuaBuddy(context: MangaLoaderContext) :
        MadthemeParser(context, MangaParserSource.MANHUABUDDY, "manhuabuddy.com") {

        private val subDomain = "sb.mbcdn.xyz"

        override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
                super.onCreateConfig(keys)
                keys.add(ConfigKey.InterceptCloudflare())
        }

        override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
                if (!filter.query.isNullOrBlank() || filter.tags.isNotEmpty()) {
                        return emptyList()
                }

                val url = buildString {
                        append("https://")
                        append(domain)
                        append('/')
                        when (order) {
                                SortOrder.NEWEST -> append("new-manga")
                                SortOrder.POPULARITY -> append("top-manga")
                                SortOrder.UPDATED -> append("new-manga")
                                else -> append("new-manga")
                        }
                        if (page > 1) {
                                append("?page=")
                                append(page.toString())
                        }
                }

                val doc = webClient.httpGet(url).parseHtml()
                return doc.select(".visual, .item, .thumb").mapNotNull { div ->
                        val a = div.selectFirst("a") ?: return@mapNotNull null
                        val href = a.attrAsRelativeUrl("href")
                        val img = div.selectFirst("img") ?: return@mapNotNull null
                        val title = img.attr("alt").takeIf { it.isNotBlank() } ?: div.selectFirst(".title")?.text() ?: ""
                        if (href.isBlank() || title.isBlank()) return@mapNotNull null

                        Manga(
                                id = generateUid(href),
                                url = href,
                                publicUrl = href.toAbsoluteUrl(domain),
                                coverUrl = img.attr("data-original").takeIf { it.isNotBlank() } ?: img.attr("src").takeIf { it.isNotBlank() } ?: "",
                                title = title.trim(),
                                altTitles = emptySet(),
                                rating = RATING_UNKNOWN,
                                tags = emptySet(),
                                authors = emptySet(),
                                state = null,
                                source = source,
                                contentRating = if (isNsfwSource) org.koitharu.kotatsu.parsers.model.ContentRating.ADULT else null,
                        )
                }
        }

        override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
                val fullUrl = chapter.url.toAbsoluteUrl(domain)
                val doc = webClient.httpGet(fullUrl).parseHtml()
                val regexPages = Regex("chapImages\\s*=\\s*['\"](.*?)['\"]")
                val pages = doc.select("script").firstNotNullOfOrNull { script ->
                        regexPages.find(script.html())?.groupValues?.getOrNull(1)
                }?.split(',')

                return pages?.map { url ->
                        val cleanUrl = url.substringAfter("/manga")
                        MangaPage(
                                id = generateUid(url),
                                url = "https://$subDomain/manga$cleanUrl",
                                preview = null,
                                source = source,
                        )
                } ?: emptyList()
        }
}
