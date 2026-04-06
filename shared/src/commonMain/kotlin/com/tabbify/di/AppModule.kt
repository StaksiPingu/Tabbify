package com.tabbify.di

import com.tabbify.data.remote.TabbifyApiClient
import com.tabbify.data.remote.TokenStorage
import com.tabbify.data.remote.createTokenStorage
import com.tabbify.data.repository.SessionRepository
import com.tabbify.data.repository.SongRepository
import com.tabbify.data.repository.SqlDelightSessionRepository
import com.tabbify.data.repository.SqlDelightSongRepository
import com.tabbify.data.sync.SyncService
import com.tabbify.db.TabbifyDatabase
import com.tabbify.domain.scoring.ScoreEngine
import com.tabbify.platform.AudioEngine
import com.tabbify.platform.DatabaseDriverFactory
import com.tabbify.platform.StorageManager
import com.tabbify.ui.analysis.AnalysisViewModel
import com.tabbify.ui.home.HomeViewModel
import com.tabbify.ui.practice.PracticeViewModel
import com.tabbify.ui.recorder.RecorderViewModel
import com.tabbify.ui.songbuilder.SongBuilderViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

val platformModule = module {
    single { AudioEngine() }
    single { StorageManager() }
    single { DatabaseDriverFactory() }
    single { TabbifyDatabase(get<DatabaseDriverFactory>().create()) }
    single<TokenStorage> { createTokenStorage() }
}

const val API_BASE_URL = "https://tabbify.mezzaluna.work"

val dataModule = module {
    single<SongRepository> { SqlDelightSongRepository(get()) }
    single<SessionRepository> { SqlDelightSessionRepository(get()) }
    single { TabbifyApiClient(API_BASE_URL, get<TokenStorage>()) }
    single { SyncService(get(), get(), get(), get()) }
}

val domainModule = module {
    single { ScoreEngine() }
}

val viewModelModule = module {
    factory { HomeViewModel(get(), get()) }
    factory { (songId: String) -> RecorderViewModel(songId, get(), get()) }
    factory { (songId: String) -> SongBuilderViewModel(songId, get(), get()) }
    factory { PracticeViewModel() }
    factory { (trackPath: String) -> AnalysisViewModel(trackPath, get(), get()) }
}

val appModules = listOf(platformModule, dataModule, domainModule, viewModelModule)

fun initKoin(config: KoinApplication.() -> Unit = {}) {
    startKoin {
        config()
        modules(appModules)
    }
}
