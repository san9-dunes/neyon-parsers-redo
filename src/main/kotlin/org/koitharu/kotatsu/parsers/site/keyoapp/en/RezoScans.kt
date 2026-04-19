package org.koitharu.kotatsu.parsers.site.keyoapp.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.keyoapp.KeyoappParser

@MangaSourceParser("REZOSCANS", "RezoScans", "en")
internal class RezoScans(context: MangaLoaderContext) :
	RezoScansParser(context, MangaParserSource.REZOSCANS, "rezoscans.com")

@Broken
@MangaSourceParser("ZEROSCANS", "ZeroScans", "en")
internal class ZeroScans(context: MangaLoaderContext) :
	RezoScansParser(context, MangaParserSource.ZEROSCANS, "zeroscans.com")

internal abstract class RezoScansParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : KeyoappParser(context, source, defaultDomain) {
	override val configKeyDomain = ConfigKey.Domain(defaultDomain)
}
