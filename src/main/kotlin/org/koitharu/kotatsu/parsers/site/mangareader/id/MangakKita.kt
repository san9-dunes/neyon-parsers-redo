package org.koitharu.kotatsu.parsers.site.mangareader.id

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser
import org.koitharu.kotatsu.parsers.util.parseHtml
import java.util.*

@MangaSourceParser("MANGAKITA", "MangaKita", "id")
internal class MangakKita(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGAKITA, "mangakita.me", pageSize = 20, searchPageSize = 10) {
	override val sourceLocale: Locale = Locale.ENGLISH
	override val listUrl = "/project"

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (!filter.query.isNullOrEmpty()) {
			return super.getListPage(page, order, filter)
		}

		val pageNumber = page.coerceAtLeast(1)
		val orderPath = when (order) {
			SortOrder.ALPHABETICAL -> "title"
			SortOrder.ALPHABETICAL_DESC -> "titlereverse"
			SortOrder.NEWEST -> "latest"
			SortOrder.POPULARITY -> "popular"
			SortOrder.UPDATED -> "update"
			else -> "update"
		}

		val url = if (pageNumber == 1) {
			"https://$domain$listUrl/?order=$orderPath"
		} else {
			"https://$domain$listUrl/page/$pageNumber/?order=$orderPath"
		}

		return parseMangaList(webClient.httpGet(url).parseHtml())
	}

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
