package org.koitharu.kotatsu.parsers.site.madara.tr

import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@Broken("WIP: Search not finished yet / WIP")
@MangaSourceParser("HAYALISTIC", "Hayalistic", "tr")
internal class Hayalistic(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HAYALISTIC, "hayalistic.com.tr", 24) {
	override val datePattern = "dd/MM/yyyy"
}
