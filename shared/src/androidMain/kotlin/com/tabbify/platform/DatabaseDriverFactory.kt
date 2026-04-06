package com.tabbify.platform

import app.cash.sqldelight.android.driver.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.tabbify.db.TabbifyDatabase

actual class DatabaseDriverFactory actual constructor() {
    actual fun create(): SqlDriver {
        return AndroidSqliteDriver(
            schema = TabbifyDatabase.Schema,
            context = appContext,
            name = "tabbify.db"
        )
    }
}
