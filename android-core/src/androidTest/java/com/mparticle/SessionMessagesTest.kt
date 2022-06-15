package com.mparticle

import android.os.Handler
import android.os.Looper
import com.mparticle.internal.AccessUtils
import com.mparticle.internal.AppStateManager
import com.mparticle.internal.Constants
import com.mparticle.testing.BaseStartedTest
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.Exception
import java.util.concurrent.CountDownLatch

class SessionMessagesTest : BaseStartedTest() {
    var mAppStateManager: AppStateManager? = null
    var mHandler: Handler? = null
    @Before
    fun before() {
        mAppStateManager = MParticle.getInstance().Internal().getAppStateManager()
        mHandler = Handler(Looper.getMainLooper())
    }

    @Test
    @Throws(Exception::class)
    fun testSessionStartMessage() {
        val sessionStartReceived = BooleanArray(1)
        sessionStartReceived[0] = false
        Assert.assertFalse(mAppStateManager.getSession().isActive())
        val latch: CountDownLatch = FailureLatch()
        val sessionId: AndroidUtils.Mutable<String> = Mutable<String>(null)
        mAppStateManager.ensureActiveSession()
        sessionId.value = mAppStateManager.getSession().mSessionID
        AccessUtils.awaitMessageHandler()
        MParticle.getInstance()!!.upload()
        mServer.waitForVerify(Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch(object :
            JSONMatch() {
            fun isMatch(jsonObject: JSONObject): Boolean {
                try {
                    val jsonArray = jsonObject.optJSONArray(Constants.MessageKey.MESSAGES)
                        ?: return false
                    for (i in 0 until jsonArray.length()) {
                        val eventObject = jsonArray.getJSONObject(i)
                        if (eventObject.getString("dt") == Constants.MessageType.SESSION_START) {
                            Assert.assertEquals(
                                eventObject.getLong("ct").toFloat(),
                                mAppStateManager.getSession().mSessionStartTime.toFloat(),
                                1000f
                            )
                            assertEquals(
                                """started sessionID = ${sessionId.value.toString()} 
current sessionId = ${mAppStateManager.getSession().mSessionID.toString()} 
sent sessionId = ${eventObject.getString("id")}""",
                                mAppStateManager.getSession().mSessionID,
                                eventObject.getString("id")
                            )
                            sessionStartReceived[0] = true
                            return true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    junit.framework.Assert.fail(e.message)
                }
                return false
            }
        }))
        junit.framework.Assert.assertTrue(sessionStartReceived[0])
    }
}