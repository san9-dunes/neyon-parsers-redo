package org.koitharu.kotatsu.parsers.site.madara.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGAHENTAI", "MangaHentai", "en", ContentType.HENTAI)
internal class MangaHentai(context: MangaLoaderContext) :
	MangaHentaiParser(context, MangaParserSource.MANGAHENTAI, "mangahentai.me")

@Broken
@MangaSourceParser("MANGAHEN", "MangaHen", "en", ContentType.HENTAI)
internal class MangaHen(context: MangaLoaderContext) :
	MangaHentaiParser(context, MangaParserSource.MANGAHEN, "mangahen.com")

internal abstract class MangaHentaiParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : MadaraParser(context, source, defaultDomain, 20) {

	override val tagPrefix = "manga-hentai-genre/"
	override val listUrl = "manga-hentai/"
}
