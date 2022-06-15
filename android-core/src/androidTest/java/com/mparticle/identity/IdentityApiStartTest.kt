package com.mparticle.identity

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.api.identity.toIdentityType
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.MPUtility
import com.mparticle.testing.BaseTest
import com.mparticle.testing.Mutable
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.startMParticle
import junit.framework.TestCase
import org.json.JSONException
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

fun randomIdentities(count: Int? = null) = (count?.let { RandomUtils.getRandomUserIdentities(count) } ?: RandomUtils.getRandomUserIdentities())!!.entries?.associate { it.key.toIdentityType() to it.value }

class IdentityApiStartTest : BaseTest() {
    
    @Test
    @Throws(Exception::class)
    fun testInitialIdentitiesPresentWithAndroidId() {
        val identities: Map<MParticle.IdentityType, String> = randomIdentities()
        val request = IdentityApiRequest.withEmptyUser()
            .userIdentities(identities)
            .build()
        startMParticle(
            MParticleOptions.builder(context)
                .androidIdEnabled(true)
                .identify(request)
        )
        Server.endpoint(EndpointType.Identity_Identify).requests.let {
            assertEquals(1, it.size)
            assertIdentitiesMatch(it[0].request.body.knownIdentities, identities, true)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testInitialIdentitiesPresentWithoutAndroidId() {
        val identities: Map<MParticle.IdentityType, String> = randomIdentities()
        val request = IdentityApiRequest.withEmptyUser()
            .userIdentities(identities)
            .build()
        startMParticle(
            MParticleOptions.builder(context)
                .androidIdEnabled(false)
                .identify(request)
        )
        Server.endpoint(EndpointType.Identity_Identify).requests.let {
            assertEquals(1, it.size)
            assertIdentitiesMatch(it[0].request.body.knownIdentities, identities, false)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testNoInitialIdentityNoStoredIdentity() {
        startMParticle()
        Server.endpoint(EndpointType.Identity_Identify).requests.let {
            assertEquals(1, it.size)
            assertIdentitiesMatch(it[0].request.body.knownIdentities, mapOf(), false)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testNoInitialIdentity() {
        val currentMpid: Long = Random.Default.nextLong()
        val identities: Map<MParticle.IdentityType, String> = randomIdentities()
        startMParticle()
        MParticle.getInstance()!!
            .Internal().configManager.setMpid(currentMpid, Random.Default.nextBoolean())
        for ((key, value) in identities) {
            AccessUtils.setUserIdentity(value, key, currentMpid)
        }
        com.mparticle.internal.AccessUtils.awaitMessageHandler()
        startMParticle()
        Server.endpoint(EndpointType.Identity_Identify).requests.let {
            assertEquals(1, it.size)
            assertIdentitiesMatch(it[0].request.body.knownIdentities, identities, false)
        }
    }

    /**
     * This asserts that when the SDK receives a new Push InstanceId in the background, it will send
     * a modify request with the background change when the SDK starts up, unless there is a pushRegistration
     * included in the startup object. Make sure the Push InstanceId logged in the background is deleted
     * after it is used in the modify() request
     */
    @Test
    @Throws(InterruptedException::class)
    fun testLogNotificationBackgroundTest() {
        TestCase.assertNull(ConfigManager.getInstance(context).getPushInstanceId())
        val instanceId: String = RandomUtils.getAlphaNumericString(10)
        com.mparticle.internal.AccessUtils.setPushInPushRegistrationHelper(
            context,
            instanceId,
            RandomUtils.getAlphaNumericString(15)
        )
        /**
         * This tests that a modify request is sent when the previous Push InstanceId is empty, with the value of "null"
         */
        Server
            .endpoint(EndpointType.Identity_Modify)
            .assertWillReceive {
                it.body.identityChanges?.let {
                    assertEquals(1, it.size)
                    it[0].apply {
                        assertEquals(instanceId, newValue)
                        assertNull(oldValue)
                        assertEquals("identity_type", identityType)
                    }
                  true
                } ?: false
            }
            .after { startMParticle() }
            .blockUntilFinished()

        MParticle.setInstance(null)
        val newInstanceId: String = RandomUtils.getAlphaNumericString(15)
        com.mparticle.internal.AccessUtils.setPushInPushRegistrationHelper(
            context,
            newInstanceId,
            RandomUtils.getAlphaNumericString(15)
        )
        /**
         * tests that the modify request was made with the correct value for the instanceId set while
         * the SDK was stopped
         */
        Server
            .endpoint(EndpointType.Identity_Modify)
            .assertWillReceive {
                it.body.identityChanges?.let {
                    assertEquals(1, it.size)
                    it[0].apply {
                        assertEquals(instanceId, oldValue)
                        assertEquals(newInstanceId, newValue)
                        assertEquals("push_token", identityType)
                    }
                    true
                } ?: false
            }
            .after { startMParticle() }
            .blockUntilFinished()
    }

    @Throws(Exception::class)
    private fun assertIdentitiesMatch(
        knownIdentities: Map<String, String?>?,
        identities: Map<MParticle.IdentityType, String>,
        androidIdEnabled: Boolean
    ) {
        val knownIdentitiesCopy = knownIdentities!!.toMutableMap()
        if (androidIdEnabled) {
            assertNotNull(knownIdentitiesCopy.remove("android_uuid"))
        } else {
            TestCase.assertFalse(knownIdentitiesCopy.containsKey("android_uuid"))
        }
        assertNotNull(knownIdentitiesCopy.remove("device_application_stamp"))
        TestCase.assertEquals(knownIdentitiesCopy.size, identities.size)
        knownIdentities.forEach {
            assertEquals(identities[MParticleIdentityClientImpl.getIdentityType(it.key)], knownIdentities[it.key])
        }
    }

    /**
     * In this scenario, a logPushRegistration's modify request is made when the current MPID is 0. Previously
     * the method's modify request would failed when a valid MPID wasn't present, but currently we will
     * defer the request until a valid MPID is present.
     *
     * Additionally, this tests that if the logPushRegistration method is called multiple times (for whatever reason)
     * before a valid MPID is present, we will ignore the previous values, and only send the most recent request.
     * This would be good in a case where the device is offline for a period of time, and logPushNotification
     * request back up.
     * @throws InterruptedException
     */
    @Test
    @Throws(InterruptedException::class, JSONException::class)
    fun testPushRegistrationModifyRequest() {
        val logPushRegistrationCalled: Mutable<Boolean> = Mutable<Boolean>(false)
        var pushRegistration: String? = null
        Server
            .endpoint(EndpointType.Identity_Modify)
            .assertWillReceive { it.url.endsWith(mStartingMpid.toString()) }
            .after {
                startMParticle(
                    MParticleOptions.builder(context)
                        .credentials("key", "value")
                )
                MParticle.getInstance()!!.Identity()
                    .addIdentityStateListener(object : IdentityStateListener {
                        override fun onUserIdentified(user: MParticleUser, previousUser: MParticleUser?) {
                            Assert.assertTrue(logPushRegistrationCalled.value)
                            MParticle.getInstance()!!.Identity().removeIdentityStateListener(this)
                        }
                    })
                for (i in 0..4) {
                    MParticle.getInstance()!!
                        .logPushRegistration(
                            RandomUtils.getAlphaString(12).also { pushRegistration = it },
                            "senderId"
                        )
                }
            }
            .blockUntilFinished()

        val modifyRequests = Server.endpoint(EndpointType.Identity_Modify).requests
        TestCase.assertEquals(1, modifyRequests.size)
        modifyRequests[0].request.body.identityChanges.let {
            assertNotNull(it)
            assertEquals(1, it!!.size)
            assertEquals(pushRegistration, it[0].newValue)
            assertEquals("push_token", it[0].identityType)
        }

        //make sure the mDeferredModifyPushRegistrationListener was successfully removed from the IdentityApi
        assertEquals(0, AccessUtils.getIdentityStateListeners().size)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testOperatingSystemSetProperly() {
        Server
            .endpoint(EndpointType.Identity_Identify)
            .assertWillReceive { it.body.clientSdk?.platform == "fire" }
            .after {
                MParticle.start(
                    MParticleOptions.builder(context)
                        .credentials("key", "secret")
                        .operatingSystem(MParticle.OperatingSystem.FIRE_OS)
                        .build()
                )
            }
            .blockUntilFinished()
        MParticle.setInstance(null)
        Server
            .endpoint(EndpointType.Identity_Identify)
            .assertWillReceive { it.body.clientSdk?.platform == "fire" }
            .after {
                MParticle.start(
                    MParticleOptions.builder(context)
                        .credentials("key", "secret")
                        .operatingSystem(MParticle.OperatingSystem.FIRE_OS)
                        .build())
            }
    }

    /**
     * This builds on the previous test. The common scenario where we send a modify() request
     * when a valid MPID is not present, is when a client sets a pushRegistration in MParticleOptions
     * on the applications initial install
     */
    @Test
    fun testPushRegistrationInMParticleOptions() {
        var ex: Exception? = null
        try {
            startMParticle(
                MParticleOptions
                    .builder(context)
                    .pushRegistration("instanceId", "senderId")
                    .environment(MParticle.Environment.Development)
            )
            Assert.assertTrue(MPUtility.isDevEnv())
        } catch (e: Exception) {
            ex = e
        }
        TestCase.assertNull(ex)
    }
}