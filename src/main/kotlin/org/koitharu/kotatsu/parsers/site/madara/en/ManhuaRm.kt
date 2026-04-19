package org.koitharu.kotatsu.parsers.site.madara.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHUARM", "Manhuarm MTL", "en")
internal class ManhuaRm(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHUARM, "manhuarmtl.com") {
	override val configKeyDomain = ConfigKey.Domain("manhuarmtl.com")
}
