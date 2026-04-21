package org.koitharu.kotatsu.parsers.site.pizzareader.it

import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.pizzareader.PizzaReaderParser

@Broken("WIP: Search not finished yet / WIP")
@MangaSourceParser("HASTATEAM", "HastaTeamDdt", "it")
internal class HastaTeam(context: MangaLoaderContext) :
	PizzaReaderParser(context, MangaParserSource.HASTATEAM, "ddt.hastateam.com")
