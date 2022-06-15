package com.mparticle.internal.database

import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.testutils.MPLatch
import java.util.concurrent.CountDownLatch

class TestSQLiteOpenHelper @JvmOverloads constructor(
    helper: SQLiteOpenHelperWrapper,
    databaseName: String?,
    version: Int = 1
) : SQLiteOpenHelper(
    InstrumentationRegistry.getInstrumentation().context, databaseName, null, version
) {
    var helper: SQLiteOpenHelperWrapper
    var onCreateLatch: CountDownLatch
    var onUpgradeLatch: CountDownLatch
    var onDowngradeLatch: CountDownLatch
    override fun onCreate(db: SQLiteDatabase) {
        helper.onCreate(db)
        onCreateLatch.countDown()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        helper.onUpgrade(db, oldVersion, newVersion)
        onUpgradeLatch.countDown()
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        helper.onDowngrade(db, oldVersion, newVersion)
        onDowngradeLatch.countDown()
    }

    init {
        this.helper = helper
        onCreateLatch = FailureLatch()
        onUpgradeLatch = FailureLatch()
        onDowngradeLatch = FailureLatch()
        getWritableDatabase()
    }
}