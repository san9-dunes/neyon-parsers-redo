package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import kotlin.time.Duration.Companion.minutes

internal class RequestedSourcesSmokeTest {

    private val context = MangaLoaderContextMock

    @ParameterizedTest(name = "{index}|requested|{0}")
    @EnumSource(
        value = MangaParserSource::class,
        names = [
            "BONDAGECOMIXXX",
            "ADULTCOMIXXX",
            "EIGHTMUSES",
            "EIGHTMUSES_COM",
            "MULTPORN",
            "MANGASHIINA",
            "WEBTOONPORN"
        ],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun checkRequestedSource(source: MangaParserSource) = runTest(timeout = 2.minutes) {
        val parser = context.newParserInstance(source)
        val list = parser.getList(0, parser.availableSortOrders.first(), MangaListFilter())
        println("SMOKE_TEST: ${source.name} => FOUND ${list.size} items")
        check(list.isNotEmpty()) { "List is empty for ${source.name}" }
        
        // Find first manga that actually has a title (to avoid dummy entries)
        val manga = list.firstOrNull { it.title.isNotBlank() } 
            ?: error("All found items for ${source.name} have blank titles")

        val details = parser.getDetails(manga)
        println("SMOKE_TEST: ${source.name} => DETAILS chapters: ${details.chapters?.size}")
        
        // For MangaShiina/others, if first item has 0 chapters, try the second one
        var targetChapters = details.chapters
        if (targetChapters.isNullOrEmpty() && list.size > 1) {
            println("SMOKE_TEST: ${source.name} => First item had 0 chapters, trying second item...")
            val details2 = parser.getDetails(list[1])
            targetChapters = details2.chapters
        }

        checkNotNull(targetChapters) { "Chapters list is null for ${source.name}" }
        check(targetChapters.isNotEmpty()) { "Chapters list is empty for ${source.name}" }

        val pages = parser.getPages(targetChapters.first())
        println("SMOKE_TEST: ${source.name} => PAGES: ${pages.size}")
        check(pages.isNotEmpty()) { "Pages list is empty for ${source.name}" }
    }
}
