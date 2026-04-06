package com.tabbify.ui.analysis

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tabbify.data.model.InstrumentType
import com.tabbify.data.model.TrackAnalysis
import com.tabbify.domain.scoring.DefaultScoreConfigs
import com.tabbify.domain.scoring.ScoreConfig
import com.tabbify.domain.scoring.ScoreEngine
import com.tabbify.domain.scoring.ScoreResult
import com.tabbify.platform.AudioEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AnalysisState(
    val isAnalyzing: Boolean = true,
    val analysis: TrackAnalysis? = null,
    val scoreResult: ScoreResult? = null,
    val config: ScoreConfig? = null,
    val instrument: InstrumentType = InstrumentType.GUITAR
)

class AnalysisViewModel(
    private val trackPath: String,
    private val audioEngine: AudioEngine = AudioEngine(),
    private val scoreEngine: ScoreEngine = ScoreEngine(),
    private val instrument: InstrumentType = InstrumentType.GUITAR
) : ScreenModel {

    private val _state = MutableStateFlow(AnalysisState(instrument = instrument))
    val state: StateFlow<AnalysisState> = _state

    init {
        analyze()
    }

    private fun analyze() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isAnalyzing = true)
            try {
                val analysis = audioEngine.analyzeAudio(trackPath)
                val config = DefaultScoreConfigs.forInstrument(instrument)
                val result = scoreEngine.calculate(analysis, config)

                _state.value = AnalysisState(
                    isAnalyzing = false,
                    analysis = analysis,
                    scoreResult = result,
                    config = config,
                    instrument = instrument
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(isAnalyzing = false)
            }
        }
    }
}
