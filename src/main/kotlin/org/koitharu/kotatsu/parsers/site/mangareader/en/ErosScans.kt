package org.koitharu.kotatsu.parsers.site.mangareader.en

import org.koitharu.kotatsu.parsers.Broken

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("EROSSCANS", "ErosScans", "en", type = ContentType.HENTAI)
internal class ErosScans(context: MangaLoaderContext) :
	ErosScansParser(context, MangaParserSource.EROSSCANS, "erosxscans.xyz")

@Broken
@MangaSourceParser("EROSSXSCANS", "ErosScans", "en", type = ContentType.HENTAI)
internal class ErossxScans(context: MangaLoaderContext) :
	ErosScansParser(context, MangaParserSource.EROSSXSCANS, "erossxscans.com")

internal abstract class ErosScansParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : MangaReaderParser(context, source, defaultDomain, pageSize = 20, searchPageSize = 10)
