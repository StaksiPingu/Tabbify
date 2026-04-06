package com.tabbify.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class InstrumentType(
    val displayName: String,
    val emoji: String
) {
    GUITAR("Gitarre", "🎸"),
    BASS("Bass", "🎸"),
    PIANO("Klavier/Piano", "🎹"),
    UKULELE("Ukulele", "🪕"),
    DRUMS("Schlagzeug", "🥁"),
    VIOLIN("Violine", "🎻"),
    VOCALS("Gesang", "🎤"),
    OTHER("Sonstiges", "🎵")
}
