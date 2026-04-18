package org.koitharu.kotatsu.parsers.site.madara.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHWAZONE", "ManhwaZone", "en")
internal class ManhwaZone(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHWAZONE, "manhwa.zone")
