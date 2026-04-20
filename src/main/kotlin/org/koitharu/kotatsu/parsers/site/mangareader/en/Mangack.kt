package org.koitharu.kotatsu.parsers.site.mangareader.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANGACK", "Mangack", "en")
internal class Mangack(context: MangaLoaderContext) :
        MangaReaderParser(context, MangaParserSource.MANGACK, "mangack.com", pageSize = 20, searchPageSize = 20) {

        override val selectMangaList = "table.table.table-striped"
        override val selectMangaListTitle = ".latest-title a"
        override val selectMangaListImg = "img"
}
