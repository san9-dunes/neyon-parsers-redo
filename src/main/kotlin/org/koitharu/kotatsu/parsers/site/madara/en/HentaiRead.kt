package org.koitharu.kotatsu.parsers.site.madara.en

import okhttp3.Headers
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser
import org.koitharu.kotatsu.parsers.util.*
import java.text.SimpleDateFormat

@MangaSourceParser("HENTAIREAD", "HentaiRead", "en", ContentType.HENTAI)
internal class HentaiRead(context: MangaLoaderContext) :
        MadaraParser(context, MangaParserSource.HENTAIREAD, "hentairead.com", pageSize = 24) {

        override val configKeyDomain = ConfigKey.Domain("hentairead.com")

        override val listUrl = "hentai/"
        override val tagPrefix = "hentai-tag/"

        override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
                super.onCreateConfig(keys)
                keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
        }

        override fun getRequestHeaders(): Headers {
                return super.getRequestHeaders().newBuilder()
                        .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36")
                        .set("sec-ch-ua", "\"Chromium\";v=\"147\", \"Not.A/Brand\";v=\"8\"")
                        .set("sec-ch-ua-mobile", "?0")
                        .set("sec-ch-ua-platform", "\"Windows\"")
                        .set("Referer", "https://$domain/")
                        .build()
        }

        override suspend fun getChapters(manga: Manga, doc: Document): List<MangaChapter> {
                val chapters = super.getChapters(manga, doc)
                if (chapters.isNotEmpty()) return chapters

                val mangaId = doc.select("div#manga-chapters-holder").attr("data-id")
                if (mangaId.isEmpty()) return chapters

                val url = "https://$domain/wp-admin/admin-ajax.php"
                val postData = "action=manga_get_chapters&manga=$mangaId"
                val ajaxDoc = webClient.httpPost(url, postData).parseHtml()

                val dateFormat = SimpleDateFormat(datePattern, sourceLocale)
                return ajaxDoc.select(selectChapter).mapChapters(reversed = true) { i, li ->
                        val a = li.selectFirstOrThrow("a")
                        val href = a.attrAsRelativeUrl("href")
                        val link = href + stylePage
                        val dateText = li.selectFirst("a.c-new-tag")?.attr("title") ?: li.selectFirst(selectDate)?.text()
                        val name = a.selectFirst("p")?.text() ?: a.ownText()
                        MangaChapter(
                                id = generateUid(href),
                                title = name,
                                number = i + 1f,
                                volume = 0,
                                url = link,
                                uploadDate = parseChapterDate(
                                        dateFormat,
                                        dateText,
                                ),
                                source = source,
                                scanlator = null,
                                branch = null,
                        )
                }
        }
}
