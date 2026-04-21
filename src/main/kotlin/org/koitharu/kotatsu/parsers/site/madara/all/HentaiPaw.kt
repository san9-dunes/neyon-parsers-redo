package org.koitharu.kotatsu.parsers.site.madara.all

import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser
import org.koitharu.kotatsu.parsers.util.parseHtml

@Broken("WIP: Search not finished yet / WIP")
@MangaSourceParser("HENTAIPAW", "HentaiPaw", type = ContentType.HENTAI)
internal class HentaiPaw(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HENTAIPAW, "hentaipaw.com") {
	override val withoutAjax = true

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (filter.query.isNullOrEmpty() && filter.tags.isEmpty()) {
			val pages = page + 1
			val url = buildString {
				append("https://")
				append(domain)
				append("/hentai-manga/")
				if (pages > 1) {
					append("page/")
					append(pages)
					append("/")
				}
				append("?m_orderby=")
				append(when (order) {
					SortOrder.POPULARITY -> "views"
					SortOrder.NEWEST -> "new-manga"
					SortOrder.UPDATED -> "latest"
					else -> "latest"
				})
			}
			return parseMangaList(webClient.httpGet(url).parseHtml())
		}
		return super.getListPage(page, order, filter)
	}

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}
}
