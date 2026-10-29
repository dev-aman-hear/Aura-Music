package com.aman.auramusic.data.repository

internal object CallRecordingFilter {

    private val folderExclusionMarkers = listOf(
        "/recordings/",
        "/voicerecorder/",
        "/voice recorder/",
        "/call recorder/",
        "/callrecorder/",
        "/miui/sound_recorder/",
        "/standardrecorder/",
        "/recordings/voice/",
        "/call recordings/",
        "/call_recordings/",
        "/call-recordings/",
        "/callrecordings/",
        "/call_rec/",
        "/call-rec/",
        "/callrec/",
        "/call_record/",
        "/call-record/",
        "/callrecord/",
        "/waenhanser/"
    )

    private val callRecordingPhrases = listOf(
        "call recording",
        "call recordings",
        "call recorder",
        "call record",
        "recorded call",
        "recording call",
        "phone recording",
        "phone recorder"
    )

    fun shouldExclude(
        title: String,
        artist: String,
        album: String,
        filePath: String?
    ): Boolean {
        val normalizedPath = filePath.orEmpty()
            .replace('\\', '/')
            .lowercase()

        val fileName = normalizedPath.substringAfterLast('/')
        if (fileName.startsWith("aud-")) return true

        if (folderExclusionMarkers.any { normalizedPath.contains(it) }) return true

        if (hasLargeNumericPrefix(fileName) &&
            (artist.isUnknownMetadata() || album.isUnknownMetadata())
        ) {
            return true
        }

        if (normalizedPath.split('/').any { segment ->
                val compactSegment = segment.filter(Char::isLetterOrDigit)
                compactSegment.contains("call") && compactSegment.contains("rec")
            }
        ) {
            return true
        }

        val searchableText = "$title $artist $album ${filePath.orEmpty()}"
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

        return callRecordingPhrases.any { searchableText.contains(it) }
    }

    private fun hasLargeNumericPrefix(fileName: String): Boolean {
        val prefix = fileName.takeWhile(Char::isDigit)
        return prefix.toLongOrNull()?.let { it > 10_000L } == true
    }

    private fun String.isUnknownMetadata(): Boolean {
        return isBlank() || equals("<unknown>", ignoreCase = true) ||
            equals("unknown artist", ignoreCase = true) ||
            equals("unknown album", ignoreCase = true)
    }
}
