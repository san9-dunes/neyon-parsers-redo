package org.koitharu.kotatsu.parsers.site.iken.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.iken.IkenParser

@MangaSourceParser("HIVESCANS", "Hive Scans", "en")
internal class HiveScans(context: MangaLoaderContext) :
	IkenParser(context, MangaParserSource.HIVESCANS, "hivetoons.org", 18, true) {
	override val configKeyDomain = ConfigKey.Domain("hivetoons.org")
}
