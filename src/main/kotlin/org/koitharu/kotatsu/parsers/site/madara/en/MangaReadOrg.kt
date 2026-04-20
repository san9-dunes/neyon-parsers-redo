package org.koitharu.kotatsu.parsers.site.madara.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAREADORG", "MangaRead", "en")
internal class MangaReadOrg(context: MangaLoaderContext) :
        MadaraParser(context, MangaParserSource.MANGAREADORG, "www.mangaread.org", pageSize = 20) {
        
}
