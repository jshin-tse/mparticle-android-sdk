package com.mparticle.internal.database.services

import android.location.Location
import com.mparticle.testutils.BaseCleanInstallEachTest
import org.json.JSONException
import org.junit.Before
import java.lang.Exception
import java.util.UUID

abstract class BaseMPServiceTest : BaseCleanInstallEachTest() {
    @Before
    @Throws(Exception::class)
    fun beforeBaseMPService() {
        val openHelper: SQLiteOpenHelper = TestSQLiteOpenHelper(
            MParticleDatabaseHelper(context),
            MParticleDatabaseHelper.getDbName()
        )
        database = MPDatabaseImpl(openHelper.getWritableDatabase())
    }

    @get:Throws(JSONException::class)
    val mpMessage: BaseMPMessage
        get() = getMpMessage(UUID.randomUUID().toString())

    @Throws(JSONException::class)
    fun getMpMessage(sessionId: String): BaseMPMessage {
        return getMpMessage(sessionId, RandomUtils.randomLong(Long.MIN_VALUE, Long.MAX_VALUE))
    }

    @Throws(JSONException::class)
    fun getMpMessage(sessionId: String, mpid: Long): BaseMPMessage {
        val session = InternalSession()
        session.mSessionID = sessionId
        return BaseMPMessage.Builder(
            RandomUtils.getAlphaNumericString(
                RandomUtils.randomInt(
                    20,
                    48
                )
            )
        ).build(
            session,
            Location(RandomUtils.getAlphaNumericString(RandomUtils.randomInt(1, 55))),
            mpid
        )
    }

    companion object {
        protected var database: MPDatabaseImpl? = null
    }
}