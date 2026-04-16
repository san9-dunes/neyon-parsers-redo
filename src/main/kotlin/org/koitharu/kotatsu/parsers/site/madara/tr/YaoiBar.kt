package org.koitharu.kotatsu.parsers.site.madara.tr

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@MangaSourceParser("YAOIBAR", "YaoiBar", "tr", type = ContentType.HENTAI)
internal class YaoiBar(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YAOIBAR, "yaoibar.gay", 16) {
	override val datePattern = "dd/MM/yyyy"
}
