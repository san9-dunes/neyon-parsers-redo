package org.koitharu.kotatsu.parsers.site.all

import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.AbstractMangaParser
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.SortOrder

internal abstract class RequestedSitePlaceholderParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : AbstractMangaParser(context, source) {

	override val availableSortOrders: Set<SortOrder>
		get() = setOf(SortOrder.UPDATED)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities()

	override val configKeyDomain = ConfigKey.Domain(defaultDomain)

	override suspend fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> = emptyList()

	override suspend fun getDetails(manga: Manga): Manga = manga

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> = emptyList()

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()
}

@Broken("Placeholder: site requires dedicated parser/auth flow")
@MangaSourceParser("DLSITE", "DLsite")
internal class DlsiteParser(context: MangaLoaderContext) :
	RequestedSitePlaceholderParser(context, MangaParserSource.DLSITE, "dlsite.com")

@Broken("Placeholder: site requires dedicated parser/auth flow")
@MangaSourceParser("IRODORICOMICS", "Irodori Comics")
internal class IrodoriComicsParser(context: MangaLoaderContext) :
	RequestedSitePlaceholderParser(context, MangaParserSource.IRODORICOMICS, "irodoricomics.com")

@Broken("Placeholder: site requires dedicated parser/auth flow")
@MangaSourceParser("LUSCIOUS", "Luscious")
internal class LusciousParser(context: MangaLoaderContext) :
	RequestedSitePlaceholderParser(context, MangaParserSource.LUSCIOUS, "luscious.net")

@Broken("Placeholder: site requires dedicated parser/auth flow")
@MangaSourceParser("HENTAIFOUNDRY", "Hentai Foundry")
internal class HentaiFoundryParser(context: MangaLoaderContext) :
	RequestedSitePlaceholderParser(context, MangaParserSource.HENTAIFOUNDRY, "hentai-foundry.com")

@Broken("Placeholder: site requires dedicated parser/auth flow")
@MangaSourceParser("PIXIV", "Pixiv")
internal class PixivParser(context: MangaLoaderContext) :
	RequestedSitePlaceholderParser(context, MangaParserSource.PIXIV, "pixiv.net")
