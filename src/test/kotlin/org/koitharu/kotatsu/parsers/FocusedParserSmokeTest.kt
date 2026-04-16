package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.test_util.isUrlAbsolute
import kotlin.time.Duration.Companion.minutes

internal class FocusedParserSmokeTest {

    private val context = MangaLoaderContextMock

    @ParameterizedTest(name = "{index}|list_relative_url|{0}")
    @EnumSource(
        value = MangaParserSource::class,
        names = [
            "GUYACUBARI",
            "HACHIRUMI",
            "DANKE",
            "MAHOUSHOUJOBU",
            "DANDADAN",
            "KAIJUNO8",
            "WELOMA",
            "HENTAISLAYER",
            "LELSCANVF",
            "PIXHENTAI",
            "HENTAICROT",
        ],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun listUrlsAreRelative(source: MangaParserSource) = runTest(timeout = 2.minutes) {
        val parser = context.newParserInstance(source)
        val list = parser.getList(0, parser.availableSortOrders.first(), MangaListFilter())
        assertTrue(list.isNotEmpty(), "Manga list for $source is empty")
        assertTrue(list.all { !it.url.isUrlAbsolute() }, "Some manga urls are absolute for $source")
    }
}
