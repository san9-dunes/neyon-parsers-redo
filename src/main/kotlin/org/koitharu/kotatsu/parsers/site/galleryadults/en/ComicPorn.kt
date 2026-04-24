package org.koitharu.kotatsu.parsers.site.galleryadults.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.galleryadults.GalleryAdultsParser

@MangaSourceParser("COMICPORN", "ComicPorn.xxx", "en", type = ContentType.HENTAI)
internal class ComicPorn(context: MangaLoaderContext) :
	GalleryAdultsParser(context, MangaParserSource.COMICPORN, "comicporn.xxx")
