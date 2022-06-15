package com.mparticle.internal.database.tables

import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.testutils.BaseCleanInstallEachTest
import org.junit.Before
import java.lang.Exception
import java.util.concurrent.CountDownLatch

open class BaseTableTest : BaseCleanInstallEachTest() {
    var onCreateLatch: CountDownLatch = FailureLatch()
    var onUpgradeLatch: CountDownLatch = FailureLatch()
    @Throws(InterruptedException::class)
    protected fun runTest(helper: SQLiteOpenHelperWrapper?, oldVersion: Int = 6) {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
        var openHelper = TestSQLiteOpenHelper(helper, DB_NAME, oldVersion)
        openHelper.getWritableDatabase()
        openHelper.onCreateLatch.await()
        openHelper = TestSQLiteOpenHelper(helper, DB_NAME, MParticleDatabaseHelper.DB_VERSION)
        openHelper.getWritableDatabase()
        if (oldVersion < MParticleDatabaseHelper.DB_VERSION) {
            openHelper.onUpgradeLatch.await()
        }
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
    }

    @Before
    @Throws(Exception::class)
    fun before() {
        deleteTestingDatabase()
    }

    protected fun deleteTestingDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
    }

    companion object {
        const val DB_NAME = "test_database"
    }
}