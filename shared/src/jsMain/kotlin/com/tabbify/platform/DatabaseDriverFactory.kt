package com.tabbify.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.tabbify.db.TabbifyDatabase
import org.w3c.dom.Worker

actual class DatabaseDriverFactory actual constructor() {
    actual fun create(): SqlDriver {
        return WebWorkerDriver(
            Worker(js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)"""))
        ).also { TabbifyDatabase.Schema.create(it) }
    }
}
