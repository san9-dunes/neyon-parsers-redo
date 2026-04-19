package org.koitharu.kotatsu.parsers.site.iken.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.iken.IkenParser

@MangaSourceParser("DIVASCANS", "Diva Scans", "en")
internal class DivaScans(context: MangaLoaderContext) :
	IkenParser(context, MangaParserSource.DIVASCANS, "divatoon.com", 18, true) {
	override val configKeyDomain = ConfigKey.Domain("divatoon.com")
}
