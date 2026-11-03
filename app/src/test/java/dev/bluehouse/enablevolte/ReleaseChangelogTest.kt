package dev.bluehouse.enablevolte

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseChangelogTest {
    @Test
    fun radioLogReleaseHasBundledChangelog() {
        val changelog = ReleaseChangelogCatalog.forVersion("v2.2.8")

        assertEquals("2.2.8", changelog?.version)
        assertTrue(changelog.orEmptyItems().any { it.titleRes == R.string.changelog_v228_title_1 })
        assertTrue(changelog.orEmptyItems().any { it.tone == ChangelogTone.IMPORTANT && it.titleRes == R.string.changelog_v228_title_4 })
    }

    @Test
    fun liquidReleaseHasBundledChangelogAndStatesThePixel10Limit() {
        val changelog = ReleaseChangelogCatalog.forVersion("v2.2.7")

        assertEquals("2.2.7", changelog?.version)
        assertTrue(changelog.orEmptyItems().any { it.titleRes == R.string.changelog_v227_title_1 })
        assertTrue(changelog.orEmptyItems().any { it.tone == ChangelogTone.IMPORTANT && it.titleRes == R.string.changelog_v227_title_7 })
    }

    @Test
    fun combinedReleaseIncludesRootAndDiagnosticFixes() {
        val changelog = ReleaseChangelogCatalog.forVersion("v2.2.6")

        assertEquals("2.2.6", changelog?.version)
        assertTrue(changelog.orEmptyItems().any { it.titleRes == R.string.changelog_v226_title_1 })
        assertTrue(changelog.orEmptyItems().any { it.titleRes == R.string.changelog_v226_title_1 })
        assertTrue(changelog.orEmptyItems().any { it.titleRes == R.string.changelog_v226_title_3 })
        assertTrue(changelog.orEmptyItems().any { it.titleRes == R.string.changelog_v226_title_4 })
    }

    @Test
    fun finalKernelSuBuildHasBundledChangelog() {
        val changelog = ReleaseChangelogCatalog.forVersion("2.2.5")

        assertEquals("2.2.5", changelog?.version)
        assertTrue(changelog?.items?.any { it.titleRes == R.string.changelog_v225_title_1 } == true)
    }

    @Test
    fun currentReleaseHasStructuredHighlightedItems() {
        val changelog = ReleaseChangelogCatalog.forVersion("v1.0.6")

        assertNotNull(changelog)
        assertEquals("1.0.6", changelog?.version)
        assertTrue(changelog.orEmptyItems().any { it.titleRes == R.string.changelog_v106_title_1 })
    }

    @Test
    fun githubMarkdownBulletsBecomeCards() {
        val changelog =
            ReleaseChangelogCatalog.fromReleaseNotes(
                "1.1.0",
                """
                ## Pixel IMS 5G 1.1.0
                - **New monitor:** Adds a live modem timeline.
                - **Crash fix:** Corrects Pixel 6 band selection.
                """.trimIndent(),
            )

        assertEquals(2, changelog?.items?.size)
        assertEquals("New monitor", changelog?.items?.first()?.titleText)
        assertEquals(ChangelogTone.FIX, changelog?.items?.last()?.tone)
    }

    private fun InstalledChangelog?.orEmptyItems(): List<ChangelogItem> = this?.items.orEmpty()
}
