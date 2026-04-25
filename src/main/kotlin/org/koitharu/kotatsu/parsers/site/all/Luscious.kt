package org.koitharu.kotatsu.parsers.site.all

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

@MangaSourceParser("LUSCIOUS", "Luscious", type = ContentType.HENTAI)
internal class Luscious(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.LUSCIOUS, pageSize = 30) {

	override val configKeyDomain = ConfigKey.Domain("luscious.net")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("Referer", "https://$domain/")
		.build()

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.NEWEST, SortOrder.POPULARITY)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = true)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val variables = JSONObject().apply {
			val input = JSONObject().apply {
				put("page", page + 1)
				put("display", if (order == SortOrder.POPULARITY) "rating_all_time" else "date_newest")
				val filters = org.json.JSONArray()
				if (!filter.query.isNullOrEmpty()) {
					filters.put(JSONObject().apply {
						put("name", "search_query")
						put("value", filter.query)
					})
				}
				put("filters", filters)
			}
			put("input", input)
		}

		val url = buildString {
			append("https://www.")
			append(domain)
			append("/graphql/nobatch/?operationName=AlbumList&query=")
			append(ALBUM_LIST_QUERY.urlEncoded())
			append("&variables=")
			append(variables.toString().urlEncoded())
		}

		val json = webClient.httpGet(url).parseJson()
		val items = json.getJSONObject("data").getJSONObject("album").getJSONObject("list").getJSONArray("items")

		return items.mapJSONNotNull { item ->
			val url = item.getString("url")
			Manga(
				id = generateUid(url),
				title = item.getString("title"),
				altTitles = emptySet(),
				url = url,
				publicUrl = "https://www.$domain$url",
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = item.getJSONObject("cover").getString("url"),
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val id = manga.url.substringAfterLast("_").removeSuffix("/")
		val variables = JSONObject().apply {
			put("id", id)
		}

		val url = buildString {
			append("https://www.")
			append(domain)
			append("/graphql/nobatch/?operationName=AlbumGet&query=")
			append(ALBUM_GET_QUERY.urlEncoded())
			append("&variables=")
			append(variables.toString().urlEncoded())
		}

		val json = webClient.httpGet(url).parseJson()
		val data = json.getJSONObject("data").getJSONObject("album").getJSONObject("get")

		val tags = data.optJSONArray("tags")?.mapJSONNotNull { 
			MangaTag(title = it.getString("text"), key = it.getString("text"), source = source)
		}?.toSet() ?: emptySet()

		val artist = tags.find { it.title.contains("Artist:", ignoreCase = true) }?.title?.substringAfter(":")?.trim()

		return manga.copy(
			tags = tags,
			authors = setOfNotNull(artist),
			description = data.getStringOrNull("description"),
			chapters = listOf(
				MangaChapter(
					id = generateUid(manga.url),
					title = "Album",
					number = 1f,
					url = manga.url,
					source = source,
					scanlator = null,
					uploadDate = 0,
					branch = null,
					volume = 0
				)
			)
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val id = chapter.url.substringAfterLast("_").removeSuffix("/")
		val pages = mutableListOf<MangaPage>()
		var pageNum = 1
		var hasNext = true

		while (hasNext) {
			val variables = JSONObject().apply {
				val input = JSONObject().apply {
					put("page", pageNum)
					put("display", "position")
					put("filters", org.json.JSONArray().put(JSONObject().apply {
						put("name", "album_id")
						put("value", id)
					}))
				}
				put("input", input)
			}

			val url = buildString {
				append("https://www.")
				append(domain)
				append("/graphql/nobatch/?operationName=AlbumListOwnPictures&query=")
				append(ALBUM_PICTURES_QUERY.urlEncoded())
				append("&variables=")
				append(variables.toString().urlEncoded())
			}

			val json = webClient.httpGet(url).parseJson()
			val list = json.getJSONObject("data").getJSONObject("picture").getJSONObject("list")
			val items = list.getJSONArray("items")
			
			for (i in 0 until items.length()) {
				val item = items.getJSONObject(i)
				val imgUrl = item.getStringOrNull("url_to_original") 
					?: item.getJSONArray("thumbnails").getJSONObject(0).getString("url")
				
				pages.add(MangaPage(
					id = generateUid(imgUrl),
					url = if (imgUrl.startsWith("//")) "https:$imgUrl" else imgUrl,
					preview = null,
					source = source
				))
			}

			hasNext = list.getJSONObject("info").getBoolean("has_next_page")
			pageNum++
			if (pageNum > 10) break // Limit to prevent infinite loop just in case
		}

		return pages
	}

	companion object {
		private val ALBUM_LIST_QUERY = """
			query AlbumList(${'$'}input: AlbumListInput!) {
				album {
					list(input: ${'$'}input) {
						info {
							page
							has_next_page
						}
						items {
							title
							url
							cover {
								url
							}
						}
					}
				}
			}
		""".trimIndent()

		private val ALBUM_GET_QUERY = """
			query AlbumGet(${'$'}id: ID!) {
				album {
					get(id: ${'$'}id) {
						... on Album {
							title
							url
							description
							cover {
								url
							}
							tags {
								text
							}
						}
					}
				}
			}
		""".trimIndent()

		private val ALBUM_PICTURES_QUERY = """
			query AlbumListOwnPictures(${'$'}input: PictureListInput!) {
				picture {
					list(input: ${'$'}input) {
						info {
							has_next_page
						}
						items {
							url_to_original
							thumbnails {
								url
							}
						}
					}
				}
			}
		""".trimIndent()
	}
}
