package org.koitharu.kotatsu.parsers.site.en

import androidx.collection.ArraySet
import androidx.collection.MutableIntList
import androidx.collection.MutableIntObjectMap
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.HttpStatusException
import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.network.UserAgents
import org.koitharu.kotatsu.parsers.util.*
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

private const val SERVER_DATA_SAVER = "?type="
private const val SERVER_DATA = ""

@MangaSourceParser("HENTALK", "Hentalk", "en", type = ContentType.HENTAI)
internal class Hentalk(context: MangaLoaderContext) :
	HentalkBaseParser(context, MangaParserSource.HENTALK, "hentalk.pw")

@Broken("WIP: Search not finished yet / WIP")
@MangaSourceParser("FAKKU", "FAKKU", "en", type = ContentType.HENTAI)
internal class FakkuParser(
	context: MangaLoaderContext,
) : PagedMangaParser(context, MangaParserSource.FAKKU, pageSize = 24) {

	override val configKeyDomain = ConfigKey.Domain("fakku.net")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.NEWEST,
		SortOrder.ALPHABETICAL,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(isSearchSupported = true)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = when {
			!filter.query.isNullOrEmpty() && page > 0 -> return emptyList()
			!filter.query.isNullOrEmpty() -> "https://$domain/search/${filter.query.urlEncoded()}"
			page > 0 -> "https://$domain/hentai/page/${page + 1}"
			else -> "https://$domain/hentai"
		}
		val doc = webClient.httpGet(url).parseHtml()
		val seen = HashSet<String>()
		return doc.select("a[href^=/hentai/]").mapNotNull { a ->
			val href = a.attrAsRelativeUrlOrNull("href")?.substringBefore('?') ?: return@mapNotNull null
			if (href == "/hentai" || href.endsWith("/read") || !seen.add(href)) {
				return@mapNotNull null
			}

			val title = a.attr("title").ifBlank { a.text().trim() }
				.ifBlank { href.substringAfterLast('/').replace('-', ' ') }

			val cover = a.selectFirst("img")?.src()
				?: a.parent()?.selectFirst("img")?.src()

			Manga(
				id = generateUid(href),
				title = title,
				altTitles = emptySet(),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = cover,
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()

		val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
			?.substringBefore(" - FAKKU")
			?.takeIf { it.isNotBlank() }
			?: manga.title

		val description = doc.selectFirst("meta[name=description]")?.attr("content")

		val tags = doc.select("a[href^=/tags/]").mapNotNullToSet { a ->
			val href = a.attrAsRelativeUrlOrNull("href") ?: return@mapNotNullToSet null
			val key = href.removeSuffix('/').substringAfterLast('/')
			val name = a.text().trim()
			if (key.isBlank() || name.isBlank()) return@mapNotNullToSet null
			MangaTag(key = key, title = name, source = source)
		}

		val authors = doc.select("a[href^=/artists/]").mapNotNullToSet { a ->
			a.text().trim().takeIf { it.isNotBlank() }
		}

		val readUrl = doc.selectFirst("a[href$=/read], a[href*=/read]")
			?.attrAsRelativeUrlOrNull("href")
			?: "${manga.url.removeSuffix('/')}/read"

		val chapter = MangaChapter(
			id = generateUid(readUrl),
			title = null,
			number = 1f,
			volume = 0,
			url = readUrl,
			scanlator = null,
			uploadDate = 0,
			branch = null,
			source = source,
		)

		return manga.copy(
			title = title,
			description = description,
			tags = tags,
			authors = authors,
			chapters = listOf(chapter),
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		val pageUrls = doc.select("img[src*='t.fakku.net/images/'], img[data-src*='t.fakku.net/images/']")
			.mapNotNull { img ->
				img.attr("src").ifBlank { img.attr("data-src") }.takeIf { it.isNotBlank() }
			}
			.distinct()

		return pageUrls.map { url ->
			val relative = url.toRelativeUrl(domain)
			MangaPage(
				id = generateUid(relative),
				url = relative,
				preview = null,
				source = source,
			)
		}
	}

	override suspend fun getPageUrl(page: MangaPage): String = page.url.toAbsoluteUrl(domain)
}

internal abstract class HentalkBaseParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : PagedMangaParser(context, source, 24) {

	override val configKeyDomain = ConfigKey.Domain(defaultDomain)
	override val userAgentKey = ConfigKey.UserAgent(UserAgents.KOTATSU)

	private val preferredServerKey = ConfigKey.PreferredImageServer(
		presetValues = mapOf(
			SERVER_DATA to "Original quality",
			SERVER_DATA_SAVER to "Compressed quality",
		),
		defaultValue = SERVER_DATA,
	)

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
		keys.add(preferredServerKey)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.NEWEST,
		SortOrder.NEWEST_ASC,
		SortOrder.ALPHABETICAL,
		SortOrder.ALPHABETICAL_DESC,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isMultipleTagsSupported = true,
			isSearchWithFiltersSupported = true,
			isAuthorSearchSupported = true,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions() // not found any URLs for it

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			append("/__data.json?x-sveltekit-trailing-slash=1&x-sveltekit-invalidated=001")

			when {
				!filter.query.isNullOrEmpty() || filter.tags.isNotEmpty() || !filter.author.isNullOrEmpty() -> {
					append("&q=")

					if (!filter.author.isNullOrEmpty()) {
						append("artist:\"${space2plus(filter.author)}\"")
						append('+')
					}

					if (filter.tags.isNotEmpty()) {
						filter.tags.forEach { tag ->
							append("tag:\"${space2plus(tag.key)}\"")
							append('+')
						}
					}

					if (!filter.query.isNullOrEmpty()) {
						append(space2plus(filter.query))
					} else {
						append('+')
					}
				}
			}

			when (order) {
				SortOrder.UPDATED -> append("&sort=released_at")
				SortOrder.NEWEST_ASC -> append("&sort=created_at&order=asc")
				SortOrder.NEWEST -> append("&sort=created_at&order=desc")
				SortOrder.ALPHABETICAL -> append("&sort=title&order=asc")
				SortOrder.ALPHABETICAL_DESC -> append("&sort=title&order=desc")
				else -> {}
			}

			if (page > 1) {
				append("&page=")
				append(page)
			}
		}

		val json = try {
			webClient.httpGet(url).parseJson()
		} catch (e: HttpStatusException) {
			if (e.statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
				return emptyList()
			} else {
				throw ParseException("Can't get data from source!", url)
			}
		}

		val nodes = json.getJSONArray("nodes")
		var dataArray: JSONArray? = null
		for (i in 0 until nodes.length()) {
			val node = nodes.optJSONObject(i)
			if (node?.optString("type") == "data") {
				dataArray = node.optJSONArray("data")
				if (dataArray != null && dataArray.length() > 5) break // Found the main data array
			}
		}
		
		if (dataArray == null) return emptyList()

		val dataValues = MutableIntObjectMap<Any>(dataArray.length())
		for (i in 0 until dataArray.length()) {
			dataValues[i] = dataArray.get(i)
		}

		val archiveH = MutableIntList(dataArray.length())
		for (i in 0 until dataArray.length()) {
			val item = dataArray.opt(i)
			if (item is JSONObject && item.has("id") && item.has("hash") &&
				item.has("title") && item.has("thumbnail") && item.has("tags")
			) {
				archiveH.add(i)
			}
		}

		val mangaList = ArrayList<Manga>()
		archiveH.forEach { tempIndex ->
			val temp = dataArray.getJSONObject(tempIndex)
			val idRef = temp.getInt("id")
			val hashRef = temp.getInt("hash")
			val titleRef = temp.getInt("title")
			val thumbnailRef = temp.getInt("thumbnail")
			val tagsRef = temp.getInt("tags")

			val mangaId = dataArray.getLong(idRef)

			val key = dataArray.getString(hashRef)
			val title = dataArray.getString(titleRef)
			val idThumbnail = dataArray.getInt(thumbnailRef)

			val tagsList = dataArray.optJSONArray(tagsRef)
			val tags = ArraySet<MangaTag>()
			var author: String? = null

			if (tagsList != null) {
				var i = 0
				while (i < tagsList.length()) {
					val tagRefIndex = tagsList.getInt(i)

					if (dataValues.containsKey(tagRefIndex) &&
						dataValues[tagRefIndex] is JSONObject &&
						(dataValues[tagRefIndex] as JSONObject).has("namespace")
					) {

						val nsObj = dataValues[tagRefIndex] as JSONObject
						val nsIndex = nsObj.getInt("namespace")
						val nameIndex = nsObj.getInt("name")

						val nsValue = if (dataValues.containsKey(nsIndex)) dataValues[nsIndex].toString() else null
						val nameValue =
							if (dataValues.containsKey(nameIndex)) dataValues[nameIndex].toString() else null

						if (nsValue == "artist") {
							author = nameValue?.nullIfEmpty()
						} else if (nsValue == "tag" && nameValue != null) {
							tags.add(
								MangaTag(
									key = nameValue,
									title = nameValue,
									source = source,
								),
							)
						}
					}
					i++
				}
			}

			mangaList.add(
				Manga(
					id = generateUid(mangaId),
					url = "/g/$mangaId/__data.json?x-sveltekit-invalidated=001",
					publicUrl = "https://$domain/g/$mangaId",
					title = title,
					altTitles = emptySet(),
					coverUrl = "https://$domain/image/$key/$idThumbnail?type=cover",
					largeCoverUrl = null,
					authors = setOfNotNull(author),
					tags = tags,
					state = null,
					description = null,
					contentRating = ContentRating.ADULT,
					source = source,
					rating = RATING_UNKNOWN,
				),
			)
		}

		return mangaList
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val json = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseJson()
		val mangaId = manga.url.substringAfter("/g/").substringBefore('/')

		val dataArray = json.getJSONArray("nodes")
			.optJSONObject(2)
			?.optJSONArray("data")
			?: return manga

		var createdAt = ""

		for (i in 0 until dataArray.length()) {
			val item = dataArray.opt(i)
			if (item is JSONObject && item.has("createdAt")) {
				val addedAt = item.getInt("createdAt")
				if (dataArray.length() > addedAt) {
					createdAt = dataArray.optString(addedAt, "")
					break
				}
			}
		}

		val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
		val parseTime = dateFormat.parseSafe(createdAt)
		val chapter = MangaChapter(
			id = generateUid("/g/$mangaId/read/1"),
			url = "/g/$mangaId/read/1/__data.json?x-sveltekit-invalidated=011",
			title = "Oneshot", // for all, just has 1 chapter
			number = 0f,
			uploadDate = parseTime,
			volume = 0,
			branch = null,
			scanlator = null,
			source = source,
		)

		return manga.copy(
			chapters = listOf(chapter),
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val json = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseJson()
		val nodes = json.getJSONArray("nodes")
		
		var dataArray: JSONArray? = null
		for (i in nodes.length() - 1 downTo 0) { // Gallery is usually in the last data node
			val node = nodes.optJSONObject(i)
			if (node?.optString("type") == "data") {
				val arr = node.optJSONArray("data")
				if (arr != null) {
					// Check if this array contains image related info
					var hasImages = false
					for (j in 0 until (arr.length().coerceAtMost(50))) {
						val item = arr.opt(j)
						if (item is JSONObject && (item.has("images") || item.has("gallery") || item.has("filename"))) {
							hasImages = true
							break
						}
					}
					if (hasImages) {
						dataArray = arr
						break
					}
				}
			}
		}

		if (dataArray == null) return emptyList()

		var imagesRef = -1
		for (i in 0 until dataArray.length()) {
			val item = dataArray.opt(i)
			if (item is JSONObject && item.has("images") && item.has("title")) {
				imagesRef = item.getInt("images")
				break
			}
		}

		val imgList = ArrayList<String>()
		if (imagesRef != -1 && imagesRef < dataArray.length()) {
			val imagesArray = dataArray.optJSONArray(imagesRef)
			if (imagesArray != null) {
				for (i in 0 until imagesArray.length()) {
					val imgObjRef = imagesArray.getInt(i)
					val imgObj = dataArray.optJSONObject(imgObjRef)
					if (imgObj != null && imgObj.has("filename")) {
						val filenameRef = imgObj.getInt("filename")
						val filename = dataArray.optString(filenameRef, "")
						if (filename.isNotEmpty()) {
							imgList.add(filename)
						}
					}
				}
			}
		}

		// Fallback to old loose scanning if specific array not found
		if (imgList.isEmpty()) {
			for (i in 0 until dataArray.length()) {
				val item = dataArray.opt(i)
				if (item is JSONObject && item.has("filename")) {
					val filenameIndex = item.getInt("filename")
					if (dataArray.length() > filenameIndex) {
						val filename = dataArray.optString(filenameIndex, "")
						if (filename.isNotEmpty()) {
							imgList.add(filename)
						}
					}
				}
			}
		}

		var hashID = ""
		for (i in 0 until dataArray.length()) {
			val item = dataArray.opt(i)
			if (item is JSONObject && item.has("hash") && (item.has("id") || item.has("title"))) {
				val hashIndex = item.getInt("hash")
				hashID = dataArray.getString(hashIndex)
				if (hashID.length > 8) break // Found the main gallery hash
			}
		}

		var compressID = ""
		for (i in 0 until dataArray.length()) {
			val item = dataArray.opt(i)
			if (item is JSONObject && item.has("hash") && item.has("format")) {
				val hashIndex = item.getInt("hash")
				val hashValue = dataArray.getString(hashIndex)
				if (hashValue.length == 8) {
					compressID = hashValue
					break
				}
			}
		}

		val server = config[preferredServerKey] ?: SERVER_DATA
		return imgList.map { imgEx ->
			val baseUrl = "https://$domain/image/$hashID/$imgEx"
			val imageUrl = when (server) {
				SERVER_DATA -> baseUrl
				SERVER_DATA_SAVER -> baseUrl + SERVER_DATA_SAVER + compressID
				else -> baseUrl
			}

			MangaPage(
				id = generateUid(imageUrl),
				url = imageUrl,
				preview = null,
				source = source,
			)
		}
	}

	private fun space2plus(input: String): String = input.replace(' ', '+')
}

@MangaSourceParser("FAKKU_CC", "FAKKU (.cc)", "en", type = ContentType.HENTAI)
internal class FakkuCc(context: MangaLoaderContext) :
    HentalkBaseParser(context, MangaParserSource.FAKKU_CC, "fakku.cc")
