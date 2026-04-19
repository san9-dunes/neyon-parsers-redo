package org.koitharu.kotatsu.parsers.site.madara.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@MangaSourceParser("ALLPORN_COMIC", "AllPornComic", "en", ContentType.HENTAI)
internal class AllPornComic(context: MangaLoaderContext) :
        AllPornComicParser(context, MangaParserSource.ALLPORN_COMIC, "allporncomic.com")

@MangaSourceParser("ALLPORN_COMICS_CO", "AllPornComics.co", "en", ContentType.HENTAI)
internal class AllPornComicsCo(context: MangaLoaderContext) :
        AllPornComicParser(context, MangaParserSource.ALLPORN_COMICS_CO, "allporncomics.co")

internal abstract class AllPornComicParser(
        context: MangaLoaderContext,
        source: MangaParserSource,
        defaultDomain: String,
) : MadaraParser(context, source, defaultDomain, pageSize = 24) {
        override val configKeyDomain = ConfigKey.Domain(defaultDomain)

        override val tagPrefix = "porncomic-genre/"
        override val datePattern = "MMMM dd, yyyy"
}
