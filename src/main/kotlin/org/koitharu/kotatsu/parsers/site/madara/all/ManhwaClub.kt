package org.koitharu.kotatsu.parsers.site.madara.all

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHWACLUB", "ManhwaClub.net", type = ContentType.HENTAI)
internal class ManhwaClub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.valueOf("MANHWACLUB"), "manhwaclub.net", 24) {
	override val withoutAjax = true
}
