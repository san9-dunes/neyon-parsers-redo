package org.koitharu.kotatsu.parsers.site.manga18.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.manga18.Manga18Parser

@MangaSourceParser("PORNCOMIC18", "18PornComic", "en", ContentType.HENTAI)
internal class PornComic18(context: MangaLoaderContext) :
	PornComic18Parser(context, MangaParserSource.PORNCOMIC18, "18porncomic.com")

@MangaSourceParser("SRC_18KAMI", "18kami", "en", ContentType.HENTAI)
internal class Src18Kami(context: MangaLoaderContext) :
	PornComic18Parser(context, MangaParserSource.SRC_18KAMI, "18kami.com")

internal abstract class PornComic18Parser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : Manga18Parser(context, source, defaultDomain) {
	override val configKeyDomain = ConfigKey.Domain(defaultDomain, "18porncomic.com", "18comic.vip", "18kami.com")

	override val selectTag = "div.item:not(.info_label) div.info_value a"
}
