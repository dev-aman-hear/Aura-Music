package com.aman.auramusic.util

import android.content.Context
import com.aman.auramusic.data.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object ArtistArtworkCache {
    private val memoryMap = ConcurrentHashMap<String, String>()
    @Volatile
    private var isInitialized = false

    fun get(artistName: String): String? {
        val key = cleanKey(artistName)
        return if (key.isNotBlank()) memoryMap[key] else null
    }

    fun put(context: Context, artistName: String, url: String) {
        val key = cleanKey(artistName)
        if (key.isBlank() || url.isBlank()) return
        memoryMap[key] = url
        saveToDisk(context)
    }

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        try {
            val file = File(context.filesDir, "artist_portraits_cache.json")
            if (file.exists()) {
                val json = file.readText()
                if (json.isNotBlank()) {
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val map: Map<String, String>? = Gson().fromJson(json, type)
                    map?.let { memoryMap.putAll(it) }
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveToDisk(context: Context) {
        try {
            val file = File(context.filesDir, "artist_portraits_cache.json")
            val json = Gson().toJson(memoryMap)
            file.writeText(json)
        } catch (_: Exception) {}
    }

    private fun cleanKey(name: String): String =
        name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
}

object ArtistHelper {

    private val splitRegex = Regex(
        """,|;|&|/|\||\\|\s+feat\.?\s+|\s+ft\.?\s+|\s+with\s+|\s+and\s+|\s+x\s+|\s+vs\.?\s+""",
        RegexOption.IGNORE_CASE
    )

    private val parentheticalRegex = Regex(
        """[\(\[\{](?:feat\.?|ft\.?|with)\s+([^\)\]\}]+)[\)\]\}]""",
        RegexOption.IGNORE_CASE
    )

    private val invalidNames = setOf(
        "unknown", "unknown artist", "various", "various artists", "various artist",
        "soundtrack", "ost", "compilation", "audio", "music", "null", "undefined",
        "remix", "records", "producer", "feat", "ft", "instrumental",
        "video", "videos", "official video", "official audio", "clip", "channel",
        "top hits", "hits", "mix", "best of", "trending",
        "song", "songs", "track", "tracks", "album", "single", "singles"
    )

    fun extractArtistNames(raw: String): List<String> {
        if (raw.isBlank() || raw.equals("<unknown>", ignoreCase = true)) return emptyList()

        var cleaned = raw
            .replace(" - Topic", "", ignoreCase = true)
            .replace("Official", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .trim()

        val fromParens = mutableListOf<String>()
        parentheticalRegex.findAll(cleaned).forEach { match ->
            fromParens.add(match.groupValues[1])
        }
        cleaned = cleaned.replace(parentheticalRegex, " ")

        val parts = cleaned.split(splitRegex)

        val result = (parts + fromParens)
            .map { it.trim().trim('"', '\'', '(', ')', '[', ']', '{', '}', '-', '_', '.', '~') }
            .filter { name ->
                val lower = name.lowercase(Locale.ROOT)
                name.length in 2..40 &&
                !invalidNames.contains(lower) &&
                !lower.startsWith("top ") &&
                !lower.endsWith(" hits") &&
                !lower.contains("202") &&
                name.any { it.isLetter() }
            }
            .distinctBy { it.lowercase(Locale.ROOT) }

        return if (result.isEmpty()) {
            val fallback = raw.trim().trim('"', '\'', '(', ')', '[', ']', '{', '}', '-', '_', '.', '~')
            if (fallback.isNotBlank() && !invalidNames.contains(fallback.lowercase(Locale.ROOT))) {
                listOf(fallback)
            } else emptyList()
        } else {
            result
        }
    }

    fun groupSongsByArtist(songs: List<Song>): List<Pair<String, List<Song>>> {
        val map = linkedMapOf<String, Pair<String, MutableList<Song>>>()
        fun key(value: String): String =
            value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

        songs.forEach { song ->
            val names = extractArtistNames(song.artist)
            val effectiveNames = if (names.isEmpty()) {
                val fb = song.artist.trim().ifBlank { "Unknown Artist" }
                listOf(fb)
            } else names

            effectiveNames.forEach { rawName ->
                val artistName = rawName.trim().replace(Regex("\\s+"), " ")
                val k = key(artistName)
                if (k.isNotBlank()) {
                    val entry = map.getOrPut(k) { artistName to mutableListOf() }
                    if (entry.second.none { it.id == song.id }) {
                        entry.second.add(song)
                    }
                }
            }
        }

        return map.values
            .sortedBy { it.first.lowercase(Locale.ROOT) }
            .map { it.first to it.second.toList() }
    }

    fun songHasArtist(song: Song, targetArtist: String): Boolean {
        val cleanTarget = targetArtist.trim().lowercase(Locale.ROOT)
        if (song.artist.trim().lowercase(Locale.ROOT) == cleanTarget) return true
        val names = extractArtistNames(song.artist)
        return names.any { it.trim().lowercase(Locale.ROOT) == cleanTarget }
    }
}
