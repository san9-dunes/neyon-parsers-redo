package org.koitharu.kotatsu.parsers.site.madara.en

import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@Broken
internal abstract class MangaFreakParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : MadaraParser(context, source, defaultDomain) {
	override val configKeyDomain = ConfigKey.Domain(defaultDomain, "mangafreak.online", "mfreak.net")
	override val postReq = true
	override val datePattern = "dd MMMM، yyyy"
}

@Broken
@MangaSourceParser("MANGAFREAK", "MangaFreak", "en")
internal class MangaFreak(context: MangaLoaderContext) :
	MangaFreakParser(context, MangaParserSource.MANGAFREAK, "mangafreak.online")

@Broken
@MangaSourceParser("MFREAK", "MangaFreak", "en")
internal class Mfreak(context: MangaLoaderContext) :
	MangaFreakParser(context, MangaParserSource.MFREAK, "mfreak.net")
