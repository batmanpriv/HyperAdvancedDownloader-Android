package com.had.downloader.service

import com.had.downloader.data.model.ParsedFileInfo

object FileNameParser {
    private val seasonEpisodeRegex = Regex("""[Ss](\d{1,2})[Ee](\d{1,3})""")
    private val seasonEpisodeTextRegex = Regex("""[Ss]eason\s*(\d{1,2})\s*[Ee]pisode\s*(\d{1,3})""")
    private val seasonOnlyRegex = Regex("""[Ss](\d{1,2})(?![Ee])""")
    private val episodeOnlyRegex = Regex("""[Ee](\d{1,3})""")
    private val qualityRegex = Regex("""(\d{3,4}[pP]|4K|1080|720|480|360)""")
    private val sourceRegex = Regex("""(WEB-DL|WEBRip|BluRay|HDTV|DVD|WEB|Blu|HD)""", RegexOption.IGNORE_CASE)
    private val subDubRegex = Regex("""(SoftSub|HardSub|Dub|Dubbed|Subbed|Sub|Dub)""", RegexOption.IGNORE_CASE)
    private val yearRegex = Regex("""(19|20)\d{2}""")

    fun parse(raw: String, url: String = ""): ParsedFileInfo {
        val name = raw.substringBefore('?').substringBefore('#')
        val ext = name.substringAfterLast('.').lowercase()
        val base = name.substringBeforeLast('.')

        var title = base
        var season: Int? = null
        var episode: Int? = null
        var quality = ""
        var source = ""
        var subDub = ""
        var group = ""
        var year = ""

        val seMatch = seasonEpisodeRegex.find(base)
        if (seMatch != null) {
            season = seMatch.groupValues[1].toIntOrNull()
            episode = seMatch.groupValues[2].toIntOrNull()
            val before = base.substringBefore(seMatch.value)
            title = before.trimEnd('.', '-', '_', ' ')
        } else {
            val seTextMatch = seasonEpisodeTextRegex.find(base)
            if (seTextMatch != null) {
                season = seTextMatch.groupValues[1].toIntOrNull()
                episode = seTextMatch.groupValues[2].toIntOrNull()
                val before = base.substringBefore(seTextMatch.value)
                title = before.trimEnd('.', '-', '_', ' ')
            } else {
                val seasonMatch = seasonOnlyRegex.find(base)
                if (seasonMatch != null) {
                    season = seasonMatch.groupValues[1].toIntOrNull()
                    val before = base.substringBefore(seasonMatch.value)
                    title = before.trimEnd('.', '-', '_', ' ')
                }
                val episodeMatch = episodeOnlyRegex.find(base)
                if (episodeMatch != null && episode == null) {
                    episode = episodeMatch.groupValues[1].toIntOrNull()
                    val before = base.substringBefore(episodeMatch.value)
                    if (title.isEmpty() || title == base) {
                        title = before.trimEnd('.', '-', '_', ' ')
                    }
                }
            }
        }

        val qualityMatch = qualityRegex.find(base)
        if (qualityMatch != null) {
            quality = qualityMatch.value
        }

        val sourceMatch = sourceRegex.find(base)
        if (sourceMatch != null) {
            source = sourceMatch.value
        }

        val subDubMatch = subDubRegex.find(base)
        if (subDubMatch != null) {
            subDub = subDubMatch.value
        }

        val yearMatch = yearRegex.find(base)
        if (yearMatch != null) {
            year = yearMatch.value
        }

        val parts = base.split(Regex("[._\\- ]"))
        val possibleGroup = parts.find { it.length >= 4 && it[0].isUpperCase() && it.any { c -> c.isLowerCase() } }
        if (possibleGroup != null && !possibleGroup.matches(Regex("""\d+""")) &&
            !possibleGroup.equals("WEB", ignoreCase = true) &&
            !possibleGroup.equals("HD", ignoreCase = true)) {
            group = possibleGroup
        }

        if (title.isEmpty()) {
            val cleaned = base.replace(Regex("[._\\-]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            title = cleaned
        }

        val displayParts = mutableListOf<String>()
        if (season != null && episode != null) {
            displayParts.add("S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}")
        } else if (season != null) {
            displayParts.add("S${season.toString().padStart(2, '0')}")
        } else if (episode != null) {
            displayParts.add("E${episode.toString().padStart(2, '0')}")
        }
        if (quality.isNotEmpty()) displayParts.add(quality)
        if (source.isNotEmpty()) displayParts.add(source)
        if (subDub.isNotEmpty()) displayParts.add(subDub)

        val displayName = if (displayParts.isNotEmpty()) {
            displayParts.joinToString(" · ")
        } else {
            title.take(30)
        }

        return ParsedFileInfo(
            title = title,
            season = season,
            episode = episode,
            quality = quality,
            source = source,
            subDub = subDub,
            group = group,
            extension = ext,
            fullName = name,
            displayName = displayName,
            year = year
        )
    }
}