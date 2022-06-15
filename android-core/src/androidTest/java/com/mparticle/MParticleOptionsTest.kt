package com.mparticle

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.internal.AccessUtils
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.ArrayList
import java.util.UUID
import java.util.concurrent.CountDownLatch

class MParticleOptionsTest : BaseAbstractTest() {
    var context: Context? = null
    var mProductionContext: Context? = null
    @Before
    fun before() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        context = InstrumentationRegistry.getInstrumentation().context
        mProductionContext = AndroidUtils().getProductionContext(context)
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
    }

    @Test
    @Throws(Exception::class)
    fun testCrashOnNoCredentials() {
        var thrown = false
        clearStoredPreferences()
        try {
            MParticleOptions.builder(context!!).build()
        } catch (ex: IllegalArgumentException) {
            thrown = true
        }
        Assert.assertTrue(thrown)
        clearStoredPreferences()
        thrown = false
        try {
            MParticleOptions.builder(context!!)
                .credentials(null, null)
                .build()
        } catch (ex: IllegalArgumentException) {
            thrown = true
        }
        Assert.assertTrue(thrown)
        clearStoredPreferences()
        thrown = false
        try {
            MParticleOptions.builder(context!!)
                .credentials("key", null)
                .build()
        } catch (ex: IllegalArgumentException) {
            thrown = true
        }
        Assert.assertTrue(thrown)
        clearStoredPreferences()
        thrown = false
        try {
            MParticleOptions.builder(context!!)
                .credentials(null, "key")
                .build()
        } catch (ex: IllegalArgumentException) {
            thrown = true
        }
        Assert.assertTrue(thrown)
        setStoredPreference("key", "secret")
        try {
            MParticleOptions.builder(context!!).buildForInternalRestart()
        } catch (ex: IllegalArgumentException) {
            Assert.fail("MParticleOptions should build without credentials if the internal build function is used")
        }
        try {
            MParticleOptions.builder(mProductionContext!!).build()
        } catch (ex: IllegalArgumentException) {
            Assert.fail("MParticleOptions should build without credentials in a Production environment")
        }
        try {
            MParticleOptions.builder(mProductionContext!!)
                .credentials(null, null)
                .build()
        } catch (ex: IllegalArgumentException) {
            Assert.fail("MParticleOptions should build without credentials in a Production environment")
        }
    }

    private fun clearStoredPreferences() {
        credentialsPreferences
            .edit()
            .remove(Constants.PrefKeys.API_KEY)
            .remove(Constants.PrefKeys.API_SECRET)
            .commit()
    }

    private fun setStoredPreference(apiKey: String, apiSecret: String) {
        credentialsPreferences
            .edit()
            .putString(Constants.PrefKeys.API_KEY, apiKey)
            .putString(Constants.PrefKeys.API_SECRET, apiSecret)
            .commit()
    }

    private val credentialsPreferences: SharedPreferences
        private get() = context!!.getSharedPreferences("mp_preferences", Context.MODE_PRIVATE)

    @Test
    @Throws(Exception::class)
    fun testSetCredentials() {
        val key = UUID.randomUUID().toString()
        val secret = UUID.randomUUID().toString()
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .credentials(key, secret)
        )
        Assert.assertEquals(MParticle.getInstance().Internal().getConfigManager().getApiKey(), key)
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getApiSecret(),
            secret
        )
    }

    @Test
    @Throws(Exception::class)
    fun testAndroidIdDisabled() {
        //test defaults
        Assert.assertFalse(MParticle.isAndroidIdEnabled())
        Assert.assertTrue(MParticle.isAndroidIdDisabled())
        MParticle.setInstance(null)
        startMParticle(MParticleOptions.builder(context!!))
        Assert.assertFalse(MParticle.isAndroidIdEnabled())
        Assert.assertTrue(MParticle.isAndroidIdDisabled())

        //test androidIdDisabled == true
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(context!!)
                .androidIdDisabled(true)
        )
        Assert.assertFalse(MParticle.isAndroidIdEnabled())
        Assert.assertTrue(MParticle.isAndroidIdDisabled())
        MParticle.setInstance(null)

        //test androidIdEnabled == false
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(context!!)
                .androidIdEnabled(false)
        )
        Assert.assertFalse(MParticle.isAndroidIdEnabled())
        Assert.assertTrue(MParticle.isAndroidIdDisabled())
        MParticle.setInstance(null)

        //test androidIdDisabled == false
        startMParticle(
            MParticleOptions.builder(context!!)
                .androidIdDisabled(false)
        )
        Assert.assertTrue(MParticle.isAndroidIdEnabled())
        Assert.assertFalse(MParticle.isAndroidIdDisabled())

        //test androidIdEnabled == true
        startMParticle(
            MParticleOptions.builder(context!!)
                .androidIdEnabled(true)
        )
        Assert.assertTrue(MParticle.isAndroidIdEnabled())
        Assert.assertFalse(MParticle.isAndroidIdDisabled())
    }

    @Test
    @Throws(Exception::class)
    fun testDevicePerformanceMetricsDisabled() {
        startMParticle()
        Assert.assertFalse(MParticle.getInstance()!!.isDevicePerformanceMetricsDisabled)
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(context!!)
                .devicePerformanceMetricsDisabled(false)
        )
        Assert.assertFalse(MParticle.getInstance()!!.isDevicePerformanceMetricsDisabled)
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(context!!)
                .devicePerformanceMetricsDisabled(true)
        )
        Assert.assertTrue(MParticle.getInstance()!!.isDevicePerformanceMetricsDisabled)
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testLogLevel() {
        startMParticle()
        Assert.assertEquals(Logger.getMinLogLevel(), Logger.DEFAULT_MIN_LOG_LEVEL)
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .logLevel(MParticle.LogLevel.VERBOSE)
        )
        Assert.assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.VERBOSE)
        startMParticle(
            MParticleOptions.builder(mProductionContext!!).logLevel(MParticle.LogLevel.ERROR)
        )
        Assert.assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR)
    }

    @Test
    @Throws(Exception::class)
    fun testEnvironment() {
        startMParticle()
        Assert.assertEquals(
            MParticle.getInstance()!!.environment,
            MParticle.Environment.Development
        )
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .environment(MParticle.Environment.Production)
        )
        Assert.assertEquals(MParticle.getInstance()!!.environment, MParticle.Environment.Production)
        MParticle.setInstance(null)
        val productionContext = mProductionContext
        val debuggable: Boolean = MPUtility.isAppDebuggable(productionContext)
        Assert.assertFalse(debuggable)
        startMParticle(
            MParticleOptions.builder(productionContext!!)
                .environment(MParticle.Environment.AutoDetect)
        )
        Assert.assertEquals(MParticle.getInstance()!!.environment, MParticle.Environment.Production)
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testEnableUncaughtExceptionLogging() {
        val options = MParticleOptions.builder(mProductionContext!!)
            .credentials("key", "secret")
            .build()
        MParticle.start(options)
        Assert.assertFalse(
            MParticle.getInstance().Internal().getConfigManager().getLogUnhandledExceptions()
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .enableUncaughtExceptionLogging(true)
        )
        Assert.assertTrue(
            MParticle.getInstance().Internal().getConfigManager().getLogUnhandledExceptions()
        )
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .enableUncaughtExceptionLogging(false)
        )
        Assert.assertFalse(
            MParticle.getInstance().Internal().getConfigManager().getLogUnhandledExceptions()
        )
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testSessionTimeout() {
        startMParticle()
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getSessionTimeout().toLong(),
            60000
        )
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .sessionTimeout(-123)
        )
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getSessionTimeout().toLong(),
            60000
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .sessionTimeout(123)
        )
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getSessionTimeout().toLong(),
            123000
        )

        //make sure it resets if the session timeout is not specified
        startMParticle()
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getSessionTimeout().toLong(),
            60000
        )
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testInstallType() {
        startMParticle()
        assertEquals(
            AccessUtils.getInstallType(MParticle.getInstance()!!.mMessageManager),
            MParticle.InstallType.AutoDetect
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .installType(MParticle.InstallType.KnownInstall)
        )
        assertEquals(
            AccessUtils.getInstallType(MParticle.getInstance()!!.mMessageManager),
            MParticle.InstallType.KnownInstall
        )
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .installType(MParticle.InstallType.KnownUpgrade)
        )
        assertEquals(
            AccessUtils.getInstallType(MParticle.getInstance()!!.mMessageManager),
            MParticle.InstallType.KnownUpgrade
        )
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .installType(MParticle.InstallType.AutoDetect)
        )
        assertEquals(
            AccessUtils.getInstallType(MParticle.getInstance()!!.mMessageManager),
            MParticle.InstallType.AutoDetect
        )
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testUploadInterval() {
        //default upload interval for production
        startMParticle()
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getUploadInterval(), 10000
        )
        MParticle.setInstance(null)


        //default upload interval for production
        startMParticle(MParticleOptions.builder(mProductionContext!!))
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getUploadInterval(), 600000
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .uploadInterval(123)
        )
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getUploadInterval(), 123000
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .uploadInterval(-123)
        )
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getUploadInterval(), 600000
        )
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testAttributionListener() {
        startMParticle()
        Assert.assertNull(MParticle.getInstance()!!.attributionListener)
        startMParticle(
            MParticleOptions.builder(context!!)
                .attributionListener(object : AttributionListener {
                    override fun onResult(result: AttributionResult) {}
                    override fun onError(error: AttributionError) {}
                })
        )
        Assert.assertNotNull(MParticle.getInstance()!!.attributionListener)
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(context!!)
                .attributionListener(null)
        )
        Assert.assertNull(MParticle.getInstance()!!.attributionListener)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setOperatingSystemTest() {
        val called: Mutable<Boolean> = Mutable<Boolean>(false)
        val latch: CountDownLatch = FailureLatch()
        startMParticle(
            MParticleOptions.builder(context!!)
                .operatingSystem(MParticle.OperatingSystem.FIRE_OS)
        )
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().getEventsUrl()),
            object : RequestReceivedCallback() {
                fun onRequestReceived(request: Request) {
                    assertEquals(
                        "FireTV",
                        request.getBodyJson().optJSONObject("di").optString("dp")
                    )
                    called.value = true
                    latch.countDown()
                }
            })
        MParticle.getInstance()!!
            .logEvent(MPEvent.Builder("event name", MParticle.EventType.Location).build())
        MParticle.getInstance()!!.upload()
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setOperatingSystemDefault() {
        val called: Mutable<Boolean> = Mutable<Boolean>(false)
        val latch1: CountDownLatch = FailureLatch()
        startMParticle(MParticleOptions.builder(context!!))
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().getEventsUrl()),
            object : RequestReceivedCallback() {
                fun onRequestReceived(request: Request) {
                    assertEquals(
                        "Android",
                        request.getBodyJson().optJSONObject("di").optString("dp")
                    )
                    called.value = true
                    latch1.countDown()
                }
            })
        MParticle.getInstance()!!
            .logEvent(MPEvent.Builder("event name", MParticle.EventType.Location).build())
        MParticle.getInstance()!!.upload()
        latch1.await()
        Assert.assertTrue(called.value)
    }

    @Rule
    var mRuntimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    @Throws(InterruptedException::class)
    fun testLocationTracking() {
        startMParticle(
            MParticleOptions.builder(context!!)
                .locationTrackingDisabled()
        )
        Assert.assertFalse(MParticle.getInstance()!!.isLocationTrackingEnabled)
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle(
            MParticleOptions.builder(context!!)
                .locationTrackingEnabled("passive", 100, 20)
        )
        Assert.assertTrue(MParticle.getInstance()!!.isLocationTrackingEnabled)
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle()
        Assert.assertFalse(MParticle.getInstance()!!.isLocationTrackingEnabled)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testTimeout() {
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .identityConnectionTimeout(-123)
        )
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getIdentityConnectionTimeout()
                .toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getConnectionTimeout().toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .identityConnectionTimeout(0)
        )
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getIdentityConnectionTimeout()
                .toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getConnectionTimeout().toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext!!)
                .identityConnectionTimeout(123)
        )
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getIdentityConnectionTimeout()
                .toLong(), 123000
        )
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getConnectionTimeout().toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        MParticle.setInstance(null)
        startMParticle(MParticleOptions.builder(mProductionContext!!))
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getIdentityConnectionTimeout()
                .toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        Assert.assertEquals(
            MParticle.getInstance().Internal().getConfigManager().getConnectionTimeout().toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
    }

    @Test
    fun testNetworkOptions() {
        val options = MParticleOptions.builder(mProductionContext!!)
            .credentials("key", "secret")
            .build()
        Assert.assertTrue(
            com.mparticle.networking.AccessUtils.equals(
                options.networkOptions,
                com.mparticle.networking.AccessUtils.getDefaultNetworkOptions()
            )
        )
    }

    @Test
    fun testConfigStaleness() {
        //nothing set, should return null
        var options = MParticleOptions.builder(context!!)
            .credentials("key", "secret")
            .build()
        Assert.assertNull(options.configMaxAge)

        //0 should return 0
        options = MParticleOptions.builder(context!!)
            .credentials("key", "secret")
            .configMaxAgeSeconds(0)
            .build()
        Assert.assertEquals(0, options.configMaxAge.toLong())

        //positive number should return positive number
        val testValue: Int = Math.abs(Random.Default.nextInt())
        options = MParticleOptions.builder(context!!)
            .credentials("key", "secret")
            .configMaxAgeSeconds(testValue)
            .build()
        Assert.assertEquals(testValue.toLong(), options.configMaxAge.toLong())

        //negative number should get thrown out and return null
        options = MParticleOptions.builder(context!!)
            .credentials("key", "secret")
            .configMaxAgeSeconds(-5)
            .build()
        Assert.assertNull(options.configMaxAge)
    }

    @Test
    fun testAndroidIdLogMessage() {
        val infoLogs: MutableList<String?> = ArrayList<Any?>()
        Logger.setLogHandler(object : DefaultLogHandler() {
            override fun log(priority: MParticle.LogLevel, error: Throwable, messages: String) {
                super.log(priority, error, messages)
                if (priority == MParticle.LogLevel.INFO) {
                    infoLogs.add(messages)
                }
            }
        })
        MParticleOptions.builder(context!!)
            .credentials("this", "that")
            .androidIdDisabled(true)
            .build()
        Assert.assertTrue(infoLogs.contains("ANDROID_ID will not be collected based on MParticleOptions settings"))
        infoLogs.clear()
        MParticleOptions.builder(context!!)
            .credentials("this", "that")
            .androidIdDisabled(false)
            .build()
        Assert.assertTrue(infoLogs.contains("ANDROID_ID will be collected based on MParticleOptions settings"))
        infoLogs.clear()

        //test default
        MParticleOptions.builder(context!!)
            .credentials("this", "that")
            .build()
        Assert.assertTrue(infoLogs.contains("ANDROID_ID will not be collected based on default settings"))
        infoLogs.clear()
    }
}