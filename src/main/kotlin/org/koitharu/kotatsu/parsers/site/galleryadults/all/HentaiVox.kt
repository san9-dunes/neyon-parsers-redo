package org.koitharu.kotatsu.parsers.site.galleryadults.all

import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.util.*
import java.util.Base64

@MangaSourceParser("HENTAIVOX", "HentaiVox", type = ContentType.HENTAI)
internal class HentaiVox(context: MangaLoaderContext) :
	HentaiForceParser(context, MangaParserSource.HENTAIVOX, "hentaivox.com") {

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		val base64 = doc.html().substringAfter("var readerPages = JSON.parse(atob(\"")
			.substringBefore("\"));")

		val jsonString = Base64.getDecoder().decode(base64).decodeToString()
		val json = JSONObject(jsonString)
		val pages = json.getJSONArray("pages")
		
		return (0 until pages.length()).map { i ->
			val url = pages.getJSONObject(i).getString("image")
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}

	override suspend fun getPageUrl(page: MangaPage): String = page.url
}
