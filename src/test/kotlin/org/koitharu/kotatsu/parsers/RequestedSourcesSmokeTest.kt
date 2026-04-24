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
            "NHENTAI_COM",
            "HENTAIREAD",
            "MANGASHIINA",
            "BONDAGECOMIXXX",
            "YIFFER",
            "WEBTOONPORN",
            "FREECOMICS_XXX",
            "COMICPORN",
            "ADULTCOMIXXX",
            "EIGHTMUSES",
            "EIGHTMUSES_COM",
            "EIGHTMUSES_XXX",
            "MULTPORN",
            "LXMANGA",
            "MANGAKITA",
            "SVSCOMICS"
        ],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun checkRequestedSource(source: MangaParserSource) = runTest(timeout = 2.minutes) {
        val parser = context.newParserInstance(source)
        val list = parser.getList(0, parser.availableSortOrders.first(), MangaListFilter())
        println("SMOKE_TEST: ${source.name} => FOUND ${list.size} items")
        check(list.isNotEmpty()) { "List is empty for ${source.name}" }
        
        // Check more than just existence
        if (list.size == 1 && list.first().title.isBlank()) {
             error("Suspicious result for ${source.name}: only 1 empty item")
        }

        val details = parser.getDetails(list.first())
        println("SMOKE_TEST: ${source.name} => DETAILS chapters: ${details.chapters?.size}")
        checkNotNull(details.chapters) { "Chapters list is null for ${source.name}" }
        check(details.chapters!!.isNotEmpty()) { "Chapters list is empty for ${source.name}" }

        val pages = parser.getPages(details.chapters!!.first())
        println("SMOKE_TEST: ${source.name} => PAGES: ${pages.size}")
        check(pages.isNotEmpty()) { "Pages list is empty for ${source.name}" }
    }
}
