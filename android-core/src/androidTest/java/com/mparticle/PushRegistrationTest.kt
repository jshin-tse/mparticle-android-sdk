package com.mparticle

import android.content.Context
import com.mparticle.networking.Matcher
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.lang.RuntimeException
import java.util.concurrent.CountDownLatch

class PushRegistrationTest : BaseStartedTest() {
    //So other classes can use the test fields
    fun setContext(context: Context) {
        context = context
    }

    @Test
    @Throws(InterruptedException::class)
    fun testPushEnabledOnStartup() {
        MParticle.reset(context)
        val newToken: String = RandomUtils.getAlphaNumericString(30)
        startMParticle()
        TestingUtils.setFirebasePresent(true, newToken)
        val latch: CountDownLatch = FailureLatch()
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid)),
            object : RequestReceivedCallback() {
                fun onRequestReceived(request: Request) {
                    val identitChanges: List<JSONObject> =
                        request.asIdentityRequest().getBody().identity_changes
                    Assert.assertEquals(1, identitChanges.size.toLong())
                    try {
                        Assert.assertEquals(newToken, identitChanges[0].getString("new_value"))
                        latch.countDown()
                    } catch (e: JSONException) {
                        RuntimeException(e)
                    }
                }
            })
        MParticle.getInstance()!!.Messaging().enablePushNotifications("12345")
        latch.await()
        TestingUtils.setFirebasePresent(false, null)
    }

    @Test
    fun testPushRegistrationSet() {
        assertEquals(
            mStartingMpid.longValue(), MParticle.getInstance()!!
                .Identity().currentUser!!.id
        )
        for (setPush in setPushes) {
            val pushRegistration = PushRegistration(
                RandomUtils.getAlphaNumericString(10),
                RandomUtils.getAlphaNumericString(15)
            )
            setPush.setPushRegistration(pushRegistration)
            for (getPush in getPushes) {
                val fetchedPushValue: PushRegistration = getPush.pushRegistration
                val fetchedSenderId: String = fetchedPushValue.senderId
                val fetchedInstanceId: String = fetchedPushValue.instanceId
                if (pushRegistration.senderId != fetchedSenderId) {
                    Assert.fail("Mismatch! When push value of \"" + pushRegistration.senderId + "\" is set with: " + setPush.name + ". A different value \"" + fetchedSenderId + "\" is returned with:" + getPush.name)
                }
                if (pushRegistration.instanceId != fetchedInstanceId) {
                    Assert.fail("Mismatch! When push value of \"" + pushRegistration.instanceId + "\" is set with: " + setPush.name + ". A different value \"" + fetchedInstanceId + "\" is returned with:" + getPush.name)
                }
            }
        }
    }

    @Test
    fun testPushRegistrationCleared() {
        for (setPush in setPushes) {
            val pushRegistration = PushRegistration(
                RandomUtils.getAlphaNumericString(10),
                RandomUtils.getAlphaNumericString(15)
            )
            setPush.setPushRegistration(pushRegistration)
            for (clearPush in clearPushes) {
                clearPush.clearPush()
                for (getPush in getPushes) {
                    val fetchedPushRegistration: PushRegistration = getPush.pushRegistration
                    if (fetchedPushRegistration != null && fetchedPushRegistration.instanceId != null && fetchedPushRegistration.senderId != null) {
                        Assert.fail("Mismatch! When push value of \"" + pushRegistration + "\" is set with: " + setPush.name + ", and cleared with: " + clearPush.name + ", the value is not null when fetched with:" + getPush.name)
                    }
                }
            }
        }
    }

    @Test
    fun testPushRegistrationEnabledDisabled() {
        for (setPush in setPushes) {
            val pushRegistration = PushRegistration(
                RandomUtils.getAlphaNumericString(10),
                RandomUtils.getAlphaNumericString(15)
            )
            setPush.setPushRegistration(pushRegistration)
            for (pushEnabled in pushEnableds) {
                if (!pushEnabled.isPushEnabled) {
                    Assert.fail("Mismatch! When push value of \"" + pushRegistration + "\" is set with: " + setPush.name + ", push IS NOT enabled with:" + pushEnabled.name)
                }
            }
            for (clearPush in clearPushes) {
                clearPush.clearPush()
                for (pushEnabled in pushEnableds) {
                    if (pushEnabled.isPushEnabled) {
                        Assert.fail("Mismatch! When push value of \"" + pushRegistration + "\" is set with: " + setPush.name + ", and cleared with: " + clearPush.name + ", push IS enabled with:" + pushEnabled.name)
                    }
                }
            }
        }
    }

    var setPushes = arrayOf(
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.getInstance()!!
                    .logPushRegistration(pushRegistration.instanceId, pushRegistration.senderId)
            }

            override val name: String
                get() = "MParticle.getInstance().logPushRegistration(senderId, instanceId)"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.getInstance().Internal().getConfigManager()
                    .setPushRegistration(pushRegistration)
            }

            override val name: String
                get() = "ConfigManager.setPushRegistration(pushRegistration())"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.getInstance().Internal().getConfigManager()
                    .setPushSenderId(pushRegistration.senderId)
                MParticle.getInstance().Internal().getConfigManager()
                    .setPushInstanceId(pushRegistration.instanceId)
            }

            override val name: String
                get() = "ConfigManager.setPushSenderId(senderId) + ConfigManager.setPushRegistration(instanceId)"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                //For enablePushNotifications() to set the push registration, we need to mimic
                //the Firebase dependency, and clear the push-fetched flags
                TestingUtils.setFirebasePresent(true, pushRegistration.instanceId)
                MParticle.getInstance()!!.Messaging()
                    .enablePushNotifications(pushRegistration.senderId)
                //this method setting push is async, so wait for confirmation before continuing
                val configManager: ConfigManager = ConfigManager.getInstance(context)
                while (!configManager.isPushEnabled()) {
                    try {
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                TestingUtils.setFirebasePresent(false, null)
            }

            override val name: String
                get() = "MessagingApi.enablePushNotification(senderId)"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.setInstance(null)
                try {
                    startMParticle(
                        MParticleOptions.builder(context).pushRegistration(
                            pushRegistration.instanceId,
                            pushRegistration.senderId
                        )
                    )
                } catch (e: InterruptedException) {
                    Assert.fail(e.message)
                }
            }

            override val name: String
                get() = "MParticleOptions.pushRegistration(instanceId, senderId)"
        }
    )
    var clearPushes = arrayOf(
        object : ClearPush {
            override fun clearPush() {
                MParticle.getInstance()!!.Messaging().disablePushNotifications()
            }

            override val name: String
                get() = "MessagingApi.disablePushNotifications"
        },
        object : ClearPush {
            override fun clearPush() {
                MParticle.getInstance().Internal().getConfigManager().setPushSenderId(null)
            }

            override val name: String
                get() = "ConfigManager.setPushSenderId(null)"
        },
        object : ClearPush {
            override fun clearPush() {
                MParticle.getInstance().Internal().getConfigManager().setPushRegistration(null)
            }

            override val name: String
                get() = "ConfigManager.setPushRegistration(null)"
        },
        object : ClearPush {
            override fun clearPush() {
                MParticle.getInstance().Internal().getConfigManager()
                    .setPushRegistration(PushRegistration("instanceId", null))
            }

            override val name: String
                get() = "ConfigManager.setPushRegistration(PushRegistration(\"instanceId\", null))"
        },
        object : ClearPush {
            override fun clearPush() {
                MParticle.setInstance(null)
                try {
                    startMParticle(MParticleOptions.builder(context).pushRegistration(null, null))
                } catch (e: InterruptedException) {
                    Assert.fail(e.message)
                }
            }

            override val name: String
                get() = "startMParticle(MParticleOptions.builder(context).pushRegistration(null, null))"
        }
    )
    var getPushes = arrayOf(
        object : GetPush {
            override val pushRegistration: PushRegistration
                get() {
                    val senderId: String =
                        MParticle.getInstance().Internal().getConfigManager().getPushSenderId()
                    val instanceId: String =
                        MParticle.getInstance().Internal().getConfigManager().getPushInstanceId()
                    return PushRegistration(instanceId, senderId)
                }
            override val name: String
                get() = "ConfigManager.getPushSenderId() + ConfigManager.getPushInstanceId()"
        },
        object : GetPush {
            override val pushRegistration: PushRegistration
                get() = PushRegistrationHelper.getLatestPushRegistration(context)
            override val name: String
                get() = "PushRegistrationHelper.getLatestPushRegistration(context)"
        },
        object : GetPush {
            override val pushRegistration: PushRegistration
                get() = MParticle.getInstance().Internal().getConfigManager().getPushRegistration()
            override val name: String
                get() = "ConfigManager.getPushRegistration()"
        }
    )
    var pushEnableds = arrayOf<PushEnabled>(
        object : PushEnabled {
            override val isPushEnabled: Boolean
                get() = MParticle.getInstance().Internal().getConfigManager().isPushEnabled()
            override val name: String
                get() = "ConfigManager.isPushEnabled()"
        }
    )

    interface SynonymousMethod {
        val name: String
    }

    interface SetPush : SynonymousMethod {
        fun setPushRegistration(pushRegistration: PushRegistration)
    }

    interface ClearPush : SynonymousMethod {
        fun clearPush()
    }

    interface GetPush : SynonymousMethod {
        val pushRegistration: PushRegistration
    }

    interface PushEnabled : SynonymousMethod {
        val isPushEnabled: Boolean
    }

    fun setServer(server: MockServer): PushRegistrationTest {
        mServer = server
        return this
    }
}