package com.mushinlab.launcher.core.data

import androidx.test.core.app.ApplicationProvider
import com.mushinlab.launcher.core.data.db.EnvDatabase
import org.junit.rules.ExternalResource

class DbTestRule : ExternalResource() {
    lateinit var db: EnvDatabase
        private set

    var now: Long = 1_000_000_000_000L

    val clock: () -> Long = { now }

    override fun before() {
        db = EnvDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        db.openHelper.writableDatabase
    }

    override fun after() {
        db.close()
    }
}
