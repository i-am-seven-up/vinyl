package com.vinyl.library

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class MediaStoreScannerTest {
    @Test fun durationBoundaryIncludesExactlyThirtySeconds() {
        assertFalse(isLibrarySong(-1, false, false, false))
        assertFalse(isLibrarySong(29_999, false, false, false))
        assertTrue(isLibrarySong(30_000, false, false, false))
    }

    @Test fun excludesEachSystemSoundFlagEvenForLongAudio() {
        assertFalse(isLibrarySong(120_000, true, false, false))
        assertFalse(isLibrarySong(120_000, false, true, false))
        assertFalse(isLibrarySong(120_000, false, false, true))
        assertTrue(isLibrarySong(120_000, false, false, false))
    }

    @Test fun normalizesVietnameseIncludingDStrokeAndCombiningMarks() {
        assertEquals("dang di dau", normalizeSearchText("Đặng đi đâu"))
        assertEquals("dien", normalizeSearchText("Đie\u0323\u0302n"))
        assertEquals("", normalizeSearchText(""))
    }

    @Test fun normalizationDoesNotDependOnDeviceLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("indie", normalizeSearchText("INDIE"))
        } finally {
            Locale.setDefault(original)
        }
    }
}
