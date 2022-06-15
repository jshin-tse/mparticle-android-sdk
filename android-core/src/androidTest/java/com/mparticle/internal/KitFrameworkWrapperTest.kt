package com.mparticle.internal

import android.content.Context
import android.util.MutableBoolean
import com.mparticle.AccessUtils
import com.mparticle.MParticle
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CountDownLatch

class KitFrameworkWrapperTest : BaseStartedTest() {
    private fun setKitManager(kitManager: KitFrameworkWrapper) {
        AccessUtils.setKitManager(kitManager)
        com.mparticle.identity.AccessUtils.setKitManager(kitManager)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIdentify() {
        val mpid: Long = Random.Default.nextLong()
        val latch: CountDownLatch = FailureLatch()
        val called = MutableBoolean(false)
        setKitManager(object : StubKitManager(context) {
            override fun onIdentifyCompleted(user: MParticleUser, request: IdentityApiRequest) {
                if (user.id == mStartingMpid) {
                    return
                }
                Assert.assertEquals(mpid, user.id)
                called.value = true
                latch.countDown()
            }
        })
        mServer.setupHappyIdentify(mpid)
        MParticle.getInstance()!!
            .Identity().identify(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLogin() {
        val mpid: Long = Random.Default.nextLong()
        val latch: CountDownLatch = FailureLatch()
        val called = MutableBoolean(false)
        setKitManager(object : StubKitManager(context) {
            override fun onLoginCompleted(user: MParticleUser, request: IdentityApiRequest) {
                if (user.id == mStartingMpid) {
                    return
                }
                Assert.assertEquals(mpid, user.id)
                called.value = true
                latch.countDown()
            }
        })
        mServer.setupHappyLogin(mpid)
        MParticle.getInstance()!!
            .Identity().login(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLogout() {
        val mpid: Long = Random.Default.nextLong()
        val latch: CountDownLatch = FailureLatch()
        val called = MutableBoolean(false)
        setKitManager(object : StubKitManager(context) {
            override fun onLogoutCompleted(user: MParticleUser, request: IdentityApiRequest) {
                if (user.id == mStartingMpid) {
                    return
                }
                Assert.assertEquals(mpid, user.id)
                called.value = true
                latch.countDown()
            }
        })
        mServer.setupHappyLogout(mpid)
        MParticle.getInstance()!!
            .Identity().logout(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testModify() {
        val latch: CountDownLatch = FailureLatch()
        val called = MutableBoolean(false)
        setKitManager(object : StubKitManager(context) {
            override fun onModifyCompleted(user: MParticleUser, request: IdentityApiRequest) {
                assertEquals(mStartingMpid.longValue(), user.id)
                called.value = true
                latch.countDown()
            }
        })
        MParticle.getInstance()!!.Identity().modify(
            IdentityApiRequest.withUser(
                MParticle.getInstance()!!.Identity().currentUser
            ).build()
        )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testModifyUserChanged() {
        val latch: CountDownLatch = FailureLatch()
        val called = MutableBoolean(false)
        setKitManager(object : StubKitManager(context) {
            override fun onModifyCompleted(user: MParticleUser, request: IdentityApiRequest) {
                assertEquals(mStartingMpid.longValue(), user.id)
                called.value = true
                latch.countDown()
            }
        })
        MParticle.getInstance()!!
            .Identity().modify(IdentityApiRequest.withEmptyUser().build())
        MParticle.getInstance()!!.Internal().configManager.setMpid(0, Random.Default.nextBoolean())
        latch.await()
        val latch2: CountDownLatch = FailureLatch()
        val mpid2: Long = Random.Default.nextLong()
        MParticle.getInstance()!!.Internal().configManager.setMpid(mpid2, Random.Default.nextBoolean())
        Assert.assertTrue(called.value)
        called.value = false
        setKitManager(object : StubKitManager(context) {
            override fun onModifyCompleted(user: MParticleUser, request: IdentityApiRequest) {
                Assert.assertEquals(mpid2, user.id)
                called.value = true
                latch2.countDown()
            }
        })
        MParticle.getInstance()!!.Identity().modify(
            IdentityApiRequest.withUser(
                MParticle.getInstance()!!.Identity().currentUser
            ).build()
        )
        MParticle.getInstance()!!
            .Internal().configManager.setMpid(Random.Default.nextLong(), Random.Default.nextBoolean())
        latch2.await()
        Assert.assertTrue(called.value)
    }

    internal open class StubKitManager(context: Context?) :
        KitFrameworkWrapper(context, null, null, null, true, null) {
        override fun loadKitLibrary() {}

        init {
            setKitManager(null)
        }
    }
}