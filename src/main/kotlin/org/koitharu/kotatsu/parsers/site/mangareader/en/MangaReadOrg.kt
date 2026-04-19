package org.koitharu.kotatsu.parsers.site.mangareader.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANGAREADORG", "MangaRead", "en")
internal class MangaReadOrg(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGAREADORG, "www.mangaread.org", pageSize = 20, searchPageSize = 10)
