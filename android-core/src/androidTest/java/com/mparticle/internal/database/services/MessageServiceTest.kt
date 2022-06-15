package com.mparticle.internal.database.services

import androidx.test.rule.GrantPermissionRule
import com.mparticle.internal.ConfigManager
import com.mparticle.networking.DomainMapping
import com.mparticle.AttributionListener
import com.mparticle.AttributionResult
import com.mparticle.AttributionError
import com.mparticle.identity.BaseIdentityTask
import com.mparticle.identity.TaskFailureListener
import com.mparticle.identity.TaskSuccessListener
import com.mparticle.identity.IdentityApiResult
import com.mparticle.testing.BaseStartedTest
import com.mparticle.identity.IdentityStateListener
import com.mparticle.MParticleTask
import android.os.HandlerThread
import android.os.Looper
import com.mparticle.identity.MParticleIdentityClientImpl
import com.mparticle.internal.MPUtility
import IdentityRequest.IdentityRequestBody
import com.mparticle.identity.MParticleUserDelegate
import com.mparticle.consent.GDPRConsent
import com.mparticle.consent.CCPAConsent
import com.mparticle.identity.MParticleIdentityClientImplTest.MockIdentityApiClient
import com.mparticle.networking.MPConnection
import com.mparticle.identity.MParticleUserImpl
import com.mparticle.networking.MParticleBaseClientImpl
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import com.mparticle.internal.database.tables.BaseTableTest
import com.mparticle.internal.database.TestSQLiteOpenHelper
import com.mparticle.internal.database.tables.MParticleDatabaseHelper
import android.database.sqlite.SQLiteDatabase
import com.mparticle.internal.database.tables.UploadTable
import com.mparticle.internal.database.tables.MessageTable
import com.mparticle.internal.database.tables.MessageTableTest
import android.provider.BaseColumns
import com.mparticle.internal.database.tables.SessionTable
import com.mparticle.internal.database.tables.ReportingTable
import com.mparticle.internal.database.tables.BreadcrumbTable
import com.mparticle.internal.database.tables.UserAttributesTable
import com.mparticle.internal.database.services.MParticleDBManager
import android.database.sqlite.SQLiteOpenHelper
import com.mparticle.internal.database.services.BaseMPServiceTest
import com.mparticle.internal.database.MPDatabaseImpl
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.internal.InternalSession
import com.mparticle.internal.database.services.MessageService
import com.mparticle.internal.database.services.MessageService.ReadyMessage
import com.mparticle.internal.database.services.SessionService
import com.mparticle.internal.BatchId
import com.mparticle.internal.MessageBatch
import com.mparticle.internal.database.services.SessionServiceTest.MockMessageBatch
import com.mparticle.internal.JsonReportingMessage
import com.mparticle.internal.database.services.ReportingService
import com.mparticle.internal.database.services.BreadcrumbServiceTest
import com.mparticle.internal.database.services.BreadcrumbService
import com.mparticle.internal.database.services.MParticleDBManager.UserAttributeRemoval
import com.mparticle.internal.database.services.MParticleDBManager.UserAttributeResponse
import com.mparticle.internal.database.services.UserAttributesService
import com.mparticle.internal.database.UpgradeVersionTest
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.services.UploadService
import com.mparticle.internal.database.tables.SessionTableTest
import com.mparticle.internal.database.tables.BreadcrumbTableTest
import com.mparticle.internal.database.tables.ReportingTableTest
import com.mparticle.internal.database.tables.UserAttributeTableTest
import com.mparticle.internal.database.tables.MpIdDependentTable
import com.mparticle.internal.database.UpgradeVersionTest.FcmMessageTableColumns
import com.mparticle.internal.database.UpgradeMessageTableTest
import android.telephony.TelephonyManager
import com.mparticle.internal.UserStorage
import com.mparticle.internal.MessageManager
import android.content.SharedPreferences
import com.mparticle.internal.DeviceAttributes
import com.mparticle.internal.KitFrameworkWrapper
import com.mparticle.internal.KitFrameworkWrapperTest.StubKitManager
import androidx.test.rule.ActivityTestRule
import com.mparticle.WebViewActivity
import com.mparticle.internal.MParticleJSInterfaceITest
import android.webkit.WebView
import com.mparticle.internal.MParticleJSInterface
import android.webkit.JavascriptInterface
import com.mparticle.test.R
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.mparticle.internal.MParticleJSInterfaceITest.OptionsAllowResponse
import android.webkit.WebChromeClient
import android.annotation.TargetApi
import android.location.Location
import com.mparticle.internal.ConfigManagerInstrumentedTest.BothConfigsLoadedListener
import com.mparticle.internal.ConfigManagerInstrumentedTest.AddConfigListener
import com.mparticle.internal.ConfigManager.ConfigLoadedListener
import com.mparticle.internal.ConfigManager.ConfigType
import com.mparticle.internal.AppStateManager
import com.mparticle.internal.AppStateManagerInstrumentedTest.KitManagerTester
import com.mparticle.internal.ReportingManager
import com.mparticle.networking.NetworkOptionsManager
import com.mparticle.networking.PinningTestHelper
import com.mparticle.identity.MParticleIdentityClient
import com.mparticle.internal.MParticleApiClientImpl
import com.mparticle.internal.MParticleApiClientImpl.MPNoConfigException
import com.mparticle.internal.MParticleApiClient
import com.mparticle.networking.MParticleBaseClient
import com.mparticle.networking.BaseNetworkConnection
import com.mparticle.networking.MPUrl
import com.mparticle.networking.PinningTest
import com.mparticle.InstallReferrerHelper
import com.mparticle.MParticle.ResetListener
import com.mparticle.PushRegistrationTest.SetPush
import com.mparticle.internal.PushRegistrationHelper.PushRegistration
import com.mparticle.internal.MParticleApiClientImpl.MPThrottleException
import com.mparticle.internal.MParticleApiClientImpl.MPRampException
import com.mparticle.internal.Logger.DefaultLogHandler
import com.mparticle.PushRegistrationTest.GetPush
import com.mparticle.PushRegistrationTest.ClearPush
import com.mparticle.PushRegistrationTest.PushEnabled
import com.mparticle.internal.PushRegistrationHelper
import com.mparticle.PushRegistrationTest.SynonymousMethod
import com.mparticle.internal.Constants
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.Exception
import java.lang.StringBuilder
import java.util.HashMap
import java.util.UUID

class MessageServiceTest : BaseMPServiceTest() {
    var mpid1: Long? = null
    var mpid2: Long? = null
    var mpid3: Long? = null
    @Before
    @Throws(Exception::class)
    fun before() {
        mpid1 = Random.Default.nextLong()
        mpid2 = Random.Default.nextLong()
        mpid3 = Random.Default.nextLong()
    }

    @Test
    @Throws(Exception::class)
    fun testMessagesForUploadByMpid() {
        for (i in 0..19) {
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                mpMessage,
                mpid1!!,
                null,
                null
            )
        }
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                BaseMPServiceTest.Companion.database,
                true,
                mpid1!!
            ).size.toLong(), 20
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                BaseMPServiceTest.Companion.database,
                true,
                Constants.TEMPORARY_MPID
            ).size.toLong(), 0
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database).size.toLong(),
            20
        )
        for (i in 0..29) {
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                mpMessage,
                Constants.TEMPORARY_MPID,
                null,
                null
            )
        }
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                BaseMPServiceTest.Companion.database,
                true,
                mpid1!!
            ).size.toLong(), 20
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                BaseMPServiceTest.Companion.database,
                true,
                mpid2!!
            ).size.toLong(), 0
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                BaseMPServiceTest.Companion.database,
                true,
                Constants.TEMPORARY_MPID
            ).size.toLong(), 30
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database).size.toLong(),
            20
        )
        for (i in 0..34) {
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                mpMessage,
                mpid2!!,
                null,
                null
            )
        }
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                BaseMPServiceTest.Companion.database,
                true,
                mpid1!!
            ).size.toLong(), 20
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                BaseMPServiceTest.Companion.database,
                true,
                mpid2!!
            ).size.toLong(), 35
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                BaseMPServiceTest.Companion.database,
                true,
                Constants.TEMPORARY_MPID
            ).size.toLong(), 30
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database).size.toLong(),
            55
        )
        Assert.assertEquals(
            MessageService.markMessagesAsUploaded(
                BaseMPServiceTest.Companion.database,
                Int.MAX_VALUE
            ).toLong(), 55
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                BaseMPServiceTest.Companion.database,
                true,
                mpid1!!
            ).size.toLong(), 0
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                BaseMPServiceTest.Companion.database,
                true,
                mpid2!!
            ).size.toLong(), 0
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                BaseMPServiceTest.Companion.database,
                true,
                Constants.TEMPORARY_MPID
            ).size.toLong(), 30
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database).size.toLong(),
            0
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSessionHistoryByMpid() {
        val currentSession = UUID.randomUUID().toString()
        val previousSession = UUID.randomUUID().toString()
        for (i in 0..19) {
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                getMpMessage(currentSession),
                mpid1!!,
                null,
                null
            )
        }
        for (i in 0..29) {
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                getMpMessage(currentSession),
                Constants.TEMPORARY_MPID,
                null,
                null
            )
        }
        for (i in 0..34) {
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                getMpMessage(currentSession),
                mpid2!!,
                null,
                null
            )
        }
        Assert.assertEquals(
            MessageService.markMessagesAsUploaded(
                BaseMPServiceTest.Companion.database,
                Int.MAX_VALUE
            ).toLong(), 55
        )
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                previousSession
            ).size.toLong(), 55
        )
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                previousSession,
                true,
                mpid1!!
            ).size.toLong(), 20
        )
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                previousSession,
                true,
                mpid2!!
            ).size.toLong(), 35
        )
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                previousSession,
                false,
                mpid1!!
            ).size.toLong(), 35
        )
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                previousSession,
                false,
                Constants.TEMPORARY_MPID
            ).size.toLong(), 55
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSessionHistoryAccuracy() {
        val currentSession = UUID.randomUUID().toString()
        val previousSession = UUID.randomUUID().toString()
        var testMessage: BaseMPMessage?
        val mpids = arrayOf(mpid1, mpid2, mpid3)
        var testMpid: Long?
        val testMessages: MutableMap<String, BaseMPMessage?> = HashMap()
        for (i in 0..99) {
            testMpid = mpids[RandomUtils.randomInt(0, 3)]
            testMessage = getMpMessage(currentSession, testMpid!!)
            testMessages[testMessage.toString()] = testMessage
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                testMessage,
                testMpid,
                null,
                null
            )
        }
        Assert.assertEquals(
            MessageService.markMessagesAsUploaded(
                BaseMPServiceTest.Companion.database,
                Int.MAX_VALUE
            ).toLong(), 100
        )
        val readyMessages = MessageService.getSessionHistory(
            BaseMPServiceTest.Companion.database,
            previousSession,
            false,
            Constants.TEMPORARY_MPID
        )
        Assert.assertEquals(readyMessages.size.toLong(), testMessages.size.toLong())
        for (readyMessage in readyMessages) {
            val message = testMessages[readyMessage.message]
            Assert.assertNotNull(message)
            Assert.assertEquals(readyMessage.mpid, message!!.mpId)
            Assert.assertEquals(readyMessage.message, message.toString())
            Assert.assertEquals(readyMessage.sessionId, currentSession)
        }
    }

    @Test
    @Throws(JSONException::class)
    fun testMessageFlow() {
        for (i in 0..9) {
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                mpMessage,
                1,
                "dataplan1",
                1
            )
        }
        val messageList = MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database)
        Assert.assertEquals(messageList.size.toLong(), 10)
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                "123"
            ).size.toLong(), 0
        )
        val max = getMaxId(messageList)
        val numUpldated =
            MessageService.markMessagesAsUploaded(BaseMPServiceTest.Companion.database, max)
        Assert.assertEquals(numUpldated.toLong(), 10)
        Assert.assertEquals(
            MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database).size.toLong(),
            0
        )
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                ""
            ).size.toLong(), 10
        )
    }

    @Test
    @Throws(JSONException::class)
    fun testMessageFlowMax() {
        for (i in 0..109) {
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                mpMessage,
                1,
                null,
                null
            )
        }
        var messages = MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database)
        Assert.assertEquals(messages.size.toLong(), 100)
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                ""
            ).size.toLong(), 0
        )
        var max = getMaxId(messages)
        var numUpdated =
            MessageService.markMessagesAsUploaded(BaseMPServiceTest.Companion.database, max)
        Assert.assertEquals(numUpdated.toLong(), 100)
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                ""
            ).size.toLong(), 100
        )
        messages = MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database)
        max = getMaxId(messages)
        numUpdated =
            MessageService.markMessagesAsUploaded(BaseMPServiceTest.Companion.database, max)
        Assert.assertEquals(numUpdated.toLong(), 110)
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                ""
            ).size.toLong(), 100
        )
    }

    @Test
    @Throws(JSONException::class)
    fun testDeleteOldMessages() {
        val currentSession = UUID.randomUUID().toString()
        val newSession = UUID.randomUUID().toString()
        for (i in 0..9) {
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                getMpMessage(currentSession),
                1,
                null,
                null
            )
        }
        Assert.assertEquals(
            MessageService.markMessagesAsUploaded(
                BaseMPServiceTest.Companion.database,
                10
            ).toLong(), 10
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database).size.toLong(),
            0
        )
        MessageService.deleteOldMessages(BaseMPServiceTest.Companion.database, currentSession)
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                newSession
            ).size.toLong(), 10
        )
        MessageService.deleteOldMessages(BaseMPServiceTest.Companion.database, newSession)
        Assert.assertEquals(
            MessageService.getSessionHistory(
                BaseMPServiceTest.Companion.database,
                newSession
            ).size.toLong(), 0
        )
    }

    @Test
    @Throws(JSONException::class)
    fun testMessagesMaxSize() {
        for (i in 0..9) {
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                mpMessage,
                1,
                "a",
                1
            )
        }
        Assert.assertEquals(
            MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database).size.toLong(),
            10
        )
        val builder = StringBuilder()
        for (i in 0 until Constants.LIMIT_MAX_MESSAGE_SIZE) {
            builder.append("ab")
        }
        val message = BaseMPMessage.Builder(builder.toString())
            .build(InternalSession(), Location("New York City"), 1)
        MessageService.insertMessage(
            BaseMPServiceTest.Companion.database,
            "apiKey",
            message,
            1,
            "b",
            2
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database).size.toLong(),
            10
        )
        for (i in 0..9) {
            MessageService.insertMessage(
                BaseMPServiceTest.Companion.database,
                "apiKey",
                mpMessage,
                1,
                "c",
                3
            )
        }
        Assert.assertEquals(
            MessageService.getMessagesForUpload(BaseMPServiceTest.Companion.database).size.toLong(),
            20
        )
    }

    private fun getMaxId(messages: List<ReadyMessage>): Int {
        var max = 0
        for (message in messages) {
            if (message.messageId > max) {
                max = message.messageId
            }
        }
        return max
    }
}