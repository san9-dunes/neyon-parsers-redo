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

@MangaSourceParser("ALLPORN_COMIC", "AllPornComic.com", "en", ContentType.HENTAI)
internal class AllPornComic(context: MangaLoaderContext) :
        AllPornComicParser(context, MangaParserSource.ALLPORN_COMIC, "allporncomic.com")

@MangaSourceParser("ALLPORN_COMICS", "AllPornComics.com", "en", ContentType.HENTAI)
internal class AllPornComics(context: MangaLoaderContext) :
        AllPornComicParser(context, MangaParserSource.ALLPORN_COMICS, "allporncomics.com")

@MangaSourceParser("ALLPORN_COMICS_CO", "AllPornComics.co", "en", ContentType.HENTAI)
internal class AllPornComicsCo(context: MangaLoaderContext) :
        AllPornComicParser(context, MangaParserSource.ALLPORN_COMICS_CO, "allporncomics.co")

internal abstract class AllPornComicParser(
        context: MangaLoaderContext,
        source: MangaParserSource,
        defaultDomain: String,
) : MadaraParser(context, source, defaultDomain, pageSize = 24) {
        override val configKeyDomain = ConfigKey.Domain(defaultDomain)

        override val listUrl = "porncomic/"
        override val withoutAjax = true
        override val tagPrefix = "porncomic-genre/"
        override val datePattern = "MMMM dd, yyyy"

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
                        .set("Referer", "https://allporncomic.com/home-3/")
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
