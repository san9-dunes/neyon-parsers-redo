package org.koitharu.kotatsu.parsers.site.madara.es

import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

// Other domain name : toonx.net
@Broken("WIP: Search not finished yet / WIP")
@MangaSourceParser("VERCOMICSPORNO", "VerComicsPorno", "es", ContentType.HENTAI)
internal class VerComicsPorno(context: MangaLoaderContext) :
    MadaraParser(context, MangaParserSource.VERCOMICSPORNO, "vercomicsporno.com") {
    override val configKeyDomain = ConfigKey.Domain("vercomicsporno.com")
    override val withoutAjax = true
}

@Broken("WIP: Search not finished yet / WIP")
@MangaSourceParser("VERCOMICSPORNO_XXX", "VerComicsPorno (.xxx)", "es", ContentType.HENTAI)
internal class VerComicsPornoXxx(context: MangaLoaderContext) :
    MadaraParser(context, MangaParserSource.VERCOMICSPORNO_XXX, "comiquetaxxx.com") {
    override val configKeyDomain = ConfigKey.Domain("comiquetaxxx.com")
    override val withoutAjax = true
}
