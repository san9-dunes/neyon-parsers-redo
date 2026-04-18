package org.koitharu.kotatsu.parsers.site.madara.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@MangaSourceParser("GEDECOMIX", "GedeComix", "en", ContentType.HENTAI)
internal class GedeComix(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GEDECOMIX, "gedecomix.com", 18) {
	override val configKeyDomain = ConfigKey.Domain(
		"gedecomix.com",
		"botcomics.com",
		"freeadultcomix.com",
		"ilikecomix.com",
		"kingcomix.com",
		"nxtcomics.biz",
		"upcomics.net",
		"xyzcomics.com",
	)

	override val tagPrefix = "comics-tag/"
	override val listUrl = "porncomic/"
}
