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
import android.database.Cursor
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
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.lang.Exception
import java.util.ArrayList
import java.util.HashMap
import java.util.UUID

class SessionServiceTest : BaseMPServiceTest() {
    @Test
    @Throws(Exception::class)
    fun testUpdateSessionInstallReferrer() {
        var fooObject = JSONObject()
        val sessionId = UUID.randomUUID().toString()
        fooObject.put("foo", "bar")
        val mpMessage = getMpMessage(sessionId)
        SessionService.insertSession(
            BaseMPServiceTest.Companion.database,
            mpMessage,
            "foo-app-key",
            fooObject.toString(),
            fooObject.toString(),
            1
        )
        fooObject = JSONObject()
        val randomId = UUID.randomUUID().toString()
        fooObject.put("foo", randomId)
        SessionService.updateSessionInstallReferrer(
            BaseMPServiceTest.Companion.database,
            fooObject,
            sessionId
        )
        var cursor: Cursor? = null
        try {
            cursor = SessionService.getSessions(BaseMPServiceTest.Companion.database)
            while (cursor.moveToNext()) {
                val currentSessionId =
                    cursor.getString(cursor.getColumnIndexOrThrow(SessionTable.SessionTableColumns.SESSION_ID))
                val appInfo =
                    cursor.getString(cursor.getColumnIndexOrThrow(SessionTable.SessionTableColumns.APP_INFO))
                if (sessionId == currentSessionId) {
                    val appInfoObject = JSONObject(appInfo)
                    Assert.assertEquals(randomId, appInfoObject.getString("foo"))
                    return
                }
            }
        } finally {
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
        junit.framework.Assert.fail("Failed to find updated app customAttributes object.")
    }

    @Test
    fun flattenMessagesByBatchIdTest() {
        val batchMap: MutableMap<BatchId, MessageBatch> = HashMap()
        batchMap[BatchId(Random.Default.nextLong(), "1", "a", 1)] = MockMessageBatch(1)
        batchMap[BatchId(Random.Default.nextLong(), "2", null, null)] = MockMessageBatch(2)
        batchMap[BatchId(Random.Default.nextLong(), "1", "a", 2)] = MockMessageBatch(3)
        batchMap[BatchId(Random.Default.nextLong(), "1", "ab", null)] = MockMessageBatch(4)
        batchMap[BatchId(Random.Default.nextLong(), "2", null, 3)] = MockMessageBatch(5)
        batchMap[BatchId(Random.Default.nextLong(), "3", null, 3)] = MockMessageBatch(6)
        batchMap[BatchId(Random.Default.nextLong(), "1", null, 3)] = MockMessageBatch(7)
        val batchBySessionId = SessionService.flattenBySessionId(batchMap)
        Assert.assertEquals(4, batchBySessionId["1"]!!.size.toLong())
        Assert.assertEquals(2, batchBySessionId["2"]!!.size.toLong())
        Assert.assertEquals(1, batchBySessionId["3"]!!.size.toLong())

        //make sure the elements in the list are unique..no inadvertent copies
        val session1Batches: List<MessageBatch?> = ArrayList<Any?>(
            batchBySessionId["1"]
        )
        val size = session1Batches.size
        for (messageBatch in session1Batches) {
            batchBySessionId["1"]!!.remove(messageBatch)
            Assert.assertEquals(--size.toLong(), batchBySessionId["1"]!!.size.toLong())
        }
    }

    internal inner class MockMessageBatch(var id: Int) : MessageBatch() {
        override fun equals(obj: Any?): Boolean {
            return if (obj is MockMessageBatch) {
                id == obj.id
            } else {
                super.equals(obj)
            }
        }
    }
}