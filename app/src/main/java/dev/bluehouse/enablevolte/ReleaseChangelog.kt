package dev.bluehouse.enablevolte

import androidx.annotation.StringRes

enum class ChangelogTone {
    FEATURE,
    IMPROVEMENT,
    FIX,
    IMPORTANT,
}

data class ChangelogItem(
    @StringRes val titleRes: Int,
    @StringRes val detailRes: Int,
    val tone: ChangelogTone,
    // Runtime-only fallback: fromReleaseNotes() parses release notes at runtime,
    // which cannot map to a string resource. Catalog items leave these null.
    val titleText: String? = null,
    val detailText: String? = null,
)

data class InstalledChangelog(
    val version: String,
    val items: List<ChangelogItem>,
)

object ReleaseChangelogCatalog {
    fun forVersion(version: String): InstalledChangelog? =
        when (version.removePrefix("v")) {
            "2.2.9" ->
                InstalledChangelog(
                    version = "2.2.9",
                    items = listOf(
                        ChangelogItem(
                            titleRes = R.string.changelog_v229_title_1,
                            detailRes = R.string.changelog_v229_detail_1,
                            tone = ChangelogTone.FIX,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v229_title_2,
                            detailRes = R.string.changelog_v229_detail_2,
                            tone = ChangelogTone.IMPROVEMENT,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v229_title_3,
                            detailRes = R.string.changelog_v229_detail_3,
                            tone = ChangelogTone.IMPORTANT,
                        ),
                    ),
                )
            "2.2.8" ->
                InstalledChangelog(
                    version = "2.2.8",
                    items = listOf(
                        ChangelogItem(
                            titleRes = R.string.changelog_v228_title_1,
                            detailRes = R.string.changelog_v228_detail_1,
                            tone = ChangelogTone.FEATURE,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v228_title_2,
                            detailRes = R.string.changelog_v228_detail_2,
                            tone = ChangelogTone.FEATURE,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v228_title_3,
                            detailRes = R.string.changelog_v228_detail_3,
                            tone = ChangelogTone.IMPROVEMENT,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v228_title_4,
                            detailRes = R.string.changelog_v228_detail_4,
                            tone = ChangelogTone.IMPORTANT,
                        ),
                    ),
                )
            "2.2.7" ->
                InstalledChangelog(
                    version = "2.2.7",
                    items = listOf(
                        ChangelogItem(
                            titleRes = R.string.changelog_v227_title_1,
                            detailRes = R.string.changelog_v227_detail_1,
                            tone = ChangelogTone.FEATURE,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v227_title_2,
                            detailRes = R.string.changelog_v227_detail_2,
                            tone = ChangelogTone.FEATURE,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v227_title_3,
                            detailRes = R.string.changelog_v227_detail_3,
                            tone = ChangelogTone.IMPROVEMENT,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v227_title_4,
                            detailRes = R.string.changelog_v227_detail_4,
                            tone = ChangelogTone.FIX,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v227_title_5,
                            detailRes = R.string.changelog_v227_detail_5,
                            tone = ChangelogTone.IMPROVEMENT,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v227_title_6,
                            detailRes = R.string.changelog_v227_detail_6,
                            tone = ChangelogTone.FIX,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v227_title_7,
                            detailRes = R.string.changelog_v227_detail_7,
                            tone = ChangelogTone.IMPORTANT,
                        ),
                    ),
                )
            "2.2.6" ->
                InstalledChangelog(
                    version = "2.2.6",
                    items = listOf(
                        ChangelogItem(
                            titleRes = R.string.changelog_v226_title_1,
                            detailRes = R.string.changelog_v226_detail_1,
                            tone = ChangelogTone.FIX,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v226_title_2,
                            detailRes = R.string.changelog_v226_detail_2,
                            tone = ChangelogTone.IMPROVEMENT,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v226_title_3,
                            detailRes = R.string.changelog_v226_detail_3,
                            tone = ChangelogTone.FIX,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v226_title_4,
                            detailRes = R.string.changelog_v226_detail_4,
                            tone = ChangelogTone.FIX,
                        ),
                    ),
                )
            "2.2.5" ->
                InstalledChangelog(
                    version = "2.2.5",
                    items =
                        listOf(
                            ChangelogItem(
                                titleRes = R.string.changelog_v225_title_1,
                                detailRes = R.string.changelog_v225_detail_1,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v225_title_2,
                                detailRes = R.string.changelog_v225_detail_2,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v225_title_3,
                                detailRes = R.string.changelog_v225_detail_3,
                                tone = ChangelogTone.FIX,
                            ),
                        ),
                )
            "2.2.1" ->
                InstalledChangelog(
                    version = "2.2.1",
                    items = listOf(
                        ChangelogItem(
                            titleRes = R.string.changelog_v221_title_1,
                            detailRes = R.string.changelog_v221_detail_1,
                            tone = ChangelogTone.FIX,
                        ),
                        ChangelogItem(
                            titleRes = R.string.changelog_v221_title_2,
                            detailRes = R.string.changelog_v221_detail_2,
                            tone = ChangelogTone.FIX,
                        ),
                    ),
                )
            "2.2.0" ->
                InstalledChangelog(
                    version = "2.2.0",
                    items =
                        listOf(
                            ChangelogItem(
                                titleRes = R.string.changelog_v220_title_1,
                                detailRes = R.string.changelog_v220_detail_1,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v220_title_2,
                                detailRes = R.string.changelog_v220_detail_2,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v220_title_3,
                                detailRes = R.string.changelog_v220_detail_3,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v220_title_4,
                                detailRes = R.string.changelog_v220_detail_4,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v220_title_5,
                                detailRes = R.string.changelog_v220_detail_5,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v220_title_6,
                                detailRes = R.string.changelog_v220_detail_6,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v220_title_7,
                                detailRes = R.string.changelog_v220_detail_7,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v220_title_8,
                                detailRes = R.string.changelog_v220_detail_8,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v220_title_9,
                                detailRes = R.string.changelog_v220_detail_9,
                                tone = ChangelogTone.IMPORTANT,
                            ),
                        ),
                )
            "2.1.0" ->
                InstalledChangelog(
                    version = "2.1.0",
                    items =
                        listOf(
                            ChangelogItem(
                                titleRes = R.string.changelog_v210_title_1,
                                detailRes = R.string.changelog_v210_detail_1,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v210_title_2,
                                detailRes = R.string.changelog_v210_detail_2,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v210_title_3,
                                detailRes = R.string.changelog_v210_detail_3,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v210_title_4,
                                detailRes = R.string.changelog_v210_detail_4,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v210_title_5,
                                detailRes = R.string.changelog_v210_detail_5,
                                tone = ChangelogTone.IMPORTANT,
                            ),
                        ),
                )
            "2.0.0" ->
                InstalledChangelog(
                    version = "2.0.0",
                    items =
                        listOf(
                            ChangelogItem(
                                titleRes = R.string.changelog_v200_title_1,
                                detailRes = R.string.changelog_v200_detail_1,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v200_title_2,
                                detailRes = R.string.changelog_v200_detail_2,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v200_title_3,
                                detailRes = R.string.changelog_v200_detail_3,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v200_title_4,
                                detailRes = R.string.changelog_v200_detail_4,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v200_title_5,
                                detailRes = R.string.changelog_v200_detail_5,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v200_title_6,
                                detailRes = R.string.changelog_v200_detail_6,
                                tone = ChangelogTone.IMPORTANT,
                            ),
                        ),
                )
            "1.0.9" ->
                InstalledChangelog(
                    version = "1.0.9",
                    items =
                        listOf(
                            ChangelogItem(
                                titleRes = R.string.changelog_v109_title_1,
                                detailRes = R.string.changelog_v109_detail_1,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v109_title_2,
                                detailRes = R.string.changelog_v109_detail_2,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v109_title_3,
                                detailRes = R.string.changelog_v109_detail_3,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v109_title_4,
                                detailRes = R.string.changelog_v109_detail_4,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v109_title_5,
                                detailRes = R.string.changelog_v109_detail_5,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v109_title_6,
                                detailRes = R.string.changelog_v109_detail_6,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                        ),
                )
            "1.0.8" ->
                InstalledChangelog(
                    version = "1.0.8",
                    items =
                        listOf(
                            ChangelogItem(
                                titleRes = R.string.changelog_v108_title_1,
                                detailRes = R.string.changelog_v108_detail_1,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v108_title_2,
                                detailRes = R.string.changelog_v108_detail_2,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v108_title_3,
                                detailRes = R.string.changelog_v108_detail_3,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v108_title_4,
                                detailRes = R.string.changelog_v108_detail_4,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v108_title_5,
                                detailRes = R.string.changelog_v108_detail_5,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v108_title_6,
                                detailRes = R.string.changelog_v108_detail_6,
                                tone = ChangelogTone.IMPORTANT,
                            ),
                        ),
                )
            "1.0.7" ->
                InstalledChangelog(
                    version = "1.0.7",
                    items =
                        listOf(
                            ChangelogItem(
                                titleRes = R.string.changelog_v107_title_1,
                                detailRes = R.string.changelog_v107_detail_1,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v107_title_2,
                                detailRes = R.string.changelog_v107_detail_2,
                                tone = ChangelogTone.IMPORTANT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v107_title_3,
                                detailRes = R.string.changelog_v107_detail_3,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                        ),
                )
            "1.0.6" ->
                InstalledChangelog(
                    version = "1.0.6",
                    items =
                        listOf(
                            ChangelogItem(
                                titleRes = R.string.changelog_v106_title_1,
                                detailRes = R.string.changelog_v106_detail_1,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v106_title_2,
                                detailRes = R.string.changelog_v106_detail_2,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v106_title_3,
                                detailRes = R.string.changelog_v106_detail_3,
                                tone = ChangelogTone.IMPORTANT,
                            ),
                        ),
                )
            "1.0.5" ->
                InstalledChangelog(
                    version = "1.0.5",
                    items =
                        listOf(
                            ChangelogItem(
                                titleRes = R.string.changelog_v105_title_1,
                                detailRes = R.string.changelog_v105_detail_1,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v105_title_2,
                                detailRes = R.string.changelog_v105_detail_2,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v105_title_3,
                                detailRes = R.string.changelog_v105_detail_3,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v105_title_4,
                                detailRes = R.string.changelog_v105_detail_4,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v105_title_5,
                                detailRes = R.string.changelog_v105_detail_5,
                                tone = ChangelogTone.IMPORTANT,
                            ),
                        ),
                )
            "1.0.4r" ->
                InstalledChangelog(
                    version = "1.0.4 rev",
                    items =
                        listOf(
                            ChangelogItem(
                                titleRes = R.string.changelog_v104r_title_1,
                                detailRes = R.string.changelog_v104r_detail_1,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v104r_title_2,
                                detailRes = R.string.changelog_v104r_detail_2,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v104r_title_3,
                                detailRes = R.string.changelog_v104r_detail_3,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v104r_title_4,
                                detailRes = R.string.changelog_v104r_detail_4,
                                tone = ChangelogTone.IMPORTANT,
                            ),
                        ),
                )
            "1.0.4" ->
                InstalledChangelog(
                    version = "1.0.4",
                    items =
                        listOf(
                            ChangelogItem(
                                titleRes = R.string.changelog_v104_title_1,
                                detailRes = R.string.changelog_v104_detail_1,
                                tone = ChangelogTone.FEATURE,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v104_title_2,
                                detailRes = R.string.changelog_v104_detail_2,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v104_title_3,
                                detailRes = R.string.changelog_v104_detail_3,
                                tone = ChangelogTone.FIX,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v104_title_4,
                                detailRes = R.string.changelog_v104_detail_4,
                                tone = ChangelogTone.IMPORTANT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v104_title_5,
                                detailRes = R.string.changelog_v104_detail_5,
                                tone = ChangelogTone.IMPROVEMENT,
                            ),
                            ChangelogItem(
                                titleRes = R.string.changelog_v104_title_6,
                                detailRes = R.string.changelog_v104_detail_6,
                                tone = ChangelogTone.IMPORTANT,
                            ),
                        ),
                )
            else -> null
        }

    fun fromReleaseNotes(
        version: String,
        notes: String,
    ): InstalledChangelog? {
        val items =
            notes
                .lineSequence()
                .map(String::trim)
                .filter { it.startsWith("- ") || it.startsWith("* ") }
                .map { it.drop(2).trim() }
                .filter(String::isNotBlank)
                .map { line ->
                    val titleMatch = Regex("^\\*\\*(.+?)\\*\\*[:—-]?\\s*(.*)$").find(line)
                    val title = titleMatch?.groupValues?.get(1)?.trim()?.trimEnd(':', '—', '-')
                        ?: line.substringBefore(":").trim().take(72)
                    val detail = titleMatch?.groupValues?.get(2)?.trim().orEmpty()
                        .ifBlank {
                            line.substringAfter(":", missingDelimiterValue = line).trim()
                        }
                    ChangelogItem(
                        titleRes = 0,
                        detailRes = 0,
                        tone = toneFor("$title $detail"),
                        titleText = title,
                        detailText = detail.takeIf { it != title }.orEmpty(),
                    )
                }
                .take(10)
                .toList()
        return items.takeIf(List<*>::isNotEmpty)?.let {
            InstalledChangelog(version.removePrefix("v"), it)
        }
    }

    private fun toneFor(value: String): ChangelogTone {
        val normalized = value.lowercase()
        return when {
            listOf("warning", "important", "root", "shizuku", "safety").any(normalized::contains) ->
                ChangelogTone.IMPORTANT
            listOf("fix", "crash", "restore", "correct").any(normalized::contains) ->
                ChangelogTone.FIX
            listOf("improve", "redesign", "refine", "report").any(normalized::contains) ->
                ChangelogTone.IMPROVEMENT
            else -> ChangelogTone.FEATURE
        }
    }
}
