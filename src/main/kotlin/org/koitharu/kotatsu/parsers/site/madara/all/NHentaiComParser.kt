package org.koitharu.kotatsu.parsers.site.madara.all

import okhttp3.Headers
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNull
import java.util.*

@MangaSourceParser("NHENTAI_COM", "NHentai.com", type = ContentType.HENTAI)
internal class NHentaiComParser(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.NHENTAI_COM, pageSize = 18) {

	override val configKeyDomain = ConfigKey.Domain("nhentai.com")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("Referer", "https://$domain/")
		.add("Accept", "application/json, text/plain, */*")
		.build()

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.NEWEST,
		SortOrder.POPULARITY
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			append("/api/comics?page=")
			append(page + 1)
			append("&lang=en&nsfw=false")

			if (!filter.query.isNullOrEmpty()) {
				append("&q=")
				append(filter.query.urlEncoded())
			}

			when (order) {
				SortOrder.NEWEST -> {
					append("&sort=uploaded_at&order=desc")
				}
				SortOrder.POPULARITY -> {
					append("&sort=comics_count&order=desc")
				}
				SortOrder.UPDATED -> {
					append("&sort=uploaded_at&order=desc")
				}
				else -> {
					append("&sort=uploaded_at&order=desc")
				}
			}
		}

		val response = webClient.httpGet(url).parseJson()
		val data = response.optJSONArray("data") ?: return emptyList()

		return data.mapJSONNotNull { item ->
			val slug = item.getStringOrNull("slug") ?: return@mapJSONNotNull null
			val relativeUrl = "/en/comic/$slug"

			Manga(
				id = generateUid(relativeUrl),
				title = item.getStringOrNull("title") ?: item.getStringOrNull("alternative_title") ?: "Unknown",
				altTitles = setOfNotNull(item.getStringOrNull("alternative_title")),
				url = slug, // Use slug for API detail calls
				publicUrl = relativeUrl.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = item.getStringOrNull("thumb_url") ?: item.getStringOrNull("image_url"),
				tags = parseTags(item),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val url = "https://$domain/api/comics/${manga.url}?lang=en&nsfw=false"
		val item = webClient.httpGet(url).parseJson()

		val chapter = MangaChapter(
			id = generateUid(manga.url),
			title = "Comic",
			number = 1f,
			volume = 0,
			url = manga.url, // Store the slug here for getPages
			scanlator = null,
			uploadDate = 0L,
			branch = null,
			source = source,
		)

		return manga.copy(
			title = item.getStringOrNull("title") ?: manga.title,
			description = item.getStringOrNull("description"),
			tags = parseTags(item),
			chapters = listOf(chapter),
		)
	}
override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
	val url = "https://$domain/api/comics/${chapter.url}?lang=en&nsfw=false"
	val item = webClient.httpGet(url).parseJson()

	val pagesCount = item.optInt("pages", 0)
	if (pagesCount == 0) return emptyList()

	val comicId = item.optInt("id", 0)
	if (comicId == 0) return emptyList()

	return (1..pagesCount).map { i ->
		val pageUrl = "https://cdn.nhentai.com/nhentai/storage/images/$comicId/$i.webp"
		MangaPage(
			id = generateUid(pageUrl),
			url = pageUrl,
			preview = null,
			source = source,
		)
	}
}


	private fun parseTags(item: JSONObject): Set<MangaTag> {
		val tags = mutableSetOf<MangaTag>()
		item.optJSONArray("tags")?.let { jsonTags ->
			tags.addAll(
				jsonTags.mapJSONNotNull { tagObj ->
					val name = tagObj.getStringOrNull("name") ?: return@mapJSONNotNull null
					val slug = tagObj.getStringOrNull("slug") ?: return@mapJSONNotNull null
					MangaTag(title = name, key = slug, source = source)
				}
			)
		}
		item.optJSONObject("category")?.let { cat ->
			val name = cat.getStringOrNull("name")
			val slug = cat.getStringOrNull("slug")
			if (name != null && slug != null) {
				tags.add(MangaTag(title = name, key = slug, source = source))
			}
		}
		item.optJSONObject("language")?.let { lang ->
			val name = lang.getStringOrNull("name")
			val slug = lang.getStringOrNull("slug")
			if (name != null && slug != null) {
				tags.add(MangaTag(title = name, key = slug, source = source))
			}
		}
		return tags
	}
}
