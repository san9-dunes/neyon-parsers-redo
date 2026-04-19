package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import kotlin.time.Duration.Companion.minutes

internal class FocusedParserSmokeTest {

    private val context = MangaLoaderContextMock

    @ParameterizedTest(name = "{index}|list_relative_url|{0}")
    @EnumSource(
        value = MangaParserSource::class,
        names = [
            "HENTAI2NET", "MANGAREADORG", "GOCTRUYENTRANH", "KORELISCANS", "OPIATOON",
            "JIANGZAITOON", "LAVINIAFANSUB", "KLIKMANGA", "MANGASPARK", "LEKMANGACOM",
            "SAMURAISCAN", "HADESNOFANSUB", "MANTRAZSCAN", "COFFEEMANGA", "KIRYUU_03",
            "DIVASCANS_COM", "SRC_3HENTAI", "IMHENTAI_COM", "DOUJINDESUUK", "SCHALENETWORK"
        ],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun checkSourceIsAlive(source: MangaParserSource) = runTest(timeout = 2.minutes) {
        val parser = context.newParserInstance(source)
        try {
            val list = parser.getList(0, parser.availableSortOrders.first(), MangaListFilter())
            println(source.name + " => FOUND " + list.size + " items")
            if (list.isNotEmpty()) {
                val details = parser.getDetails(list.first())
                println(source.name + " => DETAILS chapters: " + details.chapters?.size)
            } else {
                throw Exception("List is empty for " + source.name)
            }
        } catch (e: Exception) {
            System.err.println(source.name + " => FAILED: " + e.message)
            e.printStackTrace()
            throw e
        }
    }
}
