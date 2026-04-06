package com.tabbify.domain.scoring

import com.tabbify.data.model.TrackAnalysis
import kotlin.math.roundToInt

class ScoreEngine {

    fun calculate(analysis: TrackAnalysis, config: ScoreConfig): ScoreResult {
        val enabledCriteria = config.criteria.filter { it.enabled }
        if (enabledCriteria.isEmpty()) return ScoreResult(0f, emptyMap(), "Keine Kriterien aktiviert")

        val criteriaScores = enabledCriteria.associate { criteria ->
            val rawScore = getRawScore(criteria.id, analysis)
            criteria.id to rawScore
        }

        val totalWeight = enabledCriteria.sumOf { it.weight.toDouble() }.toFloat()
        val weightedSum = enabledCriteria.sumOf { criteria ->
            val score = criteriaScores[criteria.id] ?: 0f
            (score * criteria.weight).toDouble()
        }.toFloat()

        val finalScore = if (totalWeight > 0f) (weightedSum / totalWeight) * 100f else 0f
        val feedback = generateFeedback(criteriaScores, enabledCriteria)

        return ScoreResult(
            totalScore = finalScore.coerceIn(0f, 100f),
            criteriaScores = criteriaScores,
            feedback = feedback,
            grade = scoreToGrade(finalScore)
        )
    }

    private fun getRawScore(criteriaId: String, analysis: TrackAnalysis): Float = when (criteriaId) {
        "pitch" -> analysis.pitchAccuracy
        "timing" -> analysis.rhythmConsistency
        "rhythm", "consistency" -> analysis.rhythmConsistency
        "dynamics" -> analysis.dynamicsRange.coerceIn(0f, 1f)
        "groove" -> (analysis.rhythmConsistency * 0.6f + analysis.dynamicsRange * 0.4f).coerceIn(0f, 1f)
        "chord_transitions" -> analysis.pitchAccuracy * 0.8f + analysis.rhythmConsistency * 0.2f
        "intonation" -> analysis.pitchAccuracy
        "note_accuracy" -> analysis.pitchAccuracy
        "hand_coordination" -> analysis.rhythmConsistency
        "tone" -> analysis.pitchAccuracy * 0.7f + analysis.dynamicsRange * 0.3f
        "breath_control" -> analysis.dynamicsRange
        else -> 0f
    }

    private fun generateFeedback(
        criteriaScores: Map<String, Float>,
        criteria: List<ScoreCriteria>
    ): String {
        val weak = criteria.filter { (criteriaScores[it.id] ?: 0f) < 0.5f }
        val strong = criteria.filter { (criteriaScores[it.id] ?: 0f) >= 0.8f }

        return buildString {
            if (strong.isNotEmpty()) {
                append("Stark: ${strong.joinToString(", ") { it.name }}. ")
            }
            if (weak.isNotEmpty()) {
                append("Verbesserungspotenzial: ${weak.joinToString(", ") { it.name }}.")
            }
            if (strong.isEmpty() && weak.isEmpty()) {
                append("Solide Leistung – weiter üben!")
            }
        }
    }

    private fun scoreToGrade(score: Float): String = when {
        score >= 90f -> "S"
        score >= 80f -> "A"
        score >= 70f -> "B"
        score >= 60f -> "C"
        score >= 50f -> "D"
        else -> "F"
    }
}

data class ScoreResult(
    val totalScore: Float,
    val criteriaScores: Map<String, Float>,
    val feedback: String,
    val grade: String = "F"
) {
    val totalScoreInt: Int get() = totalScore.roundToInt()
}
