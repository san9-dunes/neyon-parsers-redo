package org.koitharu.kotatsu.parsers.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType.MANGA
import org.koitharu.kotatsu.parsers.model.ContentType.MANHUA
import org.koitharu.kotatsu.parsers.model.Demographic.SEINEN
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import java.util.Locale

class MangaListFilterBuilderTest {

    @Test
    fun builderPopulatesAllFields() {
        val tags = setOf(buildMangaTag("tag1"), buildMangaTag("tag2"))
        val excludedTags = setOf(buildMangaTag("exclude_tag"))
        val states = setOf(MangaState.ONGOING)
        val contentRatings = setOf(ContentRating.SAFE)
        val contentTypes = setOf(MANGA, MANHUA)
        val demographics = setOf(SEINEN)

        val listFilter = MangaListFilter.Builder()
            .query("title_name")
            .addTags(tags)
            .excludeTags(excludedTags)
            .locale(Locale.ENGLISH)
            .originalLocale(Locale.JAPANESE)
            .addStates(states)
            .addContentRatings(contentRatings)
            .addTypes(contentTypes)
            .addDemographics(demographics)
            .yearFrom(1997)
            .yearTo(2024)
            .year(2020)
            .build()

        assertEquals("title_name", listFilter.query)
        assertEquals(tags, listFilter.tags)
        assertEquals(excludedTags, listFilter.tagsExclude)
        assertEquals(Locale.ENGLISH, listFilter.locale)
        assertEquals(Locale.JAPANESE, listFilter.originalLocale)
        assertEquals(states, listFilter.states)
        assertEquals(contentRatings, listFilter.contentRating)
        assertEquals(contentTypes, listFilter.types)
        assertEquals(demographics, listFilter.demographics)
        assertEquals(2020, listFilter.year)
        assertEquals(1997, listFilter.yearFrom)
        assertEquals(2024, listFilter.yearTo)
    }

    @Test
    fun builderMergesTagCalls() {
        val tags1 = setOf(buildMangaTag("tag1"), buildMangaTag("tag2"))
        val tags2 = setOf(buildMangaTag("tag3"), buildMangaTag("tag4"))

        val listFilter = MangaListFilter.Builder()
            .addTags(tags1)
            .addTags(tags2)
            .build()

        assertEquals(tags1 union tags2, listFilter.tags)
    }

    @Test
    fun emptyStateHelpers() {
        assertTrue(MangaListFilter.EMPTY.isEmpty())
        assertFalse(MangaListFilter.EMPTY.isNotEmpty())
        assertFalse(MangaListFilter.EMPTY.hasNonSearchOptions())

        val onlyQuery = MangaListFilter(query = "naruto")
        assertFalse(onlyQuery.isEmpty())
        assertTrue(onlyQuery.isNotEmpty())
        assertFalse(onlyQuery.hasNonSearchOptions())

        val withTag = MangaListFilter(tags = setOf(buildMangaTag("action")))
        assertTrue(withTag.isNotEmpty())
        assertTrue(withTag.hasNonSearchOptions())
    }

    private fun buildMangaTag(name: String): MangaTag {
        return MangaTag(
            key = "${name}Key",
            title = name,
            source = MangaParserSource.MANGADEX,
        )
    }
}
