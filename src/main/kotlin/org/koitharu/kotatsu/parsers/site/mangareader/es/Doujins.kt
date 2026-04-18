package org.koitharu.kotatsu.parsers.site.mangareader.es

import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("DOUJINS", "Doujins.lat", "es", ContentType.HENTAI)
internal class Doujins(context: MangaLoaderContext) :
	DoujinsParser(context, MangaParserSource.DOUJINS, "doujins.lat")

@Broken
@MangaSourceParser("DOUJINSEXY", "Doujin.sexy", "es", ContentType.HENTAI)
internal class DoujinSexy(context: MangaLoaderContext) :
	DoujinsParser(context, MangaParserSource.DOUJINSEXY, "doujin.sexy")

@Broken
@MangaSourceParser("HDOUJIN", "Hdoujin", "es", ContentType.HENTAI)
internal class HDoujin(context: MangaLoaderContext) :
	DoujinsParser(context, MangaParserSource.HDOUJIN, "hdoujin.com")

@Broken
@MangaSourceParser("DOUJIVA", "Doujiva", "es", ContentType.HENTAI)
internal class Doujiva(context: MangaLoaderContext) :
	DoujinsParser(context, MangaParserSource.DOUJIVA, "doujiva.com")

@Broken
@MangaSourceParser("DOUJINLI", "Doujinli", "es", ContentType.HENTAI)
internal class DoujinLi(context: MangaLoaderContext) :
	DoujinsParser(context, MangaParserSource.DOUJINLI, "doujinli.com")

internal abstract class DoujinsParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : MangaReaderParser(context, source, defaultDomain, pageSize = 20, searchPageSize = 10) {
	override val configKeyDomain = ConfigKey.Domain(
		defaultDomain,
		"doujins.lat",
		"doujins.com",
		"doujin.sexy",
		"hdoujin.com",
		"doujiva.com",
		"doujinli.com",
	)

	override val listUrl = "/comic"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
