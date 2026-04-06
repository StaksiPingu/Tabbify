package com.tabbify.domain.scoring

import com.tabbify.data.model.InstrumentType
import kotlinx.serialization.Serializable

@Serializable
data class ScoreCriteria(
    val id: String,
    val name: String,
    val description: String,
    val weight: Float = 0.25f,
    val enabled: Boolean = true,
    val isCustom: Boolean = false
)

@Serializable
data class ScoreConfig(
    val instrumentType: InstrumentType,
    val criteria: List<ScoreCriteria>
) {
    val totalWeight: Float get() = criteria.filter { it.enabled }.sumOf { it.weight.toDouble() }.toFloat()
}

object DefaultScoreConfigs {
    private fun pitchCriteria() = ScoreCriteria(
        id = "pitch",
        name = "Tonhöhe (Pitch)",
        description = "Wie genau die gespielten Töne die Zieltonhöhe treffen",
        weight = 0.35f
    )

    private fun timingCriteria() = ScoreCriteria(
        id = "timing",
        name = "Timing",
        description = "Wie präzise die Noten zum Beat gespielt werden",
        weight = 0.30f
    )

    private fun rhythmCriteria() = ScoreCriteria(
        id = "rhythm",
        name = "Rhythmus-Konsistenz",
        description = "Wie gleichmäßig und stabil der Rhythmus gehalten wird",
        weight = 0.20f
    )

    private fun dynamicsCriteria() = ScoreCriteria(
        id = "dynamics",
        name = "Dynamik",
        description = "Variation in der Lautstärke und Ausdrucksstärke",
        weight = 0.15f
    )

    fun forInstrument(instrument: InstrumentType): ScoreConfig = when (instrument) {
        InstrumentType.GUITAR -> ScoreConfig(
            instrumentType = instrument,
            criteria = listOf(
                pitchCriteria(),
                timingCriteria(),
                rhythmCriteria(),
                dynamicsCriteria(),
                ScoreCriteria(
                    id = "chord_transitions",
                    name = "Akkord-Wechsel",
                    description = "Flüssigkeit und Sauberkeit beim Wechseln zwischen Akkorden",
                    weight = 0.20f
                )
            ).normalizeWeights()
        )

        InstrumentType.BASS -> ScoreConfig(
            instrumentType = instrument,
            criteria = listOf(
                ScoreCriteria(
                    id = "groove",
                    name = "Groove",
                    description = "Wie gut der Bass mit dem Schlagzeug/Rhythmus groovt",
                    weight = 0.30f
                ),
                timingCriteria(),
                ScoreCriteria(
                    id = "intonation",
                    name = "Intonation",
                    description = "Sauberkeit der Töne und Muting ungewollter Saiten",
                    weight = 0.25f
                ),
                dynamicsCriteria()
            ).normalizeWeights()
        )

        InstrumentType.PIANO -> ScoreConfig(
            instrumentType = instrument,
            criteria = listOf(
                ScoreCriteria(
                    id = "note_accuracy",
                    name = "Notengenauigkeit",
                    description = "Wie korrekt die richtigen Tasten getroffen werden",
                    weight = 0.35f
                ),
                timingCriteria(),
                dynamicsCriteria(),
                ScoreCriteria(
                    id = "hand_coordination",
                    name = "Händekoordination",
                    description = "Zusammenspiel beider Hände",
                    weight = 0.20f
                )
            ).normalizeWeights()
        )

        InstrumentType.DRUMS -> ScoreConfig(
            instrumentType = instrument,
            criteria = listOf(
                timingCriteria().copy(weight = 0.40f),
                ScoreCriteria(
                    id = "groove",
                    name = "Groove & Feel",
                    description = "Natürlichkeit und Musikalität des Rhythmus",
                    weight = 0.30f
                ),
                dynamicsCriteria(),
                ScoreCriteria(
                    id = "consistency",
                    name = "Konsistenz",
                    description = "Gleichmäßigkeit über die gesamte Aufnahme",
                    weight = 0.15f
                )
            ).normalizeWeights()
        )

        InstrumentType.VOCALS -> ScoreConfig(
            instrumentType = instrument,
            criteria = listOf(
                pitchCriteria().copy(weight = 0.40f),
                timingCriteria().copy(weight = 0.25f),
                ScoreCriteria(
                    id = "tone",
                    name = "Tonqualität",
                    description = "Klangfarbe und Reinheit der Stimme",
                    weight = 0.20f
                ),
                ScoreCriteria(
                    id = "breath_control",
                    name = "Atemkontrolle",
                    description = "Effizienter Einsatz des Atems",
                    weight = 0.15f
                )
            ).normalizeWeights()
        )

        else -> ScoreConfig(
            instrumentType = instrument,
            criteria = listOf(
                pitchCriteria(),
                timingCriteria(),
                rhythmCriteria(),
                dynamicsCriteria()
            ).normalizeWeights()
        )
    }

    private fun List<ScoreCriteria>.normalizeWeights(): List<ScoreCriteria> {
        val total = sumOf { it.weight.toDouble() }.toFloat()
        return if (total == 0f) this
        else map { it.copy(weight = it.weight / total) }
    }
}
