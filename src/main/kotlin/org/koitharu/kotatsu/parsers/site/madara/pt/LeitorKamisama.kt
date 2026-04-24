package org.koitharu.kotatsu.parsers.site.madara.pt

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@MangaSourceParser("LEITORKAMISAMA", "Kami Sama Explorer", "pt")
internal class LeitorKamisama(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LEITORKAMISAMA, "leitor.kamisama.com.br") {
	override val withoutAjax = true
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}
