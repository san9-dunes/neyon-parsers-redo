package org.koitharu.kotatsu.parsers.site.hotcomics

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource

internal object TooMics {

	@MangaSourceParser("TOOMICSEN", "TooMicsEn", "en")
	internal class English(context: MangaLoaderContext) :
		HotComicsParser(context, MangaParserSource.TOOMICSEN, "toomics.com/en")

	@MangaSourceParser("TOOMICSFR", "TooMicsFr", "fr")
	internal class French(context: MangaLoaderContext) :
		HotComicsParser(context, MangaParserSource.TOOMICSFR, "toomics.com/fr")

	@MangaSourceParser("TOOMICSES", "TooMicsEs", "es")
	internal class Spanish(context: MangaLoaderContext) :
		HotComicsParser(context, MangaParserSource.TOOMICSES, "toomics.com/es")

	@MangaSourceParser("TOOMICSESLA", "TooMicsEsLa", "es")
	internal class SpanishLa(context: MangaLoaderContext) :
		HotComicsParser(context, MangaParserSource.TOOMICSESLA, "toomics.com/mx")

	@MangaSourceParser("TOOMICSDE", "TooMicsDe", "de")
	internal class German(context: MangaLoaderContext) :
		HotComicsParser(context, MangaParserSource.TOOMICSDE, "toomics.com/de")

	@MangaSourceParser("TOOMICS", "Toomics", "de")
	internal class GermanTop(context: MangaLoaderContext) :
		HotComicsParser(context, MangaParserSource.TOOMICS, "toomics.top/de")

	@MangaSourceParser("TOOMICSIT", "TooMicsIt", "it")
	internal class Italian(context: MangaLoaderContext) :
		HotComicsParser(context, MangaParserSource.TOOMICSIT, "toomics.com/it")

	@MangaSourceParser("TOOMICSJA", "TooMicsJa", "ja")
	internal class Japanese(context: MangaLoaderContext) :
		HotComicsParser(context, MangaParserSource.TOOMICSJA, "toomics.com/ja")

	@MangaSourceParser("TOOMICSPT", "TooMicsPt", "pt")
	internal class Portuguese(context: MangaLoaderContext) :
		HotComicsParser(context, MangaParserSource.TOOMICSPT, "toomics.com/por")

	@MangaSourceParser("TOOMICSSC", "TooMicsSc", "zh")
	internal class SimplifiedChinese(context: MangaLoaderContext) :
		HotComicsParser(context, MangaParserSource.TOOMICSSC, "toomics.com/sc")

	@MangaSourceParser("TOOMICSTC", "TooMicsTc", "zh")
	internal class TraditionalChinese(context: MangaLoaderContext) :
		HotComicsParser(context, MangaParserSource.TOOMICSTC, "toomics.com/tc")
}
