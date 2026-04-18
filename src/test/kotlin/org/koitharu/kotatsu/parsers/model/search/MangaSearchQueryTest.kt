package org.koitharu.kotatsu.parsers.model.search

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities

class MangaListFilterCapabilitiesTest {

    @Test
    fun defaultsAreDisabled() {
        val capabilities = MangaListFilterCapabilities()

        assertFalse(capabilities.isMultipleTagsSupported)
        assertFalse(capabilities.isTagsExclusionSupported)
        assertFalse(capabilities.isSearchSupported)
        assertFalse(capabilities.isSearchWithFiltersSupported)
        assertFalse(capabilities.isYearSupported)
        assertFalse(capabilities.isYearRangeSupported)
        assertFalse(capabilities.isOriginalLocaleSupported)
        assertFalse(capabilities.isAuthorSearchSupported)
    }

    @Test
    fun explicitFlagsAreApplied() {
        val capabilities = MangaListFilterCapabilities(
            isMultipleTagsSupported = true,
            isTagsExclusionSupported = true,
            isSearchSupported = true,
            isSearchWithFiltersSupported = true,
            isYearSupported = true,
            isYearRangeSupported = true,
            isOriginalLocaleSupported = true,
            isAuthorSearchSupported = true,
        )

        assertTrue(capabilities.isMultipleTagsSupported)
        assertTrue(capabilities.isTagsExclusionSupported)
        assertTrue(capabilities.isSearchSupported)
        assertTrue(capabilities.isSearchWithFiltersSupported)
        assertTrue(capabilities.isYearSupported)
        assertTrue(capabilities.isYearRangeSupported)
        assertTrue(capabilities.isOriginalLocaleSupported)
        assertTrue(capabilities.isAuthorSearchSupported)
    }
}
