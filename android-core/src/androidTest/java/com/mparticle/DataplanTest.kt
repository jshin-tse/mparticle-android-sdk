package com.mparticle

import com.mparticle.internal.Constants
import com.mparticle.networking.Matcher
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.lang.Exception

class DataplanTest : BaseCleanInstallEachTest() {
    var testingUtils: TestingUtils = TestingUtils.getInstance()
    @Test
    @Throws(InterruptedException::class)
    fun noDataPlanTest() {
        startMParticle(
            MParticleOptions.builder(context)
                .dataplan(null, null)
        )
        val messageCount: AndroidUtils.Mutable<Int> = Mutable<Int>(0)
        val latch = FailureLatch()
        MockServer.getInstance().waitForVerify(Matcher().bodyMatch(object : JSONMatch() {
            fun isMatch(bodyJson: JSONObject): Boolean {
                try {
                    TestCase.assertNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT))
                    messageCount.value += getMessageCount(bodyJson)
                    if (messageCount.value === 3) {
                        latch.countDown()
                        return true
                    }
                } catch (ex: JSONException) {
                }
                return false
            }
        }), latch)
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.upload()
        latch.await()
        assertEquals(3, messageCount.value.intValue())
    }

    @Test
    @Throws(InterruptedException::class)
    fun dataplanPartialTest() {
        startMParticle(
            MParticleOptions.builder(context)
                .dataplan("plan1", null)
        )
        val messageCount: AndroidUtils.Mutable<Int> = Mutable<Int>(0)
        val latch = FailureLatch()
        MockServer.getInstance()
            .waitForVerify(Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch(object :
                JSONMatch() {
                fun isMatch(bodyJson: JSONObject): Boolean {
                    try {
                        Assert.assertNotNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT))
                        val dataplanContext =
                            bodyJson.getJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT)
                        val dataplanJSON =
                            dataplanContext.getJSONObject(Constants.MessageKey.DATA_PLAN_KEY)
                        Assert.assertEquals(
                            "plan1",
                            dataplanJSON.getString(Constants.MessageKey.DATA_PLAN_ID)
                        )
                        TestCase.assertNull(
                            dataplanJSON.optString(
                                Constants.MessageKey.DATA_PLAN_VERSION,
                                null
                            )
                        )
                        messageCount.value += getMessageCount(bodyJson)
                        if (messageCount.value === 3) {
                            latch.countDown()
                            return true
                        }
                    } catch (ex: JSONException) {
                    }
                    return false
                }
            }), latch)
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.upload()
        latch.await()
        assertEquals(3, messageCount.value.intValue())
    }

    @Test
    @Throws(InterruptedException::class)
    fun noDataPlanIdTest() {
        startMParticle(
            MParticleOptions.builder(context)
                .dataplan(null, 1)
        )
        val messageCount: AndroidUtils.Mutable<Int> = Mutable<Int>(0)
        val latch = FailureLatch()
        MockServer.getInstance()
            .waitForVerify(Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch(object :
                JSONMatch() {
                fun isMatch(bodyJson: JSONObject): Boolean {
                    try {
                        TestCase.assertNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT))
                        messageCount.value += getMessageCount(bodyJson)
                        if (messageCount.value === 3) {
                            latch.countDown()
                            return true
                        }
                    } catch (ex: JSONException) {
                    }
                    return false
                }
            }), latch)
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.upload()
        latch.await()
        assertEquals(3, messageCount.value.intValue())
    }

    @Test
    @Throws(InterruptedException::class)
    fun dataPlanSetTest() {
        startMParticle(
            MParticleOptions.builder(context)
                .dataplan("dataplan1", 1)
        )
        val messageCount: AndroidUtils.Mutable<Int> = Mutable<Int>(0)
        val latch = FailureLatch()
        MockServer.getInstance()
            .waitForVerify(Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch(object :
                JSONMatch() {
                fun isMatch(bodyJson: JSONObject): Boolean {
                    try {
                        Assert.assertNotNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT))
                        val dataplanContext =
                            bodyJson.getJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT)
                        val dataplanJSON =
                            dataplanContext.getJSONObject(Constants.MessageKey.DATA_PLAN_KEY)
                        Assert.assertEquals(
                            "dataplan1",
                            dataplanJSON.getString(Constants.MessageKey.DATA_PLAN_ID)
                        )
                        Assert.assertEquals(
                            "1",
                            dataplanJSON.optString(Constants.MessageKey.DATA_PLAN_VERSION, null)
                        )
                        val messages = bodyJson.optJSONArray("msgs")
                        messageCount.value += getMessageCount(bodyJson)
                        if (messageCount.value === 3) {
                            latch.countDown()
                            return true
                        }
                    } catch (ex: Exception) {
                        Assert.fail(ex.toString())
                    }
                    return false
                }
            }), latch)
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.upload()
        latch.await()
        assertEquals(3, messageCount.value.intValue())
    }

    @Test
    @Throws(InterruptedException::class)
    fun dataplanChanged() {
        startMParticle(
            MParticleOptions.builder(context)
                .dataplan("dataplan1", 1)
        )
        val totalMessageCount: AndroidUtils.Mutable<Int> = Mutable<Int>(0)
        val dataplan1MessageCount: AndroidUtils.Mutable<Int> = Mutable<Int>(0)
        val dataplan2MessageCount: AndroidUtils.Mutable<Int> = Mutable<Int>(0)
        val latch = FailureLatch()
        MockServer.getInstance()
            .waitForVerify(Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch(object :
                JSONMatch() {
                fun isMatch(bodyJson: JSONObject): Boolean {
                    try {
                        Assert.assertNotNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT))
                        val dataplanContext =
                            bodyJson.getJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT)
                        val dataplanJSON =
                            dataplanContext.getJSONObject(Constants.MessageKey.DATA_PLAN_KEY)
                        val dataplanId = dataplanJSON.getString(Constants.MessageKey.DATA_PLAN_ID)
                        val dataplanVersion =
                            dataplanJSON.optInt(Constants.MessageKey.DATA_PLAN_VERSION, -1)
                        val messageCount = getMessageCount(bodyJson)
                        if (1 == dataplanVersion) {
                            Assert.assertEquals("dataplan1", dataplanId)
                            dataplan1MessageCount.value += messageCount
                        }
                        if (2 == dataplanVersion) {
                            Assert.assertEquals("dataplan1", dataplanId)
                            dataplan2MessageCount.value += messageCount
                        }
                        totalMessageCount.value += messageCount
                        if (totalMessageCount.value === 5) {
                            latch.countDown()
                        }
                    } catch (ex: Exception) {
                        Assert.fail(ex.toString())
                    }
                    return false
                }
            }), latch)
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(context)
                .dataplan("dataplan1", 2)
        )
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.logEvent(testingUtils.getRandomMPEventRich())
        MParticle.getInstance()!!.upload()

        //not sure why it needs upload() twice, but this cuts the runtime down from 10s to .7s
        MParticle.getInstance()!!.upload()
        MParticle.getInstance()!!.upload()
        latch.await()
        assertEquals(3, dataplan1MessageCount.value.intValue())
        assertEquals(2, dataplan2MessageCount.value.intValue())
        assertEquals(5, totalMessageCount.value.intValue())
    }

    @Throws(JSONException::class)
    private fun getMessageCount(bodyJson: JSONObject): Int {
        var count = 0
        val messages = bodyJson.optJSONArray("msgs")
        if (messages != null) {
            for (i in 0 until messages.length()) {
                val messageJSON = messages.getJSONObject(i)
                if (messageJSON.getString("dt") == "e") {
                    count++
                }
            }
        }
        return count
    }
}