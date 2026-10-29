package com.aman.auramusic.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallRecordingFilterTest {

    @Test
    fun excludesCommonCallRecordingPathAndNameVariants() {
        assertTrue(
            CallRecordingFilter.shouldExclude(
                title = "2026-10-29 12-00-00",
                artist = "Unknown Artist",
                album = "Unknown Album",
                filePath = "/storage/emulated/0/Call_Recording/incoming.m4a"
            )
        )
        assertTrue(
            CallRecordingFilter.shouldExclude(
                title = "Unknown",
                artist = "Unknown Artist",
                album = "Unknown Album",
                filePath = "C:\\storage\\emulated\\0\\CallRec\\outgoing.mp3"
            )
        )
        assertTrue(
            CallRecordingFilter.shouldExclude(
                title = "Call-Recording 2026-10-29",
                artist = "Unknown Artist",
                album = "Unknown Album",
                filePath = "/storage/emulated/0/Audio/recording.m4a"
            )
        )
    }

    @Test
    fun excludesFilesWhoseNameStartsWithAud() {
        assertTrue(
            CallRecordingFilter.shouldExclude(
                title = "Unknown",
                artist = "Unknown Artist",
                album = "Unknown Album",
                filePath = "/storage/emulated/0/Recordings/AUD-20261029-WA0001.mp3"
            )
        )
        assertTrue(
            CallRecordingFilter.shouldExclude(
                title = "Unknown",
                artist = "Unknown Artist",
                album = "Unknown Album",
                filePath = "/storage/emulated/0/Recordings/aud-20261029.m4a"
            )
        )
    }

    @Test
    fun excludesFilesInsideWaEnhanserFolder() {
        assertTrue(
            CallRecordingFilter.shouldExclude(
                title = "Audio",
                artist = "Known Artist",
                album = "Known Album",
                filePath = "/storage/emulated/0/Android/media/WhatsApp/WaEnhanser/audio.mp3"
            )
        )
    }

    @Test
    fun excludesLargeNumericFilenameWhenArtistOrAlbumIsUnknown() {
        assertTrue(
            CallRecordingFilter.shouldExclude(
                title = "1293845",
                artist = "Unknown Artist",
                album = "Unknown Album",
                filePath = "/storage/emulated/0/Recordings/1293845.m4a"
            )
        )
        assertTrue(
            CallRecordingFilter.shouldExclude(
                title = "1293845",
                artist = "Recorded Artist",
                album = "Unknown Album",
                filePath = "/storage/emulated/0/Audio/1293845-track.mp3"
            )
        )
    }

    @Test
    fun keepsNumericFilenameWhenMetadataIsKnownOrNumberIsNotAboveThreshold() {
        assertFalse(
            CallRecordingFilter.shouldExclude(
                title = "1293845",
                artist = "Known Artist",
                album = "Known Album",
                filePath = "/storage/emulated/0/Music/1293845.mp3"
            )
        )
        assertFalse(
            CallRecordingFilter.shouldExclude(
                title = "10000",
                artist = "Unknown Artist",
                album = "Unknown Album",
                filePath = "/storage/emulated/0/Audio/10000.mp3"
            )
        )
    }

    @Test
    fun keepsOrdinaryMusicAndVoiceMessages() {
        assertFalse(
            CallRecordingFilter.shouldExclude(
                title = "Call Me Maybe",
                artist = "Carly Rae Jepsen",
                album = "Kiss",
                filePath = "/storage/emulated/0/Music/Call Me Maybe.mp3"
            )
        )
        assertFalse(
            CallRecordingFilter.shouldExclude(
                title = "PTT-20261029-WA0001",
                artist = "Unknown Artist",
                album = "WhatsApp Audio",
                filePath = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio/PTT-20261029-WA0001.opus"
            )
        )
    }
}
