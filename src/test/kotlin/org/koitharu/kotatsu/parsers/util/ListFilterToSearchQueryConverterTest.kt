package org.koitharu.kotatsu.parsers.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.YEAR_UNKNOWN
import java.util.Locale

class MangaListFilterStateTest {

    @Test
    fun copyPreservesAndUpdatesFields() {
        val initial = MangaListFilter(
            query = "one piece",
            tags = setOf(buildMangaTag("shounen")),
            locale = Locale.ENGLISH,
        )

        val updated = initial.copy(
            query = "bleach",
            originalLocale = Locale.JAPANESE,
        )

        assertEquals("bleach", updated.query)
        assertEquals(initial.tags, updated.tags)
        assertEquals(Locale.ENGLISH, updated.locale)
        assertEquals(Locale.JAPANESE, updated.originalLocale)
    }

    @Test
    fun yearFieldsAffectNonSearchOptionsState() {
        val empty = MangaListFilter()
        assertFalse(empty.hasNonSearchOptions())

        val withYear = MangaListFilter(year = 2024)
        assertTrue(withYear.hasNonSearchOptions())

        val withRange = MangaListFilter(year = YEAR_UNKNOWN, yearFrom = 1990, yearTo = 2000)
        assertTrue(withRange.hasNonSearchOptions())
    }

    private fun buildMangaTag(name: String): MangaTag {
        return MangaTag(
            key = "${name}Key",
            title = name,
            source = MangaParserSource.MANGADEX,
        )
    }
}
