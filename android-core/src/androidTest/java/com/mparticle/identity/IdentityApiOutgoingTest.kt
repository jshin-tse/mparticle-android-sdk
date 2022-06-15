package com.mparticle.identity

import com.mparticle.MParticle
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import org.junit.Test
import kotlin.random.Random

class IdentityApiOutgoingTest : BaseStartedTest() {
    @Test
    @Throws(Exception::class)
    fun testLogin() {
        Server.endpoint(EndpointType.Identity_Login)
            .assertWillReceive { it.body.previousMpid == mStartingMpid }
            .after {
                MParticle.getInstance()?.Identity()?.login()
            }
            .blockUntilFinished()
    }

    @Test
    @Throws(Exception::class)
    fun testLoginNonEmpty() {
        Server
            .endpoint(EndpointType.Identity_Login)
            .assertWillReceive { it.body.previousMpid == mStartingMpid }
            .after { MParticle.getInstance()?.Identity()?.login() }
            .blockUntilFinished()
    }

    @Test
    @Throws(Exception::class)
    fun testLogout() {
        Server
            .endpoint(EndpointType.Identity_Logout)
            .assertWillReceive { it.body.previousMpid == mStartingMpid }
            .after { MParticle.getInstance()?.Identity()?.logout() }
            .blockUntilFinished()
    }

    @Test
    @Throws(Exception::class)
    fun testLogoutNonEmpty() {
        Server
            .endpoint(EndpointType.Identity_Logout)
            .assertWillReceive { it.body.previousMpid == mStartingMpid }
            .after { MParticle.getInstance()?.Identity()?.logout(IdentityApiRequest.withEmptyUser().build()) }
            .blockUntilFinished()
    }

    @Test
    @Throws(Exception::class)
    fun testModify() {
        Server
            .endpoint(EndpointType.Identity_Modify)
            .assertWillReceive { it.body.knownIdentities?.get("customer_id") != null }
            .after {
                MParticle.getInstance()?.Identity()?.modify(
                    IdentityApiRequest.withEmptyUser()
                        .customerId(Random.Default.nextLong().toString())
                        .build()
                )
            }
            .blockUntilFinished()
    }

    @Test
    @Throws(Exception::class)
    fun testIdentify() {
        Server
            .endpoint(EndpointType.Identity_Identify)
            .assertWillReceive { it.body.previousMpid == mStartingMpid }
            .after { MParticle.getInstance()?.Identity()?.identify(IdentityApiRequest.withEmptyUser().build()) }
            .blockUntilFinished()
    }
}