package org.koitharu.kotatsu.parsers.site.guya.en

import org.koitharu.kotatsu.parsers.Broken

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.guya.GuyaParser

@MangaSourceParser("GUYACUBARI", "GuyaCubari", "en")
internal class GuyaCubari(context: MangaLoaderContext) :
	GuyaCubariParser(context, MangaParserSource.GUYACUBARI, "guya.cubari.moe")

@MangaSourceParser("CUBARIPROXY", "Cubari Proxy", "en")
internal class CubariProxy(context: MangaLoaderContext) :
	GuyaCubariParser(context, MangaParserSource.CUBARIPROXY, "guya.cubari.moe")

internal abstract class GuyaCubariParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : GuyaParser(context, source, defaultDomain) {
	override val configKeyDomain = ConfigKey.Domain(defaultDomain)
}

@Broken
@MangaSourceParser("CUBARI_MOE", "Cubari.moe", "en")
internal class CubariMoe(context: MangaLoaderContext) :
    GuyaCubariParser(context, MangaParserSource.CUBARI_MOE, "cubari.moe")
