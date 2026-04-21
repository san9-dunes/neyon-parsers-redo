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
            "KIRYUU_03",
            "DOUJINDESUUK",
            "HIVECOMIC",
            "DIVASCANS",
            "MANGAGEKO",
            "KOHARU",
            "SCHALENETWORK",
            "HENTAIHERE",
            "HENTAIPAW",
            "HENTAIKISU",
            "YAOIX33",
            "DOUJINZA",
            "MANGALIVRE",
            "EIGHTMUSES_XXX",
            "FAKKU",
            "FHENTAI",
            "HENTAIHUG",
            "HENTAINAME",
            "HENTAIREAD",
        ],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun checkRequestedSource(source: MangaParserSource) = runTest(timeout = 2.minutes) {
        val parser = context.newParserInstance(source)
        val list = parser.getList(0, parser.availableSortOrders.first(), MangaListFilter())
        println(source.name + " => FOUND " + list.size + " items")
        check(list.isNotEmpty()) { "List is empty for ${source.name}" }
        val details = parser.getDetails(list.first())
        println(source.name + " => DETAILS chapters: " + details.chapters?.size)
    }
}
