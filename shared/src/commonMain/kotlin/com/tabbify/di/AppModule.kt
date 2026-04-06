package com.tabbify.di

import com.tabbify.domain.scoring.ScoreEngine
import com.tabbify.platform.AudioEngine
import com.tabbify.platform.StorageManager
import com.tabbify.ui.home.HomeViewModel
import com.tabbify.ui.recorder.RecorderViewModel
import com.tabbify.ui.songbuilder.SongBuilderViewModel
import com.tabbify.ui.practice.PracticeViewModel
import com.tabbify.ui.analysis.AnalysisViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

val platformModule = module {
    single { AudioEngine() }
    single { StorageManager() }
}

val domainModule = module {
    single { ScoreEngine() }
}

val viewModelModule = module {
    factory { HomeViewModel(get(), get()) }
    factory { (songId: String) -> RecorderViewModel(songId, get(), get(), get()) }
    factory { (songId: String) -> SongBuilderViewModel(songId, get(), get()) }
    factory { PracticeViewModel(get()) }
    factory { (trackId: String) -> AnalysisViewModel(trackId, get(), get()) }
}

val appModules = listOf(platformModule, domainModule, viewModelModule)

fun initKoin(config: KoinApplication.() -> Unit = {}) {
    startKoin {
        config()
        modules(appModules)
    }
}
