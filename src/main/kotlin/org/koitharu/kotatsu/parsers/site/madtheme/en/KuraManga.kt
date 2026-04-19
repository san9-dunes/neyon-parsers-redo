package org.koitharu.kotatsu.parsers.site.madtheme.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madtheme.MadthemeParser

@Broken
@MangaSourceParser("KURAMANGA", "KuraManga", "en")
internal class KuraManga(context: MangaLoaderContext) :
	MadthemeParser(context, MangaParserSource.KURAMANGA, "kuramanga.com")
