package org.koitharu.kotatsu.parsers.site.mangareader.ar

import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser

@Broken("WIP: Search not finished yet / WIP")
@MangaSourceParser("LAGOONSCANS", "Lagoon Scans", "en")
internal class LagoonScans(context: MangaLoaderContext) :
    MangaReaderParser(context, MangaParserSource.LAGOONSCANS, "lagoonscans.com", pageSize = 20, searchPageSize = 10)
