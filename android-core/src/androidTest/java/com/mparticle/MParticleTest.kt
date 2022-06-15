package com.mparticle

import android.content.Context
import android.location.Location
import android.os.Handler
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import com.mparticle.networking.Matcher
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.Arrays
import java.util.HashMap
import java.util.concurrent.CountDownLatch

class MParticleTest : BaseStartedTest() {
    private val configResponse =
        "{\"dt\":\"ac\", \"id\":\"fddf1f96-560e-41f6-8f9b-ddd070be0765\", \"ct\":1434392412994, \"dbg\":false, \"cue\":\"appdefined\", \"pmk\":[\"mp_message\", \"com.urbanairship.push.ALERT\", \"alert\", \"a\", \"message\"], \"cnp\":\"appdefined\", \"soc\":0, \"oo\":false, \"eks\":[] }, \"pio\":30 }"

    @Test
    fun testEnsureSessionActive() {
        MParticle.getInstance()!!.mAppStateManager.ensureActiveSession()
        ensureSessionActive()
    }

    @Test
    fun testEnsureSessionActiveAtStart() {
        Assert.assertFalse(MParticle.getInstance()!!.isSessionActive)
    }

    @Test
    fun testSessionEndsOnOptOut() {
        MParticle.getInstance()!!.mAppStateManager.ensureActiveSession()
        Assert.assertTrue(MParticle.getInstance()!!.mAppStateManager.session.isActive)
        MParticle.getInstance()!!.optOut = true
        Assert.assertFalse(MParticle.getInstance()!!.mAppStateManager.session.isActive)
    }

    @Test
    fun testSetInstallReferrer() {
        MParticle.getInstance()!!.installReferrer = "foo install referrer"
        junit.framework.Assert.assertEquals(
            "foo install referrer", MParticle.getInstance()!!
                .installReferrer
        )
    }

    @Test
    fun testInstallReferrerUpdate() {
        val randomName: String = RandomUtils.getAlphaNumericString(RandomUtils.randomInt(4, 64))
        MParticle.getInstance()!!.installReferrer = randomName
        Assert.assertTrue(MParticle.getInstance()!!.installReferrer == randomName)
    }

    /**
     * These tests are to make sure that we are not missing any instances of the InstallReferrer
     * being set at any of the entry points, without the corresponding installReferrerUpdated() calls
     * being made.
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testCalledUpdateInstallReferrer() {
        val called = BooleanArray(2)
        MParticle.getInstance()!!.mMessageManager = object : MessageManager() {
            override fun installReferrerUpdated() {
                called[0] = true
            }
        }
        MParticle.getInstance()!!.mKitManager =
            object : KitFrameworkWrapper(context, null, null, null, true, null) {
                override fun installReferrerUpdated() {
                    called[1] = true
                }
            }

        //Test when the InstallReferrer is set directly on the InstallReferrerHelper.
        var installReferrer: String = RandomUtils.getAlphaNumericString(10)
        InstallReferrerHelper.setInstallReferrer(context, installReferrer)
        Assert.assertTrue(called[0])
        Assert.assertTrue(called[1])
        Arrays.fill(called, false)

        //Test when it is set through the MParticle object in the public API.
        installReferrer = RandomUtils.getAlphaNumericString(10)
        MParticle.getInstance()!!.installReferrer = installReferrer
        Assert.assertTrue(called[0])
        Assert.assertTrue(called[1])
        Arrays.fill(called, false)

        //Just a sanity check, if Context is null, it should not set mark the InstallReferrer as updated.
        installReferrer = RandomUtils.getAlphaNumericString(10)
        InstallReferrerHelper.setInstallReferrer(null, installReferrer)
        Assert.assertFalse(called[0])
        Assert.assertFalse(called[1])
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testRegisterWebView() {
        MParticle.setInstance(null)
        val token: String = RandomUtils.getAlphaNumericString(15)
        mServer.setupConfigResponse(
            JSONObject().put(ConfigManager.WORKSPACE_TOKEN, token).toString()
        )
        startMParticle()
        val jsInterfaces: MutableMap<String, Any> = HashMap()
        val latch = FailureLatch()
        Handler(Looper.getMainLooper()).post(Runnable {
            val webView: WebView = object : WebView(context) {
                override fun addJavascriptInterface(`object`: Any, name: String) {
                    jsInterfaces[name] = `object`
                }
            }
            MParticle.getInstance()!!.registerWebView(webView)
            Assert.assertTrue(jsInterfaces[MParticleJSInterface.INTERFACE_BASE_NAME + "_" + token + "_v2"] is MParticleJSInterface)
            val clientToken: String = RandomUtils.getAlphaNumericString(15)
            MParticle.getInstance()!!.registerWebView(webView, clientToken)
            Assert.assertTrue(jsInterfaces[MParticleJSInterface.INTERFACE_BASE_NAME + "_" + clientToken + "_v2"] is MParticleJSInterface)
            latch.countDown()
        })
        latch.await()
        Assert.assertEquals(2, jsInterfaces.size.toLong())
    }

    private fun ensureSessionActive() {
        if (!MParticle.getInstance()!!.isSessionActive) {
            MParticle.getInstance()!!.logEvent(TestingUtils.getInstance().getRandomMPEventSimple())
            Assert.assertTrue(MParticle.getInstance()!!.isSessionActive)
        }
    }

    @OrchestratorOnly
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testResetSync() {
        testReset { MParticle.reset(context) }
    }

    @OrchestratorOnly
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testResetAsync() {
        testReset {
            val latch: CountDownLatch = FailureLatch()
            MParticle.reset(context, object : ResetListener {
                override fun onReset() {
                    latch.countDown()
                }
            })
            try {
                latch.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    @OrchestratorOnly
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testResetIdentitySync() {
        testResetIdentityCall { MParticle.reset(context) }
    }

    @OrchestratorOnly
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testResetIdentityAsync() {
        testResetIdentityCall {
            val latch: CountDownLatch = FailureLatch()
            MParticle.reset(context, object : ResetListener {
                override fun onReset() {
                    latch.countDown()
                }
            })
            try {
                latch.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    @OrchestratorOnly
    @Test
    @Throws(InterruptedException::class)
    fun testResetConfigCall() {
        mServer.setupConfigResponse(configResponse, 100)
        MParticle.getInstance()!!.refreshConfiguration()
        MParticle.reset(context)
        //This sleep is here just to
        Thread.sleep(100)
        assertSDKGone()
    }

    /**
     * Test that Identity calls in progress will exit gracefully, and not trigger any callbacks.
     */
    @Throws(InterruptedException::class)
    fun testResetIdentityCall(resetRunnable: Runnable) {
        val called = BooleanArray(2)
        val crashListener: IdentityStateListener = object : IdentityStateListener {
            override fun onUserIdentified(user: MParticleUser, previousUser: MParticleUser?) {
                Assert.assertTrue(called[0])
                throw IllegalStateException("Should not be getting callbacks after reset")
            }
        }
        mServer.setupHappyIdentify(Random.Default.nextLong(), 100)
        MParticle.getInstance()!!.Identity().addIdentityStateListener(crashListener)
        MParticle.getInstance()!!
            .Identity().identify(IdentityApiRequest.withEmptyUser().build())
        called[0] = true
        mServer.waitForVerify(Matcher(mServer.Endpoints().getIdentifyUrl()))
        resetRunnable.run()
        assertSDKGone()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testPushEnabledApi() {
        val senderId = "senderId"
        startMParticle()
        MParticle.getInstance()!!.Messaging().enablePushNotifications(senderId)
        var fetchedSenderId: String =
            MParticle.getInstance().Internal().getConfigManager().getPushSenderId()
        Assert.assertTrue(MParticle.getInstance().Internal().getConfigManager().isPushEnabled())
        Assert.assertEquals(senderId, fetchedSenderId)
        val otherSenderId = "senderIdLogPushRegistration"
        MParticle.getInstance()!!.logPushRegistration("instanceId", otherSenderId)
        fetchedSenderId = MParticle.getInstance().Internal().getConfigManager().getPushSenderId()
        Assert.assertEquals(otherSenderId, fetchedSenderId)
        MParticle.getInstance()!!.Messaging().disablePushNotifications()
        fetchedSenderId = MParticle.getInstance().Internal().getConfigManager().getPushSenderId()
        Assert.assertFalse(MParticle.getInstance().Internal().getConfigManager().isPushEnabled())
        TestCase.assertNull(fetchedSenderId)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLogPushRegistrationModifyMessages() {
        val pushRegistrationTest = PushRegistrationTest().setServer(mServer)
        pushRegistrationTest!!.setContext(context)
        for (setPush in pushRegistrationTest.setPushes) {
            val oldRegistration = PushRegistration(
                RandomUtils.getAlphaNumericString(10),
                RandomUtils.getAlphaNumericString(15)
            )
            setPush.setPushRegistration(oldRegistration)
            val newPushRegistration = PushRegistration(
                RandomUtils.getAlphaNumericString(10),
                RandomUtils.getAlphaNumericString(15)
            )
            val latch: CountDownLatch = FailureLatch()
            val received: AndroidUtils.Mutable<Boolean> = Mutable<Boolean>(false)
            mServer.waitForVerify(
                Matcher(
                    mServer.Endpoints().getModifyUrl(mStartingMpid)
                ).bodyMatch(object : JSONMatch() {
                    fun isMatch(jsonObject: JSONObject): Boolean {
                        if (jsonObject.has("identity_changes")) {
                            try {
                                val identityChanges = jsonObject.getJSONArray("identity_changes")
                                Assert.assertEquals(1, identityChanges.length().toLong())
                                val identityChange = identityChanges.getJSONObject(0)
                                val failureMessage =
                                    "When " + oldRegistration + " set with: " + setPush.name

                                //This is a wierd case. We might be setting the old pushRegistration with "logPushRegistration()",
                                //which will kick of its own modify request. We want to ignore this if this is the case.
                                if (identityChange.getString("new_value") == oldRegistration.instanceId) {
                                    return false
                                }
                                Assert.assertEquals(
                                    failureMessage,
                                    oldRegistration.instanceId,
                                    identityChange.getString("old_value")
                                )
                                Assert.assertEquals(
                                    failureMessage,
                                    newPushRegistration.instanceId,
                                    identityChange.getString("new_value")
                                )
                                Assert.assertEquals(
                                    failureMessage,
                                    "push_token",
                                    identityChange.getString("identity_type")
                                )
                            } catch (jse: JSONException) {
                                jse.toString()
                            }
                            return true
                        }
                        return false
                    }
                }), object : RequestReceivedCallback() {
                    fun onRequestReceived(request: Request?) {
                        received.value = true
                        latch.countDown()
                    }
                })
            MParticle.getInstance()!!
                .logPushRegistration(newPushRegistration.instanceId, newPushRegistration.senderId)
            latch.await()
        }
    }

    @Test
    fun testSetLocation() {
        val location = Location("")
        MParticle.getInstance()!!.setLocation(location)
        Assert.assertEquals(location, MParticle.getInstance()!!.mMessageManager.location)
        MParticle.getInstance()!!.setLocation(null)
        TestCase.assertNull(MParticle.getInstance()!!.mMessageManager.location)
    }

    @Throws(JSONException::class, InterruptedException::class)
    private fun testReset(resetRunnable: Runnable) {
        for (i in 0..9) {
            MParticle.getInstance()!!.logEvent(TestingUtils.getInstance().getRandomMPEventRich())
        }
        for (i in 0..9) {
            MParticle.getInstance().Internal().getConfigManager()
                .setMpid(Random.Default.nextLong(), Random.Default.nextBoolean())
        }
        val databaseJson: JSONObject = getDatabaseContents(listOf("messages"))
        Assert.assertTrue(databaseJson.getJSONArray("messages").length() > 0)
        assertEquals(6, getAllTables().size())
        Assert.assertTrue(
            10 < MParticle.getInstance().Internal().getConfigManager().getMpids().size
        )

        //Set strict mode, so if we get any warning or error messages during the reset/restart phase,
        //it will throw an exception.
        TestingUtils.setStrictMode(MParticle.LogLevel.WARNING)
        resetRunnable.run()
        assertSDKGone()

        //Restart the SDK, to the point where the initial Identity call returns, make sure there are no errors on startup.
        TestingUtils.setStrictMode(
            MParticle.LogLevel.WARNING,
            "Failed to get MParticle instance, getInstance() called prior to start()."
        )
        beforeBase()
    }

    private fun assertSDKGone() {
        //Check post-reset state:
        //should be 2 entries in default SharedPreferences (the install boolean and the original install time)
        //and 0 other SharedPreferences tables.
        //Make sure the 2 entries in default SharedPreferences are the correct values.
        //0 tables should exist.
        //Then we call DatabaseHelper.getInstance(Context).openDatabase, which should create the database,
        //and make sure it is created without an error message, and that all the tables are empty.
        val sharedPrefsDirectory: String =
            context.getFilesDir().getPath().replace("files", "shared_prefs/")
        val files = File(sharedPrefsDirectory).listFiles()
        for (file in files) {
            val sharedPreferenceName =
                file.path.replace(sharedPrefsDirectory, "").replace(".xml", "")
            if (sharedPreferenceName != "WebViewChromiumPrefs" && sharedPreferenceName != "com.mparticle.test_preferences") {
                junit.framework.Assert.fail(
                    """
    SharedPreference file failed to clear:
    ${getSharedPrefsContents(sharedPreferenceName)}
    """.trimIndent()
                )
            }
        }
        assertEquals(0, context.databaseList().length)
        try {
            val databaseJson: JSONObject = getDatabaseContents()
            val keys = databaseJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                Assert.assertEquals(key, 0, databaseJson.getJSONArray(key).length().toLong())
            }
        } catch (e: JSONException) {
            junit.framework.Assert.fail(e.message)
        }
    }

    private fun getSharedPrefsContents(name: String): String {
        return try {
            val prefs: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            """
     $name:
     ${JSONObject(prefs.getAll()).toString(4)}
     """.trimIndent()
        } catch (e: JSONException) {
            "error printing SharedPrefs :/"
        }
    }
}