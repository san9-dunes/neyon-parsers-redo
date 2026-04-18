package org.koitharu.kotatsu.parsers.site.madara.pt

import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser
import org.koitharu.kotatsu.parsers.util.attrAsRelativeUrl
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl

@MangaSourceParser("CVNSCAN", "CvnScan", "pt", ContentType.HENTAI)
internal class CvnScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.CVNSCAN, "covendasbruxonas.com") {

	override fun parseMangaList(doc: Document): List<Manga> {
		val cards = doc.select(".manga__item, .manga-card")
		if (cards.isEmpty()) {
			return super.parseMangaList(doc)
		}

		val seen = HashSet<String>(cards.size)
		return cards.mapNotNull { card ->
			val link = card.selectFirst("a[href*=/manga/]") ?: return@mapNotNull null
			val href = runCatching { link.attrAsRelativeUrl("href") }.getOrNull() ?: return@mapNotNull null
			if (!seen.add(href) || href.endsWith("/manga/") || href.contains("post_type=wp-manga")) {
				return@mapNotNull null
			}

			val title = card.selectFirst(".manga-title, .post-title, h3, h4")?.text()?.trim().orEmpty()
				.ifEmpty { link.attr("title").trim() }
				.ifEmpty { link.text().trim() }
			if (title.isEmpty()) {
				return@mapNotNull null
			}

			Manga(
				id = generateUid(href),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				coverUrl = card.selectFirst("img")?.attr("src"),
				title = title,
				altTitles = emptySet(),
				rating = RATING_UNKNOWN,
				tags = emptySet(),
				authors = emptySet(),
				state = null,
				source = source,
				contentRating = if (isNsfwSource) ContentRating.ADULT else null,
			)
		}
	}
}
